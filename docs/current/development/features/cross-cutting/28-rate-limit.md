# Rate Limiting — Bucket4j (Token Bucket per Endpoint)

> **Status:** Current
> **Audience:** Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-28 (screening kode aktual `src/`, `application.yml`)
> **Source of truth:** Repository source code (`src/`), `application*.yml`, konfigurasi runtime

## Ringkasan

OMS menerapkan **rate limiting per endpoint** menggunakan token bucket Bucket4j. Implementasi berupa `HandlerInterceptor` (`RateLimitInterceptor`) yang terdaftar di `WebConfig` untuk path `/api/**`. Bucket disimpan **in-memory per instance** (`LocalBucketStore`, default) atau **di Redis lintas-pod** (`DistributedBucketStore`, opsional). Setiap respons membawa header `X-RateLimit-*`; saat kuota habis request ditolak dengan **`429 Too Many Requests`** berformat RFC 7807 ProblemDetail.

## Cara kerja saat ini

### Alur tiap request `/api/**`

```
Request /api/**
  └─ RateLimitInterceptor.preHandle (terdaftar di WebConfig utk /api/**)
      ├─ app.rate-limit.enabled=false? → lanjut (header sentinel Long.MAX_VALUE)
      ├─ resolveEndpointName(handler)
      │    ├─ @RateLimit(name=...) pada method   (didahulukan)
      │    ├─ @RateLimit(name=...) pada class
      │    └─ fallback: <SimpleClassName>.<methodName>
      ├─ IpResolver.resolve(request) → client key
      │    ├─ user terautentikasi → "user:<username>"
      │    └─ anonim → "ip:<remoteAddr>" (X-Forwarded-For dipakai bila
      │           remoteAddr ada di app.rate-limit.trusted-proxies)
      ├─ RateLimitService.tryConsume(endpoint, clientKey)
      │    ├─ DistributedBucketStore.isActive() → Redis (counter lintas pod)
      │    └─ else → LocalBucketStore (in-memory)
      ├─ set header X-RateLimit-Limit / Remaining / Reset
      └─ token habis? → log warn + RateLimitResponseBuilder → 429, request dihentikan
```

### Token bucket (`BucketFactory`)

- Bucket dibuat dari `EndpointConfig` (`app.rate-limit.endpoints.<name>`; bila tidak ada → `app.rate-limit.defaults`):
  - `capacity` — burst maksimum
  - `refillTokens` per `refillPeriod` — kecepatan isi ulang (refill *greedy*)
- Local: `Bucket.builder().addLimit(bandwidth).build()`
- Distributed: `BucketConfiguration.builder().addLimit(bandwidth).build()` (via `ProxyManager`)

### Bucket store lokal (`LocalBucketStore` — default)

- `ConcurrentHashMap` ber-bounded, key `endpointName:clientKey`.
- Idle eviction: bucket tidak diakses > 30 menit dihapus; soft cap **50.000 bucket**; sweep terjadwal tiap 5 menit + eager sweep saat melewati cap.
- Cleanup thread daemon `rate-limit-cleanup`; di-shutdown saat aplikasi mati.

### Bucket store distributed (`DistributedBucketStore` — opsional)

- Diaktifkan oleh `RateLimitServiceRedisConfig` saat **`app.rate-limit.distributed=true`** DAN **`spring.data.redis.enabled=true`**.
- Menggunakan `LettuceBasedProxyManager` dengan `ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(30 menit)`.
- Bila connection factory bukan `LettuceConnectionFactory` standalone atau inisialisasi gagal → log warning + **fallback in-memory**.

### Client key & trusted proxy (`IpResolver`)

- User terautentikasi di-rate-limit per **username** (`user:<name>`), anonim per **IP** (`ip:<addr>`).
- Header `X-Forwarded-For` **hanya dipercaya** bila koneksi langsung (remoteAddr) termasuk `app.rate-limit.trusted-proxies` (support exact IP dan CIDR, IPv4 & IPv6). Tanpa trusted proxy yang dikonfigurasi, selalu pakai `request.getRemoteAddr()`.

## Konfigurasi terkait (`application.yml`)

```yaml
app:
  rate-limit:
    enabled: true
    distributed: ${RATE_LIMIT_DISTRIBUTED:false}
    trusted-proxies: []          # IP/CIDR proxy yg diizinkan men-supply X-Forwarded-For
    defaults:
      capacity: 10
      refill-tokens: 10
      refill-period: 1m
    endpoints:
      auth-login:
        capacity: 5
        refill-tokens: 5
        refill-period: 1m
      auth:          { capacity: 10, refill-tokens: 10, refill-period: 1m }
      products:      { capacity: 30, refill-tokens: 30, refill-period: 1m }
      orders:        { capacity: 20, refill-tokens: 20, refill-period: 1m }
      admin:         { capacity: 15, refill-tokens: 15, refill-period: 1m }
```

### Limit efektif per endpoint

| Endpoint name (`@RateLimit`) | Path | Limit aktual (application.yml) |
|---|---|---|
| `auth-register` (method-level) | `POST /api/v1/auth/register` | default 10/min (tidak ada entry `auth-register` di yml) |
| `auth-login` (method-level) | `POST /api/v1/auth/login` | 5/min |
| `auth` (class-level) | `POST /auth/refresh`, `POST /auth/logout`, dsb. | 10/min |
| `products` (class-level) | seluruh `ProductController` | 30/min |
| `orders` (class-level) | seluruh `OrderController` | 20/min |
| `admin` (class-level) | seluruh `AdminController` | 15/min |
| *(tanpa `@RateLimit`)* | `GET /api/version` → `VersionController.getVersionInfo`, `GET /api/v1/auth/csrf` → `CsrfController.getCsrfToken` | default 10/min (fallback `<Class>.<method>`) |

> ⚠️ **Nuansa penting:** parameter numerik `@RateLimit(capacity, refillTokens, refillPeriodSeconds)` **tidak dipakai saat runtime** — `RateLimitInterceptor` hanya membaca `name()`, sedangkan bucket dibangun dari `application.yml` (`BucketFactory.resolveConfig` → fallback `defaults`). Contoh: `@RateLimit(name="auth-register", capacity=5, ...)` tetap berlimit **10/min** karena entry `auth-register` tidak ada di yml.

## Header respons `X-RateLimit-*`

| Header | Sumber | Contoh |
|---|---|---|
| `X-RateLimit-Limit` | `config.capacity` (atau `Long.MAX_VALUE` saat disabled) | `10` |
| `X-RateLimit-Remaining` | `probe.getRemainingTokens()` (bucket4j) | `7` |
| `X-RateLimit-Reset` | `probe.getNanosToWaitForRefill() / 1e9` (detik) | `42` |

- Saat `enabled=false`: `Limit` & `Remaining` = `9223372036854775807`, `Reset` = `0`.
- `Reset > 0` hanya muncul saat token habis (request yang diizinkan mendapati `0`).
- Header selalu diset pada respons **sebelum** handler controller berjalan (kecuali request ditolak).

## Response 429 (RFC 7807 ProblemDetail)

Dibangun `RateLimitResponseBuilder` (`RateLimitConstants.RATE_LIMIT_ERROR_URL`, `MSG_RATE_LIMIT_EXCEEDED`):

```json
{
  "type": "https://api.oms.com/errors/429",
  "title": "Too Many Requests",
  "status": 429,
  "detail": "Too many requests. Please try again later.",
  "instance": "/api/v1/products",
  "timestamp": "2026-08-10T12:00:00"
}
```

## Catatan / batasan

- Interceptor berlaku untuk **semua** `/api/**` — termasuk endpoint public (`/auth/register`, `/auth/login`, `/auth/csrf`, `/api/version`) tetap dihitung kuotanya (default 10/min atau per-endpoint).
- `/actuator/**` berada di luar pola `/api/**`, sehingga **tidak** di-rate-limit.
- Local mode = counter **per instance** (dengan replika > 1, limit efektif per-pod); distributed mode = counter **lintas pod** via Redis.
- Saat bucket idle di-evict, kuota "reset" otomatis (bucket dibuat ulang penuh pada request berikutnya); idle timeout 30 menit jauh melebihi refill period sehingga tidak mengubah limit berjalan.

## Referensi

- `src/main/java/com/oms/ratelimit/` — `RateLimit`, `RateLimitProperties`, `RateLimitInterceptor`, `RateLimitService`, `BucketFactory`, `LocalBucketStore`, `DistributedBucketStore`, `IpResolver`, `RateLimitResponseBuilder`, `RateLimitResult`
- `src/main/java/com/oms/config/WebConfig.java` — registrasi interceptor `/api/**`
- `src/main/java/com/oms/config/RateLimitServiceRedisConfig.java` — aktivasi mode distributed
- `src/main/java/com/oms/constant/RateLimitConstants.java` — nama header, URL error, pesan
- `src/main/resources/application.yml` — `app.rate-limit.*`