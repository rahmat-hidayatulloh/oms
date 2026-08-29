# Task 15: JWT Authentication Filter

> **Status:** Current
> **Audience:** Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-28 (screening kode aktual `src/`, `pom.xml`)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## Tujuan
Membuat `OncePerRequestFilter` yang memvalidasi JWT token untuk setiap request yang masuk ke endpoint terproteksi (`/api/v1/**`), serta menskip endpoint publik seperti `/api/v1/auth/login` dan `/api/v1/auth/refresh`.

## Acceptance Criteria
- [x] OncePerRequestFilter implementation
- [x] Extract token dari Authorization header
- [x] Validate token dan set Authentication ke SecurityContext
- [x] Skip jika token tidak ada atau invalid
- [x] Endpoint `/api/v1/auth/login` dan `/api/v1/auth/refresh` tidak perlu token

## Detail Pekerjaan

### 1. File yang Dibuat/Diubah

```
src/main/java/com/oms/auth/security/
├── JwtAuthenticationFilter.java   ← OncePerRequestFilter
└── JwtTokenProvider.java          ← utility JWT (sudah ada)

src/main/java/com/oms/auth/service/
└── RefreshTokenService.java       ← blacklist check (Redis)

src/main/java/com/oms/config/
└── SecurityFilterChainConfig.java ← register filter (addFilterBefore)

src/test/java/com/oms/auth/security/
└── JwtAuthenticationFilterTest.java ← unit test
```

### 2. JwtAuthenticationFilter

```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenServiceInterface refreshTokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) {
        // 1. Extract token dari header Authorization
        String jwt = extractJwtFromRequest(request);

        // 2. Jika token ada dan valid
        if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
            // 2a. Tolak token yang bukan access token (mis. refresh token)
            if (!"access".equals(jwtTokenProvider.extractTokenType(jwt))) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            // 2b. Tolak access token yang sudah di-blacklist (logout)
            if (refreshTokenService.isAccessTokenBlacklisted(jwt)) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            String username = jwtTokenProvider.extractUsername(jwt);
            String role = jwtTokenProvider.extractRole(jwt);

            // 3. Set Authentication ke SecurityContext
            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + role));
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        // 4. Lanjutkan filter chain (skip jika token tidak ada / invalid)
        filterChain.doFilter(request, response);
    }
}
```

### 3. Alur Filter

```
Request → JwtAuthenticationFilter
  │
  ├─ Header "Authorization: Bearer <token>" ?
  │   ├─ Ya → validateToken()
  │   │   ├─ Valid → set Authentication → lanjut
  │   │   └─ Invalid → skip (anonymous) → lanjut
  │   └─ Tidak → skip (anonymous) → lanjut
  │
  └─ FilterChain.doFilter()
```

### 4. Endpoints yang di-skip (tidak perlu token)

Diatur di `SecurityFilterChainConfig` via `.requestMatchers(...).permitAll()`:

| Endpoint | Keterangan |
|---|---|
| `/api/v1/auth/**` | Login, register, refresh, logout, CSRF |
| `/api/version` | Version info (public) |
| `/h2-console/**` | H2 Console (dev) |
| `/actuator/health` | Health check |
| `/actuator/info` | Info |
| `/swagger-ui/**` | Swagger UI |
| `/v3/api-docs/**` | OpenAPI docs |

Filter tetap berjalan untuk endpoint di atas (karena `OncePerRequestFilter`), tapi jika token tidak ada/invalid, request tetap dilanjutkan karena `SecurityContext` tetap kosong (anonymous).

### 5. Unit Test (JwtAuthenticationFilterTest)

Mockito test, 10 test case:

| Test | Status | Keterangan |
|---|---|---|
| `shouldExtractTokenAndSetAuthentication` | ✅ | Token valid → SecurityContext terisi (username + ROLE_ADMIN) |
| `shouldSkipWhenNoAuthorizationHeader` | ✅ | Tanpa header → skip, filterChain tetap dipanggil |
| `shouldSkipWhenNotBearerToken` | ✅ | Header "Basic ..." → skip |
| `shouldSkipWhenTokenIsInvalid` | ✅ | Token invalid → skip, extractUsername tidak dipanggil |
| `shouldSkipWhenTokenValidationThrowsException` | ✅ | Exception di validateToken → skip aman |
| `shouldSetAuthenticationWithCustomerRole` | ✅ | Token CUSTOMER → ROLE_CUSTOMER |
| `shouldSkipWhenTokenIsBlacklisted` | ✅ | Access token ter-blacklist → skip |
| `shouldSkipWhenRoleIsNull` | ✅ | Token tanpa claim role → skip |
| `shouldRejectRefreshTokenAsBearer` | ✅ | Token `token_type=refresh` ditolak sebagai Bearer |
| `shouldRejectTokenWithNullTokenType` | ✅ | Token tanpa `token_type` ditolak |

### 6. Hasil Verifikasi

Verifikasi penuh dijalankan melalui CI: `./mvnw clean verify -Pci`
(compile, Checkstyle, SpotBugs, seluruh test suite, dan gate JaCoCo).
Angka test per kelas adalah snapshot dan tidak di-hardcode di sini —
rujuk `docs/history/` untuk snapshot per versi.

### 7. Catatan
- `OncePerRequestFilter` menjamin filter hanya dijalankan sekali per request (tidak di-repeat oleh forward/include).
- Filter **tidak menghentikan** request jika token tidak ada/invalid → request tetap dilanjutkan sebagai anonymous user, dan `SecurityFilterChainConfig` yang akan menolak akses ke endpoint terproteksi via `.anyRequest().authenticated()`.
- Filter menolak token dengan `token_type` selain `access` (mis. refresh token) dan access token yang sudah di-blacklist.
- Endpoint publik tetap bisa diakses tanpa token karena filter hanya men-set authentication jika token valid.