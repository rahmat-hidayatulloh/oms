# OMS-020: Implementasi Soft Delete untuk Semua Entity

> **Status:** Current
> **Audience:** Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-29 (screening kode aktual `src/`, `pom.xml` — V5-P0 by-feature layout)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## 📋 Deskripsi

Mengimplementasikan **Soft Delete Pattern** untuk semua entity (`User`, `Product`, `Order`, `OrderItem`) menggunakan Hibernate `@SQLDelete` dan `@SQLRestriction`. Data yang dihapus tidak benar-benar dihapus dari database, melainkan ditandai dengan `deleted_at` timestamp.

## 🎯 Acceptance Criteria

| No | Kriteria | Status |
|----|----------|--------|
| 1 | Setiap entity punya field `deletedAt` (LocalDateTime) | ✅ |
| 2 | `@SQLRestriction("deleted_at IS NULL")` di semua entity | ✅ |
| 3 | Repository method: `softDelete(UUID id)` | ✅ |
| 4 | `@SQLDelete` untuk override delete operation | ✅ |

## 🔍 Masalah yang Diselesaikan

### Hard Delete vs Soft Delete

**Hard Delete (tanpa soft delete):**
```sql
DELETE FROM products WHERE id = 'xxx';
-- Data hilang permanen, tidak bisa di-recovery
```

**Soft Delete (dengan @SQLDelete):**
```sql
UPDATE products SET deleted_at = CURRENT_TIMESTAMP WHERE id = 'xxx' AND deleted_at IS NULL;
-- Data tetap ada, bisa di-recovery jika diperlukan
```

### Keuntungan Soft Delete:
1. **Data Recovery** — Data yang dihapus bisa dikembalikan
2. **Audit Trail** — Menyimpan riwayat data lengkap
3. **Referential Integrity** — Tidak ada orphaned records di foreign key
4. **Compliance** — Memenuhi regulatory requirements (GDPR, etc.)

## 🏗️ Struktur File yang Diubah/Dibuat

```
src/main/java/com/oms/
├── entity/
│   └── BaseEntity.java                    ← [SUDAH ADA] deletedAt + softDelete() method
├── auth/entity/User.java                  ← [DIUBAH] @SQLDelete + @SQLRestriction
├── product/
│   ├── entity/Product.java                ← [DIUBAH] @SQLDelete + @SQLRestriction
│   ├── repository/ProductRepository.java  ← [DIUBAH] softDelete() method
│   └── service/ProductService.java        ← [DIUBAH] delete() menggunakan repository.deleteById()
└── order/
    ├── entity/Order.java, OrderItem.java  ← [DIUBAH] @SQLDelete + @SQLRestriction
    ├── repository/OrderRepository.java, OrderItemRepository.java ← [DIUBAH] softDelete() method
    └── service/OrderService.java          ← [DIUBAH] delete()/deleteForUser() menggunakan deleteById()

src/test/java/com/oms/
└── auth/service/
│   └── SoftDeleteIntegrationTest.java  ← [BARU] Integration test soft delete (extends base IT)

docs/
└── 23-soft-delete-all-entities.md     ← [BARU] Dokumentasi ini (kini di cross-cutting/)
```

## 🔧 Implementasi

### 1. BaseEntity — deletedAt Field

```java
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false, length = 100)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now(ZoneId.of(OrderConstants.APP_TIMEZONE));
    }
}
```

### 2. Entity — @SQLDelete dan @SQLRestriction

```java
@Entity
@Table(name = "products")
@SQLDelete(sql = "UPDATE products SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted_at IS NULL")
@SQLRestriction("deleted_at IS NULL")
public class Product extends BaseEntity {
    // ...
}
```

**Cara kerja:**
- `@SQLDelete` — Override Hibernate `DELETE` statement menjadi `UPDATE deleted_at = ...`
- `@SQLRestriction` — Menambahkan `WHERE deleted_at IS NULL` ke semua query SELECT

### 3. Repository — softDelete() Method

```java
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, QuerydslPredicateExecutor<Product> {

    boolean existsByName(String name);

    /**
     * Soft deletes a product by setting deleted_at timestamp.
     *
     * @param id the product UUID
     */
    @Modifying
    @Query("UPDATE Product p SET p.deletedAt = CURRENT_TIMESTAMP WHERE p.id = :id AND p.deletedAt IS NULL")
    void softDelete(UUID id);
}
```

**Catatan:** Method `softDelete()` disediakan untuk explicit soft delete via JPQL. Namun, service layer menggunakan `repository.deleteById()` yang akan memicu `@SQLDelete` override.

### 4. Service — delete() Method

```java
@Service
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * Soft-deletes a product by setting deleted_at timestamp.
     * Uses Hibernate {@code @SQLDelete} which overrides DELETE with UPDATE.
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = CacheConstants.CACHE_PRODUCTS, key = "#id"),
            @CacheEvict(value = CacheConstants.CACHE_PRODUCTS_LIST, key = "'all'")
    })
    @Transactional
    public void delete(UUID id) {
        findProductOrThrow(id);
        productRepository.deleteById(id);
        log.info("Soft-deleted product with id {}", id);
    }
}
```

**Cara kerja:**
1. `productRepository.deleteById(id)` dipanggil
2. Hibernate intercept DELETE operation
3. `@SQLDelete` override menjadi: `UPDATE products SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted_at IS NULL`
4. `@SQLRestriction` memastikan entity dengan `deleted_at IS NOT NULL` tidak muncul di query SELECT

## 🧪 Test Coverage

### SoftDeleteIntegrationTest (2 tests)

| Test Case | Verifikasi |
|-----------|------------|
| `shouldSoftDeleteProduct` | Product di-soft delete, tidak bisa di-findById |
| `shouldSoftDeleteOrder` | Order di-soft delete, tidak bisa di-findById |

### Test Results

```
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 ✅
```

## 📊 Perbandingan: Hard Delete vs Soft Delete

| Aspek | Hard Delete | Soft Delete |
|-------|-------------|-------------|
| SQL Statement | `DELETE FROM ...` | `UPDATE ... SET deleted_at = ...` |
| Data Recovery | ❌ Tidak bisa | ✅ Bisa |
| Audit Trail | ❌ Tidak ada | ✅ Ada timestamp |
| Foreign Key Constraint | ⚠️ Bisa menyebabkan error | ✅ Aman |
| Query Performance | Fast | Sedikit lebih lambat (WHERE clause) |
| Storage | Menghapus data | Menyimpan data |

## 🚀 Running Tests

```bash
# Run soft delete tests only
./mvnw test -Dtest=SoftDeleteIntegrationTest

# Run all service tests
./mvnw test -Dtest="ProductServiceTest,OrderServiceTest,SoftDeleteIntegrationTest"

# Run all tests
./mvnw test
```

## ⚠️ Catatan Penting

### 1. @SQLDelete vs Manual softDelete()

**@SQLDelete (Recommended):**
- Otomatis override semua `delete()` operation
- Tidak perlu ubah service code
- Konsisten di seluruh aplikasi

**Manual softDelete() query:**
- Lebih eksplisit
- Bisa ditambahkan kondisi WHERE custom
- Cocok untuk bulk soft delete

### 2. @SQLRestriction Impact

`@SQLRestriction("deleted_at IS NULL")` menambahkan WHERE clause ke SEMUA query:
- `findAll()` — hanya return non-deleted
- `findById()` — hanya return non-deleted
- `existsByUsername()` / `existsByEmail()` / `existsByName()` — hanya check non-deleted (artinya lookup user login `findByUsername`/`findByEmail` otomatis mengabaikan akun yang sudah di-soft delete)

**Catatan:** Jika butuh include soft-deleted data, gunakan `@Query` dengan explicit JOIN atau remove filter temporarily.

### 2b. Uniqueness & Soft Delete

- Constraint `unique` pada `username`, `email`, `name` (product) **tetap berlaku untuk seluruh baris**, termasuk yang sudah di-soft delete, karena constraint berada di level tabel bukan data aktif.
- Konsekuensinya: mendaftarkan user dengan username/email yang sama dengan data terhapus akan gagal di level database. Ini adalah trade-off yang diterima untuk mempertahankan integritas audit trail.
- `@SQLRestriction` hanya memfilter query entity (SELECT/UPDATE/DELETE Hibernate). Query native SQL / `@Query` JPQL harus menambahkan filter secara eksplisit bila perlu.

### 3. Indexing

Untuk performa optimal, tambahkan index pada `deleted_at` column:

```sql
CREATE INDEX idx_products_deleted_at ON products(deleted_at);
CREATE INDEX idx_orders_deleted_at ON orders(deleted_at);
CREATE INDEX idx_order_items_deleted_at ON order_items(deleted_at);
```

### 4. Flyway Migration

Pastikan migration script menambahkan `deleted_at` column:

```sql
-- V1__create_users_table.sql
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP;

-- V2__create_products_table.sql
ALTER TABLE products ADD COLUMN deleted_at TIMESTAMP;

-- V3__create_orders_table.sql
ALTER TABLE orders ADD COLUMN deleted_at TIMESTAMP;

-- V4__create_order_items_table.sql
ALTER TABLE order_items ADD COLUMN deleted_at TIMESTAMP;
```

> Note: `@SQLDelete` increment `version = version + 1` (product/user/order) sehingga klaim optimistic locking tetap konsisten meski operasinya soft delete. Pastikan kolom `version` sudah ada sebelum menambah `@SQLDelete` yang mereferensikannya.

## 🔗 Referensi

- [Hibernate @SQLDelete Documentation](https://docs.jboss.org/hibernate/orm/6.4/userguide/html_single/Hibernate_User_Guide.html#soft-delete)
- [Spring Data JPA - Soft Delete](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.soft-delete)
- [Baeldung - Soft Delete with Hibernate](https://www.baeldung.com/hibernate-soft-delete)