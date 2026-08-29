# Task 14: AuthenticationController (Login)

> **Status:** Current
> **Audience:** Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-28 (screening kode aktual `src/`, `pom.xml`)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## Tujuan
Membuat endpoint login yang mengembalikan JWT token menggunakan Spring Security AuthenticationManager, serta konfigurasi Swagger UI dengan Bearer JWT untuk menguji endpoint terproteksi.

## Acceptance Criteria
- [x] `POST /api/v1/auth/login` dengan body: {username, password}
- [x] Validasi credentials menggunakan AuthenticationManager
- [x] Success return {accessToken, refreshToken, tokenType: "Bearer"}
- [x] Failed return 401 Unauthorized
- [x] Password encoding menggunakan BCryptPasswordEncoder
- [x] Swagger UI Authorize button dengan Bearer JWT

## Detail Pekerjaan

### 1. File yang Dibuat/Diubah

```
src/main/java/com/oms/auth/
├── dto/
│   ├── LoginRequest.java          ← request: username, password (@NotBlank)
│   └── LoginResponse.java         ← response: accessToken, refreshToken, tokenType
├── service/
│   └── CustomUserDetailsService.java ← loadUserByUsername dari database
├── security/
│   └── JwtTokenProvider.java      ← generate access + refresh token
└── controller/
    └── AuthController.java        ← POST /api/v1/auth/login

src/main/java/com/oms/config/
├── SecurityFilterChainConfig.java ← + AuthenticationManager bean + permit /api/v1/auth/**
└── OpenApiConfig.java             ← + Bearer JWT security scheme

src/test/java/com/oms/auth/controller/
└── AuthControllerTest.java        ← unit/integration test
```

### 2. Flow Login

```
Client                        Server
  │                              │
  │ POST /api/v1/auth/login         │
  │ {username, password}         │
  │─────────────────────────────>│
  │                              │
  │  1. Validasi input (@Valid)   │
  │  2. AuthenticationManager     │
  │     ↓                        │
  │  3. CustomUserDetailsService  │
  │     .loadUserByUsername()     │
  │     ↓                        │
  │  4. BCryptPasswordEncoder     │
  │     .matches()                │
  │     ↓                        │
  │  5. JwtTokenProvider          │
  │     .generateAccessToken()    │
  │     .generateRefreshToken()   │
  │     ↓                        │
  │ {accessToken, refreshToken,   │
  │  tokenType: "Bearer"}         │
  │<─────────────────────────────│
```

### 3. Endpoint

| Method | URL | Auth | Request | Success Response | Failed |
|---|---|---|---|---|---|
| POST | `/api/v1/auth/login` | Public (permitAll) | `LoginRequest {username, password}` | `200 OK` + `LoginResponse {accessToken, refreshToken, tokenType}` | `401 Unauthorized` |

### 4. Swagger UI — Authorize Button

Dengan `OpenApiConfig`, Swagger UI sekarang memiliki tombol **Authorize** di kanan atas:

```java
@Bean
public OpenAPI customOpenAPI() {
    return new OpenAPI()
            .info(...)
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
            .components(new Components()
                    .addSecuritySchemes("bearerAuth", new SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("Paste your JWT token here...")));
}
```

**Cara menggunakan:**
1. Jalankan `curl -X POST ... /api/v1/auth/login` untuk mendapat token
2. Buka Swagger UI: `http://localhost:8080/swagger-ui/index.html`
3. Klik tombol **Authorize** (🔒)
4. Paste token: `eyJhbGciOiJIUzI1NiJ9...`
5. Klik **Authorize** → semua endpoint terproteksi bisa diuji langsung dari Swagger

### 5. Test

```bash
# Login sebagai admin
# ADMIN_PASSWORD diambil dari environment (github action secret) — tidak di-hardcode di docs.
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"${ADMIN_PASSWORD}\"}"

# Response:
# {"accessToken":"eyJhbGciOiJIUzI1NiJ9...","refreshToken":"eyJhbGciOiJIUzI1NiJ9...","tokenType":"Bearer"}

# Gunakan token untuk akses endpoint terproteksi
curl -X GET http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

### 6. Hasil Verifikasi

Verifikasi penuh dijalankan melalui CI: `./mvnw clean verify -Pci`
(compile, Checkstyle, SpotBugs, seluruh test suite, dan gate JaCoCo).
`AuthControllerTest` mencakup: login sukses, password salah, user tidak ada, dan validasi input.
Angka test per kelas adalah snapshot dan tidak di-hardcode di sini —
rujuk `docs/history/` untuk snapshot per versi.

### 7. Catatan
- Endpoint login bersifat **public** (tidak butuh token) — hanya untuk mendapatkan token.
- `AuthenticationManager` secara otomatis menggunakan `CustomUserDetailsService` dan `BCryptPasswordEncoder` yang sudah di-register sebagai beans.
- Credentials salah akan menghasilkan `AuthenticationException` yang ditangani `GlobalExceptionHandler` menjadi response 401.
- Token type "Bearer" sesuai standar RFC 6750.
- `OpenApiConfig` menambahkan security scheme ke seluruh endpoint (kecuali `/api/v1/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**` yang di-permitAll di `SecurityFilterChainConfig`).
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`