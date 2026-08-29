# Task 2: Clean Code & Quality Gates

> **Status:** Current
> **Audience:** Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-28 (screening kode aktual `src/`, `pom.xml`)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## Tujuan
Memastikan project mengikuti prinsip clean code, optimal, dan bebas dari kesalahan Sonar.

## Acceptance Criteria
- [x] Review existing code untuk clean code & Sonar issues
- [x] Setup Sonar properties & quality gates
- [x] Buat struktur package yang clean
- [x] Implementasi best practices (exception handling, security config, dll)
- [x] Add code quality plugins (checkstyle, spotbugs, sonar)
- [x] Verifikasi hasil dengan ./mvnw clean verify

## Detail Pekerjaan

### 1. Struktur Package yang Clean

Project menggunakan **package-by-feature/domain** (bukan by-layer). Setiap
domain memuat `controller`, `service`, `dto`, `entity`, dan `repository`
miliknya sendiri; kelas lintas domain diletakkan di package khusus.

```
src/main/java/com/oms/
├── OmsApplication.java              ← Main class
├── admin/                           ← Modul admin (controller/service/dto)
├── audit/                           ← Audit logging (annotation/aspect/entity/repository/service)
├── auth/                            ← Autentikasi & registrasi
│   ├── controller/                  ← AuthController
│   ├── dto/                         ← Request/response DTO
│   ├── entity/                      ← User, RefreshToken
│   ├── repository/                  ← JPA repositories
│   ├── security/                    ← JwtTokenProvider, JwtAuthenticationFilter, UserDetailsAdapter
│   ├── service/                     ← RegisterService, RefreshTokenService (NoOpRefreshTokenService)
│   └── validation/                  ← Custom validators
├── config/                          ← Konfigurasi lintas domain
│   ├── SecurityFilterChainConfig.java ← Spring Security filter chain
│   ├── SecurityPasswordEncoderConfig.java ← BCrypt PasswordEncoder
│   ├── SecurityCorsConfig.java      ← CORS
│   ├── RedisConfig.java             ← Redis (conditional)
│   ├── ErrorProperties.java         ← app.error.base-url
│   ├── OpenApiConfig.java           ← Swagger/OpenAPI
│   ├── AsyncConfig.java / CacheConfig.java / CircuitBreakerConfig.java / ...
│   ├── versioning/                  ← ApiVersionFilter, VersioningResolver
│   └── annotation/                  ← Custom annotations
├── constant/                        ← Constants (Cache, Error, MDC, OpenApi, RateLimit, Security)
├── entity/                          ← BaseEntity (shared JPA base)
├── exception/                       ← Exception handling RFC 7807
│   ├── ErrorCode.java               ← Enum kode error + HTTP status + title
│   ├── BaseExceptionHandler.java    ← Abstract builder ProblemDetail
│   ├── SecurityExceptionHandler.java
│   ├── ValidationExceptionHandler.java
│   ├── BusinessExceptionHandler.java
│   ├── InfrastructureExceptionHandler.java
│   ├── AsyncExceptionHandler.java   ← AsyncUncaughtExceptionHandler
│   ├── BusinessException.java       ← Custom 400 exception
│   ├── BadRequestException.java     ← Custom 400 exception
│   ├── ResourceNotFoundException.java ← Custom 404 exception
│   └── GlobalExceptionHandler.java  ← @Deprecated(forRemoval) placeholder
├── health/                          ← Health indicators (Database, Redis, ExternalAPI)
├── order/                           ← Modul order (controller/service/dto/entity/repository)
├── product/                         ← Modul product (controller/service/dto/entity/repository)
├── ratelimit/                       ← Rate limiting (Bucket4j)
├── system/                          ← CsrfController, VersionController
└── validation/                      ← InputSanitizer, SafeHtml validator
```

### 2. Code Quality Plugins

| Plugin | Versi | Kegunaan |
|---|---|---|
| **Checkstyle** | 3.3.1 | Coding standards & style checking |
| **SpotBugs** | 4.8.6.0 | Bug pattern detection |
| **JaCoCo** | 0.8.12 | Code coverage |
| **Sonar Maven** | 3.11.0.3922 | SonarCloud integration |

### 3. Best Practices Terimplementasi

#### a. Exception Handler RFC 7807 (per-domain)
- Handler `@RestControllerAdvice(basePackages = "com.oms")` dipecah per domain:
  `SecurityExceptionHandler`, `ValidationExceptionHandler`,
  `BusinessExceptionHandler`, `InfrastructureExceptionHandler`
- Semua mewarisi `BaseExceptionHandler` (shared builder `ProblemDetail`)
- `ErrorCode` enum terpusat memetakan error → HTTP status + title
- Custom exceptions: `ResourceNotFoundException`, `BusinessException`, `BadRequestException`
- Validation error handling dengan field-level errors
- Base URL `type` di-externalize via `app.error.base-url` (`ErrorProperties`)
- `GlobalExceptionHandler` kini `@Deprecated(forRemoval)` (placeholder kosong)

#### b. Security Configuration
- `SecurityFilterChainConfig` dengan filter chain terpisah (management + API)
- Stateless session management, `httpBasic` dinonaktifkan
- BCrypt password encoder di `SecurityPasswordEncoderConfig` (terpisah)
- CORS di `SecurityCorsConfig` (terpisah)
- Public endpoints: `/api/v1/auth/**`, `/api/version`, `/actuator/health`, `/actuator/info`, Swagger, H2 console
- Proteksi admin via URL + method-level (`/api/v1/admin/**`)

#### c. JWT Token Provider
- `com.oms.auth.security.JwtTokenProvider` (HS256, secret key)
- Token generation dengan expiry
- Token validation dengan proper exception handling
- Username extraction from token

#### d. Error Response (RFC 7807 ProblemDetail)
- Menggunakan `ProblemDetail` bawaan Spring Framework (bukan wrapper custom)
- Field: `type`, `title`, `status`, `detail`, `instance`, `timestamp`
- `ErrorResponse.java` dihapus — digantikan `ProblemDetail`

### 4. Sonar Properties
File: `sonar-project.properties`
```properties
sonar.projectKey=com.oms:oms
sonar.projectName=oms
sonar.projectVersion=0.0.1-SNAPSHOT
sonar.sources=src/main/java
sonar.tests=src/test/java
sonar.java.binaries=target/classes
sonar.sourceEncoding=UTF-8
sonar.java.source=21
sonar.exclusions=**/dto/**,**/entity/**,**/config/**
sonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml
sonar.java.checkstyle.reportPaths=target/checkstyle-result.xml
sonar.java.spotbugs.reportPaths=target/spotbugsXml.xml
```

### 5. Hasil Verifikasi (baseline CI)
```
BUILD SUCCESS ✅
├── Checkstyle: 0 violations
├── SpotBugs: No bugs detected
├── JaCoCo: All coverage checks met
├── Tests: 316 passed
└── JAR: oms-0.0.1-SNAPSHOT.jar
```

### 6. Cara Menjalankan Quality Checks
```bash
# Full build dengan quality checks
./mvnw clean verify

# Hanya checkstyle
./mvnw checkstyle:check

# Hanya spotbugs
./mvnw spotbugs:check

# Sonar analysis (butuh SonarCloud token)
./mvnw sonar:sonar -Dsonar.login=YOUR_TOKEN
```