# OMS-018: Fix N+1 Problem di Order dengan OrderItem

> **Status:** Current
> **Audience:** Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-28 (screening kode aktual `src/`, `pom.xml`)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## 📋 Deskripsi

Mengatasi **N+1 query problem** ketika mengambil Order beserta OrderItem-nya. Relasi `@OneToMany` antara `Order` dan `OrderItem` menggunakan fetch LAZY secara default, sehingga setiap akses ke `order.getOrderItems()` menghasilkan query tambahan.

## 🎯 Acceptance Criteria

| No | Kriteria | Status |
|----|----------|--------|
| 1 | Identifikasi N+1 menggunakan show-sql | ✅ |
| 2 | Implementasi @EntityGraph di OrderRepository | ✅ |
| 3 | Alternatif: JOIN FETCH di JPQL | ✅ |
| 4 | Test performance: sebelum dan sesudah fix | ✅ |
| 5 | Documentasikan solution di code comment | ✅ |

## 🔍 Identifikasi N+1 Problem

### Sebelum Fix (N+1 Query)

```sql
-- 1 query untuk mengambil semua orders
SELECT * FROM orders;

-- N queries untuk mengambil items setiap order
SELECT * FROM order_items WHERE order_id = 'order-1';  -- Query ke-2
SELECT * FROM order_items WHERE order_id = 'order-2';  -- Query ke-3
SELECT * FROM order_items WHERE order_id = 'order-3';  -- Query ke-4
-- ... N queries total
```

**Total: 1 + N queries** (misal 3 order = 4 queries)

### Setelah Fix (1 Query)

```sql
-- 1 query dengan LEFT JOIN
SELECT o.*, oi.*
FROM orders o
LEFT JOIN order_items oi ON oi.order_id = o.id;
```

**Total: 1 query** (untuk berapapun jumlah order)

## 🏗️ Struktur File yang Diubah

```
src/main/java/com/oms/
├── repository/
│   └── OrderRepository.java   ← [DIUBAH] @EntityGraph + JOIN FETCH
└── service/
    └── OrderService.java      ← [DIUBAH] findById()/findByIdForUser() pakai JOIN FETCH

src/test/java/com/oms/
└── service/
    └── OrderServiceNPlusOneTest.java  ← [BARU] Integration test N+1

docs/
└── 21-fix-n-plus-one-order-items.md   ← [BARU] Dokumentasi ini
```

## 🔧 Implementasi

### 1. OrderRepository — Pendekatan 1: @EntityGraph

```java
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Menggunakan @EntityGraph untuk eager fetch orderItems dalam 1 query.
     * Menghasilkan LEFT JOIN antara orders dan order_items.
     */
    @EntityGraph(attributePaths = {"orderItems"})
    @Override
    List<Order> findAll();
}
```

**Cara kerja:** `@EntityGraph(attributePaths = {"orderItems"})` memberitahu Hibernate untuk melakukan JOIN FETCH pada field `orderItems` ketika `findAll()` dipanggil. Ini meng-override implementasi default dari Spring Data JPA.

### 2. OrderRepository — Pendekatan 2: JPQL JOIN FETCH

```java
@Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems WHERE o.id = :id")
Optional<Order> findByIdWithItems(UUID id);
```

**Cara kerja:** JPQL `LEFT JOIN FETCH` secara eksplisit melakukan join dan langsung mengisi koleksi `orderItems` di entity `Order`. Tidak perlu query terpisah.

### 3. OrderService

Method `findById` memakai `findByIdWithItems` (JOIN FETCH) — 1 query, bukan 2:

```java
/**
 * findById menggunakan JOIN FETCH — 1 query, bukan 2.
 */
@Transactional(readOnly = true)
public OrderResponse findById(UUID id) {
    Order order = orderRepository.findByIdWithItems(id)
            .orElseThrow(() -> new ResourceNotFoundException(OrderConstants.ENTITY_ORDER, "id", id));
    return toResponse(order);
}
```

Serta varian dengan pemeriksaan kepemilikan untuk akses user non-admin:

```java
/**
 * Admin: sama seperti findById. CUSTOMER: hanya bisa melihat order miliknya sendiri.
 */
@Transactional(readOnly = true)
public OrderResponse findByIdForUser(UUID id, String username, boolean isAdmin) {
    OrderResponse order = findById(id);
    if (!isAdmin) {
        // dibandingkan dengan userId milik username; jika bukan miliknya → ResourceNotFoundException
    }
    return order;
}
```

> **Catatan pagination:** `findAll(int page, int size)` (listing admin, `OrderController.getOrders`) menggunakan `orderRepository.findAll(pageable)` bawaan Spring Data:
>
> ```java
> @Transactional(readOnly = true)
> public Page<OrderResponse> findAll(int page, int size) {
>     Pageable pageable = PageRequest.of(page, size);
>     return orderRepository.findAll(pageable).map(this::toResponse);
> }
> ```
>
> `JOIN FETCH` / `@EntityGraph` tidak bisa dikombinasikan langsung dengan pagination untuk koleksi *bag* (non-unique) — oleh karena itu listing per halaman tetap membaca `orderItems` secara lazy dengan jumlah query `1 + items-per-halaman`. Optimasi 1-query berlaku penuh pada jalur **detail** (`findById`/`findByIdForUser`) dan pada override `@EntityGraph findAll()` untuk penggunaan non-paginated. Dengan default page size kecil (10-20), impact N+1 di listing terbatas pada 1 halaman saja.

## 📊 Perbandingan Performa

| Skenario | Sebelum Fix | Sesudah Fix | Penghematan |
|----------|-------------|-------------|-------------|
| 1 Order (2 items) | 2 queries | 1 query | 50% |
| 3 Orders (2 items each) | 4 queries | 1 query | 75% |
| 10 Orders (3 items each) | 11 queries | 1 query | 91% |
| 100 Orders (5 items each) | 101 queries | 1 query | 99% |

**Rumus:** `N+1` queries → `1` query (penghematan = N queries)

## 🧪 Test Coverage

### OrderServiceTest (existing — 6 tests, tetap passing)

| Test Case | Status |
|-----------|--------|
| `createOrder_shouldCreateOrderAndReduceStock` | ✅ |
| `createOrder_shouldGenerateOrderNumberFormat` | ✅ |
| `createOrder_shouldThrowWhenProductNotFound` | ✅ |
| `createOrder_shouldThrowWhenStockInsufficient` | ✅ |
| `createOrder_shouldThrowWhenQuantityNotPositive` | ✅ |
| `createOrder_shouldPersistOrderItemWithOrderReference` | ✅ |

### OrderServiceNPlusOneTest (BARU — 4 tests)

| Test Case | Verifikasi |
|-----------|------------|
| `shouldFetchAllOrdersWithItemsInSingleQuery` | findAll → items ter-load semua (total 6 items) |
| `shouldFetchOrderByIdWithItemsInSingleQuery` | findById → items ter-load dengan benar |
| `shouldAccessOrderItemsWithoutLazyInitializationException` | Items bisa diakses tanpa error |
| `shouldReturnCorrectTotalAmountForOrders` | Total amount kalkulasi benar |

## 🚀 Running Tests

```bash
# Run N+1 specific tests
./mvnw test -Dtest=OrderServiceNPlusOneTest

# Run all OrderService tests
./mvnw test -Dtest="OrderServiceNPlusOneTest,OrderServiceTest"

# Run all tests
./mvnw test
```

## 📝 Code Comment Documentation

File `OrderRepository.java` berisi dokumentasi lengkap tentang:

```java
/**
 * <b>N+1 Problem Fix:</b>
 * <p>
 * Before fix (N+1):
 * SELECT * FROM orders;                    -- 1 query
 * SELECT * FROM order_items WHERE order_id=?;  -- N queries
 * <p>
 * After fix (1+1 → 1):
 * SELECT o.*, oi.* FROM orders o LEFT JOIN order_items oi ON oi.order_id = o.id;
 * <p>
 * Two approaches:
 * - findAll() — @EntityGraph (type-safe)
 * - findByIdWithItems() — JPQL JOIN FETCH (explicit control)
 */
```

## ⚠️ Catatan Penting

1. **@EntityGraph vs JOIN FETCH:**
   - `@EntityGraph` — lebih deklaratif, tidak perlu menulis JPQL
   - `JOIN FETCH` — kontrol lebih eksplisit, bisa dikombinasikan dengan kondisi WHERE

2. **LEFT JOIN vs INNER JOIN:**
   - `LEFT JOIN` — memastikan Order tanpa items tetap direturn
   - `INNER JOIN` — hanya return Order yang punya items

3. **Batasan JOIN FETCH:**
   - Tidak bisa di-paginate dengan `Pageable` jika multiple collections / bag collection
   - Untuk listing paginated, gunakan page size kecil atau query terpisah per halaman
   - Jalur yang benar-benar butuh 1-query adalah detail order (`findById`/`findByIdForUser`)

## 🔗 Referensi

- [Hibernate - N+1 Select Problem](https://docs.jboss.org/hibernate/orm/6.4/userguide/html_single/Hibernate_User_Guide.html#batch)
- [Spring Data JPA - EntityGraph](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.entity-graph)
- [Baeldung - N+1 Problem in Hibernate & Spring Data JPA](https://www.baeldung.com/spring-hibernate-n-plus-one-problem)