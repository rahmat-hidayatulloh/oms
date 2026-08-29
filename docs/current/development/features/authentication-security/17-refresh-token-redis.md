# Task 17: Implementasi Refresh Token dengan Redis

> **Status:** Current
> **Audience:** Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-28 (screening kode aktual `src/`, `pom.xml`)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## Tujuan
Mengimplementasikan mekanisme refresh token menggunakan Redis untuk menyimpan refresh token dan blacklist access token saat logout.

## Acceptance Criteria
- [x] Refresh token expiration: 7 hari
- [x] POST /api/v1/auth/refresh - menerima refresh token, return access token baru dan refresh token baru (rotation)
- [x] POST /api/v1/auth/logout - validasi token_type=refresh, hapus refresh token dari Redis
- [x] Refresh token disimpan di Redis dengan userId sebagai key

## Detail Pekerjaan

### 1. File yang Dibuat/Diubah

```
src/main/java/com/oms/
├── config/
│   └── RedisConfig.java              ← Redis connection + RedisTemplate + CacheManager
├── auth/
│   ├── service/
│   │   ├── RefreshTokenServiceInterface.java ← interface
│   │   ├── RefreshTokenService.java          ← Redis operations (aktif saat Redis on)
│   │   └── NoOpRefreshTokenService.java      ← fallback tanpa Redis
│   ├── security/
│   │   └── JwtAuthenticationFilter.java      ← blacklist check
│   ├── controller/
│   │   └── AuthController.java               ← endpoint /refresh dan /logout
│   └── dto/
│       ├── LoginResponse.java                ← include refreshToken
│       └── RefreshTokenRequest.java          ← request DTO

src/test/java/com/oms/auth/service/
└── RefreshTokenServiceTest.java      ← 3 test case
```

### 2. Redis Configuration

`RedisConfig` hanya aktif saat `spring.data.redis.enabled=true` dan memakai
`BasicPolymorphicTypeValidator` (whitelist `com.oms.*`, `java.util.*`,
`java.lang.*`) untuk mencegah deserialisasi kelas sembarangan.

```java
@Configuration
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true",
    matchIfMissing = false)
public class RedisConfig {

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        config.setDatabase(redisDatabase);
        config.setPassword(redisPassword);
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper redisObjectMapper) {
        // String serializer for keys
        // GenericJackson2JsonRedisSerializer (with type validator) for values
    }
}
```

### 3. RefreshTokenService

Operasi Redis:

| Method | Key Pattern | TTL | Value |
|---|---|---|---|
| `saveRefreshToken(userId, token)` | `refresh_token:{userId}` | 7 hari | refresh token string |
| `findRefreshToken(userId)` | `refresh_token:{userId}` | - | token atau null |
| `deleteRefreshToken(userId)` | `refresh_token:{userId}` | - | delete key |
| `blacklistAccessToken(token)` | `blacklist:{token}` | 1 jam | "blacklisted" |
| `isAccessTokenBlacklisted(token)` | `blacklist:{token}` | - | true/false |

### 4. AuthController Endpoints

#### POST /api/v1/auth/login
Response sekarang include `refreshToken`:
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "tokenType": "Bearer"
}
```

#### POST /api/v1/auth/refresh
Request:
```json
{
  "refreshToken": "eyJ..."
}
```
Response:
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "tokenType": "Bearer"
}
```

#### POST /api/v1/auth/logout
Request:
```json
{
  "refreshToken": "eyJ..."
}
```
Response: `200 OK` (empty body)

### 5. JwtAuthenticationFilter - Blacklist Check

Filter sekarang mengecek apakah access token ada di blacklist Redis sebelum meng-authentikasi:

```java
if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
    if (refreshTokenService.isAccessTokenBlacklisted(jwt)) {
        log.warn("Attempted to use blacklisted token");
        SecurityContextHolder.clearContext();
        filterChain.doFilter(request, response);
        return;
    }
    // ... set authentication
}
```

### 6. Unit Test (RefreshTokenServiceTest)

3 test case:

| Test | Status | Keterangan |
|---|---|---|
| `saveAndFindRefreshToken` | ✅ | Simpan dan cari refresh token by userId |
| `deleteRefreshToken` | ✅ | Hapus refresh token by userId |
| `blacklistAndCheckAccessToken` | ✅ | Blacklist access token dan cek status |

### 7. Hasil Verifikasi

Verifikasi penuh dijalankan melalui CI: `./mvnw clean verify -Pci`
(compile, Checkstyle, SpotBugs, seluruh test suite, dan gate JaCoCo).
Angka test per kelas adalah snapshot dan tidak di-hardcode di sini —
rujuk `docs/history/` untuk snapshot per versi.

### 8. Catatan
- Refresh token disimpan di Redis dengan key `refresh_token:{userId}` dan TTL 7 hari.
- Access token blacklist disimpan di Redis dengan key `blacklist:{token}` dan TTL 1 jam (sesuai dengan access token expiry).
- **Refresh endpoint** melakukan validasi token, verifikasi token yang disimpan di Redis (replay detection), lalu menghasilkan access token dan refresh token baru (rotation). Token lama ditimpa.
- **Logout endpoint** memvalidasi `token_type=refresh`, mengekstrak user dari token, dan menghapus refresh token dari Redis.
- Jika terdeteksi replay (token masuk tidak cocok dengan yang tersimpan di Redis), semua token untuk user tersebut dicabut.
- JwtAuthenticationFilter mengecek blacklist sebelum meng-authentikasi request, dan menolak token dengan `token_type=refresh` sebagai Bearer.
- Redis menggunakan Lettuce connection factory dengan konfigurasi pool.
- Untuk dev profile, Redis dinonaktifkan (`spring.data.redis.enabled: false`), sehingga fitur refresh token hanya aktif di staging/prod.