# Changelog

All notable changes to OMS are documented in this file.

## [Unreleased]

### Fixed
- **Refresh token rotasi tidak efektif (07 Nov 2026):** `JwtTokenProvider.generateToken` tidak menyertakan klaim `jti` (nonce), sehingga token yang dibuat pada detik yang sama (login → `/auth/refresh` seketika) memiliki payload identik (`sub`, `role`, `token_type`, `iat`, `exp`) dan tanda tangan HMAC deterministik — token refresh "baru" hasil rotasi bisa byte-identik dengan yang lama, membuat deteksi replay lolos dalam window detik yang sama. Ditambahkan `.id(UUID.randomUUID().toString())` sehingga setiap token (access & refresh) unik. Test baru: `JwtTokenProviderTest#generateRefreshToken_shouldBeUniqueForSameUserAndInstant`, `#generateAccessToken_shouldBeUniqueForSameUserAndInstant`, `#generateToken_shouldCarryUniqueJwtIdClaim`.

- **Base URL tidak lagi hardcoded (06 Nov 2026):** `RateLimitConstants.RATE_LIMIT_ERROR_URL` (`https://api.oms.com/errors/429`) dihapus; `RateLimitResponseBuilder` kini menyusun body 429 lewat `ProblemDetailFactory` + `ErrorCode.TOO_MANY_REQUESTS` sehingga field `type` mengikuti `app.error.base-url` (`ERROR_BASE_URL`) per environment, dan `Content-Type` dikoreksi menjadi `application/problem+json`. `OpenApiConfig` tidak lagi memakai `app.docs.base-url`/`ErrorProperties` secara ad-hoc: base URL dokumentasi dibaca dari properti baru `app.api.base-url` (`API_BASE_URL`) via `ApiProperties` + `ApiPropertiesConfig` (pola sama dengan `ErrorPropertiesConfig`). `API_BASE_URL` ditambahkan ke `application.yml` dan ketiga file `env/*.env` (dev/staging `http://localhost:8080`, prod `https://api.oms.com`). Test: `RateLimitInterceptorTest` memverifikasi `type` mengikuti base URL non-default (staging) + `Content-Type` problem+json.

- **Cache list produk tidak pernah ter-invalidasi saat create (06 Nov 2026):** `ProductService.create` hanya `@CachePut` ke `products::<id>`, sehingga `GET /api/v1/products` (`products_list::all`) tetap menyajikan daftar lama (produk baru tidak muncul) sampai TTL habis. Ditambahkan `@CacheEvict(value = CACHE_PRODUCTS_LIST, allEntries = true)` pada `create`, dan `update`/`delete` diubah dari `key = "'all'"` menjadi `allEntries = true` agar skema key list apa pun tetap ter-invalidasi.

- **Redis down tidak lagi membuat endpoint 500 (06 Nov 2026):** `CacheConfig` kini mengimplementasikan `CachingConfigurer` dan mengekspos `ResilientCacheErrorHandler` baru — kegagalan cache (GET/PUT/EVICT/CLEAR) di-log WARN lalu request dilayani dari database, alih-alih `SimpleCacheErrorHandler` yang me-rethrow dan mengubah gangguan Redis menjadi HTTP 500 (standard 4.2 — graceful degradation). Redis tetap wajib untuk refresh token & rate limiter terdistribusi (fail-closed).

- **Audit log mencatat hash code, bukan entity id (06 Nov 2026):** `AuditLogAspect.resolveEntityId` hanya mencari `getId()`, padahal seluruh `*Response` domain adalah `record` (accessor `id()`), sehingga `audit_log.entity_id` berisi hash code dan tidak bisa dikorelasikan dengan entity. Kini mendukung `getId()` maupun `id()` (`invokeAccessor`). Test baru: `AuditLogAspectTest#auditMethod_shouldRecordEntityIdFromRecordAccessor` dan `#auditMethod_shouldRecordEntityIdFromJavaBeanGetter`.

- **Order approval kini tercatat di audit log (06 Nov 2026):** `OrderService.approve` diberi `@Auditable(action = "APPROVE", entityType = OrderConstants.ENTITY_ORDER)` sehingga transisi `PENDING -> PAID` oleh admin tercatat (standard 12.11 — centralized audit log). Pesan penolakan transisi ilegal dipindah ke konstanta `OrderConstants.MSG_APPROVE_INVALID_STATUS` (sebelumnya string literal di service).

- **Anonymous requests kini `401` bukan `403` (06 Nov 2026):** request tanpa kredensial ke endpoint terproteksi sebelumnya menjawab `403 Forbidden` karena filter chain (tanpa entry point eksplisit) jatuh ke `Http403ForbiddenEntryPoint`. Ditambahkan `RestAuthenticationEntryPoint` (401) dan `RestAccessDeniedHandler` (403) yang di-wire di `SecurityFilterChainConfig`, keduanya memakai `ProblemDetailFactory` sehingga envelope RFC 7807 identik dengan handler MVC. Klien kini bisa membedakan "belum login" (401) vs "login tapi tidak punya role" (403). Test diupdate: `AdminControllerTest`, `GlobalExceptionHandlerTest`, `ProductControllerTest`, `ProductControllerUnitTest`, `OrderFlowIntegrationTest` mengharapkan `401` untuk request unauthenticated.

- **Cache TTL terbaca sebagai milidetik (06 Nov 2026):** `spring.cache.redis.time-to-live` tanpa unit di-parse Spring sebagai milidetik, sehingga TTL default `1h` menjadi 3,6 detik. Default diubah ke `1h` dengan unit eksplisit (`PT1H`) di `application.yml` dan semua file `application-{dev,staging,prod}.yml`; env files (`env/*.env`) kini mengekspor `CACHE_TTL` dengan unit. E2E script menambahkan GET list sebelum verifikasi Redis key (key sempat ter-evict oleh update sebelumnya).

- **Order owner tidak pernah tersimpan benar (06 Nov 2026):** `OrderController.createOrder` mengirim `UUID.randomUUID()` sebagai `userId` (komentar "resolved by service" tidak pernah diimplementasikan), sehingga `orders.user_id` tidak pernah sama dengan user pembuat — `GET`/`DELETE /api/v1/orders/{id}` oleh owner selalu `404` (IDOR check gagal) dan lookup email `OrderCreatedEvent` selalu fallback. Ditambahkan `OrderServiceInterface#createOrderForUser(request, username)` (service memetakan username → `User.id`); controller kini memakai principal `SecurityContextHolder`. Regression test: `OrderControllerTest#createOrder_shouldPassAuthenticatedUsernameToService` + `OrderServiceTest#createOrderForUser_shouldResolveUserIdFromUsername`. Verifikasi real-data (staging, Postgres+Redis): `orders.user_id == users.id`, GET owner `200`, approve `200/409/404/403`.

- **Redis `ObjectMapper` no longer hijacks MVC JSON (06 Nov 2026):** `RedisConfig.redisObjectMapper()` diturunkan dari `@Bean` menjadi mapper internal (`private final` field, dibuat sekali lewat `createRedisObjectMapper()`) yang dipakai bersama oleh `redisTemplate()` dan `cacheConfiguration()`. Sebelumnya, ketika Redis aktif (staging/prod), mapper ber-`DefaultTyping` itu menjadi **satu-satunya** bean `ObjectMapper`, sehingga `JacksonAutoConfiguration` back-off dan **seluruh JSON API rusak**: request body dengan tipe non-final (`BigDecimal`, `List`) ditolak `400 Bad Request` dan setiap respons dibungkus type-id (mis. `["java.util.LinkedHashMap",{...}]`). MVC kini kembali memakai `ObjectMapper` standar Spring Boot; serialisasi Redis tetap memakai default typing.
- **Redis cache kini bisa membaca tipe final & `BigDecimal` (06 Nov 2026):** `activateDefaultTyping` dinaikkan `NON_FINAL` → `EVERYTHING` (DTO `ProductResponse`/`OrderResponse` adalah `record` yang bersifat final; dengan `NON_FINAL` type-id tidak ditulis sehingga cache read gagal `need Array value to contain As.WRAPPER_ARRAY type information`), dan whitelist `BasicPolymorphicTypeValidator` ditambah `java.math.` + `java.time.` (sebelumnya `BigDecimal` non-final ditolak validator sehingga setiap `@Cacheable` product read gagal). Regression test baru di `RedisConfigTest`: `shouldNotExposeRedisObjectMapperAsSpringBean` (mapper Redis tidak diekspos sebagai bean), `shouldRoundTripBigDecimalAndNestedList`, dan `shouldRoundTripProductResponseRecord` (round-trip nilai cache riil).


### Security / Docs
- **Postman collection & environment sync (06 Nov 2026):** `OMS_API_Collection.json` kini 21 endpoint — tambah `Login (Customer)` (token disimpan ke `customerAccessToken`/`customerRefreshToken` untuk skenario RBAC) dan `Approve Order` (`POST /api/v1/orders/{id}/approve`, contoh 200/409). Contoh response `401` (GET /orders) dan `403` (GET /admin/dashboard) ditambahkan dengan envelope ProblemDetail aktual. Ketiga environment files (Dev/Staging/Prod) ditambah `tokenType`, `customerAccessToken`, `customerRefreshToken`. Dokumentasi `62-oms-034-postman-collection.md` disinkronkan (struktur folder, tabel endpoint, variabel, assertion summary, catatan 13.7).
- **Docs secret cleanup (29 Agu 2026):** semua password/secret plaintext yang tercatat di dokumentasi (password admin/customer lama, kata sandi contoh `rahasia…`, dsb.) dihapus dari dokumentasi, Postman collection/environment, dan seed script — diganti referensi secret (`secrets.ADMIN_PASSWORD`, `secrets.DB_PASSWORD`, `secrets.JWT_SECRET`, `secrets.REDIS_PASSWORD`, `secrets.CUSTOMER_PASSWORD`) dari GitHub Actions.
- Mock payment API kini didokumentasikan sebagai `http://localhost:9090/health` (image `ariefwara/mock-bi-snap`) di health-indicator, deployment guide, Dockerize doc, dan README.
- Benchmark docs (`67-oms-040-benchmark.md`, `benchmark_results.md`) memberes pula referensi, hasil, dan perintah auth yang bersandar pada secret env (`ADMIN_PASSWORD`) — lihat hasil benchmark terbaru di `benchmark_results.md`.
- `seed-data.sh` wajib menerima `ADMIN_PASSWORD` dari environment (fail-fast bila tidak di-set), tidak lagi berisi secret plaintext.
- **Repo hygiene & on-boarding (29 Agu 2026):** tambah template `.env.example` di root (contoh semua environment variable tanpa nilai rahasia — `DB_PASSWORD`, `JWT_SECRET`, `REDIS_PASSWORD`, `ADMIN_PASSWORD`, dst. kosong/placeholder); README kini memuat badge shields.io (Spring Security 6, PostgreSQL 16, Redis 7, Flyway, Docker, Kubernetes, CI/CD, OpenAPI, JUnit 5, Gitleaks).
- **Docs screening & update (29 Agu 2026):** screening menyeluruh seluruh dokumentasi `docs/current/**` vs kode aktual pasca V5-P0; dokumen usang diperbarui: `07-entity-product.md` (path by-feature + ProductRepository + QuerydslPredicateExecutor), `08-entity-order.md` (cascade=ALL, last verified, helper methods), `10-product-service.md` (caching annotations, RetryTemplate, cache key, by-feature path), `11-order-service.md` (alur OrderItemFactory + cascade, format order number 8 karakter, atomicity RetryTemplate+TransactionTemplate), `22-optimistic-locking-stock.md` (path by-feature), `architecture.md` (by-feature, v1.1, 29 Agu), `docs/index.md` (V5 baseline link, last verified), `ONBOARDING.md` (ADR-006 + V0-V5 history range).

### Added
- **Order approval flow (06 Nov 2026):** endpoint `POST /api/v1/orders/{id}/approve` (role `ADMIN` via `@PreAuthorize`) di `OrderController`; `OrderService.approve` kini melempar `InvalidStatusTransitionException` (RFC 7807 `INVALID_STATUS_TRANSITION`, HTTP 409) untuk transisi status ilegal (mis. non-PENDING); `Order.setStatus()` helper; handler baru di `BusinessExceptionHandler`; `ErrorCode.INVALID_STATUS_TRANSITION`. Test: `OrderControllerTest` (200 sukses + 409 transisi ilegal), `ApiContractTest` (endpoint approve terdaftar). Docs: `api-index.md` (endpoint + 409) & `11-order-service.md` (bagian 8 alur approve, role, caching, exception, test).
- **Payment API config hardening (06 Nov 2026):** `PaymentApiProperties.healthUrl` diinisialisasi `""` (SpotBugs EI_EXPOSE_REP2/URF clear) + `PaymentApiPropertiesConfig` untuk registrasi `@ConfigurationProperties`; `ExternalAPIHealthIndicatorErrorTest` memakai helper `buildProperties()`.
- **Repo hygiene (06 Nov 2026):** `.gitignore` kini mengabaikan `env/*.env` (file berisi secret real per environment — JANGAN commit; distribusi via `dist/oms-env-secrets.zip`).
- `ErrorProperties`, `ErrorPropertiesConfig` and `ErrorPropertiesTest` (3 tests, incl. binder-based binding verification)
- Documentation governance tooling: canonical templates (`docs/_templates/`), per-version optimization workflow (`docs/optimization-workflow.md`), local/CI docs checker (`scripts/check-docs.py`, `.github/workflows/docs-quality.yml`), and PR checklist items for docs sync
- API documentation index ([docs/current/api/api-index.md](docs/current/api/api-index.md))
- Deployment guide ([docs/current/operations/deployment-guide.md](docs/current/operations/deployment-guide.md))
- Architecture documentation ([docs/current/overview/architecture.md](docs/current/overview/architecture.md))
- CONTRIBUTING.md guide
- CHANGELOG.md tracking
- Integration tests for RateLimit and API Versioning

### Changed
- **V5-P0 (F-14):** restructure package by-layer → by-feature (ADR-006): domain kini di `com.oms.{auth,order,product,admin,audit}` + `system`/`validation`; infra lintas-fitur tetap di `config`, `exception`, `constant`, `health`, `ratelimit`; test dimirror ke struktur feature; pola exclude JaCoCo (`pom.xml`) dan SpotBugs (`config/spotbugs/spotbugs-exclude.xml`) diperbarui ke layout baru tanpa melonggarkan cakupan bug-pattern
- Spring Boot 3.2.5 → 3.2.12 (final OSS release of the 3.2.x line) — Trivy fixable CRITICAL/HIGH findings reduced 43 → 37; the remainder is deferred via `.trivyignore` (fixes only exist in Spring Boot 3.3+/3.4+ dependency lines) pending a dedicated upgrade sprint
- RFC 7807 error `type` base URL externalized via `app.error.base-url` (`ErrorProperties`) instead of a hardcoded constant in `BaseExceptionHandler`; all 4 domain handlers now constructor-inject it
- Re-enabled the two Trivy filesystem scan steps in the CI security-scan job (were commented out)
- K8s manifests restructured: flat `k8s/*.yaml` → `k8s/base/` + `k8s/overlays/{dev,staging,prod}/` + `k8s/components/datastore/` (V4-06); deploy command now `kubectl apply -k k8s/overlays/<env>`
- Docs sync (28 Agu 2026): `25-redis-cache-config.md` & `26-product-caching.md` (Redis caching Product-only, TTL global 1 jam `spring.data.redis.cache.ttl`, `products`/`products_list`), `27-async-email-notification.md` (AsyncConfig + `OrderCreatedEventListener` + `EmailService` event-based), `28-rate-limit.md` (baru — Bucket4j token bucket per endpoint `/api/**`, mode lokal vs distributed Redis), `api-index.md` & `README.md` (auth register, DTO fields, delete product 200 OK, auth flags), dan `OMS_API_Collection.json` + docs 62 (endpoint `POST /api/v1/auth/register` ditambahkan beserta test script, contoh response 201/400, dan publicEndpoints)
- Docs audit ulang (28 Agu 2026 — stale yang tersisa): `docs/index.md` (daftar lengkap dokumen fitur, `Last verified`), `03-multi-profile-configuration.md` (JWT default 1 jam bukan 24 jam, `batch_size` 50, exposure actuator `prometheus,circuitbreakers,…` + port 8081, `ddl-auto` dev `validate` via Flyway, pool staging 15), `01-inisialisasi-project.md` (`Spring Boot 3.2.12`), `09-flyway-migration.md` (V6 indexes + V7 `audit_log`; profil dev/staging/prod), `28-hikaricp-connection-pool-tuning.md` (prod `leakDetectionThreshold` = 60000ms, bukan 0/disabled), `65-oms-038-minikube.md` (key `app.rate-limit.*` aktual + path `k8s/base/`), `66-oms-039-rate-limiting.md` (`RateLimitConstants` bukan `AppConstants`, register method-level `@RateLimit`, message 429, referensi, `IpResolver`/`RateLimitResponseBuilder`), `25-redis-cache-config.md` (constructor 5-param + `enabled` per profil), `k8s/base/configmap.yaml` (key lama yang tak ter-bind `app.rate-limit.max-requests-per-minute`/`window-ms` diganti `app.rate-limit.{enabled,defaults.*}`)
- Docs sync (28 Agu 2026): `25-redis-cache-config.md` & `26-product-caching.md` (Redis caching Product-only, TTL global 1 jam `spring.data.redis.cache.ttl`, `products`/`products_list`), `27-async-email-notification.md` (AsyncConfig + `OrderCreatedEventListener` + `EmailService` event-based), `28-rate-limit.md` (baru — Bucket4j token bucket per endpoint `/api/**`, mode lokal vs distributed Redis), `api-index.md` & `README.md` (auth register, DTO fields, delete product 200 OK, auth flags), dan `OMS_API_Collection.json` + docs 62 (endpoint `POST /api/v1/auth/register` ditambahkan beserta test script, contoh response 201/400, dan publicEndpoints)

### Security
- V4-03: `StartupConfigGuard` no longer treats an empty active-profile set as dev — JWT secret validation is now enforced even when `SPRING_PROFILES_ACTIVE` is unset (prevents a prod deployment from silently booting with a weak/predictable secret)
- V4-09: removed the weak fallback JWT secret from `application.yml` (`${JWT_SECRET:default-secret-key-…}` → `${JWT_SECRET:?…}`, fail-fast on non-dev); `application-dev.yml` now supplies an explicit non-production dev secret so local dev still runs without exporting `JWT_SECRET`
- Hardened `k8s/postgres.yaml` and `k8s/redis.yaml`: pod/container securityContext with `runAsNonRoot`, `readOnlyRootFilesystem`, dropped ALL capabilities and `RuntimeDefault` seccomp (Trivy KSV-0014/KSV-0118 clear); writable scratch dirs via emptyDir; postgres switched from `subPath` to `PGDATA` subdirectory to stay writable as non-root with `fsGroup`
- Moved the `app.jwt.min-secret-length` and `app.refresh-token.expiration-days` policy keys from ConfigMap to Secret (Trivy KSV-0109 clear — flagged by key name only; values are numeric policies, not credentials)

### Code Quality (Phase V4-P1)
- V4-01: `RateLimitService` 290 baris → facade tipis (93 baris) + kolaborator `BucketFactory`, `LocalBucketStore`, `DistributedBucketStore`, dan record top-level `RateLimitResult`
- V4-02: `@Value` field injection → constructor injection di `RedisConfig`, `TracingConfig`, `RefreshTokenService`, `ExternalAPIHealthIndicator`
- V4-04: `env` dihapus dari `management.endpoints.web.exposure.include` staging (mencegah kebocoran environment variables via actuator)
- V4-06: Base ConfigMap tidak lagi hardcode `SPRING_PROFILES_ACTIVE`; profil di-inject per-environment via Kustomize overlay

### Operational Maturity (Phase V4-P2)
- V4-05: Redis Lettuce connection pooling untuk staging (max-active: 8, max-idle: 4, min-idle: 2)
- V4-07: Ingress whitelist per-environment — staging/prod di-restrict ke `10.0.0.0/8` (bukan `0.0.0.0/0`)
- V4-08: Kustomize `commonLabels` → `labels` (v5+) untuk hindari immutable selector errors
- V4-10: Audit SpotBugs exclude rules — 33 → 26 Match rules; hapus UUF_UNUSED_FIELD, UPM_UNCALLED_PRIVATE_METHOD, URF_UNREAD_FIELD

## [1.0.0] - 2026-08-10

### Added
- Complete Phase 7: DevOps, Security & Code Cleanup
  - Redundant interfaces removed (ARS-03)
  - Trusted proxy + CIDR for rate limit (KEAM-05)
  - PersistentVolumeClaim for PostgreSQL (DEVOPS-01)
  - Ingress controller (DEVOPS-02)
  - HPA, PDB (DEVOPS-03, DEVOPS-04)
  - CI/CD pipeline with GitHub Actions (DEVOPS-06)
  - Comprehensive README.md rewrite (DOK-01)
  - Checkstyle exclusions for generated sources

### Changed
- OrderService refactored, OrderItemFactory extracted (Phase 6)
- RateLimitInterceptor now validates trusted proxies
- All DevOps manifests updated with proper labels

### Fixed
- Rate limit bypass via X-Forwarded-For spoofing
- Missing persistent storage for PostgreSQL
- Various Checkstyle violations

## [0.9.0] - 2026-08-09

### Added
- Phase 5: Input validation, pagination & test coverage
- Phase 6: OrderService refactoring

### Changed
- Product search now sanitizes input against SQL injection
- Admin user listing uses pagination

### Fixed
- findAll() without pagination
- Weak admin password seed

## [0.8.0] - 2026-08-08

### Added
- Phase 1-4: Security configuration, rate limiting, code cleanup
- Phase 2: Code cleanup, configuration consolidation
- Phase 3: Data integrity, concurrency, DB performance
- Phase 4: Configuration & security hardening

### Changed
- Rate limiting consolidated to single interceptor
- JWT secrets moved to runtime configuration
- Database indexes added for performance

### Fixed
- Triple rate limiting implementations
- Race condition in stock reduction
- Hardcoded credentials in source code
- Weak database passwords
