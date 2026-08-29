# Task 6: Swagger/OpenAPI Documentation

> **Status:** Superseded (historical snapshot)
> **Audience:** Developer
> **Owner:** Engineering
> **Snapshot date:** 2026-08-06
> **Superseded by:** [OMS-037: Dokumentasi API dengan OpenAPI/Swagger](../../current/api/64-oms-037-swagger-openapi.md)
> **Catatan:** Laporan implementasi awal. Untuk konfigurasi Swagger/OpenAPI current-state, lihat dokumen pengganti dan [API index](../../current/api/api-index.md).

## Tujuan
Menambahkan Swagger UI untuk dokumentasi API yang interaktif.

## Acceptance Criteria
- [x] SpringDoc OpenAPI dependency ditambahkan
- [x] Swagger UI accessible di `/swagger-ui/index.html`
- [x] API docs accessible di `/v3/api-docs`
- [x] Security config mengizinkan akses ke Swagger endpoints

## Detail Pekerjaan

### 1. Dependency yang Ditambahkan

```xml
<!-- SpringDoc OpenAPI (Swagger) -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

### 2. Security Configuration

Swagger endpoints diizinkan tanpa autentikasi:

```java
.requestMatchers("/swagger-ui/**").permitAll()
.requestMatchers("/v3/api-docs/**").permitAll()
```

### 3. Cara Mengakses Swagger

```bash
# Jalankan aplikasi (dev profile)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Buka browser:
# Swagger UI: http://localhost:8080/swagger-ui/index.html
# API Docs:  http://localhost:8080/v3/api-docs
```

### 4. Fitur yang Tersedia

| Fitur | URL | Keterangan |
|---|---|---|
| **Swagger UI** | `/swagger-ui/index.html` | Interactive API documentation |
| **API Docs (JSON)** | `/v3/api-docs` | OpenAPI 3.0 specification |
| **Swagger UI (alternate)** | `/swagger-ui.html` | Redirect ke index.html |

### 5. Konfigurasi Default

SpringDoc OpenAPI otomatis:
- Scan semua `@RestController` beans
- Generate schema dari model classes
- Document semua endpoints
- Provide interactive UI untuk test API

### 6. Hasil Verifikasi

```
BUILD SUCCESS ✅
├── Compile: OK
├── Checkstyle: 0 violations
├── SpotBugs: 0 bugs
├── Tests: 1/1 passed
└── Swagger UI: Ready at /swagger-ui/index.html
```

### 7. Catatan

- Swagger UI hanya untuk **development/staging**
- Di production, bisa di-disable dengan profile
- Untuk menambahkan custom API documentation, gunakan annotations:
  - `@Operation` - Deskripsi endpoint
  - `@Parameter` - Deskripsi parameter
  - `@Schema` - Deskripsi model