# Laporan Final Fase 2 - Code Cleanup & Configuration Consolidation

> **Tanggal:** 9 Agustus 2026
> **Tujuan:** Membersihkan kode mati, menghilangkan magic values dan config duplikat, menyatukan pembentukan objek entity dengan Lombok, serta membuat base URL error RFC 7807 environment-aware.
> **Status:** ✅ **COMPLETED**
> **Tipe Fase:** Implementation (code changes + test updates, behavior-preserving refactor)
> **Referensi:** `docs/history/optimalisasi-v1/ANALISIS-PROYEK-OMS-V1.md` Section 17, `docs/history/optimalisasi-v1/01-final-report-phase1.md`

---

## 1. RINGKASAN MASALAH

Audit baseline mengidentifikasi beberapa masalah kebersihan kode dan konfigurasi yang belum ditangani di Fase 1:

1. **CC-01 (#2):** Konstanta `AppConstants` yang tidak dipakai (`MSG_PRODUCT_CREATED`, `MSG_PRODUCT_UPDATED`, `MSG_PRODUCT_DELETED`, `MSG_ORDER_CREATED`, `MSG_ADMIN_DASHBOARD`, `MSG_ALL_USERS`). *Diverifikasi: `DEFAULT_FALLBACK_EMAIL` dan `MSG_INVALID_REFRESH_TOKEN` **tetap dipakai** (`OrderService`, `AuthController`), sehingga dipertahankan.*
2. **CC-02 (#3):** `BASE_ERROR_URL` di-hardcode sebagai konstanta, tidak environment-aware.
3. **CC-03 (#4):** `app.dashboard.default-total-*` dan `app.default.fallback-email` di-seed di seluruh YAML namun tidak dibaca oleh source code mana pun.
4. **CC-04 (#7):** Magic value `Long.MAX_VALUE` sebagai fallback kapasitas rate limit.
5. **CC-06 (#11):** `@Setter` level class pada `BaseEntity` memberi akses ke field auditing `updatable = false`.
6. **CC-08 (#15):** Impor `User` di `AuthController` dilaporkan tidak terpakai — **diverifikasi tidak akurat**, impor tersebut **dipakai** pada baris 61 (`(User) authentication.getPrincipal()`). Tidak ada perubahan.
7. **CC-09 (#16) / KONF-01 (#25):** Blok `app.*` diulang identik di `application-dev.yml`, `application-staging.yml`, `application-prod.yml`.
8. **CC-10 (#17):** `OrderItem` memakai manual builder + constructor + getter/setter `order` manual.
9. **CC-11 (#18):** Exception classes memakai `@Slf4j` dan melakukan logging di constructor.
## 2. PERUBAHAN YANG DILAKUKAN

### 2.1 AppConstants — hapus konstanta mati

**File:** `src/main/java/com/oms/util/AppConstants.java`

**Sebelum:**
```java
public static final String BASE_ERROR_URL = "https://api.oms.com/errors";

// ...
public static final String MSG_PRODUCT_CREATED = "Product created successfully";
public static final String MSG_PRODUCT_UPDATED = "Product updated successfully";
public static final String MSG_PRODUCT_DELETED = "Product deleted successfully";
public static final String MSG_ORDER_CREATED = "Order created successfully";
public static final String MSG_ADMIN_DASHBOARD = "Admin dashboard";
public static final String MSG_ALL_USERS = "All users list";
public static final String MSG_INVALID_REFRESH_TOKEN = "Invalid refresh token";
public static final String DEFAULT_FALLBACK_EMAIL = "unknown@example.com";
```

**Sesudah:**
```java
// BASE_ERROR_URL dihapus; diganti konfigurasi app.error.base-url (bukan konstanta)
public static final String MSG_INVALID_REFRESH_TOKEN = "Invalid refresh token";
public static final String DEFAULT_FALLBACK_EMAIL = "unknown@example.com";
```

**Penjelasan:** Menghapus konstanta yang tidak direferensikan di main maupun test. `DEFAULT_FALLBACK_EMAIL` dan `MSG_INVALID_REFRESH_TOKEN` dipertahankan karena masih digunakan.

### 2.2 Error base URL environment-aware

**File:** `src/main/java/com/oms/exception/GlobalExceptionHandler.java`

**Sebelum:**
```java
public class GlobalExceptionHandler {
    private final HttpServletRequest request;
    public GlobalExceptionHandler(HttpServletRequest request) { this.request = request; }
    // ...
    problemDetail.setType(URI.create(AppConstants.BASE_ERROR_URL + "/" + errorCode.getStatusValue()));
}
```

**Sesudah:**
```java
@Value("${app.error.base-url:https://api.oms.com/errors}")
private String errorBaseUrl = "https://api.oms.com/errors";
// ...
problemDetail.setType(URI.create(errorBaseUrl + "/" + errorCode.getStatusValue()));
```

**Penjelasan:** Base URL dipindahkan dari konstanta hardcoded ke properti `app.error.base-url` yang dapat dioverride per environment. Nilai default tetap sama sehingga kontrak respons RFC 7807 tidak berubah.

### 2.3 Konfigurasi — buang config mati & deduplikasi

**File:** `src/main/resources/application.yml`, `application-dev.yml`, `application-staging.yml`, `application-prod.yml`

**Sebelum:**
```yaml
# application.yml + diulang di ketiga profile:
  dashboard:
    default-total-users: 150
    default-total-products: 45
    default-total-orders: 320
    default-revenue: 15000000
  default:
    fallback-email: unknown@example.com
```

**Sesudah:**
```yaml
# application.yml ditambah:
  error:
    base-url: ${ERROR_BASE_URL:https://api.oms.com/errors}
# application-dev.yml / staging.yml / prod.yml: blok app.* dihapus (disentralisasi di application.yml)
```

**Penjelasan:** Menghapus key config yang tidak pernah dibaca, menambah key baru untuk base URL error, dan menghapus blok `app.*` yang terkonsolidasi di `application.yml` dari ketiga profile (tidak ada nilai khusus per profile). Verifikasi runtime: profil dev/test tetap memuat seluruh nilai `app.rate-limit.*` dari file base.
### 2.4 BaseEntity — hapus @Setter

**File:** `src/main/java/com/oms/entity/BaseEntity.java`

**Sebelum:** `@Getter @Setter` — **Sesudah:** `@Getter` (import `Setter` dihapus).

**Penjelasan:** Menghapus akses setter ke field auditing (`createdAt`, `createdBy`) yang `updatable = false`; `softDelete()` masih dapat menulis `deletedAt` secara internal.

### 2.5 OrderItem — Lombok @Builder

**File:** `src/main/java/com/oms/entity/OrderItem.java`

**Sebelum:** manual `OrderItemBuilder` (inner class), constructor 6-arg, getter/setter `order` manual.

**Sesudah:**
```java
@Entity
@Table(name = "order_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLDelete(...)
@SQLRestriction("deleted_at IS NULL")
public class OrderItem extends BaseEntity {
    private UUID id;
    private UUID productId;
    private String productName;
    private Integer quantity;
    private BigDecimal price;
    private Order order;
}
```

**Penjelasan:** Menggantikan builder/constructor/getter-setter manual dengan Lombok, konsisten dengan entity lain. API `OrderItem.builder()....build()` dan `setOrder()` di `Order.java` tetap berfungsi.

### 2.6 RateLimitService — hapus magic value

**File:** `src/main/java/com/oms/ratelimit/RateLimitService.java`

**Sebelum:** `RateLimitResult.allowed(Long.MAX_VALUE, 0)` — **Sesudah:** konstanta bernama `NO_LIMIT = Long.MAX_VALUE` dengan Javadoc.

### 2.7 Exception classes — hapus logging di constructor

**File:** `src/main/java/com/oms/exception/BusinessException.java`, `ResourceNotFoundException.java`, `BadRequestException.java`

**Sebelum:** `@Slf4j` + `log.warn(...)` di constructor. **Sesudah:** anotasi dan logging dihapus; hanya delegasi `super(...)`.

**Penjelasan:** Menghilangkan efek sampling logging pada pembangkitan exception (hot path). Logging tetap ditangani di `GlobalExceptionHandler`.

### 2.8 AsyncConfig — hapus field static

**File:** `src/main/java/com/oms/config/AsyncConfig.java`

**Sebelum:** `private static ThreadPoolTaskExecutor emailExecutor;` / `emailExecutor = executor;`
**Sesudah:** `private ThreadPoolTaskExecutor emailExecutor;` / `this.emailExecutor = executor;`

**Penjelasan:** Menghapus state global dari bean config; instance field cukup karena bean `emailExecutor` diinisialisasi saat konfigurasi.

### 2.9 Test baru

**File:** `src/test/java/com/oms/exception/GlobalExceptionHandlerEdgeTest.java`

Menambahkan 2 test:
- `buildProblemDetail_shouldUseEnvironmentAwareErrorBaseUrl` — memverifikasi `type` RFC 7807 terbentuk dari `errorBaseUrl` custom via `ReflectionTestUtils`.
- `buildProblemDetail_shouldDefaultToCanonicalErrorBaseUrl` — menjamin nilai default tetap `https://api.oms.com/errors/404`.

Seluruh perubahan lain bersifat behavior-preserving dan divalidasi oleh test yang sudah ada (`OrderTest`, `OrderRepositoryTest` memakai Lombok `OrderItem.builder()`; `GlobalExceptionHandlerTest` tetap lulus kontrak `type`).

## 3. HASIL BUILD

```
Compilation: ✅ PASS  (./mvnw -DskipTests compile)
Tests:       ✅ 259 PASS, 0 FAIL, 0 ERROR, 13 SKIPPED  (./mvnw clean test)
                     (naik dari 257 — penambahan 2 test error base URL)
Checkstyle:  ✅ PASS  (./mvnw checkstyle:check)
SpotBugs:    ✅ PASS  (0 bugs)  (./mvnw spotbugs:check)
JaCoCo:      ✅ Terkonfigurasi (minimum 0.00%)
```

Testcontainers/integration yang membutuhkan Docker tetap skipped sesuai environment, bukan failure.

## 4. KESIMPULAN

Fase 2 menyelesaikan 10 issue baseline (CC-01, CC-02, CC-03, CC-04, CC-06, CC-09, CC-10, CC-11, CC-12, dan KONF-01) serta memverifikasi bahwa satu klaim baseline (CC-08 unused import di `AuthController`) **tidak akurat** sehingga tidak ada perubahan. Semua perubahan bersifat refactor/kebersihan kode dan konfigurasi tanpa mengubah perilaku API; 259 test lulus dan semua quality gate pass.

**Status:** ✅ **COMPLETED**

---

*Dokumen ini dibuat pada 9 Agustus 2026*

10. **CC-12 (#19):** Field `emailExecutor` bersifat `static` di `AsyncConfig`.