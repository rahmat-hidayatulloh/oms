# Task 7: Entity Product untuk Merchandise

> **Status:** Current
> **Audience:** Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-29 (screening kode aktual `src/`, `pom.xml` — V5-P0 by-feature layout)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## Tujuan
Membuat entity Product untuk merchandise yang dijual dengan fitur lengkap.

## Acceptance Criteria
- [x] Fields: id (UUID), name, description, price (BigDecimal), stock (int), category
- [x] @Version untuk optimistic locking
- [x] @SQLRestriction untuk soft delete
- [x] Validation constraints (@NotNull, @Positive, @Size)

## Detail Pekerjaan

### 1. File yang Dibuat

> **Catatan (V5-P0):** Struktur package telah di-restructure dari by-layer ke by-feature. `Category` dan `Product` kini berada di `com.oms.product.entity`; `ProductRepository` berada di `com.oms.product.repository`.

```
src/main/java/com/oms/
├── entity/
│   └── BaseEntity.java             ← Abstract @MappedSuperclass (shared, tetap di com.oms.entity)
└── product/
    ├── entity/
    │   ├── Category.java           ← Enum kategori produk (com.oms.product.entity)
    │   └── Product.java            ← Entity utama (com.oms.product.entity)
    └── repository/
        └── ProductRepository.java  ← JPA Repository (com.oms.product.repository)
```

### 2. Category Enum

```java
public enum Category {
    CLOTHING,              // Pakaian
    ACCESSORIES,           // Aksesoris
    ELECTRONICS,           // Elektronik
    BOOKS,                 // Buku
    FOOD_AND_BEVERAGE,     // Makanan & Minuman
    OTHER                  // Lainnya
}
```

### 3. Product Entity - Fields

| Field | Type | Constraint | Keterangan |
|---|---|---|---|
| `id` | UUID (auto) | PK, not null | Generated via `GenerationType.UUID` |
| `name` | String(100) | @NotNull, @Size(3-100) | Nama produk |
| `description` | String(500) | @Size(max 500) | Deskripsi produk |
| `price` | BigDecimal | @NotNull, @DecimalMin("0.01") | Harga (precision 10, scale 2) |
| `stock` | Integer | @NotNull | Stok tersedia |
| `category` | Enum(Category) | @NotNull | Kategori produk |
| `version` | Integer | @Version | Optimistic locking |
| `createdAt` | LocalDateTime | auto | @CreatedDate (dari BaseEntity) |
| `updatedAt` | LocalDateTime | auto | @LastModifiedDate (dari BaseEntity) |
| `deletedAt` | LocalDateTime | nullable | Soft delete marker (dari BaseEntity) |
| `version` | Integer | @Version | Optimistic locking |

### 4. Validation Constraints

```java
@NotNull(message = "Product name is required")
@Size(min = 3, max = 100, message = "Product name must be between 3 and 100 characters")
private String name;

@Size(max = 500, message = "Description must not exceed 500 characters")
private String description;

@NotNull(message = "Price is required")
@DecimalMin(value = "0.01", message = "Price must be greater than 0")
private BigDecimal price;

@NotNull(message = "Stock is required")
private Integer stock;

@NotNull(message = "Category is required")
private Category category;
```

### 5. Optimistic Locking

```java
@Version
@Column(name = "version", nullable = false)
private Integer version;
```

**Cara kerja:**
- Setiap update akan increment `version` secara otomatis
- Mencegah concurrent update conflicts
- Jika version mismatch, akan throw `ObjectOptimisticLockingFailureException`

### 6. Soft Delete

```java
@Entity
@Table(name = "products")
@SQLDelete(sql = "UPDATE products SET deleted_at = CURRENT_TIMESTAMP, version = version + 1 "
        + "WHERE id = ? AND version = ? AND deleted_at IS NULL")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class Product extends BaseEntity {
    // ... fields: name, description, price, stock, category
}
```

**Cara kerja:**
- Semua query otomatis filter `deleted_at IS NULL`
- Method `softDelete()` dari BaseEntity sebagai alternatif mark-as-deleted
- Data tidak benar-benar dihapus dari database
- `@SQLDelete` ikut meng-increment `version` agar klaim optimistic locking tetap konsisten saat soft delete dilakukan

### 7. ProductRepository

```java
// com.oms.product.repository.ProductRepository
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>,
        QuerydslPredicateExecutor<Product> {

    boolean existsByName(String name);
}
```

Meng-extend `JpaRepository` + `QuerydslPredicateExecutor<Product>` dengan:
- CRUD operations
- Paging & Sorting
- Dynamic filtering via QueryDSL predicates (`ProductPredicates`)
- `existsByName(String name)` — validasi nama unik (respects `@SQLRestriction`)

> **Catatan:** `ProductRepository` meng-extend `QuerydslPredicateExecutor` untuk mendukung filtering dinamis via `ProductPredicates` (fitur `20-querydsl-product-filter.md`).

### 8. Hasil Verifikasi

```
BUILD SUCCESS ✅
├── Compile: OK
├── Checkstyle: 0 violations
├── SpotBugs: 0 bugs
├── Tests: 1/1 passed
└── Entity: Product siap pakai
```

### 9. Catatan

- **BigDecimal** digunakan untuk price agar akurat untuk perhitungan uang (`@Column(precision = 10, scale = 2)`)
- **@Version** mencegah lost update pada concurrent access
- **@SQLRestriction** memastikan data yang di-query sudah di-filter soft delete
- **Validation constraints** akan otomatis di-check oleh Spring Validation
- **Lombok** (`@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`, `@Getter`, `@Setter`) digunakan untuk menghilangkan boilerplate code