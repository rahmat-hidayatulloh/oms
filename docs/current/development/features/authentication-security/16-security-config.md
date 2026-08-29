# Task 16: SecurityFilterChainConfig dengan Method Security

> **Status:** Current
> **Audience:** Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-28 (screening kode aktual `src/`, `pom.xml`)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## Tujuan
Konfigurasi Spring Security untuk API stateless berbasis JWT, dengan role-based access control menggunakan `@EnableMethodSecurity` dan path-based rules.

## Acceptance Criteria
- [x] `@EnableMethodSecurity` untuk `@PreAuthorize`
- [x] CSRF diabaikan untuk `/api/**` (stateless JWT)
- [x] Session stateless
- [x] Public endpoints: `/api/v1/auth/**`, `/api/version`, `/actuator/health`
- [x] Protected endpoints: `/api/v1/**` membutuhkan authentication
- [x] Role-based: `/api/v1/admin/**` hanya ADMIN

## Detail Pekerjaan

### 1. File yang Dibuat/Diubah

```
src/main/java/com/oms/config/
├── SecurityFilterChainConfig.java    ← filter chain API + management, @EnableMethodSecurity
├── SecurityCorsConfig.java           ← CORS configuration source
└── SecurityPasswordEncoderConfig.java ← BCrypt PasswordEncoder bean

src/main/java/com/oms/admin/
├── controller/
│   └── AdminController.java          ← endpoint ADMIN-only
└── service/
    ├── AdminDashboardServiceInterface.java
    └── AdminUserManagementServiceInterface.java

src/test/java/com/oms/admin/controller/
└── AdminControllerTest.java          ← test case
```

> **Catatan:** Nama kelas aktual adalah `SecurityFilterChainConfig`
> (bukan `SecurityConfig`). CORS dan password encoder dipecah ke kelas
> tersendiri (`SecurityCorsConfig`, `SecurityPasswordEncoderConfig`).

### 2. SecurityFilterChainConfig Rules

Ada dua filter chain: satu untuk endpoint management (actuator, port 8081)
dan satu untuk API aplikasi.

**Management filter chain (`EndpointRequest.toAnyEndpoint()`):**

| Path Pattern | Access | Keterangan |
|---|---|---|
| `health`, `info` | `permitAll()` | Health & info actuator |
| `anyRequest()` | `authenticated()` | Actuator lain butuh auth |

**API filter chain:**

| Path Pattern | Access | Keterangan |
|---|---|---|
| `/api/v1/auth/**` | `permitAll()` | Login, register, refresh, logout |
| `/api/version` | `permitAll()` | Version info (public) |
| `/actuator/health` | `permitAll()` | Health check |
| `/actuator/info` | `permitAll()` | App info |
| `/actuator/prometheus` | `authenticated()` | Metrics (butuh auth) |
| `/actuator/metrics` | `authenticated()` | Metrics (butuh auth) |
| `/swagger-ui/**` | `permitAll()` | Swagger UI |
| `/v3/api-docs/**` | `permitAll()` | OpenAPI docs |
| `/h2-console/**` | `permitAll()` | H2 Console (dev only) |
| `/api/v1/admin/**` | `hasRole("ADMIN")` | Admin-only endpoints |
| `/api/v1/**` | `authenticated()` | Semua API v1 lain butuh login |
| `anyRequest()` | `authenticated()` | Fallback |

### 3. Security Features

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // untuk @PreAuthorize di controller
@RequiredArgsConstructor
public class SecurityFilterChainConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    // API chain (ringkasan)
    .cors(cors -> cors.configurationSource(corsConfigurationSource))
    .csrf(csrf -> csrf
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            .csrfTokenRequestHandler(new XorCsrfTokenRequestAttributeHandler())
            .ignoringRequestMatchers("/api/**", "/h2-console/**"))
    .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
    .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
    .httpBasic(AbstractHttpConfigurer::disable)
    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
}
```

### 4. AdminController

```java
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")  // (per-method di tiap endpoint)
public class AdminController {

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDashboard() { ... }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(...) { ... }
}
```

Dua lapisan proteksi:
1. **Path-based**: `.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")` di `SecurityFilterChainConfig`
2. **Method-level**: `@PreAuthorize("hasRole('ADMIN')")` di controller

### 5. Unit Test (AdminControllerTest)

`@SpringBootTest` + `@AutoConfigureMockMvc` + `@WithMockUser`:

| Test | Role | Expected |
|---|---|---|
| `getDashboard_shouldReturn200ForAdmin` | ADMIN | 200 OK |
| `getAllUsers_shouldReturn200ForAdmin` | ADMIN | 200 OK |
| `getDashboard_shouldReturn403ForCustomer` | CUSTOMER | 403 Forbidden |
| `getAllUsers_shouldReturn403ForCustomer` | CUSTOMER | 403 Forbidden |
| `getDashboard_shouldReturn403WhenUnauthenticated` | None | 403 Forbidden |

### 6. Hasil Verifikasi

Verifikasi penuh dijalankan melalui CI: `./mvnw clean verify -Pci`
(compile, Checkstyle, SpotBugs, seluruh test suite, dan gate JaCoCo).
Angka test per kelas adalah snapshot dan tidak di-hardcode di sini —
rujuk `docs/history/` untuk snapshot per versi.

### 7. Catatan
- `@EnableMethodSecurity` adalah replacement modern dari `@EnableGlobalMethodSecurity` di Spring Boot 3.
- Path `/api/v1/admin/**` dilindungi dua kali: di level URL pattern dan di level method dengan `@PreAuthorize`.
- CSRF diabaikan untuk `/api/**` dan `/h2-console/**` karena API menggunakan JWT (stateless); untuk request lain dipakai `CookieCsrfTokenRepository` + `XorCsrfTokenRequestAttributeHandler` (mitigasi BREACH).
- Session policy `STATELESS` memastikan Spring Security tidak membuat session di server.
- `httpBasic` dinonaktifkan karena kita menggunakan JWT Bearer token, bukan Basic Auth.