# OMS-039: Implementasi Rate Limiting dengan Redis (Bucket4j)

> **Status:** Current
> **Audience:** Operator, Maintainer
> **Owner:** Engineering
> **Last verified:** 2026-08-28 (screening kode aktual `src/`, `pom.xml`)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

> **Tanggal:** 7 Agustus 2026
> **Pembaruan terakhir:** 28 Agustus 2026
> **Tujuan:** Implementasi rate limiting untuk mencegah abuse API menggunakan Bucket4j dengan Redis dan in-memory support.
> **Status:** ✅ **COMPLETED**
> **Referensi:** `docs/current/overview/architecture.md`; detail teknis terkini di `docs/current/development/features/cross-cutting/28-rate-limit.md`

---

## 1. RINGKASAN

Mengimplementasikan rate limiting menggunakan Bucket4j untuk melindungi API dari abuse dan brute-force attack. Mendukung:
- **Distributed** buckets (Redis-backed via `ProxyManager`) — rate limit konsisten lintas pod
- **In-memory** fallback (ConcurrentHashMap) — per-instance rate limiting bila Redis tidak tersedia
- **Per-endpoint configuration** via application.yml
- **@RateLimit annotation** untuk deklaratif rate limiting pada controller
- **Key strategy**: IP address untuk anonymous, userId untuk authenticated
- **Response headers**: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`
- **429 response**: RFC 7807 ProblemDetail format
- **Login endpoint**: Rate limit lebih ketat (5 request/menit) untuk anti brute-force

### Acceptance Criteria

| No | Kriteria | Status |
|----|----------|:---:|
| 1 | Dependency: bucket4j-core, bucket4j-redis | ✅ **DITAMBAHKAN** |
| 2 | Rate limit configuration per endpoint di application.yml | ✅ **DIKONFIGURASI** |
| 3 | Implement @RateLimit annotation | ✅ **DIBUAT** |
| 4 | Key strategy: IP address (public) atau userId (authenticated) | ✅ **DIIMPLEMENTASIKAN** |
| 5 | Response header: X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset | ✅ **DITAMBAHKAN** |
| 6 | Response body saat limit terlampaui (429 Too Many Requests) | ✅ **RFC 7807 ProblemDetail** |
| 7 | Rate limit untuk endpoint login lebih ketat (anti brute force) | ✅ **5 request/menit** |

---

## 2. FILE YANG DIBUAT

### 2.1 RateLimitProperties.java

**Path:** `src/main/java/com/oms/ratelimit/RateLimitProperties.java`

`@ConfigurationProperties` untuk konfigurasi rate limit per endpoint dari application.yml.

```yaml
app:
  rate-limit:
    enabled: true
    distributed: ${RATE_LIMIT_DISTRIBUTED:false}   # false → in-memory fallback
    trusted-proxies: []                             # IP proxy terpercaya untuk X-Forwarded-For
    defaults:
      capacity: 10
      refill-tokens: 10
      refill-period: 1m
    endpoints:
      auth-login:
        capacity: 5
        refill-tokens: 5
        refill-period: 1m
      auth:
        capacity: 10
        refill-tokens: 10
        refill-period: 1m
      products:
        capacity: 30
        refill-tokens: 30
        refill-period: 1m
      orders:
        capacity: 20
        refill-tokens: 20
        refill-period: 1m
      admin:
        capacity: 15
        refill-tokens: 15
        refill-period: 1m
```

### 2.2 RateLimit Annotation

**Path:** `src/main/java/com/oms/ratelimit/RateLimit.java`

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    String name() default "";
    int capacity() default 10;
    int refillTokens() default 10;
    int refillPeriodSeconds() default 60;
}
```

### 2.3 RateLimitService.java

**Path:** `src/main/java/com/oms/ratelimit/RateLimitService.java`

Facade tipis (93 baris) yang mengatur mode (local vs distributed) dan mendelegasikan
konsumsi token ke store yang aktif (V4-01).

```java
@Service
public class RateLimitService {
    private final RateLimitProperties properties;
    private final LocalBucketStore localStore;
    private final DistributedBucketStore distributedStore;

    public RateLimitService(RateLimitProperties properties,
                            LocalBucketStore localStore,
                            DistributedBucketStore distributedStore) { ... }

    public RateLimitResult tryConsume(String endpointName, String clientKey) { ... }
    public void initDistributedMode(ProxyManager<byte[]> proxyManager) { ... }
    public boolean isDistributedMode() { ... }
}
```

### 2.4 BucketFactory.java

**Path:** `src/main/java/com/oms/ratelimit/BucketFactory.java`

Membangun Bucket4j `BucketConfiguration` per-endpoint dari `RateLimitProperties`.
Shared oleh `LocalBucketStore` dan `DistributedBucketStore`.

### 2.5 LocalBucketStore.java

**Path:** `src/main/java/com/oms/ratelimit/LocalBucketStore.java`

In-memory bucket store backed by `ConcurrentHashMap` dengan idle eviction (30 menit)
dan periodic cleanup sweep. Maksimum ~50.000 bucket sebelum eager sweep.

### 2.6 DistributedBucketStore.java

**Path:** `src/main/java/com/oms/ratelimit/DistributedBucketStore.java`

Redis-backed distributed bucket store via `LettuceBasedProxyManager`. Token counters
shared across all pods. Diaktivasi via `RateLimitServiceRedisConfig` saat
`app.rate-limit.distributed=true` dan Redis tersedia.

### 2.7 RateLimitResult.java

**Path:** `src/main/java/com/oms/ratelimit/RateLimitResult.java`

Record top-level hasil ekstraksi dari inner record `RateLimitService.RateLimitResult`.
Berisi `allowed`, `remainingTokens`, `limit`, `resetSeconds`.

### 2.8 RateLimitInterceptor.java

**Path:** `src/main/java/com/oms/ratelimit/RateLimitInterceptor.java`

HandlerInterceptor yang:
1. Mengecek `@RateLimit` annotation pada controller method
2. Meresolve client key (username untuk authenticated, IP untuk anonymous)
3. Memanggil `RateLimitService.tryConsume()`
4. Menambahkan response headers: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`
5. Mengembalikan 429 dengan RFC 7807 ProblemDetail jika limit terlampaui

### 2.9 WebConfig.java

**Path:** `src/main/java/com/oms/config/WebConfig.java`

Mendaftarkan `RateLimitInterceptor` untuk semua path `/api/**`.

### 2.10 IpResolver.java & RateLimitResponseBuilder.java

**Path:** `src/main/java/com/oms/ratelimit/IpResolver.java`, `src/main/java/com/oms/ratelimit/RateLimitResponseBuilder.java`

- `IpResolver` — resolve IP client dengan dukungan `X-Forwarded-For` (menghormati `app.rate-limit.trusted-proxies`)
- `RateLimitResponseBuilder` — membangun response 429 **RFC 7807 ProblemDetail** (`RATE_LIMIT_ERROR_URL`, `MSG_RATE_LIMIT_EXCEEDED`) beserta header rate-limit
---

## 3. FILE YANG DIUBAH

### 3.1 pom.xml

Menambahkan Bucket4j dependencies:

```xml
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.7.0</version>
</dependency>
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-redis</artifactId>
    <version>8.7.0</version>
</dependency>
```

### 3.2 Controllers (4 files)

Menambahkan `@RateLimit` annotation ke controllers:

| Controller | Annotation | Rate Limit |
|-----------|-----------|------------|
| AuthController | `@RateLimit(name = "auth")` | 10 req/menit |
| AuthController.login() | `@RateLimit(name = "auth-login", capacity = 5)` | **5 req/menit** |
| AuthController.register() | `@RateLimit(name = "auth-register", capacity = 5)` | **10 req/menit** (default — tidak ada entry `auth-register` di yml) |
| ProductController | `@RateLimit(name = "products")` | 30 req/menit |
| OrderController | `@RateLimit(name = "orders")` | 20 req/menit |
| AdminController | `@RateLimit(name = "admin")` | 15 req/menit |

### 3.3 application.yml

Konfigurasi per-endpoint rate limit dipusatkan di `src/main/resources/application.yml` (`app.rate-limit.*`). File profil (`application-dev/staging/prod.yml`) **tidak** meng-override rate-limit.

### 3.4 RateLimitConstants.java

**Path:** `src/main/java/com/oms/constant/RateLimitConstants.java`

Menambahkan constants: `HEADER_RATE_LIMIT_LIMIT`, `HEADER_RATE_LIMIT_REMAINING`, `HEADER_RATE_LIMIT_RESET`, `RATE_LIMIT_ERROR_URL`, dan `MSG_RATE_LIMIT_EXCEEDED`.

### 3.5 spotbugs-exclude.xml

Exclude rules untuk `EI_EXPOSE_REP` dan `EI_EXPOSE_REP2` di package `com.oms.ratelimit`.

---

## 4. ARSITEKTUR

```
Client Request
    │
    ▼
RateLimitInterceptor.preHandle()
    │
    ├── Cek @RateLimit annotation
    ├── Resolve client key (IP atau userId)
    │
    ▼
RateLimitService.tryConsume()
    │
    ├── Resolve endpoint config dari application.yml
    ├── Dapatkan/buat bucket (Bucket4j)
    ├── tryConsumeAndReturnRemaining(1)
    │
    ├── ✅ Allowed → Set headers, lanjutkan request
    └── ❌ Blocked → 429 Too Many Requests + ProblemDetail
```

### Key Strategy

| Request Type | Key | Example |
|-------------|-----|---------|
| Anonymous (public) | IP address | `ip:192.168.1.1` |
| Authenticated | Username | `user:admin` |

---

## 5. KONFIGURASI PER ENVIRONMENT

| Endpoint | Capacity | Refill | Period | Description |
|----------|:--------:|:------:|:------:|-------------|
| Default | 10 | 10 | 1m | Fallback untuk semua endpoint |
| auth-login | **5** | **5** | 1m | **Anti brute-force** |
| auth | 10 | 10 | 1m | Refresh + logout |
| products | 30 | 30 | 1m | CRUD + search |
| orders | 20 | 20 | 1m | Create + view |
| admin | 15 | 15 | 1m | Dashboard + users |

---

## 6. RESPONSE HEADERS

Setiap request yang melewati rate limit akan mendapatkan response headers:

```
X-RateLimit-Limit: 10
X-RateLimit-Remaining: 7
X-RateLimit-Reset: 23
```

### 429 Response (RFC 7807)

```json
{
    "type": "https://api.oms.com/errors/429",
    "title": "Too Many Requests",
    "status": 429,
    "detail": "Too many requests. Please try again later.",
    "instance": "/api/v1/auth/login",
    "timestamp": "2026-08-07T11:00:00"
}
```

---

## 7. DEPENDENCY VERSI

| Dependency | Version | Module |
|-----------|:-------:|--------|
| bucket4j-core | 8.7.0 | Core library dengan token bucket algorithm |

---

## 8. HASIL BUILD

```
Compilation: ✅ BUILD SUCCESS
Checkstyle:  ✅ PASS (0 violations)
SpotBugs:    ✅ PASS (0 bugs)
Tests:       ✅ 33 PASS (controller tests), 0 FAIL
```

---

## 9. KESIMPULAN

Rate limiting dengan Bucket4j telah berhasil diimplementasikan:

- ✅ **Bucket4j dependencies** — bucket4j-core 8.7.0, bucket4j-redis
- ✅ **@RateLimit annotation** — deklaratif rate limiting pada controller methods
- ✅ **Per-endpoint configuration** — via application.yml (5 endpoint groups)
- ✅ **Key strategy** — IP untuk anonymous, userId untuk authenticated
- ✅ **Response headers** — X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset
- ✅ **429 response** — RFC 7807 ProblemDetail format
- ✅ **Login rate limit** — lebih ketat (5 request/menit) untuk anti brute-force
- ✅ **Distributed mode** — Redis-backed `LettuceBasedProxyManager` saat `app.rate-limit.distributed=true`
- ✅ **In-memory fallback** — ConcurrentHashMap dengan eviction idle 30 menit bila Redis tidak tersedia
- ✅ **Clean code** — 0 Checkstyle violations, 0 SpotBugs bugs

### Konfigurasi Distributed Mode

Untuk mengaktifkan distributed rate limiting, set environment variable:

```bash
RATE_LIMIT_DISTRIBUTED=true
```

Atau konfigurasi di `application.yml`:

```yaml
app:
  rate-limit:
    distributed: true
```

Redis harus tersedia dan terkonfigurasi (`spring.data.redis.enabled=true`).

### ⚠️  Limitasi

- **Distributed mode** memerlukan Redis yang tersedia. Bila Redis tidak dapat dihubungi, secara otomatis fallback ke in-memory per-instance.
- **Key TTL** pada distributed mode diatur ke 30 menit idle (sama dengan in-memory) via `ExpirationAfterWriteStrategy`.

**Status:** ✅ **COMPLETED**

---

*Dokumen ini dibuat pada 7 Agustus 2026*
*Diperbarui pada 13 Agustus 2026 — distributed rate limiting diaktifkan*