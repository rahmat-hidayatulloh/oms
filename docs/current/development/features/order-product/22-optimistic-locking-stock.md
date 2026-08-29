# OMS-019: Implementasi Optimistic Locking untuk Stock Product

> **Status:** Current
> **Audience:** Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-29 (screening kode aktual `src/`, `pom.xml` — V5-P0 by-feature layout; path file dikoreksi)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## 📋 Deskripsi

Mengimplementasikan **Optimistic Locking** menggunakan `@Version` annotation untuk mencegah **lost update** pada stock product saat terjadi concurrent access. Ditambah dengan **`RetryTemplate` programmatic** (bukan annotation `@Retryable`) untuk otomatis retry ketika terjadi conflict.

## 🎯 Acceptance Criteria

| No | Kriteria | Status |
|----|----------|--------|
| 1 | Field @Version sudah ada di entity Product | ✅ |
| 2 | Handle OptimisticLockException di service layer | ✅ |
| 3 | Retry mechanism dengan RetryTemplate programmatic | ✅ |
| 4 | Concurrent test dengan CountDownLatch | ✅ |

## 🔍 Masalah yang Diselesaikan

### Lost Update Problem

Tanpa optimistic locking, concurrent update bisa menyebabkan **lost update**:

```
Thread A: Read stock = 100
Thread B: Read stock = 100
Thread A: Update stock = 99 (100 - 1)
Thread B: Update stock = 99 (100 - 1)  ← Lost update! Seharusnya 98
```

### Dengan Optimistic Locking

```
Thread A: Read stock = 100, version = 1
Thread B: Read stock = 100, version = 1
Thread A: Update stock = 99, version = 2 ✓
Thread B: Update stock = 99, version = 1 ✗ (OptimisticLockException)
         → Retry: Read stock = 99, version = 2
         → Update stock = 98, version = 3 ✓
```

## 🏗️ Struktur File yang Diubah/Dibuat

> **Catatan (V5-P0):** Path file mengikuti layout by-feature. Tidak ada lagi `entity/Product.java`, `service/ProductService.java`, `service/OrderService.java` di root — semua sudah berada dalam feature package masing-masing.

```
src/main/java/com/oms/
├── config/
│   └── RetryConfig.java                    ← [BARU] RetryTemplate + TransactionTemplate
├── product/
│   ├── entity/Product.java                 ← [SUDAH ADA] @Version field
│   └── service/ProductService.java         ← [DIUBAH] retryTemplate.execute() pada update()
└── order/
    └── service/OrderService.java           ← [DIUBAH] RetryTemplate + TransactionTemplate pada createOrder()

src/test/java/com/oms/product/service/
└── ProductConcurrentTest.java              ← [BARU] Concurrent test dengan CountDownLatch

pom.xml                                     ← [DIUBAH] Tambah spring-retry + spring-aspects
```

## 🔧 Implementasi

### 1. Dependency (pom.xml)

```xml
<!-- Spring Retry -->
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
</dependency>

<!-- AOP (hanya diperlukan jika memakai annotation @Retryable) -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-aspects</artifactId>
</dependency>
```

### 2. RetryConfig — RetryTemplate + TransactionTemplate

```java
@Configuration
@EnableRetry
@RequiredArgsConstructor
public class RetryConfig {

    private final RetryProperties retryProperties;

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(retryProperties.getBackoffDelayMs());  // 100ms
        backOffPolicy.setMultiplier(retryProperties.getBackoffMultiplier());    // 2.0
        backOffPolicy.setMaxInterval(retryProperties.getMaxBackoffDelayMs());   // 5s
        retryTemplate.setBackOffPolicy(backOffPolicy);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                retryProperties.getMaxAttempts(),  // 3 (1 initial + 2 retries)
                Map.of(
                        OptimisticLockingFailureException.class, true,
                        OptimisticLockException.class, true));
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
```

**Cara kerja:** nilai backoff/attempts diambil dari `RetryProperties` (prefix `app.retry.*` pada `application.yml`) sehingga bisa di-override per environment — ini alasan utama memilih `RetryTemplate` programmatic daripada annotation `@Retryable`. Retry menangkap **dua** jenis exception optimistic-lock:
- `OptimisticLockingFailureException` — Spring ekivalen (biasanya `ObjectOptimisticLockingFailureException`)
- `OptimisticLockException` — JPA/Hibernate exception

### 3. Product Entity — @Version Field

```java
@Entity
@Table(name = "products")
public class Product extends BaseEntity {
    
    @Version
    @Column(name = "version", nullable = false)
    private Integer version;
    
    // ... other fields
}
```

**Cara kerja:** Hibernate secara otomatis menambahkan `version` column ke WHERE clause pada UPDATE:

```sql
UPDATE products 
SET stock = ?, updated_at = ?, version = ?
WHERE id = ? AND version = ?
```

Jika `version` tidak cocok (row count = 0), Hibernate throws `OptimisticLockException`.

### 4. ProductService — retryTemplate.execute()

```java
@Override
@Caching(evict = {
        @CacheEvict(value = CacheConstants.CACHE_PRODUCTS, key = "#id"),
        @CacheEvict(value = CacheConstants.CACHE_PRODUCTS_LIST, key = "'all'")
})
@Transactional
public ProductResponse update(UUID id, ProductRequest request) {
    return retryTemplate.execute(context -> {
        Product product = findProductOrThrow(id);

        if (!product.getName().equals(request.name()) && productRepository.existsByName(request.name())) {
            throw new BadRequestException(String.format(ErrorConstants.MSG_PRODUCT_NAME_EXISTS, request.name()));
        }

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setCategory(request.category());

        Product updated = productRepository.save(product);
        return toResponse(updated);
    });
}
```

**Parameter retry** (dari `application.yml`, prefix `app.retry.*`):
- `max-attempts: 3` — Maksimal 3 kali percobaan (1 initial + 2 retries)
- `backoff-delay-ms: 100` — Delay awal 100ms
- `backoff-multiplier: 2.0` — Delay naik eksponensial: 100ms → 200ms → 400ms ...
- `max-backoff-delay-ms: 5000` — Batas atas delay (5 detik)

### 5. OrderService — retryTemplate.execute() pada Stock Reduction

```java
@Override
public OrderResponse createOrder(OrderRequest request, UUID userId) {
    return orderProcessingTimer.record(() -> retryTemplate.execute(context -> {
        try {
            OrderResponse response = transactionTemplate.execute(status -> doCreateOrder(request, userId));
            orderCreatedCounter.increment();
            return response;
        } catch (Exception e) {
            orderFailedCounter.increment();
            throw e;
        }
    }));
}
```

**Mengapa OrderService juga butuh retry?**
Saat create order, stock product di-reduce. Jika 2 order concurrent untuk product yang sama:
- Order A: reduce stock dari 100 → 99
- Order B: reduce stock dari 100 → 99 (stale data)
- Order A: success (version 1 → 2)
- Order B: OptimisticLockException → retry → read stock = 99 → reduce to 98

**Penting — batas transaksi per attempt:** pada `createOrder`, transaksi dijalankan **di dalam** callback retry melalui `TransactionTemplate` (`transactionTemplate.execute(status -> doCreateOrder(...))`), bukan di scope method. Ini memastikan setiap attempt berjalan dalam transaksi database-nya sendiri (begin-commit/rollback per attempt), sehingga entity hasil *stale read* dari attempt yang gagal tidak diulang pada attempt berikutnya.

### 5a. Saat Retry Tidak Sampai Commit (Catatan Implementasi)

Untuk `ProductService.update()`, method tetap bertanda `@Transactional` sehingga transaksi dibuka di awal method dan retry callback berbagi transaksi yang sama. Konsekuensinya, retry yang berguna penuh terjadi saat optimistic-lock terdeteksi pada operasi tulis (flush) — jika sudah ada tulis dirty di persistence context, attempt berikutnya bisa tetap melihat data lama sampai transaksi di-commit/rollback. Order ke depan: pastikan operasi yang rawan lock (khususnya stock reduction multi-row) menggunakan pola `retryTemplate → transactionTemplate` seperti `OrderService`.

## 🧪 Concurrent Test

### Test Strategy

```java
@Test
void shouldHandleConcurrentStockUpdates() throws Exception {
    int threadCount = 5;
    int updatesPerThread = 3;
    
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);
    
    // Each thread tries to reduce stock by 1
    for (int i = 0; i < threadCount; i++) {
        executor.submit(() -> {
            try {
                for (int j = 0; j < updatesPerThread; j++) {
                    simulateStockUpdate(productId);
                }
            } finally {
                latch.countDown();
            }
        });
    }
    
    latch.await();
    executor.shutdown();
    
    // Verify: final stock = initial - successful updates
    Product finalProduct = productRepository.findById(productId).orElseThrow();
    assertThat(finalProduct.getStock()).isEqualTo(100 - successCount);
}
```

### Test Results

```
Concurrent test completed: 13 successful updates, 2 retries
```

**Interpretasi:**
- Total attempts: 15 (5 threads × 3 updates)
- Successful: 13
- Retries: 2 (optimistic lock conflicts detected and retried)
- Final stock: 100 - 13 = 87 ✅ (consistent, no lost updates)

## 📊 Perbandingan: Dengan vs Tanpa Optimistic Locking

| Skenario | Tanpa @Version | Dengan @Version + RetryTemplate |
|----------|----------------|------------------------------|
| Concurrent update | Lost update (stale data) | Automatic retry, no data loss |
| Stock consistency | ❌ Tidak terjamin | ✅ Terjamin |
| Performance overhead | Minimal | Minimal (retry hanya saat conflict) |
| Complexity | Low | Medium (tapi aman) |

## 🚀 Running Tests

```bash
# Run concurrent test only
./mvnw test -Dtest=ProductConcurrentTest

# Run all service tests
./mvnw test -Dtest="ProductServiceTest,ProductConcurrentTest,OrderServiceTest"

# Run all tests
./mvnw test
```

## ⚠️ Catatan Penting

### 1. Kapan Menggunakan Optimistic Locking?

**Gunakan ketika:**
- Conflict jarang terjadi (read lebih sering dari write)
- Performance penting (tidak butuh database lock)
- Data tidak terlalu critical untuk real-time consistency

**Hindari ketika:**
- Conflict sangat sering terjadi (gunakan pessimistic locking instead)
- Butuh strict serializability
- Long-running transactions

### 2. Trade-offs

| Kelebihan | Kekurangan |
|-----------|------------|
| Tidak ada database lock overhead | Retry hanya untuk conflicting transactions |
| Scalable untuk concurrent reads | Bisa terjadi retry storm jika conflict tinggi |
| Simple implementation | Tidak cocok untuk high-contention scenarios |

### 3. Best Practices

1. **Jangan terlalu banyak retry** — `maxAttempts = 3` sudah cukup
2. **Backoff delay** — Tambahkan delay 50-100ms untuk menghindari retry storm
3. **Monitor retry count** — Log retry untuk observability
4. **Test concurrent scenarios** — Pastikan retry logic bekerja dengan benar

## 🔗 Referensi

- [Spring Retry Documentation](https://docs.spring.io/spring-retry/docs/current/reference/html/)
- [JPA Optimistic Locking](https://docs.jboss.org/hibernate/orm/6.4/userguide/html_single/Hibernate_User_Guide.html#locking-optimistic)
- [Baeldung - Optimistic Locking with JPA](https://www.baeldung.com/jpa-optimistic-locking)
- [Spring @Retryable Annotation](https://docs.spring.io/spring-retry/docs/current/reference/html/#annotations)