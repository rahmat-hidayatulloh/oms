# OMS-028: Implementasi API Versioning via URL Path

> **Status:** Current
> **Audience:** API Consumer, Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-23 (screening kode aktual `src/`, `pom.xml`)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## Tujuan
Mengimplementasikan API versioning strategy menggunakan URL path (`/api/v1/`, `/api/v2/`) untuk memastikan backward compatibility dan memudahkan migrasi API di masa depan.

## Acceptance Criteria
- [x] Restruktur URL pattern: `/api/v1/...` dan `/api/v2/...`
- [x] Buat custom annotation `@ApiVersion("v1")`
- [x] VersioningResolver untuk mapping request ke version-specific controller
- [x] VersionController untuk menampilkan version info: `GET /api/version`
- [x] Deprecation header untuk endpoint yang akan di-deprecate

## Detail Pekerjaan

### 1. Latar Belakang

API versioning adalah praktik penting untuk:
- **Backward Compatibility**: Client lama tetap berfungsi saat API diperbarui
- **Gradual Migration**: Client bisa migrasi ke versi baru secara bertahap
- **Clear Documentation**: Setiap versi API terdokumentasi dengan jelas
- **Deprecation Management**: Versi lama bisa di-deprecate dengan aman

### 2. Strategi Versioning

Kami menggunakan **URL Path Versioning** karena:
- Mudah dipahami dan digunakan
- Clear dan explicit di setiap request
- Mudah untuk routing dan documentation
- Bisa di-test dengan mudah via browser/curl

**Contoh URL:**
```
/api/v1/products          ← Version 1 (stable)
/api/v2/products          ← Version 2 (future)
/api/version              ← Version info (unversioned)
```

### 3. Struktur File

```
src/main/java/com/oms/
├── config/
│   ├── annotation/
│   │   └── ApiVersion.java                    ← Custom annotation
│   └── versioning/
│       ├── VersioningResolver.java            ← Version extraction logic
│       └── ApiVersionFilter.java              ← Deprecation header filter
├── system/
│   └── VersionController.java                 ← GET /api/version
├── product/controller/
│   └── ProductController.java                 ← @RequestMapping("/api/v1/products")
├── order/controller/
│   └── OrderController.java                   ← @RequestMapping("/api/v1/orders")
├── auth/controller/
│   └── AuthController.java                    ← @RequestMapping("/api/v1/auth")
└── admin/controller/
    └── AdminController.java                   ← @RequestMapping("/api/v1/admin")
```

### 4. @ApiVersion Annotation

**File:** `src/main/java/com/oms/config/annotation/ApiVersion.java`

Custom annotation untuk menandai versi API pada controller:

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiVersion {
    String value();
}
```

**Usage:**
```java
@RestController
@RequestMapping("/api/v1/products")
@ApiVersion("v1")
public class ProductController { ... }
```

### 5. VersioningResolver

**File:** `src/main/java/com/oms/config/versioning/VersioningResolver.java`

Komponen yang bertanggung jawab untuk:
- Extract version dari request URI menggunakan regex pattern `/api/v{number}/`
- Default ke `v1` jika tidak ada version di URL
- Maintain registry of deprecated versions
- Provide replacement version untuk deprecated versions

**Core Logic:**
```java
public String resolveVersion(HttpServletRequest request) {
    String uri = request.getRequestURI();
    Pattern pattern = Pattern.compile("/api/v(\\d+)/");
    Matcher matcher = pattern.matcher(uri);
    
    if (matcher.find()) {
        return "v" + matcher.group(1);
    }
    
    return "v1"; // Default
}
```

**Deprecation Management:**
```java
// Register deprecated version
versioningResolver.deprecateVersion("v1", "v2");

// Check if version is deprecated
String replacement = versioningResolver.getDeprecationReplacement("v1");
// Returns: "v2"
```

### 6. ApiVersionFilter (Deprecation Headers)

**File:** `src/main/java/com/oms/config/versioning/ApiVersionFilter.java`

Filter yang menambahkan deprecation warning headers ke response:

**Headers yang ditambahkan:**
- `X-API-Deprecated: v1` — versi yang di-deprecate
- `X-API-Deprecation-Replacement: v2` — versi pengganti
- `Sunset: 2027-01-01` — tanggal planned sunset (RFC 8594)

**Response Example:**
```http
HTTP/1.1 200 OK
X-API-Deprecated: v1
X-API-Deprecation-Replacement: v2
Sunset: 2027-01-01
Content-Type: application/json

{
  "id": "123",
  "name": "Product Name"
}
```

### 7. VersionController

**File:** `src/main/java/com.oms.system.VersionController.java`

Endpoint untuk mendapatkan informasi versi API:

**Endpoint:** `GET /api/version` (unauthenticated)

**Response:**
```json
{
  "application": "OMS",
  "currentVersion": "v1",
  "availableVersions": [
    {
      "version": "v1",
      "status": "Current stable version",
      "deprecated": false
    }
  ],
  "deprecatedVersions": [],
  "latestVersion": "v1",
  "apiDocumentation": "/swagger-ui/index.html",
  "serverTime": "2026-07-26"
}
```

### 8. Controller Restructure

Semua controller telah di-update untuk menggunakan `/api/v1/` prefix:

| Controller | Old Path | New Path |
|---|---|---|
| ProductController | `/api/products` | `/api/v1/products` |
| OrderController | `/api/orders` | `/api/v1/orders` |
| AuthController | `/api/auth` | `/api/v1/auth` |
| AdminController | `/api/admin` | `/api/v1/admin` |

**Example:**
```java
@RestController
@RequestMapping("/api/v1/products")
@ApiVersion("v1")
public class ProductController {
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(...) { ... }
    
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts(...) { ... }
}
```

### 9. Security Configuration Update

**File:** `src/main/java/com/oms/config/SecurityFilterChainConfig.java`

Updated security rules untuk support versioned URLs:

```java
.authorizeHttpRequests(auth -> auth
    // Public endpoints
    .requestMatchers("/api/v1/auth/**").permitAll()
    .requestMatchers("/api/version").permitAll()
    
    // Actuator
    .requestMatchers("/actuator/health").permitAll()
    .requestMatchers("/actuator/info").permitAll()
    .requestMatchers("/actuator/prometheus").authenticated()
    .requestMatchers("/actuator/metrics").authenticated()
    
    // Swagger / OpenAPI
    .requestMatchers("/swagger-ui/**").permitAll()
    .requestMatchers("/v3/api-docs/**").permitAll()
    .requestMatchers("/h2-console/**").permitAll()
    
    // Admin only
    .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
    
    // All v1 APIs require authentication
    .requestMatchers("/api/v1/**").authenticated()
)
```

### 10. Test Updates

Semua test files telah di-update untuk menggunakan `/api/v1/` prefix:

| Test File | Updated URLs |
|---|---|
| `AuthControllerTest.java` | `/api/v1/auth/login` |
| `AdminControllerTest.java` | `/api/v1/admin/dashboard`, `/api/v1/admin/users` |
| `GlobalExceptionHandlerTest.java` | `/api/v1/products`, `/api/v1/admin/dashboard` |

### 11. Contoh Request/Response

#### a. Create Product (v1)
```bash
POST /api/v1/products
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "name": "Kaos Polos Hitam",
  "description": "Kaos katun combed 30s",
  "price": 85000.00,
  "stock": 100,
  "category": "CLOTHING"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Product created successfully",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Kaos Polos Hitam",
    "price": 85000.00,
    "stock": 100,
    "category": "CLOTHING"
  }
}
```

#### b. Get Version Info
```bash
GET /api/version
```

**Response:**
```json
{
  "application": "OMS",
  "currentVersion": "v1",
  "availableVersions": [...],
  "latestVersion": "v1"
}
```

#### c. Deprecated Version Response (future)
```bash
GET /api/v1/products
# When v1 is deprecated, response includes:
HTTP/1.1 200 OK
X-API-Deprecated: v1
X-API-Deprecation-Replacement: v2
Sunset: 2027-01-01
```

### 12. Backward Compatibility Strategy

**Phase 1: Current (v1 stable)**
- All endpoints di `/api/v1/`
- No deprecation headers
- Fully supported

**Phase 2: v2 Development**
- Create new controllers di `/api/v2/`
- v1 tetap berfungsi
- Document breaking changes

**Phase 3: Deprecation**
- Register v1 as deprecated: `versioningResolver.deprecateVersion("v1", "v2")`
- Add deprecation headers
- Notify clients via documentation

**Phase 4: Sunset**
- Set sunset date di headers
- Monitor v1 usage via logs
- Remove v1 setelah sunset date

### 13. Best Practices

1. **Jangan breaking changes di v1** - Setiap breaking change harus di versi baru (v2)
2. **Document deprecation** - Update OpenAPI/Swagger untuk menandai deprecated endpoints
3. **Provide migration guide** - Dokumentasikan cara migrasi dari v1 ke v2
4. **Monitor usage** - Track which versions are being used via logs
5. **Set reasonable sunset period** - Minimum 6-12 bulan sebelum sunset

### 14. Files yang Diubah/Ditambahkan

| File | Tindakan | Deskripsi |
|---|---|---|
| `src/main/java/com/oms/config/annotation/ApiVersion.java` | **Dibuat** | Custom annotation untuk API version |
| `src/main/java/com/oms/config/versioning/VersioningResolver.java` | **Dibuat** | Version extraction dan deprecation registry |
| `src/main/java/com/oms/config/versioning/ApiVersionFilter.java` | **Dibuat** | Deprecation header filter |
| `src/main/java/com/oms/system/VersionController.java` | **Dibuat** | Version info endpoint |
| `src/main/java/com/oms/product/controller/ProductController.java` | **Diubah** | Update ke `/api/v1/products` + `@ApiVersion("v1")` |
| `src/main/java/com/oms/order/controller/OrderController.java` | **Diubah** | Update ke `/api/v1/orders` + `@ApiVersion("v1")` |
| `src/main/java/com/oms/auth/controller/AuthController.java` | **Diubah** | Update ke `/api/v1/auth` + `@ApiVersion("v1")` |
| `src/main/java/com/oms/admin/controller/AdminController.java` | **Diubah** | Update ke `/api/v1/admin` + `@ApiVersion("v1")` |
| `src/main/java/com/oms/config/SecurityFilterChainConfig.java` | **Diubah** | Update security rules untuk `/api/v1/**` |
| `src/test/java/com/oms/auth/controller/AuthControllerTest.java` | **Diubah** | Update URLs ke `/api/v1/auth/**` |
| `src/test/java/com/oms/admin/controller/AdminControllerTest.java` | **Diubah** | Update URLs ke `/api/v1/admin/**` |
| `src/test/java/com/oms/exception/GlobalExceptionHandlerTest.java` | **Diubah** | Update URLs ke `/api/v1/**` |
| `docs/current/api/31-api-versioning.md` | **Dibuat** | Dokumentasi |

### 15. Testing

Semua tests pass dengan URL paths yang baru:
- ✅ 316/316 tests pass (baseline CI)
- ✅ All controller tests updated
- ✅ Security config updated
- ✅ No breaking changes to test logic

### 16. Future Enhancements

1. **Header-based versioning** - Support `X-API-Version` header sebagai alternatif
2. **Content Negotiation** - Versioning via `Accept` header (media type versioning)
3. **OpenAPI grouping** - Separate Swagger docs per version
4. **Version middleware** - Automatic version extraction dan routing
5. **Client SDK generation** - Generate versioned client libraries