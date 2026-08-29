# OMS-037: Dokumentasi API dengan OpenAPI/Swagger

> **Status:** Current
> **Audience:** API Consumer, Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-28 (screening kode aktual `src/`, `pom.xml`)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

> **Tanggal:** 7 Agustus 2026
> **Tujuan:** Integrasi Swagger untuk dokumentasi API lengkap dengan @Operation annotations, JWT security scheme, dan akses Swagger UI.
> **Status:** ✅ **COMPLETED**
> **Referensi:** `docs/history/reports/06-swagger-openapi.md` (snapshot awal), `docs/current/development/features/authentication-security/14-auth-controller.md`, `docs/current/api/api-index.md`

---

## 1. RINGKASAN

Integrasi SpringDoc OpenAPI (Swagger) untuk dokumentasi API OMS secara interaktif. Mencakup konfigurasi dependency, security scheme JWT, dan anotasi `@Operation` pada seluruh controller.

### Acceptance Criteria

| No | Kriteria | Status |
|----|----------|:---:|
| 1 | springdoc-openapi dependency | ✅ **SUDAH ADA** (sejak Task 6) |
| 2 | `/swagger-ui.html` bisa diakses | ✅ **TERKONFIGURASI** (permitAll di SecurityFilterChainConfig) |
| 3 | Anotasi @Operation di controller | ✅ **DITAMBAHKAN** (semua 6 controller, 18 endpoints) |
| 4 | Security scheme untuk JWT di Swagger | ✅ **SUDAH ADA** (sejak Task 14, OpenApiConfig.java) |

---

## 2. DETAIL IMPLEMENTASI

### 2.1 springdoc-openapi Dependency ✅

**File:** `pom.xml` (lines 137-142)

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

**Status:** ✅ **SUDAH ADA** sejak Task 6 (inisialisasi project).

### 2.2 Swagger UI Accessibility ✅

**File:** `src/main/java/com/oms/config/SecurityFilterChainConfig.java`

```java
.requestMatchers("/swagger-ui/**").permitAll()
.requestMatchers("/v3/api-docs/**").permitAll()
```

**Swagger UI:** `http://localhost:8080/swagger-ui/index.html`
**API Docs:** `http://localhost:8080/v3/api-docs`

**Status:** ✅ **SUDAH ADA** sejak Task 6.

### 2.3 @Operation Annotations ✅ (BARU DITAMBAHKAN)

Anotasi `@Operation`, `@ApiResponses`, `@Parameter`, `@Tag` telah ditambahkan ke **semua 6 controller**:

| Controller | Tag | Endpoints | Method |
|-----------|-----|-----------|--------|
| **AuthController** | `Auth` | 4 | register, login, refresh, logout |
| **ProductController** | `Product` | 6 | create, getAll, getById, search, update, delete |
| **OrderController** | `Order` | 4 | create, getAll, getById, delete |
| **AdminController** | `Admin` | 2 | dashboard, users |
| **VersionController** | `System` | 1 | version |
| **CsrfController** | `Auth` | 1 | csrf |

#### AuthController Annotations

```java
@Tag(name = "Auth", description = "Auth: login, refresh, logout. Public endpoints for JWT token management.")

@Operation(
    summary = "Authenticate user and return JWT tokens",
    description = "Authenticates a user with username/password. Returns access token (1h) and refresh token (7d).",
    responses = {
        @ApiResponse(responseCode = "200", description = "Authentication successful",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Authentication failed (bad credentials)")
    })
@PostMapping("/login")
```

#### ProductController Annotations

```java
@Tag(name = "Product", description = "Product CRUD and search. ADMIN: create/update/delete; CUSTOMER: view/search.")

@Operation(
    summary = "Create a new product",
    description = "Creates a new product. Returns 400 if product name already exists.",
    security = @SecurityRequirement(name = "bearerAuth"))
@ApiResponses({
    @ApiResponse(responseCode = "201", description = "Product created successfully",
        content = @Content(schema = @Schema(implementation = ProductResponse.class))),
    @ApiResponse(responseCode = "400", description = "Validation error or duplicate product name"),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required")
})
@PostMapping
@PreAuthorize("hasRole('ADMIN')")
```

#### OrderController Annotations

```java
@Tag(name = "Order", description = "Order management. Customers: create/view own orders; Admins: view all.")

@Operation(
    summary = "Create a new order",
    description = "Creates a new order for the authenticated user. Validates stock and reduces it atomically.",
    security = @SecurityRequirement(name = "bearerAuth"))
@ApiResponses({
    @ApiResponse(responseCode = "201", description = "Order created successfully",
        content = @Content(schema = @Schema(implementation = OrderResponse.class))),
    @ApiResponse(responseCode = "400", description = "Validation error, insufficient stock, or invalid quantity"),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "404", description = "Product not found")
})
@PostMapping
@PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
```

#### AdminController Annotations

```java
@Tag(name = "Admin", description = "Admin dashboard and user management. All endpoints require ADMIN role.")

@Operation(
    summary = "Get admin dashboard statistics",
    description = "Returns dashboard statistics: total users, products, orders, revenue.",
    security = @SecurityRequirement(name = "bearerAuth"))
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Dashboard statistics",
        content = @Content(schema = @Schema(implementation = Map.class))),
    @ApiResponse(responseCode = "401", description = "Unauthorized"),
    @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required")
})
@GetMapping("/dashboard")
@PreAuthorize("hasRole('ADMIN')")
```

#### VersionController Annotations

```java
@Tag(name = "System", description = "System endpoints for API version info, health checks, and utilities.")

@Operation(
    summary = "Get API version information",
    description = "Returns current API version, available versions, and API docs URL. Public endpoint.",
    responses = {
        @ApiResponse(responseCode = "200", description = "Version information retrieved successfully",
            content = @Content(schema = @Schema(implementation = Map.class)))
    })
@GetMapping("/version")
```

#### CsrfController Annotations

```java
@Tag(name = "Auth", description = "Auth: login, refresh, logout. Public endpoints for JWT token management.")

@Operation(
    summary = "Get CSRF token",
    description = "Returns a CSRF token for the current session. Set as XSRF-TOKEN cookie. Public endpoint.",
    responses = {
        @ApiResponse(responseCode = "200", description = "CSRF token retrieved successfully",
            content = @Content(schema = @Schema(implementation = Map.class)))
    })
@GetMapping("/csrf")
```

### 2.4 Security Scheme JWT ✅

**File:** `src/main/java/com/oms/config/OpenApiConfig.java`

```java
@Bean
public OpenAPI customOpenAPI() {
    return new OpenAPI()
            .info(new Info()
                    .title("OMS API")
                    .version("1.0.0")
                    .description("Order Management System API - Documentation"))
            .addSecurityItem(new SecurityRequirement().addList(OpenApiConstants.SCHEME_NAME_BEARER_AUTH))
            .components(new Components()
                    .addSecuritySchemes(OpenApiConstants.SCHEME_NAME_BEARER_AUTH, new SecurityScheme()
                            .name(OpenApiConstants.SCHEME_NAME_BEARER_AUTH)
                            .type(SecurityScheme.Type.HTTP)
                            .scheme(OpenApiConstants.SCHEME_BEARER)
                            .bearerFormat(OpenApiConstants.FORMAT_JWT)
                            .description("Paste your JWT token here. Get it from POST /api/auth/login")));
}
```

**Fitur:**
- Tombol **Authorize** (🔒) di Swagger UI untuk memasukkan JWT token
- Security scheme: `bearerAuth` (HTTP Bearer token)
- Format: `JWT`
---

## 3. CARA AKSES

### 3.1 Swagger UI

```bash
# Jalankan aplikasi
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Buka di browser:
# http://localhost:8080/swagger-ui/index.html
```

### 3.2 OpenAPI JSON

```bash
# Download OpenAPI specification
curl http://localhost:8080/v3/api-docs | jq .
```

### 3.3 Menggunakan Swagger UI untuk Testing

1. **Login terlebih dahulu:**
   - Buka endpoint `POST /api/v1/auth/login`
   - Masukkan body: `{"username": "admin", "password": "${ADMIN_PASSWORD}"}` (nilai dari GitHub Actions secret)
   - Klik **Execute**, copy nilai `accessToken`

2. **Authorize:**
   - Klik tombol **Authorize** (🔒) di kanan atas
   - Paste token, klik **Authorize**

3. **Test endpoints:**
   - Semua endpoint terproteksi sekarang bisa diuji
   - Endpoint public (login, version, health) bisa diakses tanpa token

---

## 4. STRUKTUR TAG SWAGGER

| Tag | Description | Endpoints |
|-----|-------------|-----------|
| **Auth** | Auth: login, refresh, logout. Public endpoints for JWT token management. | register, login, refresh, logout, csrf |
| **Product** | Product CRUD and search. ADMIN: create/update/delete; CUSTOMER: view/search. | create, getAll, getById, search, update, delete |
| **Order** | Order management. | create, getAll, getById, delete |
| **Admin** | Admin dashboard and user management. All endpoints require ADMIN role. | dashboard, users |
| **System** | System endpoints for API version info, health checks, and utilities. | version |

---

## 5. FILES YANG DIMODIFIKASI

| File | Perubahan |
|------|-----------|
| `ProductController.java` | Added @Tag, @Operation, @ApiResponses, @Parameter (6 endpoints) |
| `OrderController.java` | Added @Tag, @Operation, @ApiResponses, @Parameter (4 endpoints) |
| `AuthController.java` | Added @Tag, @Operation (4 endpoints, removed unused import) |
| `AdminController.java` | Added @Tag, @Operation, @ApiResponses (2 endpoints) |
| `VersionController.java` | Added @Tag, @Operation (1 endpoint, removed unused import) |
| `CsrfController.java` | Added @Tag, @Operation (1 endpoint, removed unused import) |

**Total: 6 files modified, 18 endpoints annotated with @Operation**

---

## 6. HASIL BUILD

```
Compilation: ✅ BUILD SUCCESS
Checkstyle:  ✅ PASS (0 violations)
```

---

## 7. KESIMPULAN

Integrasi OpenAPI/Swagger untuk dokumentasi API OMS telah selesai:

- ✅ **springdoc-openapi dependency** — `springdoc-openapi-starter-webmvc-ui:2.3.0` di pom.xml
- ✅ **Swagger UI accessible** — `http://localhost:8080/swagger-ui/index.html` (permitAll di SecurityFilterChainConfig)
- ✅ **@Operation annotations** — Semua 18 endpoints di 6 controller memiliki @Operation, @ApiResponses, @Tag, dan @Parameter
- ✅ **JWT Security Scheme** — Tombol Authorize di Swagger UI dengan Bearer JWT token (OpenApiConfig.java)

**Status:** ✅ **COMPLETED**

---

*Dokumen ini dibuat pada 7 Agustus 2026*


**Status:** ✅ **SUDAH ADA** sejak Task 14.