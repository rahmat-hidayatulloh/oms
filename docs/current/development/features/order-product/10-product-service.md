# Task 10: ProductRepository dan ProductService (Basic CRUD)

> **Status:** Current
> **Audience:** Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-29 (screening kode aktual `src/`, `pom.xml` — V5-P0 by-feature layout; caching + RetryTemplate)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## Tujuan
Mengimplementasikan repository dan service layer untuk Product dengan operasi CRUD dasar
(create, findById, findAll, update, soft delete) serta validasi nama produk unik, Redis caching,
dan retry pada optimistic lock conflict.

## Acceptance Criteria
- [x] ProductRepository extends JpaRepository + QuerydslPredicateExecutor
- [x] ProductService dengan method: create, findById, findAll, update, delete (soft)
- [x] Validasi: product name tidak boleh duplikat
- [x] Redis caching: `@CachePut` pada create, `@Cacheable` pada findById/findAll, `@CacheEvict` pada update/delete
- [x] RetryTemplate untuk retry pada OptimisticLockException di `update()`
- [x] Unit test untuk service layer

## Detail Pekerjaan

### 1. File yang Dibuat/Diubah

> **Catatan (V5-P0):** Struktur package telah di-restructure dari by-layer ke by-feature. Semua class Product kini berada di `com.oms.product`.

```
src/main/java/com/oms/product/
├── repository/
│   ├── ProductRepository.java      ← extends JpaRepository + QuerydslPredicateExecutor
│   └── ProductPredicates.java      ← builder predicate QueryDSL
├── dto/
│   ├── ProductRequest.java         ← record request (validated)
│   ├── ProductResponse.java        ← record response
│   └── ProductFilterRequest.java   ← record filter (search)
└── service/
    ├── ProductServiceInterface.java ← interface
    └── ProductService.java         ← logika CRUD + validasi + caching + retry

src/test/java/com/oms/product/service/
└── ProductServiceTest.java         ← unit test (Mockito)
```

### 2. ProductRepository

```java
public interface ProductRepository extends JpaRepository<Product, UUID>,
        QuerydslPredicateExecutor<Product> {

    boolean existsByName(String name);

    @Modifying
    @Query("UPDATE Product p SET p.deletedAt = CURRENT_TIMESTAMP "
         + "WHERE p.id = :id AND p.deletedAt IS NULL")
    void softDelete(UUID id);
}
```

Method `existsByName` memanfaatkan `@SQLRestriction("deleted_at IS NULL")` pada entity Product,
sehingga pengecekan duplikat otomatis mengabaikan produk yang sudah di-soft-delete.

### 3. DTO

- **ProductRequest** (record): `name`, `description`, `price`, `stock`, `category` dengan anotasi
  validasi yang sama persis dengan entity (`@NotNull`, `@Size`, `@DecimalMin`).
- **ProductResponse** (record): `id`, `name`, `description`, `price`, `stock`, `category`,
  `createdAt`, `updatedAt` — tidak mengekspos `version`/`deletedAt`.

### 4. ProductService — Method

| Method | Return | Cache | Keterangan |
|---|---|---|---|
| `create(ProductRequest)` | `ProductResponse` | `@CachePut(products, key=id)` | Cek nama unik → simpan → put cache → return response |
| `findById(UUID)` | `ProductResponse` | `@Cacheable(products, key=id)` | Throw `ResourceNotFoundException` jika tidak ada |
| `findAll()` | `List<ProductResponse>` | `@Cacheable(products_list, key='all')` | Semua produk aktif (soft-delete di-filter) |
| `search(ProductFilterRequest, page, size)` | `Page<ProductResponse>` | — | Dynamic filter via QueryDSL predicates |
| `update(UUID, ProductRequest)` | `ProductResponse` | `@CacheEvict(products + products_list)` | RetryTemplate pada OptimisticLockException; cek nama unik (kecuali nama sendiri) |
| `delete(UUID)` | `void` | `@CacheEvict(products + products_list)` | Soft delete via `deleteById()` |

Validasi duplikat nama:
- Saat `create`: tolak jika `existsByName(name)` true → `BadRequestException`.
- Saat `update`: izinkan mempertahankan nama sendiri; tolak jika nama baru sudah dipakai produk lain.

`update()` menggunakan `retryTemplate.execute(context -> {...})` untuk retry otomatis hingga 3x (backoff 100ms→5s, multiplier 2.0) jika terjadi `OptimisticLockException`.

### 5. Unit Test (ProductServiceTest)

Menggunakan Mockito (`@ExtendWith(MockitoExtension.class)`, `@Mock` ProductRepository,
`@InjectMocks` ProductService). Coverage:
- ✅ create berhasil menyimpan & mengembalikan response
- ✅ create menolak nama duplikat (tanpa save)
- ✅ findById mengembalikan produk / throw jika tidak ada
- ✅ findAll mengembalikan seluruh produk aktif
- ✅ update memodifikasi field
- ✅ update mengizinkan mempertahankan nama sendiri
- ✅ update menolak nama duplikat
- ✅ delete melakukan soft delete (`isDeleted() == true`)
- ✅ delete throw jika tidak ada

### 6. Hasil Verifikasi

Verifikasi penuh dijalankan melalui CI: `./mvnw clean verify -Pci`
(compile, Checkstyle, SpotBugs, seluruh test suite, dan gate JaCoCo).
Angka test per kelas adalah snapshot dan tidak di-hardcode di sini —
rujuk `docs/history/` untuk snapshot per versi.

### 7. Catatan
- Menggunakan `record` untuk DTO agar immutable dan bebas boilerplate (clean code).
- `@Transactional` dipisah: write operation vs `readOnly = true` untuk query.
- Soft delete konsisten dengan pola `BaseEntity` dan `@SQLRestriction` di entity.
- Validasi nama duplikat dilakukan di service layer (bukan DB unique constraint) agar pesan
  error ramah pengguna dan tidak bentrok dengan soft-deleted rows.
- **Caching hanya aktif di profil `staging`/`prod`** (Redis `spring.data.redis.enabled=true`). Profil `dev` memakai in-memory H2 tanpa Redis — caching annotation diabaikan secara otomatis oleh Spring karena cache manager tidak aktif.
- **Cache key:** `products::{id}` untuk findById, `products_list::all` untuk findAll — sesuai `CacheConstants`.
- `delete()` menggunakan `repository.deleteById(id)` (bukan `softDelete()` JPQL eksplisit) — `@SQLDelete` pada entity Product yang mengubah DELETE menjadi UPDATE soft-delete.