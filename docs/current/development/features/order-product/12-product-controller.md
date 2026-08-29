# Task 12: Product Controller (Basic CRUD REST)

> **Status:** Current
> **Audience:** Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-28 (screening kode aktual `src/`, `pom.xml`)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## Tujuan
Membuat REST controller untuk Product dengan role-based access control.

## Acceptance Criteria
- [x] `POST /api/v1/products` — create product (ADMIN only)
- [x] `GET /api/v1/products` — list all (CUSTOMER + ADMIN)
- [x] `GET /api/v1/products/{id}` — get by id
- [x] `GET /api/v1/products/search` — search with filter (CUSTOMER + ADMIN)
- [x] `PUT /api/v1/products/{id}` — update (ADMIN only)
- [x] `DELETE /api/v1/products/{id}` — soft delete (ADMIN only)
- [x] Response mengembalikan payload `ResponseEntity<T>` langsung (tanpa envelope)

## Detail Pekerjaan

### 1. File yang Dibuat/Diubah

```
src/main/java/com/oms/product/
├── controller/
│   └── ProductController.java         ← REST endpoints
├── dto/
│   ├── ProductRequest.java            ← record request (validated)
│   ├── ProductResponse.java           ← record response
│   └── ProductFilterRequest.java      ← record filter (search)
└── service/
    ├── ProductServiceInterface.java   ← interface
    └── ProductService.java            ← logika CRUD + validasi

src/test/java/com/oms/product/controller/
├── ProductControllerTest.java         ← integration test
└── ProductControllerUnitTest.java     ← unit test (MockMvc)
```

> **Catatan:** Tidak ada wrapper `ApiResponse` di codebase. Controller
> mengembalikan `ResponseEntity<T>` langsung; error memakai RFC 7807
> `ProblemDetail` (lihat doc 19). Anotasi `@ApiResponse`/`@ApiResponses`
> di controller adalah anotasi Swagger/OpenAPI (`io.swagger.v3.oas.annotations`),
> bukan kelas wrapper.

### 2. ProductController — Endpoints

| HTTP | Endpoint | Auth | Response | Deskripsi |
|---|---|---|---|---|
| POST | `/api/v1/products` | ADMIN | `201 Created` + `ProductResponse` | Buat produk baru |
| GET | `/api/v1/products` | CUSTOMER, ADMIN | `200 OK` + `List<ProductResponse>` | List semua produk |
| GET | `/api/v1/products/{id}` | CUSTOMER, ADMIN | `200 OK` + `ProductResponse` | Cari produk by id |
| GET | `/api/v1/products/search` | CUSTOMER, ADMIN | `200 OK` + `Page<ProductResponse>` | Search + filter + pagination |
| PUT | `/api/v1/products/{id}` | ADMIN | `200 OK` + `ProductResponse` | Update produk |
| DELETE | `/api/v1/products/{id}` | ADMIN | `200 OK` (empty body) | Soft delete produk |

Role di-enforce via `@PreAuthorize("hasRole('ADMIN')")` / `@PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")`.

### 3. Security

`@EnableMethodSecurity` diaktifkan di `SecurityFilterChainConfig`
(`com.oms.config`) agar anotasi `@PreAuthorize` di controller berfungsi.

### 4. Unit Test (ProductControllerTest)

`@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")` dengan
`@MockBean ProductServiceInterface`. Coverage (13 test case):
- ✅ POST sebagai ADMIN → 201
- ✅ POST sebagai CUSTOMER → 403
- ✅ POST tanpa auth → 403
- ✅ GET sebagai ADMIN → 200
- ✅ GET sebagai CUSTOMER → 200
- ✅ GET by id → 200
- ✅ GET by id (not found) → 404
- ✅ PUT sebagai ADMIN → 200
- ✅ PUT sebagai CUSTOMER → 403
- ✅ PUT duplikat nama → 400
- ✅ DELETE sebagai ADMIN → 200
- ✅ DELETE sebagai CUSTOMER → 403
- ✅ DELETE (not found) → 404

### 5. Hasil Verifikasi

Verifikasi penuh dijalankan melalui CI: `./mvnw clean verify -Pci`
(compile, Checkstyle, SpotBugs, seluruh test suite, dan gate JaCoCo).
Angka test per kelas adalah snapshot dan tidak di-hardcode di sini —
rujuk `docs/history/` untuk snapshot per versi.

### 6. Catatan
- Controller mengembalikan `ResponseEntity<T>` langsung; tidak ada envelope `ApiResponse`.
- Untuk `DELETE` yang body-nya kosong, digunakan `ResponseEntity<Void>` (empty body).
- Endpoint `/search` mengembalikan `Page<ProductResponse>` (pagination metadata).
- `@EnableMethodSecurity` adalah replacement modern dari `@EnableGlobalMethodSecurity` di Spring Boot 3.