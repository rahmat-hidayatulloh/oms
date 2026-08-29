# Task 11: OrderService dengan Validasi Stock

> **Status:** Current
> **Audience:** Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-29 (screening kode aktual `src/`, `pom.xml` — V5-P0 by-feature layout)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## Tujuan
Mengimplementasikan `OrderService` untuk membuat order dengan validasi ketersediaan stock
product, pengurangan stock, dan generate order number terstandar.

## Acceptance Criteria
- [x] `createOrder(OrderRequest, userId)` method
- [x] Validasi setiap item: stock cukup
- [x] Kurangi stock product saat order dibuat
- [x] Generate order number (`OMS-YYYYMMDD-XXXXXXXX`)
- [x] `@Transactional` untuk atomic operation
- [x] Throw exception jika stock tidak cukup

## Detail Pekerjaan

### 1. File yang Dibuat

> **Catatan (V5-P0):** Struktur package telah di-restructure dari by-layer ke by-feature. Semua service, DTO, dan entity Order kini berada di `com.oms.order`.

```
src/main/java/com/oms/order/
├── dto/
│   ├── OrderRequest.java        ← record: list of OrderItemRequest
│   ├── OrderItemRequest.java    ← record: productId, quantity
│   ├── OrderResponse.java       ← record: order + items
│   └── OrderItemResponse.java   ← record: line item
└── service/
    ├── OrderServiceInterface.java ← interface
    ├── OrderService.java        ← logika pembuatan order (facade + RetryTemplate + TransactionTemplate)
    └── OrderItemFactory.java    ← factory: validasi stock, kurangi stock, build OrderItem

src/test/java/com/oms/order/service/
└── OrderServiceTest.java        ← unit test (Mockito)
```

### 2. OrderService — createOrder

```java
// Setiap attempt berjalan di dalam TransactionTemplate (inside RetryTemplate)
@Override
public OrderResponse createOrder(OrderRequest request, UUID userId) {
    return orderProcessingTimer.record(() -> retryTemplate.execute(context -> {
        try {
            OrderResponse response = transactionTemplate.execute(
                status -> doCreateOrder(request, userId));
            orderCreatedCounter.increment();
            return response;
        } catch (Exception e) {
            orderFailedCounter.increment();
            throw e;
        }
    }));
}
```

Alur `doCreateOrder`:
1. `OrderItemFactory.buildAndValidateItems(request.items())` — validasi produk, quantity > 0, stock cukup, kurangi stock, simpan produk via `productRepository.saveAll()`.
2. Hitung `totalAmount = Σ(price × quantity)` dengan `BigDecimal`.
3. Generate order number, build `Order` dengan `orderItems` menggunakan bidirectional `addOrderItem()`.
4. `orderRepository.save(order)` — OrderItem ikut tersimpan via `cascade = CascadeType.ALL`.
5. Publish `OrderCreatedEvent` dan kembalikan `OrderResponse`.

> **Penting:** `OrderItemFactory` bertanggung jawab atas validasi & pengurangan stock, termasuk `productRepository.saveAll(products)`. `OrderService.createOrder` tidak perlu memanggil `OrderItemRepository` secara eksplisit karena cascade ALL sudah menanganinya.

### 3. Generate Order Number

Format: `OMS-YYYYMMDD-XXXXXXXX`
- `YYYYMMDD` dari `LocalDate.now()` (zona server).
- `XXXXXXXX` = 8 karakter hex acak dari substring `UUID.randomUUID().toString()`.

Contoh: `OMS-20260729-A3F2B9C1`

### 4. Atomicity

Setiap retry attempt dibungkus dalam `TransactionTemplate` yang berada **di dalam** `RetryTemplate`. Pattern ini memastikan:
- Konflik optimistic locking (`@Version` pada `Product`) terdeteksi saat commit transaksi inner.
- Exception propagasi ke `RetryTemplate` dan di-retry terhadap state produk yang sudah di-commit.
- Jika salah satu item gagal validasi stock, seluruh transaksi inner di-rollback.

### 5. Unit Test (OrderServiceTest)

Menggunakan Mockito. Coverage:
- ✅ create berhasil: order terbuat, total benar, stock terkurangi
- ✅ order number sesuai format `OMS-\d{8}-[A-Z0-9]{8}`
- ✅ throw `ResourceNotFoundException` jika product tidak ada
- ✅ throw `BadRequestException` (Insufficient stock) jika stock kurang
- ✅ throw `BadRequestException` jika quantity ≤ 0
- ✅ OrderItem tersimpan dengan referensi order & snapshot nama

### 6. Hasil Verifikasi

Verifikasi penuh dijalankan melalui CI: `./mvnw clean verify -Pci`
(compile, Checkstyle, SpotBugs, seluruh test suite, dan gate JaCoCo).
Angka test per kelas adalah snapshot dan tidak di-hardcode di sini —
rujuk `docs/history/` untuk snapshot per versi.

### 7. Catatan
- `OrderItemFactory` melakukan validasi stock dan pengurangan dalam batch (semua produk di-fetch dengan `findAllById`, di-reduce, dan di-save dengan `saveAll`) untuk efisiensi — satu query fetch dan satu query batch save per order.
- Cascade `CascadeType.ALL` pada `@OneToMany(orderItems)` menjamin OrderItem ikut di-persist bersama Order tanpa perlu `OrderItemRepository.saveAll()` di service layer.
- Snapshot `productName` & `price` di `OrderItem` menjaga histori order tetap akurat meski product diubah di kemudian hari.
- `RetryTemplate` (programmatik, dari `RetryConfig`) di-inject ke `OrderService` untuk retry pada `OptimisticLockException` (backoff: 100ms–5s, max 3 attempts).
- `Micrometer` Counter + Timer (`orderCreatedCounter`, `orderFailedCounter`, `orderProcessingTimer`) dilaporkan ke Prometheus via Actuator.
- `BigDecimal` dipakai untuk perhitungan total agar presisi uang terjaga.