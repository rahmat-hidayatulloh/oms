# Task 13: JWT Generation dan Validation

> **Status:** Current
> **Audience:** Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-28 (screening kode aktual `src/`, `pom.xml`)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## Tujuan
Membuat utility untuk generate, validate, dan extract claims dari JWT token untuk autentikasi berbasis token.

## Acceptance Criteria
- [x] `JwtTokenProvider` dengan method: `generateAccessToken`, `generateRefreshToken`, `validateToken`, `extractUsername`, `extractRole`, `extractTokenType`
- [x] Access token expiration: 1 hour (default)
- [x] Refresh token expiration: 7 hari (default)
- [x] Claims contain: username (subject), role, token_type, issuedAt, expiration
- [x] Secret key dari application.yml

## Detail Pekerjaan

### 1. File yang Dibuat/Diubah

```
src/main/java/com/oms/auth/security/
└── JwtTokenProvider.java               ← Utility JWT (access + refresh token)

src/main/resources/
└── application.yml                     ← jwt.secret / jwt.expiration / jwt.refresh-expiration

src/test/java/com/oms/auth/security/
└── JwtTokenProviderTest.java           ← Unit test
```

> **Catatan:** Nama kelas aktual adalah `JwtTokenProvider` (bukan `JwtUtil`).
> Kelas ini menghasilkan **dua jenis token**: access token (short-lived) dan
> refresh token (long-lived), masing-masing ditandai claim `token_type`.

### 2. JwtTokenProvider — Method

| Method | Return | Keterangan |
|---|---|---|
| `generateAccessToken(username, role)` | `String` | Membuat access token JWT (HS256), `token_type=access` |
| `generateRefreshToken(username, role)` | `String` | Membuat refresh token JWT (HS256), `token_type=refresh` |
| `validateToken(token)` | `boolean` | Validasi signature & expiration |
| `extractUsername(token)` | `String` | Ambil username (subject) dari claims |
| `extractRole(token)` | `String` | Ambil role dari claims |
| `extractTokenType(token)` | `String` | Ambil token_type (`access` / `refresh`) |

### 3. JWT Claims Structure

```json
{
  "sub": "admin",            // username (subject)
  "role": "ADMIN",           // role CUSTOMER / ADMIN
  "token_type": "access",    // access | refresh
  "iat": 1747123456,         // issued at (epoch ms)
  "exp": 1747127056          // expiration (epoch ms)
}
```

### 4. Konfigurasi

**application.yml (default):**
```yaml
jwt:
  # V4-09: no weak fallback secret here. Non-dev profiles require JWT_SECRET to
  # be provided via environment/secret manager; startup fails fast otherwise.
  # The dev profile supplies an explicit non-production test secret.
  secret: ${JWT_SECRET:?JWT_SECRET environment variable is required (min 32 chars for HS256)}
  expiration: ${JWT_EXPIRATION:3600000}            # ⬅ access token: 1 jam
  refresh-expiration: ${JWT_REFRESH_EXPIRATION:604800000}  # ⬅ refresh token: 7 hari
```

- Secret key minimal 256 bit (32 bytes) untuk HS256 — dicek di constructor.
- Base config tidak lagi menyediakan default lemah; jika `JWT_SECRET` tidak
  di-set pada profil non-dev, aplikasi gagal start (fail-fast). Profil `dev`
  memakai secret dev eksplisit (bukan untuk produksi).
- Constructor melempar `IllegalArgumentException` jika secret null, kosong, atau terlalu pendek.

### 5. Unit Test (JwtTokenProviderTest)

Pure unit test (tanpa Spring context, instantiasi manual). Coverage:
- ✅ Constructor tolak null/empty/short secret
- ✅ `generateAccessToken` menghasilkan token 3-bagian (header.payload.signature)
- ✅ `generateRefreshToken` menghasilkan token dengan `token_type=refresh`
- ✅ `validateToken` true untuk token valid
- ✅ `validateToken` false untuk token expired
- ✅ `validateToken` false untuk signature salah
- ✅ `validateToken` false untuk token malformed
- ✅ `extractUsername` kembalikan username yang benar
- ✅ `extractRole` kembalikan role ADMIN / CUSTOMER
- ✅ `extractTokenType` kembalikan `access` / `refresh`
- ✅ Claims berisi username, role, token_type, issuedAt, expiration

### 6. Hasil Verifikasi

Verifikasi penuh dijalankan melalui CI: `./mvnw clean verify -Pci`
(compile, Checkstyle, SpotBugs, seluruh test suite, dan gate JaCoCo).
Angka test per kelas adalah snapshot dan tidak di-hardcode di sini —
rujuk `docs/history/` untuk snapshot per versi.

### 7. Catatan
- `JwtTokenProvider` adalah `@Component` yang mengambil konfigurasi dari `application.yml` via `@Value`.
- Default `expiration:3600000` dan `refresh-expiration:604800000` di `@Value` digunakan sebagai fallback jika property tidak ditemukan.
- Token dibuat dengan algoritma HS256 (HMAC-SHA256) via JJWT library.
- Tidak menyimpan state (stateless) — cocok untuk arsitektur REST.
- Method `getClaims` bersifat private; ekstraksi username/role/token_type dilakukan via method publik.