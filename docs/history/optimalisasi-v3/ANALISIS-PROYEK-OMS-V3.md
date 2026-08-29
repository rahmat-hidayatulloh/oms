# Analisis Proyek OMS — V3

> **Status:** Historical / snapshot
> **Audience:** Maintainer, Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-20 (snapshot; tidak diverifikasi ulang terhadap kode current-state)
> **Source of truth:** Kode aktual (`src/`), `pom.xml`, dan konfigurasi runtime; dokumen ini snapshot audit trail
> **Tanggal audit:** 14 Agustus 2026 — **Pembaruan terakhir:** 20 Agustus 2026
> **Status fase:** ✅ Phase V3-5 selesai — audit alignment & hardening backlog (4 discrepancy dokumentasi dikoreksi; Trivy CI re-enabled; Spring Boot 3.2.12; K8s hardened); sebelumnya re-verification menemukan 4 discrepancy (bukan 3 seperti tulisan lama), semua ditutup Phase R
> **Ruang lingkup:** 116 source Java, 63 test Java, 7 Flyway migration, 16 resource Kubernetes (render), 5 CI job, Docker multi-stage, konfigurasi multi-profile, dokumentasi teknis, dan seluruh artefak pendukung di branch `oms-optimalisasi`.
> **Baseline sebelumnya:** Semua temuan V2 (F-01 s.d. F-10) telah ditutup pada Phase 0–3 (lihat [V2 analysis](../optimalisasi-v2/ANALISIS-PROYEK-OMS-V2.md)).
> **Strategi dipilih:** **Opsi D — Gol Production-Grade** (Full Compliance ke 14 temuan V3)
> **Bukan:** pentest, vulnerability scan pihak ketiga, review infrastruktur cloud, atau validasi pada cluster Kubernetes nyata.

---

## 1. Ringkasan eksekutif

Proyek OMS telah mengalami peningkatan substansial melalui Phase 0–3 V2: token-type enforcement, refresh-token rotation/replay/revocation, secret externalization, distributed rate limiting, JaCoCo coverage gate, CI/CD security scanning (OWASP Dep-Check, SBOM, Trivy, Gitleaks, kubeconform), Kubernetes hardening (17 resource: ServiceAccount, RBAC, NetworkPolicy, ResourceQuota, LimitRange, ClusterIP, TLS/HSTS), dan kontrak API yang terverifikasi melalui `ApiContractTest`. Semua 10 temuan V2 telah ditutup.

Pemeriksaan komprehensif V3 terhadap 13 pilar standar (132 standar) + SOLID Principle mengidentifikasi **14 temuan** + **22 temuan SOLID** (across 5 prinsip, rata-rata 62%) yang perlu ditutup untuk mencapai level "production-grade". Proyek saat ini berada di level **~57% compliance** terhadap seluruh standar.

### 1.1 Strategi yang dipilih: Opsi D — Gol Production-Grade

Setelah evaluasi 5 opsi strategi (Quick Wins, Security+Resilience, Full Code, Full Compliance, Minimal Hardening), dipilih **Opsi D** dengan pertimbangan:
- **Satu push komprehensif** untuk mencapai compliance ~90%
- **Tutup semua 14 temuan + 22 SOLID findings** (P0 + P1 + P2 across 13 pilar + 5 SOLID prinsip)
- **Effort 8–12 hari** — selesai dalam 2 sprint
- **Hasil akhir**: production-grade sesungguhnya, siap untuk compliance audit

### 1.2 Status tool quality gate saat ini

| Tool | Status | Catatan |
|---|---|---|
| **Checkstyle** | ✅ Lolos (0 violations) | Threshold sesuai — FileLength=500, MethodLength=50, LineLength=120 |
| **SpotBugs** | ✅ Lolos (0 findings) | Package-level excludes untuk EI_EXPOSE di layer POJO; blanket `dto.*` exclude sudah dipersempit ke pattern `EI_EXPOSE_REP`/`EI_EXPOSE_REP2` (R-2.1 selesai) |
| **JaCoCo** | ✅ Lolos (threshold 0.80 LINE) | Koreksi (V3-5): gate aktual di `pom.xml` = **0.80**, bukan 50% seperti tulisan lama; aktual **84.03%** (889/1058 LINE, build Phase V3-5) |
| **SonarQube** | ⚠️ Belum tentu lolos | F-11 (Redis DefaultTyping) akan jadi **Security Hotspot** |

### 1.3 Temuan kunci dari scan komprehensif

1. **Redis serializer** (`RedisConfig.java:62-65, 88-90`) — `DefaultTyping.NON_FINAL` + **duplicated ObjectMapper** (2x pembuatan identik)
2. **CORS hardcoded** (`SecurityConfig.java:119-122`) — `localhost:3000` di semua environment
3. **Default dev profile** (`application.yml:10`) — `profiles.active: dev` bisa aktif di produksi
4. **Tidak ada Resilience4j** — 0 dependency, 0 CircuitBreaker/Bulkhead/Timeout di seluruh codebase
5. **Package by-layer** — 14 package (`controller/`, `service/`, `repository/`, `dto/`, `entity/`, dll.) bukan by-feature
6. **GlobalExceptionHandler** — 252 baris, 16+ handler, 1 class
7. **AppConstants** — 152 baris, ~60+ konstanta, 1 file mencakup semua domain
8. **SpotBugs exclude** — 149 baris, 26 Match rules, mengecualikan seluruh package `config.*`, `dto.*`, `entity.*`
9. **Tidak ada ADR** — folder `docs/adr/` tidak ada
10. **Tidak ada governance artifacts** — no ONBOARDING.md, CODEOWNERS, PR/Issue template
11. **LeakDetectionThreshold: 0** di `application-prod.yml:15` — connection leak tidak terdeteksi di production
12. **TracingConfig default** `localhost:9411` — tidak environment-aware

### 1.4 Inventaris scan komprehensif

| Komponen | Jumlah | Catatan |
|---|---|---|
| Java produksi | 116 | Termasuk 8 domain constant files, 5 exception handler (split), 7 service interface, adapter, ErrorProperties(+Config) — update V3-5 |
| Java test | 63 | Unit, integration, contract, migration |
| Migrasi Flyway | 7 | V1–V7 (schema + index + seed + audit_log) |
| Kubernetes manifest | 16 objek / 17 file | Koreksi-presisi (V3-5.9): 16 manifest + kustomization; blok resources aktif (profil Dev) = **14 entri file** → render **16 objek** (networkpolicy.yaml berisi 3 NetworkPolicy); `postgres.yaml`+`redis.yaml` ada di blok `# resources:` terkomentar (profil Staging/Prod — uncomment manual, **tanpa overlays/**); diverifikasi histogram `kubectl kustomize` |
| Controller | 6 | Auth, Order, Product, Admin, Version, CSRF |
| Service | 19 file | 12 implementasi + 7 interface (Admin split → Dashboard + UserMgmt) |
| Repository | 6 file | **5 Repository interface** (Order, Product, User, OrderItem, AuditLog) + 1 helper `ProductPredicates` (QueryDSL) — presisi V3-5.9; audit "5" menghitung interface saja |
| Entity | 9 file | User, Product, Order, OrderItem, Category, OrderStatus, Role, AuditLog, BaseEntity |
| DTO (record) | 13 | Request + Response + Filter — koreksi V3-5; verifikasi V3-5.9: 13 file **semuanya `public record`** (angka audit 18 Aug "12" = undercount) |
| Config class | 21 | Security, Redis, Cache, Retry, Async, Metrics, Tracing, RateLimit, Web, OpenAPI, JPA Auditing, Startup Guard, ErrorProperties, dsb. |
| Custom annotation | 4 | @RateLimit, @PasswordStrength, @ApiVersion, @Auditable — koreksi V3-5 (audit 20 Aug: 4 aktual) |
| CI/CD workflow | 5 job | Koreksi (V3-5): build, security-scan, **deploy-dev**, deploy-staging, deploy-prod |
| Docker | Multi-stage | 2 stage (builder + runtime), non-root, ZGC |
| Runtime/build | Java 21, Maven 3.9.6, Spring Boot **3.2.12** | Bump V3-5 (sebelumnya 3.2.5) |
| Application profiles | 4 | dev (H2), staging (PG), prod (PG), test |
| Production source lines | ~6,742 | Across 116 files (post-Phase V3-5) |
| Test source lines | ~7,494 | Across 63 files |

---

## 2. Metodologi dan batasan bukti

### 2.1 Metode

Audit komprehensif V3 dilakukan melalui:
1. Pembacaan seluruh source (80 Java produksi, 54 Java test — 12,446 total baris)
2. Pemeriksaan konfigurasi Maven, Spring, Docker, Kustomize, CI/CD
3. Quality checks statis:
```bash
./mvnw checkstyle:check -Dcheckstyle.skip=false -Dspotbugs.skip=true
./mvnw spotbugs:check -Dcheckstyle.skip=true -Dspotbugs.skip=false
./mvnw jacoco:report jacoco:check -Dcheckstyle.skip=true -Dspotbugs.skip=true
kubectl kustomize k8s/
```
4. Pencarian pola (`@Autowired`, TODO/FIXME, `System.out`, `DefaultTyping`, `localhost:3000`, `CircuitBreaker`)
5. Analisis struktur package (14 package), dependency, dan arsitektur
6. Audit SpotBugs exclude file (149 baris, 26 Match rules)
7. Audit JaCoCo configuration (threshold, excludes, metrics)
8. Audit CI/CD workflow (step-by-step execution flow)
9. Cross-check terhadap `docs/current/development/standards/STANDAR-DEVELOPMENT-CODE.md` (132 standar, 13 pilar)
10. Evaluasi 5 opsi strategi remediasi

### 2.2 Status verifikasi

| Area | Status | Bukti / hasil |
|---|---|---|
| Checkstyle | **Lulus** | `0 Checkstyle violations` |
| SpotBugs | **Lulus (dengan catatan)** | `BugInstance size is 0` — tetapi 26 Match rules mengecualikan banyak area |
| JaCoCo | ✅ Lolos (di atas standar) | Koreksi (V3-5): threshold aktual `pom.xml:383` = **0.80** LINE (bukan 50% seperti tulisan lama); coverage aktual **84.03%**; exclude dto/entity/config |
| Kustomize render | **Lulus dengan peringatan** | 17 resource dirender; `commonLabels` deprecated |
| `@Autowired` field injection | **Tidak ditemukan** | 0 occurrence — semua menggunakan constructor injection |
| TODO/FIXME/HACK | **Tidak ditemukan** | 0 occurrence di source produksi |
| `System.out/System.err` | **Tidak ditemukan** | 0 occurrence di source produksi — logging via SLF4J |
| Secret literal di source | **Tidak ditemukan** | 0 literal credential — placeholder di `k8s/secret.yaml` dan env files |
| `DefaultTyping.NON_FINAL` | **Ditemukan** | 2 occurrence di `RedisConfig.java` (lines 62-65, 88-90) |
| `localhost:3000` hardcoded | **Ditemukan** | 2 occurrence di `SecurityConfig.java` (lines 120, 122) |
| Resilience4j dependency | **Tidak ditemukan** | 0 dependency di `pom.xml`, 0 usage di codebase |
| CircuitBreaker/Bulkhead | **Tidak ditemukan** | 0 annotation/usage di seluruh source |
| ADR directory | **Tidak ditemukan** | Folder `docs/adr/` tidak ada |
| ONBOARDING.md | **Tidak ditemukan** | File tidak ada di root repositori |
| CODEOWNERS | **Tidak ditemukan** | File tidak ada di `.github/` |
| PR/Issue template | **Tidak ditemukan** | Tidak ada `.github/PULL_REQUEST_TEMPLATE.md` atau `.github/ISSUE_TEMPLATE/` |
| Kubernetes client dry-run | **Tidak terverifikasi** | Membutuhkan cluster lokal |
| Full `mvn verify` | **Belum final** | Docker tidak tersedia di environment audit ini |

### 2.3 Inventaris yang terverifikasi

| Komponen | Jumlah | Catatan |
|---|---|---|
| Java produksi | 116 | Termasuk domain constants, exception handler split, service interfaces, adapter (verifikasi V3-5) |
| Java test | 63 | Unit, integration, contract, migration |
| Migrasi Flyway | 7 | V1–V7 (schema + index + seed + audit_log) |
| Kubernetes manifest | 16 objek | Render base (V3-5.9): 14 entri file aktif → 16 objek (3 NetworkPolicy dalam 1 file); postgres/redis di blok komentar staging/prod, tanpa overlays/ |
| Controller | 6 | Auth, Order, Product, Admin, Version, CSRF |
| Service | 19 file | 12 implementasi + 7 interface |
| Repository | 6 file | 5 Repository interface + ProductPredicates (presisi V3-5.9) |
| Entity | 9 file | User, Product, Order, OrderItem, Category, OrderStatus, Role, AuditLog, BaseEntity |
| DTO (record) | 13 | Semua 13 file adalah `public record` (verifikasi V3-5.9; audit "12" undercount) |
| Config class | 21 | Security, Redis, Cache, Retry, Async, Metrics, Tracing, RateLimit, Web, OpenAPI, JPA Auditing, Startup Guard, etc. |
| Custom annotation | 4 | @RateLimit, @PasswordStrength, @ApiVersion, @Auditable (koreksi V3-5) |
| CI/CD workflow | 5 job | build, security-scan, deploy-dev, deploy-staging, deploy-prod |
| Docker | Multi-stage | 2 stage (builder + runtime), non-root, ZGC |
| Runtime/build | Java 21, Maven 3.9.6, Spring Boot 3.2.12 | |

---

## 3. Matriks kesesuaian 13 pilar standar (V3)

Status: **Sesuai** = implementasi memenuhi standar; **Sebagian** = ada fondasi tetapi ada gap; **Belum terbukti** = bukti tidak ditemukan; **Perlu optimalisasi** = berfungsi tetapi ada pendekatan lebih baik sesuai standar. Standar acuan: `docs/current/development/standards/STANDAR-DEVELOPMENT-CODE.md`.

| Pilar | Status V3 | Bukti positif | Gap / optimalisasi |
|---|---|---|---|
| **1. Clean Code** | **Sebagian** | Constructor injection ✅ (0 `@Autowired`), DTO `record` ✅, konstanta `AppConstants` ✅, Checkstyle 0 violation ✅, DRY ✅, SRP ✅, no magic numbers ✅, modern Java 21 records ✅ | **SOLID Analysis (Section 12):** SRP 6/10, OCP 6/10 — `AppConstants` 152 baris satu file; `GlobalExceptionHandler` 252 baris; duplicated ObjectMapper di `RedisConfig`; `OrderController` IDOR logic berulang; 5 service tanpa interface (ISP); 2 controller akses repository langsung (DIP). Package by-layer bukan by-feature |
| **2. Optimal** | **Sebagian** | `@EntityGraph`/JOIN FETCH Order ✅, QueryDSL ✅, pagination ✅, HikariCP ✅, resource limits ✅, distributed rate limiting ✅, `@Version` optimistic lock ✅, retry backoff ✅, `@Async` email pool ✅ | **Tidak ada Circuit Breaker/Bulkhead** untuk Payment API; `findAll()` tanpa pagination; admin dashboard 4 query; `LeakDetectionThreshold` 0 di prod |
| **3. Reusable** | **Sesuai sebagian** | `RefreshTokenServiceInterface` + NoOp impl ✅, `OrderItemFactory` ✅, `@RateLimit` annotation ✅, `AppConstants` ✅, QueryDSL `ProductPredicates` ✅ | Belum ada shared library module; prefix config tidak konsisten; filter predicates hanya untuk Product |
| **4. Environment-aware** | **Sebagian** | Multi-profile ✅, env vars ✅, `@ConditionalOnProperty` ✅, `StartupConfigGuard` ✅ | **CORS hardcoded localhost**; `TracingConfig` default `localhost:9411`; default profile `dev` di `application.yml`; ConfigMap K8s pakai `SPRING_PROFILES_ACTIVE: "dev"` |
| **5. Secure** | **Sebagian tinggi** | Token-type enforcement ✅, refresh rotation ✅, `@Valid` ✅, RBAC ✅, parameter binding ✅, BCrypt ✅, secret externalization ✅, startup guard ✅, `readOnlyRootFilesystem` ✅, NetworkPolicy ✅ | **Redis `DefaultTyping.NON_FINAL`** (5.9); CORS permissive; no dependency lockfile; no CORS env overlay |
| **6. Testable** | **Sebagian tinggi** | 54 test files ✅, JUnit 5 + Mockito + AssertJ ✅, Testcontainers ✅, `ApiContractTest` ✅, N+1 test ✅, concurrency test ✅ | ~~Coverage threshold 0.00~~ (koreksi V3-5: snapshot 14 Aug usang — gate aktual sejak Phase R = **0.80**, coverage **84.03%**; kini 63 test files); tidak ada `@WebMvcTest` terisolasi; tidak ada load test; tidak ada mutation testing |
| **7. Version Control** | **Sesuai** | `.gitignore` ✅, Conventional Commits ✅, Maven wrapper ✅, CI trigger PR/main ✅, `CONTRIBUTING.md` ✅, `CHANGELOG.md` ✅ | Tidak ada branch protection proof; tidak ada PR template; tidak ada CODEOWNERS; tidak ada GPG signing |
| **8. Documentation** | **Sebagian** | README ✅, API index + OpenAPI ✅, architecture.md ✅, feature docs ✅, `ApiContractTest` ✅ | **Tidak ada ADR** (8.3); tidak ada `ONBOARDING.md` (8.6); tidak ada API changelog |
| **9. CI/CD & Observability** | **Sebagian** | Build + test ✅, Checkstyle ✅, SpotBugs ✅, JaCoCo ✅, Docker build/push ✅, SCA ✅, SBOM ✅, Trivy ✅, Gitleaks ✅, kubeconform ✅, Actuator ✅, Micrometer tracing ✅, MDC logging ✅, business metrics ✅ | **CI profile skip Checkstyle/SpotBugs**; tidak ada staging smoke test; tidak ada SLI/SLO; tracing default `localhost` |
| **10. Collaboration** | **Sebagian** | `CONTRIBUTING.md` ✅, documentation-driven ✅, commit message terstruktur ✅ | Tidak ada PR template; tidak ada issue template; tidak ada CODEOWNERS; tidak ada code review checklist |
| **11. Database** | **Sebagian tinggi** | Flyway 6 migration ✅, `ddl-auto=validate` ✅, seed data ✅, index ✅, soft delete ✅, N+1 mitigation ✅, batch insert ✅ | Tidak ada `EXPLAIN ANALYZE` evidence; tidak ada backup procedure; tidak ada PII masking; `LeakDetectionThreshold: 0` di prod |
| **12. Enterprise** | **Sebagian** | HPA ✅, PDB ✅, rolling update ✅, RBAC ✅, NetworkPolicy ✅, ResourceQuota ✅, LimitRange ✅, ServiceAccount ✅, security context ✅, non-root ✅, multi-stage Docker ✅, health probes ✅, SonarQube config ✅ | Tidak ada DRP; tidak ada SLA/SLO; tidak ada post-mortem template; tidak ada SBOM publish; tidak ada release strategy |
| **13. Additional** | **Sebagian** | API versioning `/api/v1` ✅, rate limiting ✅, dependency management ✅, Swagger/OpenAPI ✅ | CORS production perlu overlay; tidak ada lockfile; tidak ada i18n; tidak ada a11y |

---

## 4. Temuan terprioritas

### P0 — harus diselesaikan sebelum klaim production-grade

#### F-11 — Redis serializer menggunakan `DefaultTyping.NON_FINAL` — risiko deserialisasi aman

**Severity:** High  
**Standar terkait:** 5.9 Secure Serialization, 5.3 Injection Prevention

`RedisConfig.java:62-65` mengaktifkan `mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL)`. Pola ini memungkinkan Redis menyimpan type information di samping data, yang berarti objek yang di-deserialize dapat mencakup arbitrary types. Jika attacker memperoleh akses write ke Redis (SSRF, credential leak, atau akses jaringan), ia dapat menulis payload terdesain yang akan di-deserialize ke gadget chain berbahaya.

**Dampak:** Remote Code Execution (RCE) via Redis cache poisoning. Serangan sangat bergantung pada classpath (gadget chains), tetapi dengan Jackson + Spring Boot, beberapa gadget chain telah didokumentasi.

**Bukti:** `RedisConfig.java:62-65` — `activateDefaultTyping(NON_FINAL)` dan `RedisConfig.java:88-90` — duplikasi di `cacheConfiguration()`.

**Remediasi:**
1. Ganti `DefaultTyping.NON_FINAL` dengan `BasicPolymorphicTypeValidator` yang membatasi ke package yang aman.
2. Atau gunakan `StringRedisSerializer` saja — untuk refresh token dan blacklist hanya String diperlukan.
3. Evaluasi apakah type information benar-benar diperlukan.

---

#### F-12 — CORS hardcoded ke localhost — tidak environment-aware

**Severity:** High  
**Standar terkait:** 4.1 Externalize Configuration, 4.6 12-Factor App, 5.11 CORS Configuration, 13.5 CORS Policy

`SecurityConfig.java:119-122` menghardcode CORS origins ke `localhost:3000`. Saat deployment ke staging/production, frontend tidak akan bisa mengakses API karena origin tidak cocok.

**Dampak:** Developer terpaksa mematikan CORS atau meng-hardcode production URL, yang merupakan anti-pattern keamanan.

**Remediasi:** Externalize CORS origins via `@ConfigurationProperties` dengan default development-only. Tambahkan overlay per profile di `application-staging.yml` dan `application-prod.yml`.

---

#### F-22 — `spring.profiles.active: dev` di `application.yml` — berisiko dev profile aktif di produksi

**Severity:** High  
**Standar terkait:** 4.5 Environment Detection, 4.6 12-Factor App

`application.yml:10` menetapkan `profiles.active: dev` sebagai default. Jika `SPRING_PROFILES_ACTIVE` tidak diatur di produksi, aplikasi berjalan dengan profile dev (H2 database, debug logging, H2 console).

**Dampak:** Production bisa berjalan dengan H2 in-memory database tanpa persistensi data.

**Remediasi:** Hapus default active profile dari `application.yml`. Wajibkan environment variable di CI/CD dan K8s manifest.

---

### P1 — diselesaikan pada sprint optimalisasi berikutnya

#### F-13 — Tidak ada resilience pattern untuk external dependency

**Severity:** High  
**Standar terkait:** 2.10 Resource Throttling, 12.7 High Availability, 4.2 Graceful Degradation

`ExternalAPIHealthIndicator` melakukan HTTP call ke Payment API. `EmailService` async tanpa fallback. Tidak ada Circuit Breaker, Bulkhead, atau Retry terpisah untuk external calls.

**Dampak:** Cascading failure jika Payment API down; thread pool email bisa habis tanpa fallback.

**Remediasi:** Tambahkan Resilience4j Circuit Breaker untuk Payment API, Bulkhead untuk thread pool isolasi, dan `@Retry` untuk transient failures.

---

#### F-14 — Package structure by-layer, bukan by-feature

**Severity:** Medium  
**Standar terkait:** 1.8 SRP, 1.12 Package by Feature

Struktur package saat ini by-layer (`controller/`, `service/`, `repository/`, `dto/`, `entity/`). Saat domain bertambah, file tersebar di 5+ package berbeda.

**Remediasi:** Refactor bertahap ke `domain/order/`, `domain/product/`, `domain/auth/`, `domain/admin/`, `shared/`.

---

#### F-15 — CI profile skip Checkstyle/SpotBugs — quality gate tidak enforced

**Severity:** Medium  
**Standar terkait:** 9.1 Continuous Integration, 12.13 Automated Enforcement

`pom.xml` profile `ci` mengatur `<checkstyle.skip>true</checkstyle.skip>` dan `<spotbugs.skip>true</spotbugs.skip>`. Meskipun step terpisah ada di CI, pipeline test step sudah passing tanpa quality gates.

**Remediasi:** Jalankan `./mvnw clean verify -Pci` dalam satu step atau pastikan step quality gates memiliki `continue-on-error: false`.

---

#### F-16 — `AppConstants` terlalu besar dan monolithic

**Severity:** Medium  
**Standar terkait:** 1.6 DRY, 1.8 SRP, 3.3 Generic Utilities

`AppConstants.java` memiliki 152 baris dengan ~60+ konstanta yang mencakup semua domain.

**Remediasi:** Pecah menjadi per-domain constants: `SecurityConstants`, `ErrorConstants`, `CacheConstants`, `RateLimitConstants`, `OrderConstants`.

---

#### F-17 — `GlobalExceptionHandler` terlalu besar

**Severity:** Medium  
**Standar terkait:** 1.4 Small Functions & Classes, 1.8 SRP

252 baris, 16+ exception handler dalam satu class.

**Remediasi:** Refactor ke hierarchical handler: `BaseExceptionHandler`, `SecurityExceptionHandler`, `ValidationExceptionHandler`, `BusinessExceptionHandler`, `InfrastructureExceptionHandler`.

---

### P2 — perbaikan reliability, governance, dan enterprise readiness

#### F-18 — Duplicated ObjectMapper di RedisConfig

**Severity:** Low  
**Standar terkait:** 1.6 DRY

`RedisConfig.java` membuat ObjectMapper 2 kali dengan konfigurasi identik.

**Remediasi:** Ekstrak ke `@Bean` shared ObjectMapper.

---

#### F-19 — Tidak ada ADR (Architecture Decision Record)

**Severity:** Medium  
**Standar terkait:** 8.3 ADR, 12.1 Architecture Framework

Keputusan arsitektur kritis (JWT, H2 dev/PG prod, Bucket4j, QueryDSL, Spring Event) tidak didokumentasikan.

**Remediasi:** Buat folder `docs/adr/` dan dokumentasikan 5+ keputusan kunci.

---

#### F-20 — Missing PR template, issue template, dan CODEOWNERS

**Severity:** Medium  
**Standar terkait:** 7.4 Pull/Merge Request, 10.1 Code Review Checklist

Tidak ada `.github/PULL_REQUEST_TEMPLATE.md`, `.github/ISSUE_TEMPLATE/`, atau `.github/CODEOWNERS`.

**Remediasi:** Buat template dengan checklist: code review, test, docs, security.

---

#### F-21 — Missing `ONBOARDING.md`

**Severity:** Low  
**Standar terkait:** 8.6 Onboarding Guide

Tidak ada panduan onboarding untuk anggota baru.

**Remediasi:** Buat `ONBOARDING.md` di root repositori.

---

#### F-23 — Missing dependency lockfile

**Severity:** Low  
**Standar terkait:** 13.6 Dependency Management, 5.5 Dependency Scanning

Maven tidak menggunakan lockfile. Build reproducibility bergantung pada `pom.xml` tetapi transitive dependencies bisa berubah.

**Remediasi:** Pertimbangkan `versions-maven-plugin` atau `flatten-maven-plugin`.

---

#### F-24 — Audit log hanya berbasis log file, tidak immutable

**Severity:** Low  
**Standar terkait:** 12.11 Audit Log Terpusat

Audit logging via SLF4J. Tidak ada mekanisme immutable audit trail.

**Remediasi:** Pertimbangkan audit log table di database atau `@Auditable` annotation.

---

## 5. Bukti praktik yang sudah baik

Audit ini mencatat pencapaian signifikan dari Phase 0–3 V2 yang harus dipertahankan:

### 5.1 Clean Code
1. **Zero `@Autowired` field injection** — semua dependency di-inject via constructor.
2. **DTO sebagai `record`** — immutable, thread-safe, compact.
3. **0 Checkstyle violations** — formatting konsisten.
4. **0 TODO/FIXME/HACK** — kode bersih.
5. **0 `System.out/System.err`** — semua output via SLF4J.
6. **`AppConstants` centralized** — no magic strings/numbers.
7. **Self-documenting names** — `getActiveUsers()` bukan `getData()`.
8. **Javadoc lengkap** di semua public classes.
9. **Modern Java** — records, `List.of()`, `.toList()`.

### 5.2 Security
10. **Token-type enforcement** — refresh token ditolak di filter (test 10/10 pass).
11. **Refresh token rotation** — replay detection + atomik save/delete.
12. **Zero secret literals** — placeholder di manifest, env vars everywhere.
13. **Startup guard** — weak password detection.
14. **`readOnlyRootFilesystem` + `seccompProfile`**.
15. **IDOR protection** — ownership check di OrderController.
16. **`@Valid` everywhere** — input validation.
17. **CSRF defense-in-depth** — `XorCsrfTokenRequestAttributeHandler`.
18. **Bearer prefix extraction** — safe string handling.

### 5.3 Testing
19. **54 test files** — comprehensive coverage.
20. **Testcontainers** — PostgreSQL 16 + Redis 7.
21. **`ApiContractTest`** — prevents documentation drift.
22. **Concurrency tests** — stock and product concurrent.
23. **N+1 test** — OrderServiceNPlusOneTest.
24. **Optimistic lock retry** — tested.

### 5.4 Infrastructure
25. **17 Kustomize resources** — production-ready manifest.
26. **NetworkPolicy** — default-deny + explicit allow.
27. **HPA** — CPU/memory autoscaling (2–10 pods).
28. **PDB** — `minAvailable: 1`.
29. **ResourceQuota + LimitRange**.
30. **ServiceAccount** — dedicated, no auto-mount.
31. **TLS + HSTS** — ingress SSL.
32. **Multi-stage Docker** — non-root, ZGC.

### 5.5 CI/CD
33. **OWASP Dependency-Check**.
34. **CycloneDX SBOM**.
35. **Trivy filesystem scan** — *(koreksi V3-5: sempat dinonaktifkan oleh commit `1046344` "disable trivy", 17 Aug, pasca-Phase R; di-re-enable — lihat R-CI.2 di Section 13)*.
36. **Gitleaks** secret scanning.
37. **kubeconform** manifest validation.
38. **JaCoCo** coverage.
39. **Action version pinning**.

### 5.6 Observability
40. **Structured MDC logging** — traceId, userId, method, path.
41. **Micrometer business metrics** — counters, timers.
42. **Actuator endpoints** — health, info, metrics, prometheus.
43. **Distributed tracing** — Micrometer + OpenTelemetry + Zipkin.
44. **Separate management port**.

### 5.7 Architecture
45. **Event-driven async** — `ApplicationEventPublisher` + `@Async`.
46. **Strategy pattern** — `RefreshTokenServiceInterface` + Redis/NoOp.
47. **Factory pattern** — `OrderItemFactory`.
48. **QueryDSL** — dynamic filtering, sorting, pagination.
49. **Optimistic locking** — `@Version` + retry.
50. **Cache-aside** — Spring Cache + Redis.

### 5.8 SOLID Principle (partial compliance — avg 62%)
51. **DIP via interfaces** — `RefreshTokenServiceInterface`, all Spring Data repositories, `HealthIndicator` implementations.
52. **OCP via Strategy** — `RefreshTokenServiceInterface` with Redis and NoOp implementations.
53. **OCP via SPI** — Spring Boot `HealthIndicator` auto-discovery for 3 health indicators.
54. **SRP well-applied** — `OrderItemFactory`, `EmailService`, `JwtTokenProvider`, `RegisterService`, `LoggingFilter` all have single clear responsibility.
55. **LSP maintained** — Exception hierarchy consistent; all repositories substitutable; DTO records immutable.

---

## 6. Strategi remediasi yang dipilih: Opsi D — Gol Production-Grade

> **Opsi D** dipilih setelah evaluasi 5 opsi. Opsi D menawarkan balance terbaik antara coverage komprehensif (tutup 14 temuan + 22 SOLID findings) dan effort yang terukur (8–12 hari). Satu push untuk mencapai production-grade sesungguhnya.

### Perbandingan 5 opsi strategi

| Opsi | Nama | Temuan ditutup | Estimasi effort | Compliance akhir | Risiko |
|:---:|---|---|:---:|:---:|---|
| A | Quick Wins | F-11, F-12, F-22 (3 P0) | 1–2 hari | ~62% | Masih banyak gap |
| B | Security + Resilience | F-11, F-12, F-22, F-13, F-23 (5) | 3–5 hari | ~70% | Struktur/governance tertinggal |
| C | Full Code Quality | F-11–F-18, F-22 (9) | 5–8 hari | ~82% | Governance tertinggal |
| **D** | **Full Compliance** | **Semua 14 temuan + 22 SOLID** | **8–12 hari** | **~90% + SOLID ~82%** | **Effort terbesar; hasil terbaik** |
| E | Minimal Hardening | F-11, F-12 (2 P0) | 0.5 hari | ~59% | F-22 masih open |

**Alasan Opsi D dipilih:**
- Tutup semua 14 temuan (3 P0 + 5 P1 + 6 P2) + 22 SOLID findings dalam 2 sprint
- Hasil akhir ~90% compliance + SOLID ~82% — siap untuk compliance audit
- Satu push komprehensif, mengurangi context-switching
- ADR + governance artifacts + code quality + SOLID compliance sekaligus

---

### Fase V3-0 — Security Hardening (0–3 hari)

**Tujuan:** Tutup P0 (F-11, F-12, F-22) + F-18 (DRY fix) + SOLID S-3/partial, S-4, O-2

| # | Temuan | File yang diubah | Deskripsi perubahan |
|---|---|---|---|
| 1 | F-11 | `RedisConfig.java` | Ganti `DefaultTyping.NON_FINAL` dengan `BasicPolymorphicTypeValidator` yang whitelist hanya `com.oms.*`. |
| 2 | F-12, SOLID S-3, O-2 | `SecurityConfig.java`, `CorsProperties.java` (new), `application-*.yml` | Buat `@ConfigurationProperties CorsProperties` untuk CORS origins. Ekstrak dari SecurityConfig (S-3: mengurangi responsibilities) sehingga menambah environment tanpa modifikasi code (O-2). Staging: `https://staging.oms.com`. Prod: `https://oms.com`. Dev: `localhost:3000`. |
| 3 | F-22 | `application.yml` | Hapus `spring.profiles.active: dev`. Wajibkan `SPRING_PROFILES_ACTIVE` via env var. |
| 4 | F-18, SOLID S-4 | `RedisConfig.java` | Ekstrak shared `@Bean ObjectMapper redisObjectMapper()` — hapus duplikasi, mengurangi tanggung jawab RedisConfig (S-4). |

**Test:** `RedisConfigTest`, `CorsConfigTest`, `SecurityConfigCorsTest`, `StartupConfigGuardTest`
**Validation:**
```bash
grep -n "enableDefaultTyping" src/main/java/com/oms/config/RedisConfig.java  # should return nothing (varian unsafe tanpa validator)
# (koreksi V3-5: command lama `grep DefaultTyping` memang mengembalikan 3 baris — javadoc, nama API `activateDefaultTyping()`, enum NON_FINAL — semuanya aman karena BasicPolymorphicTypeValidator whitelist; rekonsiliasi dari V3-0 §6 note 1)
grep -rn "localhost:3000" src/main/java/  # hanya default CorsProperties + javadoc (F-12); SecurityConfig sudah di-split jadi SecurityFilterChainConfig/SecurityCorsConfig (S-3/I-4)
grep -n "profiles.active" src/main/resources/application.yml  # should return nothing
```

**Exit criteria:** Redis serializer aman (whitelist only); CORS per profile; tidak ada default dev profile; ObjectMapper tidak duplikat.

### Fase V3-1 — Resilience & Reliability (minggu 1)

**Tujuan:** Tutup F-13 (Resilience4j) + F-24 (audit log)

| # | Temuan | File yang diubah | Deskripsi perubahan |
|---|---|---|---|
| 1 | F-13 | `pom.xml` | Tambahkan `resilience4j-spring-boot3`, `resilience4j-micrometer`. |
| 2 | F-13 | `CircuitBreakerConfig.java` (new) | Config Resilience4j CB untuk Payment API — `failureRateThreshold=50%`, `waitDurationInOpenState=10s`. |
| 3 | F-13 | `ExternalAPIHealthIndicator.java` | Tambahkan `@CircuitBreaker(name="paymentApi")` + `@Retry(name="paymentApi")`. |
| 4 | F-13 | `application.yml` | Tambahkan config `resilience4j.circuitbreaker.instances.paymentApi.*`. |
| 5 | F-13 | `application-prod.yml` | Aktifkan `leak-detection-threshold: 60000` (sebelumnya 0). |
| 6 | F-24 | Flyway `V7__create_audit_log.sql` | Tabel `audit_log(id, timestamp, user_id, action, entity, entity_id, ip, details)`. |
| 7 | F-24 | `AuditLogService.java` (new), `@Auditable` annotation (new) | Audit trail ke database. |

**Exit criteria:** CB active + visible di Actuator; leak detection aktif; audit log table tersedia.

---

### Fase V3-2 — Clean Code, SOLID Compliance & Structure (minggu 2)

**Tujuan:** Tutup F-14, F-16, F-17 + SOLID O-1, S-3/full, S-5, S-6, S-8, I-1, I-2, I-4, D-1–D-5

| # | Temuan | File yang diubah | Deskripsi perubahan |
|---|---|---|---|
| 1 | F-14 | Package structure | Refactor ke `domain/order/`, `domain/product/`, `domain/auth/`, `domain/admin/`, `shared/`. |
| 2 | F-16, SOLID I-2, S-6 | 5 constants files (new) | Pecah `AppConstants` → `SecurityConstants`, `ErrorConstants`, `CacheConstants`, `RateLimitConstants`, `OrderConstants`. Buat facade delegating untuk backward compat. |
| 3 | F-17, SOLID O-1, S-5 | 5 exception handler files (new) | Refactor → `BaseExceptionHandler`, `SecurityExceptionHandler`, `ValidationExceptionHandler`, `BusinessExceptionHandler`, `InfrastructureExceptionHandler`. Extensible tanpa modifikasi base (O-1). |
| 4 | SOLID I-1, D-3 | `OrderServiceInterface.java` (new), `OrderService.java` | Buat interface — controller depend abstraksi bukan concrete (DIP). |
| 5 | SOLID I-1, D-4 | `ProductServiceInterface.java` (new), `ProductService.java` | Buat interface — controller depend abstraksi (DIP). |
| 6 | SOLID I-1, D-5, S-8 | `AdminServiceInterface.java` (new), refactor `AdminService.java` | Buat interface + split: `AdminDashboardService` (statistik) + `AdminUserManagementService` (CRUD user). |
| 7 | SOLID I-1 | `RegisterServiceInterface.java` (new), `RegisterService.java` | Buat interface. |
| 8 | SOLID I-1 | `EmailServiceInterface.java` (new), `EmailService.java` | Buat interface. |
| 9 | SOLID D-1, D-2 | `AuthController.java`, `OrderController.java`, `OrderService.java` | Pindahkan IDOR check dari controller ke service — controller hanya handle HTTP, service handle authorization. Hapus direct `UserRepository` access di controller. |
| 10 | SOLID S-3, I-4 | `SecurityConfig.java` → split | Refactor ke: `SecurityFilterChainConfig.java`, `SecurityCorsConfig.java` (menggunakan CorsProperties dari V3-0), `SecurityPasswordEncoderConfig.java`. |

**Test:** Unit test untuk semua interface; test refactor IDOR; test exception handler hierarchy.
**Exit criteria:** Domain packages; constants < 30 baris; handler < 80 baris; semua service punya interface; controller tidak akses repository langsung; SecurityConfig < 50 baris.

---

### Fase V3-3 — CI/CD, Governance & SOLID Refactoring (minggu 3)

**Tujuan:** Tutup F-15, F-19, F-20, F-21 + SOLID S-7

| # | Temuan | File yang diubah | Deskripsi perubahan |
|---|---|---|---|
| 1 | F-15 | `ci-cd.yml` | Gabungkan quality gates ke step `verify` — `continue-on-error: false`. |
| 2 | F-19 | `docs/adr/001-*.md` s.d. `005-*.md` | 5 ADR: JWT, DB per profile, Bucket4j, QueryDSL, Spring Event. |
| 3 | F-20 | `.github/PULL_REQUEST_TEMPLATE.md`, `.github/ISSUE_TEMPLATE/`, `.github/CODEOWNERS` | PR/Issue templates + CODEOWNERS. |
| 4 | F-21 | `ONBOARDING.md` | Panduan onboarding: setup, run, architecture, contributing. |
| 5 | SOLID S-7 | `RateLimitInterceptor.java` → split | Ekstrak `IpResolver.java` (IP/CIDR validation + RFC 7807) dan `RateLimitResponseBuilder.java` (HTTP response construction). RateLimitInterceptor hanya orchestrasi. |

**Exit criteria:** CI enforce gates; ADR tersedia; templates tersedia; RateLimitInterceptor < 60 baris.

---

### Fase V3-4 — Enterprise Readiness & Final SOLID (minggu 4)

**Tujuan:** Tutup F-23 + Enterprise documentation + SOLID L-2

| # | Temuan | File yang diubah | Deskripsi perubahan |
|---|---|---|---|
| 1 | F-23 | `pom.xml` | Tambahkan `versions-maven-plugin`. |
| 2 | Enterprise | `docs/ops/SLO.md` | SLI/SLO: 99.9% availability, p99 < 500ms. |
| 3 | Enterprise | `docs/ops/DRP.md` | Disaster Recovery Plan. |
| 4 | Enterprise | `docs/ops/IncidentResponse.md` | Incident response runbook. |
| 5 | Enterprise | SpotBugs exclude | Kurangi 26 Match rules — hapus package-level excludes. |
| 6 | SOLID L-2 | `User.java`, `UserDetailsAdapter.java` (new), `CustomUserDetailsService.java` | Evaluate adapter pattern: buat `UserDetailsAdapter` yang wrap `User` entity — pisahkan representasi DB dari security principal. |

**Exit criteria:** SLI/SLO terukur; DRP tersedia; SpotBugs exclude reduced; User entity tidak langsung implement UserDetails (atau dokumentasi kenapa dipertahankan).

---

## 7. Daftar validasi ulang setelah remediasi

```bash
# Full verification
./mvnw clean verify -Dspring.profiles.active=dev

# Checkstyle
./mvnw checkstyle:check -Dcheckstyle.skip=false -Dspotbugs.skip=true

# SpotBugs
./mvnw spotbugs:check -Dcheckstyle.skip=true -Dspotbugs.skip=false

# JaCoCo coverage
./mvnw jacoco:report jacoco:check -Dcheckstyle.skip=true -Dspotbugs.skip=true

# Security tests
./mvnw test -Dtest=JwtAuthenticationFilterTest,AuthControllerTest,RefreshTokenServiceTest

# Integration tests (needs Docker)
./mvnw test -Pintegration -Dtest=RateLimitIntegrationTest,OrderFlowIntegrationTest,SoftDeleteIntegrationTest

# Redis serializer validation
grep -n "DefaultTyping" src/main/java/com/oms/config/RedisConfig.java

# CORS validation
grep -n "localhost:3000" src/main/java/com/oms/config/SecurityConfig.java

# Kustomize render
kubectl kustomize k8s/ > /tmp/oms-full.yaml
```

Tambahkan bukti berikut ke pull request:
- Hasil JaCoCo coverage report dan threshold
- Output scan security (Trivy, Gitleaks, OWASP)
- Test results (unit + integration)
- Redis serializer change evidence
- CORS configuration per environment
- Package structure (before/after)
- Kustomize render output

---

## 8. Kesimpulan

Proyek OMS V3 berada di level "production-functional" dengan fondasi yang sangat kuat dari Phase 0–3 V2. Temuan kritis V2 (F-01 s.d. F-10) telah ditutup sepenuhnya. Pemeriksaan komprehensif V3 menemukan **14 temuan** (3 P0, 5 P1, 6 P2) yang harus ditutup untuk mencapai level "production-grade" sesungguhnya.

Analisis SOLID Principle (Section 12) mengidentifikasi **rata-rata compliance 62%** dengan 22 temuan spesifik across 5 prinsip. Area terlemah adalah Interface Segregation (5/10) — 5 service utama tidak punya interface — dan Dependency Inversion (6/10) — 2 controller mengakses repository langsung. Semua temuan SOLID terintegrasi ke dalam roadmap Opsi D.

**Strategi Opsi D — Gol Production-Grade** dipilih untuk menutup semua temuan + 22 SOLID findings dalam 4 fase (8–12 hari):
- **Fase V3-0:** Security hardening (Redis serializer, CORS, profile) + SOLID S-3/partial, S-4, O-2 — tutup 3 P0 + F-18
- **Fase V3-1:** Resilience & reliability (Resilience4j, audit log, leak detection) — tutup F-13 + F-24
- **Fase V3-2:** Clean code, SOLID compliance & structure (service interfaces ×5, IDOR → service, split SecurityConfig, package-by-feature, split constants, refactor handler) — tutup F-14, F-16, F-17 + SOLID O-1, S-5, S-6, S-8, I-1, I-2, I-4, D-1–D-5
- **Fase V3-3:** CI/CD, governance & SOLID refactoring (CI enforcement, ADR, templates, RateLimitInterceptor refactor) — tutup F-15, F-19, F-20, F-21 + SOLID S-7
- **Fase V3-4:** Enterprise readiness & final SOLID (SLI/SLO, DRP, SpotBugs hardening, User adapter evaluation) — tutup F-23 + enterprise + SOLID L-2

**Compliance saat ini:** ~57% → **Target setelah Opsi D:** ~90%

Laporan ini adalah baseline V3 dan panduan implementasi Opsi D. Setiap fase harus diakhiri dengan pull request yang berisi perubahan implementasi, test regresi, dan bukti validasi yang dapat direproduksi.

---

## 9. RINGKASAN FASE PERBAIKAN — RE-VERIFICATION (16 Agustus 2026)

> **Prinsip pencatatan:** status **selesai** hanya diberikan bila terdapat perubahan yang dapat ditelusuri, test regresi yang relevan, dan bukti validasi yang dapat direproduksi.
>
> **Metodologi re-verification:** Pembacaan ulang seluruh file Java production (112 file source, 60 file test per inventaris terkini), semua config YAML, governance files, CI/CD, dan SpotBugs exclude — validasi manual satu per satu terhadap klaim laporan fase V3-0 s.d. V3-4. **Phase R (16 Agustus 2026)** menutup 4 discrepancy yang ditemukan.

### 9.1 Strategi yang dipilih

**Opsi D — Gol Production-Grade** dipilih berdasarkan evaluasi:
- Effort: 8–12 hari (2 sprint)
- Temuan ditutup: 14 dari 14 (100%) + 22 dari 22 SOLID findings (100%)
- Compliance target: ~90% (13 pilar) + ~82% (SOLID)
- Risiko: rendah (perubahan terukur per fase)
- Benefit: satu push komprehensif, production-grade sesungguhnya

### 9.2 Timeline analisis dan fase

#### Analisis dan laporan

1. **`docs/history/optimalisasi-v3/ANALISIS-PROYEK-OMS-V3.md`** — Audit V3 terhadap 13 pilar standar development code — **SELESAI (audit only; tidak ada perubahan kode aplikasi)**.

#### Fase perbaikan V3 (Opsi D — Full Compliance)

| Fase | Fokus | Temuan ditutup | Estimasi | Status (klaim) | Status (re-verified) | Kriteria selesai |
|---|---|---|:---:|:---:|:---:|---|
| V3 Audit/Baseline | Verifikasi source, konfigurasi, artefak | N/A | — | ✅ SELESAI | ✅ SELESAI | Laporan tersedia |
| **V3-0 — Security Hardening** | Redis serializer, CORS env-aware, hapus default dev, ObjectMapper dedup | F-11, F-12, F-22, F-18 | 0–3 hari | ✅ Selesai | ✅ **Benar** | Redis aman; CORS per profile; no default dev; ObjectMapper dedup |
| **V3-1 — Resilience & Reliability** | Resilience4j CB, audit log infra, leak detection | F-13, F-24 | minggu 1 | ⏳ Direncanakan | ✅ **Selesai (infra)** | CB active; leak detection aktif; audit log infra ada. ⚠️ `@Auditable` tidak ada consumer |
| **V3-2 — Clean Code, SOLID & Structure** | Split handler, 5 interfaces, IDOR→service, split SecurityConfig, AdminService split | F-14, F-16, F-17 + SOLID O-1, S-3, S-5, S-6, S-8, I-1, I-2, I-4, D-1–D-5 | minggu 2 | ⏳ Direncanakan | ⚠️ **Partial** | ❌ F-17/S-5/O-1 TIDAK diimplementasi; ❌ D-1 TIDAK addressed |
| **V3-3 — CI/CD, Governance & SOLID** | CI gate, ADR, templates, ONBOARDING + S-7 | F-15, F-19, F-20, F-21, SOLID S-7 | minggu 3 | ⏳ Direncanakan | ✅ **Hampir semua** | ADR ✅, templates ✅, ONBOARDING ✅, CI ✅; ⚠️ S-7 partial (94 baris) |
| **V3-4 — Enterprise & Final SOLID** | versions-maven-plugin, SLI/SLO, DRP, L-2 adapter | F-23, enterprise, SOLID L-2 | minggu 4 | ⏳ Direncanakan | ⚠️ **Partial** | Plugin ✅, Adapter ✅; ❌ Enterprise docs TIDAK ADA; L-2 partial |

### 9.3 Mapping temuan ke fase perbaikan — STATUS RE-VERIFICATION

> Status direvisi berdasarkan verifikasi kode aktual terhadap klaim laporan fase. ✅ = terimplementasi sesuai klaim; ⚠️ = partial; ❌ = klaim salah / tidak diimplementasi; ⏳ = belum dikerjakan.

| Kode | Temuan | Prioritas | Fase target | Status Klaim | Status Aktual | Catatan |
|---|---|:---:|---|:---:|:---:|---|
| F-11 | Redis serializer `DefaultTyping.NON_FINAL` | P0 | V3-0 | ✅ | ✅ | `BasicPolymorphicTypeValidator` whitelist `com.oms.*` — terverifikasi |
| F-12 | CORS hardcoded localhost | P0 | V3-0 | ✅ | ✅ | `CorsProperties` + per-profile YAML — terverifikasi |
| F-22 | Default dev profile berisiko | P0 | V3-0 | ✅ | ✅ | `spring.profiles.active: dev` dihapus — terverifikasi |
| F-18 | Duplicated ObjectMapper | P2 | V3-0 | ✅ | ✅ | Shared `@Bean redisObjectMapper()` — terverifikasi |
| F-13 | Tidak ada resilience pattern | P1 | V3-1 | ✅ | ✅ | Resilience4j + CB + Retry — terverifikasi |
| F-24 | Audit log tidak immutable | P2 | V3-1 | ✅ | ✅ | Infrastructure lengkap + **`@Auditable` diterapkan ke 7 metode production** (R-1.2 selesai) |
| F-14 | Package by-layer bukan by-feature | P1 | V3-2 | ⏳ | ⏳ | Deferred — tidak ada perubahan |
| F-16 | `AppConstants` monolithic | P1 | V3-2 | ✅ | ✅ | Dipecah ke 8 domain constant files; **`AppConstants` facade dihapus** (R-1/R-2 cleanup) — terverifikasi |
| F-17 | `GlobalExceptionHandler` besar | P1 | V3-2 | ✅ | ✅ | **Split selesai (R-1.1)**: `BaseExceptionHandler`(35), `SecurityExceptionHandler`(49), `ValidationExceptionHandler`(78), `BusinessExceptionHandler`(49), `InfrastructureExceptionHandler`(54) — semua <80 baris; `GlobalExceptionHandler` tinggal shim `@Deprecated` 20 baris |
| F-15 | CI skip Checkstyle/SpotBugs | P1 | V3-3 | ✅ | ✅ | Single `verify` step — terverifikasi |
| F-19 | Tidak ada ADR | P2 | V3-3 | ✅ | ✅ | 5 ADR di `docs/adr/` — terverifikasi |
| F-20 | Missing PR/Issue template | P2 | V3-3 | ✅ | ✅ | PR template, issue templates, CODEOWNERS — terverifikasi |
| F-21 | Missing `ONBOARDING.md` | P2 | V3-3 | ✅ | ✅ | `ONBOARDING.md` di root — terverifikasi |
| F-23 | Missing dependency lockfile | P2 | V3-4 | ✅ | ✅ | `versions-maven-plugin` v2.17.1 — terverifikasi |
| SOLID S-3 | SecurityConfig — 5 beans, mixed concerns | P1 | V3-0/2 | ✅ | ✅ | Split ke 3 config classes — terverifikasi |
| SOLID S-4 | RedisConfig — duplicated ObjectMapper | P2 | V3-0 | ✅ | ✅ | Shared `@Bean` — terverifikasi |
| SOLID S-5 | GlobalExceptionHandler — 252 baris | P1 | V3-2 | ✅ | ✅ | **Split selesai (R-1.1)** — 5 handler + base, semua <80 baris |
| SOLID S-6 | AppConstants — 152 baris | P1 | V3-2 | ✅ | ✅ | Dipecah ke 8 domain constant files; `AppConstants` dihapus — terverifikasi |
| SOLID S-7 | RateLimitInterceptor — mixed concerns | P2 | V3-3 | ✅ | ✅ | **59 baris aktual** (koreksi V3-5 audit 20 Aug; klaim Phase R "57" — drift +2, tetap <60 ✅) — `IpResolver` + `RateLimitResponseBuilder` diekstrak; header/pesan pakai `RateLimitConstants` |
| SOLID S-8 | AdminService — mixes concerns | P1 | V3-2 | ✅ | ✅ | Split ke 2 services + interface — terverifikasi |
| SOLID O-1 | GHE handler menambah = modifikasi | P1 | V3-2 | ✅ | ✅ | **Split handler selesai (R-1.1)** — handler baru dapat ditambah tanpa modifikasi file existing (OCP terpenuhi) |
| SOLID O-2 | SecurityConfig CORS hardcoded | P0 | V3-0 | ✅ | ✅ | Per-profile YAML — terverifikasi |
| SOLID I-1 | 5 service tanpa interface | P1 | V3-2 | ✅ | ✅ | 7 interfaces created — terverifikasi |
| SOLID I-2 | AppConstants monolithic import | P1 | V3-2 | ✅ | ✅ | Domain-specific constant files — terverifikasi |
| SOLID I-4 | SecurityConfig — 2 filter chains + 3 beans | P1 | V3-2 | ✅ | ✅ | 3 config classes — terverifikasi |
| SOLID D-1 | AuthController → UserRepository langsung | P1 | V3-2 | ✅ | ✅ | **AuthController tidak lagi bergantung `UserRepository` (R-1.3)** — refresh/logout didelegasikan ke `RefreshTokenService` (4 deps) |
| SOLID D-2 | OrderController → UserRepository langsung | P1 | V3-2 | ✅ | ✅ | IDOR dipindah ke service layer — terverifikasi |
| SOLID D-3 | OrderService tanpa interface | P1 | V3-2 | ✅ | ✅ | `OrderServiceInterface.java` ada — terverifikasi |
| SOLID D-4 | ProductService tanpa interface | P1 | V3-2 | ✅ | ✅ | `ProductServiceInterface.java` ada — terverifikasi |
| SOLID D-5 | AdminService tanpa interface | P1 | V3-2 | ✅ | ✅ | Koreksi (V3-5): file `AdminServiceInterface.java` TIDAK ada — substansi justru lebih baik via 2 interface granular `AdminDashboardServiceInterface` + `AdminUserManagementServiceInterface` (S-8 split) |
| SOLID L-2 | User implements UserDetails | P2 | V3-4 | ✅ | ✅ | **`User` tidak lagi `implements UserDetails` (R-3.2)** — di-wrap oleh `UserDetailsAdapter` |

### 9.4 Temuan Discrepancy — Status Penutupan (Phase R)

> **Discrepancy** = laporan fase menandai item sebagai ✅ (selesai) tetapi verifikasi kode menunjukkan sebaliknya. **Semua 4 discrepancy telah ditutup pada Phase R** (16 Agustus 2026).

#### ✅ DISCREPANCY #1 — Exception Handler Split (F-17, S-5, O-1) — DITUTUP (R-1.1)

| File | Status Sekarang | Baris |
|---|---|---|
| `GlobalExceptionHandler.java` | Shim `@Deprecated(forRemoval=true)` (private ctor, tanpa handler method) | 20 |
| `BaseExceptionHandler.java` | Ada — konstruksi RFC 7807 bersama | 35 |
| `SecurityExceptionHandler.java` | Ada | 49 |
| `ValidationExceptionHandler.java` | Ada | 78 |
| `BusinessExceptionHandler.java` | Ada | 49 |
| `InfrastructureExceptionHandler.java` | Ada | 54 |

**Dampak:** Semua handler <80 baris (SRP terpenuhi). Menambah exception baru = menambah handler baru tanpa memodifikasi file existing (OCP terpenuhi). `GlobalExceptionHandler` dipertahankan sebagai shim ter-`@Deprecated` demi kompatibilitas javadoc/test lama.

#### ✅ DISCREPANCY #2 — SOLID D-1: AuthController → UserRepository — DITUTUP (R-1.3)

`AuthController` tidak lagi meng-`import` maupun bergantung pada `UserRepository`. Logika refresh/logout didelegasikan ke `RefreshTokenService`. Total dependency turun ke 4. DIP terpenuhi.

#### ✅ DISCREPANCY #3 — `@Auditable` Consumer — DITUTUP (R-1.2)

`@Auditable` kini diterapkan pada 7 metode production di 5 kelas: `AuthController` (LOGIN), `AdminUserManagementService` (READ), `RefreshTokenService` (CREATE/DELETE/BLACKLIST), `AdminDashboardService` (READ), `RegisterService` (CREATE). Audit log fungsional.

#### ✅ DISCREPANCY #4 — SpotBugs Exclude — DITUTUP (R-2.1)

Package `com.oms.audit.*` sudah tidak ada (0 referensi). Blanket exclude `com.oms.dto.*` sudah dipersempit menjadi dua `<Match>` spesifik dengan `<Bug pattern>` `EI_EXPOSE_REP` dan `EI_EXPOSE_REP2` saja. Tidak ada dead rules.


### 9.5 Status keseluruhan — Re-Verification (16 Agustus 2026)

| Metrik | Klaim Sebelumnya | Status Aktual (Re-verified) |
|---|---|---|
| Laporan audit/baseline V3 | ✅ Selesai | ✅ Selesai |
| Strategi remediasi | ✅ Opsi D | ✅ Opsi D dipilih |
| **Temuan P0 tertutup** | **3 dari 3** | **3 dari 3** ✅ (F-11, F-12, F-22) |
| **Temuan P1 tertutup** | **0 dari 5** | **4 dari 5** (F-13 ✅, F-15 ✅, F-16 ✅, F-17 ✅; F-14 ⏳ deferred) |
| **Temuan P2 tertutup** | **1 dari 6** | **6 dari 6** (F-18 ✅, F-19 ✅, F-20 ✅, F-21 ✅, F-23 ✅, F-24 ✅) |
| **SOLID findings tertutup** | **3 dari 22** | **~21 dari 22** (hanya F-14 package refactor ⏳ deferred) |
| **Discrepancy ditemukan** | N/A | **4 discrepancy — semua DITUTUP (Phase R)** |
| Compliance aktual | ~57% | **~90%** (post-Phase R) |
| SOLID compliance | ~62% | **~82%** |

### 9.6 Ringkasan per fase — Verifikasi Kode

| Fase | Status Aktual | Ringkasan |
|---|---|---|
| **V3-0** Security | ✅ **Benar** | 7/7 item terimplementasi. Redis aman, CORS per-profile, no default dev, ObjectMapper dedup |
| **V3-1** Resilience | ✅ **Selesai** | F-13 ✅; F-24 ✅ — `@Auditable` diterapkan ke 7 metode production (R-1.2) |
| **V3-2** Code Quality | ✅ **Selesai** | F-17/S-5/O-1 ✅ (handler split, R-1.1); D-1 ✅ (AuthController lepas dari UserRepository, R-1.3); F-16 ✅ (`AppConstants` dihapus). Sisa: F-14 ⏳ deferred |
| **V3-3** Governance | ✅ **Selesai** | 5/5 utama ✅. S-7 ✅ (RateLimitInterceptor 57 baris, R-3.1); SpotBugs exclude dipersempit (R-2.1); step SonarCloud ditambahkan ke CI |
| **V3-4** Enterprise | ✅ **Selesai** | Plugin ✅, Adapter ✅; Enterprise docs (SLO/DRP/IncidentResponse) ✅; L-2 ✅ (R-3.2) |

### 9.7 Ketentuan pembaruan

Setiap implementasi fase harus membuat laporan fase tersendiri di `docs/history/optimalisasi-v3/` dengan format `NN-final-report-phaseX.md`. Minimal bukti: perubahan file, test regresi, command + exit code, status temuan.

> [!NOTE]
> **Phase R selesai — 4 discrepancy (2 kritis, 2 sedang) sudah ditutup.** Lihat Section 13 untuk listing tindakan lanjutan dan laporan `06-final-report-phaseR.md` untuk bukti validasi.

---

## 10. PROCESS FLOW MAP UNTUK OPSI D

### 10.1 Flow diagram perbaikan

```text
┌──────────────────────────────────────────────────────────────────────┐
│              CYCLE PERBAIKAN OPSI D — FULL COMPLIANCE                │
└──────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
        ┌──────────────────────────────────────────────────┐
        │ 1. TRIAGE DAN IDENTIFIKASI                         │
        │ - Baca baseline V3, temuan F-11 s.d. F-24 + SOLID  │
        │ - Strategi: Opsi D — tutup 14 temuan + 22 SOLID   │
        └──────────────────────────────────────────────────┘
                                  │
                                  ▼
        ┌──────────────────────────────────────────────────┐
        │ 2. PLANNING DAN ACCEPTANCE CRITERIA                │
        │ - 5 fase: V3-0 s.d. V3-4 (8-12 hari)             │
        │ - PR kecil dan reversible per fase                 │
        └──────────────────────────────────────────────────┘
                                  │
                                  ▼
        ┌──────────────────────────────────────────────────┐
        │ 3. IMPLEMENTASI (per fase)                         │
        │ - Fase V3-0: Security (F-11, F-12, F-22, F-18)   │
        │ - Fase V3-1: Resilience (F-13, F-24)              │
        │ - Fase V3-2: Code Quality (F-14, F-16, F-17)      │
        │ - Fase V3-3: Governance (F-15, F-19, F-20, F-21)  │
        │ - Fase V3-4: Enterprise (F-23 + ops docs)          │
        └──────────────────────────────────────────────────┘
                                  │
                                  ▼
        ┌──────────────────────────────────────────────────┐
        │ 4. VALIDASI BERLAPIS                               │
        │ - Compile, test, Checkstyle, SpotBugs, JaCoCo      │
        │ - Security scan, Kustomize, schema validation      │
        └──────────────────────────────────────────────────┘
                                  │
                                  ▼
        ┌──────────────────────────────────────────────────┐
        │ 5. REVIEW, DOKUMENTASI, DAN STATUS                 │
        │ - Peer review; buat laporan fase                   │
        │ - Update Section 9 dan mapping                     │
        └──────────────────────────────────────────────────┘
                                  │
                                  ▼
        ┌──────────────────────────────────────────────────┐
        │ 6. RELEASE ATAU ROLLBACK                           │
        │ - Merge hanya jika exit criteria terpenuhi         │
        │ - Monitor metrics; rollback bila regresi           │
        └──────────────────────────────────────────────────┘
```

### 10.2 Matriks prioritas fase V3 (Opsi D)

| Urutan | Fase | Temuan | Alasan | Bukti minimum |
|:---:|---|---|---|---|
| 1 | V3-0 Security | F-11, F-12, F-22, F-18 | Risiko keamanan langsung + DRY | Redis whitelist; CORS env-aware; no default dev; ObjectMapper dedup |
| 2 | V3-1 Resilience | F-13, F-24 | Mencegah cascading failure + audit | CB active; leak detection; audit log table |
| 3 | V3-2 Code Quality | F-14, F-16, F-17 | Maintainability | Package-by-feature; split constants; refactor handler |
| 4 | V3-3 Governance | F-15, F-19, F-20, F-21 | Proses dan enforceability | CI enforce gates; ADR; templates; ONBOARDING |
| 5 | V3-4 Enterprise | F-23 + ops docs | Maturity | SLI/SLO; DRP; SpotBugs hardening; versions plugin |

### 10.3 Quality gates V3 (post-Opsi D)

| Area | Minimum | Command |
|---|---|---|
| Compile + test | Build clean | `./mvnw clean test -Pdev` |
| Code style | 0 violation | `./mvnw checkstyle:check` |
| Static analysis | 0 finding (tanpa aggressive excludes) | `./mvnw spotbugs:check` |
| Coverage | JaCoCo ≥ 80% LINE | `./mvnw jacoco:report jacoco:check` |
| Security | No critical | Trivy, Gitleaks, OWASP output |
| Redis | Whitelist only (no DefaultTyping) | `grep -n "DefaultTyping" src/...` |
| CORS | Per-environment config | `grep -n "CorsProperties" src/...` |
| Resilience | CircuitBreaker active | `grep -n "CircuitBreaker" src/...` |
| Audit | Audit log table exists | `V7__create_audit_log.sql` |
| Package | Domain-based structure | `find src/main/java/com/oms/domain -type d` |
| Governance | ADR + templates present | `ls docs/adr/ .github/PULL_REQUEST_TEMPLATE.md` |
| Kubernetes | Render valid | `kubectl kustomize k8s/` |

---

## 11. Catatan implementasi Opsi D

### 11.1 Urutan implementasi yang direkomendasikan

Implementasi mengikuti urutan fase V3-0 → V3-1 → V3-2 → V3-3 → V3-4. Setiap fase:
1. Buat branch `feature/v3-phaseX-<description>`
2. Implementasi sesuai tabel di Section 6
3. Jalankan validasi sesuai command di Section 7
4. Buat PR dengan checklist dari Section 7
5. Merge hanya jika semua exit criteria terpenuhi
6. Buat laporan fase di `docs/history/optimalisasi-v3/`

### 11.2 Risk mitigation

| Risiko | Mitigasi |
|---|---|
| Breaking change saat refactor package (V3-2) | Refactor bertahap; gunakan package alias sementara |
| Test failure setelah split AppConstants (V3-2) | Buat facade `AppConstants` yang delegating untuk backward compat |
| CI pipeline break (V3-3) | Test di branch; merge hanya jika pipeline hijau |
| Resilience4j overhead (V3-1) | Default config overhead minimal; monitoring via Actuator |

### 11.3 Monitoring setelah implementasi

| Metrik | Target | Cara verifikasi |
|---|---|---|
| Checkstyle violations | 0 | `./mvnw checkstyle:check` |
| SpotBugs findings | 0 | `./mvnw spotbugs:check` |
| JaCoCo LINE coverage | ≥ 80% | `./mvnw jacoco:report jacoco:check` |
| DefaultTyping occurrence | 0 | `grep -rn "DefaultTyping" src/` |
| CORS hardcoded occurrence | 0 | `grep -rn "localhost:3000" src/main/java/` |
| CircuitBreaker active | true | Actuator `/actuator/circuitbreakers` |
| Audit log records | > 0 | `SELECT COUNT(*) FROM audit_log` |
| Package structure | Domain-based | `find src/main/java/com/oms/domain -type d` |

---

## 12. Analisis SOLID Principle — Seluruh Proyek OMS V3

> **Ruang lingkup:** 80 source Java produksi, 14 package, 6 controller, 11 service, 4 repository, 15 config, 7 entity, 11 DTO.
> **Metodologi:** Pembacaan dan audit seluruh file sumber produksi terhadap kelima prinsip SOLID.

### 12.1 Ringkasan skor SOLID

| Prinsip | Skor | Ringkasan |
|---|:---:|---|
| **S — Single Responsibility** | ⚠️ **6/10** | 8 kelas tanggung jawab berlebih; 20+ kelas mematuhi SRP |
| **O — Open/Closed** | ⚠️ **6/10** | Strategy pattern ada, tetapi 6+ kelas perlu modifikasi untuk ekstensi |
| **L — Liskov Substitution** | ✅ **8/10** | Minor concern pada NoOp service dan User implements UserDetails |
| **I — Interface Segregation** | ⚠️ **5/10** | 5 service utama tidak punya interface; AppConstants monolithic |
| **D — Dependency Inversion** | ⚠️ **6/10** | 2 controller akses repository langsung; tidak ada service interface |

**Rata-rata SOLID compliance: ~62%**

### 12.2 S — Single Responsibility Principle (SRP)

#### ⚠️ Pelanggaran

| # | Kelas | Baris | Masalah |
|:---:|---|:---:|---|
| S-1 | `AuthController` | 194 | 5 deps, mixes login/register/refresh/logout. Akses `UserRepository` langsung |
| S-2 | `OrderController` | 185 | IDOR logic berulang di 2 methods. Business authorization bercampur HTTP handler |
| S-3 | `SecurityConfig` | 136 | 5 beans: 2 filter chains, CORS, PasswordEncoder, AuthenticationManager |
| S-4 | `RedisConfig` | 113 | 4 beans: connection factory, redis template, cache config, cache manager |
| S-5 | `GlobalExceptionHandler` | 252 | 16+ handler methods untuk tipe exception berbeda |
| S-6 | `AppConstants` | 152 | ~60+ konstanta mencakup 10+ domain |
| S-7 | `RateLimitInterceptor` | 206 | Mixes rate limit, IP/CIDR validation, RFC 7807 response |
| S-8 | `AdminService` | 83 | Mixes dashboard statistics + user management |

#### ✅ SRP Dipatuhi

`OrderItemFactory`, `EmailService`, `JwtTokenProvider`, `CustomUserDetailsService`, `RegisterService`, `LoggingFilter`, `CacheConfig`, `JpaAuditingConfig`, semua entity, semua DTO records.

### 12.3 O — Open/Closed Principle (OCP)

> *Entitas software harus terbuka untuk ekstensi, tertutup untuk modifikasi.*

#### ⚠️ Pelanggaran

| # | Kelas | Masalah |
|:---:|---|---|
| O-1 | `GlobalExceptionHandler` | Menambah exception type = menambah @ExceptionHandler method |
| O-2 | `SecurityConfig` | CORS hardcoded — menambah environment = modifikasi code |
| O-3 | `AppConstants` | Menambah domain constants = modifikasi file 152 baris |
| O-4 | `AdminService` | Menambah dashboard metric = modifikasi class |
| O-5 | `VersionController` | Menambah version = hardcode di method |
| O-6 | `SecurityConfig` filter chains | Menambah security rule = modifikasi builder |

#### ✅ OCP Dipatuhi

- **Strategy:** `RefreshTokenServiceInterface` → Redis/NoOp — tambah impl tanpa modifikasi
- **HealthIndicator SPI:** 3 indicators independent — tambah indicator tanpa modifikasi
- **ErrorCode enum:** extensible by constant
- **@RateLimit annotation:** tambah via annotation
- **Spring Event:** `OrderCreatedEvent` + listener independent

### 12.4 L — Liskov Substitution Principle (LSP)

> *Subtype harus substitutable untuk supertype tanpa mengubah kebenaran program.*

#### ⚠️ Area Perhatian Minor

| # | Masalah | Dampak |
|:---:|---|---|
| L-1 | `NoOpRefreshTokenService.blacklistAccessToken()` diam-diam no-op | Silent failure di non-Redis mode |
| L-2 | `User implements UserDetails` — Entity JPA sekaligus security principal | Sulit bedakan representasi DB vs security |

#### ✅ LSP Dipatuhi

Exception hierarchy konsisten (`BadRequestException`, `ResourceNotFoundException`, `BusinessException` all extends `RuntimeException`). Repositories substitutable. DTO records immutable.

### 12.5 I — Interface Segregation Principle (ISP)

> *Klien tidak boleh dipaksa bergantung pada method yang tidak mereka gunakan.*

#### ⚠️ Pelanggaran

| # | Masalah | Dampak |
|:---:|---|---|
| I-1 | 5 service TANPA interface: `OrderService`, `ProductService`, `AdminService`, `RegisterService`, `EmailService` | Controller langsung depend concrete class |
| I-2 | `AppConstants` monolitik: ~60+ konstanta, 10+ domain | Import 152 baris untuk 1 konstanta |
| I-3 | `GlobalExceptionHandler` — semua exception di 1 class | Tidak bisa ganti handler per domain |
| I-4 | `SecurityConfig` — 2 filter chains + 3 beans dalam 1 class | Test tidak bisa dipisah |

#### ✅ ISP Dipatuhi

`RefreshTokenServiceInterface` (5 methods tersegmentasi), `HealthIndicator` implementations, custom annotations (`@RateLimit`, `@PasswordStrength`, `@ApiVersion`), DTO records per use case.

### 12.6 D — Dependency Inversion Principle (DIP)

> *Modul level tinggi dan rendah harus bergantung pada abstraksi.*

#### ⚠️ Pelanggaran

| # | Kelas | Dependensi | Masalah |
|:---:|---|---|---|
| D-1 | `AuthController` | → `UserRepository` langsung | Akses repository bypass service layer |
| D-2 | `OrderController` | → `UserRepository` langsung | Akses repository untuk IDOR check |
| D-3 | `OrderService` | → 10 concrete deps | Tidak ada `OrderServiceInterface` |
| D-4 | `ProductService` | → concrete deps | Tidak ada `ProductServiceInterface` |
| D-5 | `AdminService` | → 3 repos langsung | Tidak ada interface |

#### ✅ DIP Dipatuhi

`AuthController → RefreshTokenServiceInterface` ✅, `JwtAuthenticationFilter → RefreshTokenServiceInterface` ✅, semua repositories via Spring Data interfaces ✅, `AsyncConfig → AsyncUncaughtExceptionHandler` ✅.

### 12.7 Matriks SOLID per kelas

| Kelas | S | O | L | I | D |
|---|:---:|:---:|:---:|:---:|:---:|
| `AuthController` | ❌ | ⚠️ | ✅ | ❌ | ❌ |
| `OrderController` | ❌ | ⚠️ | ✅ | ❌ | ❌ |
| `ProductController` | ✅ | ✅ | ✅ | ⚠️ | ⚠️ |
| `AdminController` | ✅ | ✅ | ✅ | ⚠️ | ⚠️ |
| `VersionController` | ✅ | ❌ | ✅ | ⚠️ | ⚠️ |
| `CsrfController` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `OrderService` | ⚠️ | ⚠️ | ✅ | ❌ | ⚠️ |
| `ProductService` | ✅ | ⚠️ | ✅ | ❌ | ⚠️ |
| `AdminService` | ❌ | ❌ | ✅ | ❌ | ⚠️ |
| `RegisterService` | ✅ | ✅ | ✅ | ⚠️ | ⚠️ |
| `EmailService` | ✅ | ✅ | ✅ | ⚠️ | ✅ |
| `CustomUserDetailsService` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `RefreshTokenService` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `NoOpRefreshTokenService` | ✅ | ✅ | ⚠️ | ✅ | ✅ |
| `OrderItemFactory` | ✅ | ✅ | ✅ | ⚠️ | ✅ |
| `SecurityConfig` | ❌ | ❌ | ✅ | ❌ | ⚠️ |
| `RedisConfig` | ❌ | ⚠️ | ✅ | ⚠️ | ⚠️ |
| `GlobalExceptionHandler` | ❌ | ❌ | ✅ | ⚠️ | ✅ |
| `AppConstants` | ❌ | ❌ | ✅ | ❌ | ✅ |
| `JwtTokenProvider` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `JwtAuthenticationFilter` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `LoggingFilter` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `RateLimitInterceptor` | ❌ | ⚠️ | ✅ | ✅ | ⚠️ |
| DTO records (11) | ✅ | ✅ | ✅ | ✅ | ✅ |
| Entity classes (4) | ✅ | ⚠️ | ✅ | ⚠️ | ✅ |

### 12.8 Rekomendasi integrasi SOLID ke Opsi D

| Fase | SOLID Finding | Tindakan | V3 terkait |
|---|---|---|---|
| V3-0 | S-3 (partial), S-4, O-2 | Ekstrak `CorsProperties`; shared `ObjectMapper` | F-12, F-18 |
| V3-1 | — | Tidak ada SOLID baru (audit log & Resilience4j mengikuti SRP yang sudah baik) | F-13, F-24 |
| V3-2 | O-1, S-3 (full), S-5, S-6, S-8, I-1, I-2, I-4, D-1, D-2, D-3, D-4, D-5 | Hierarchical handlers; buat 5 service interfaces; pecah `AppConstants`; pindahkan IDOR ke service; split `SecurityConfig` | F-14, F-16, F-17 |
| V3-3 | S-7 | Refactor `RateLimitInterceptor` — ekstrak `IpResolver`, `RateLimitResponseBuilder` | F-15, F-19, F-20, F-21 |
| V3-4 | L-2 | Evaluate `User implements UserDetails` — pertimbangkan adapter | F-23, Enterprise |

### 12.9 Target SOLID post-Opsi D

| Prinsip | Saat ini | Target |
|---|:---:|:---:|
| S — Single Responsibility | 6/10 | 8/10 |
| O — Open/Closed | 6/10 | 8/10 |
| L — Liskov Substitution | 8/10 | 9/10 |
| I — Interface Segregation | 5/10 | 8/10 |
| D — Dependency Inversion | 6/10 | 8/10 |
| **Rata-rata** | **62%** | **~82%** |

---

## 13. LISTING PHASE TINDAKAN LANJUTAN

> **Tujuan:** Menutup 4 discrepancy yang ditemukan pada re-verification + menyelesaikan item yang belum tertutup. **Status: Phase R-1 s.d. R-3 SELESAI (16 Agustus 2026); Phase V3-5 (audit alignment & hardening backlog) SELESAI (20 Agustus 2026).** Phase R-4 (package refactor F-14) tetap deferred ke sprint terpisah.

### Phase R-1 — Fix Discrepancy Kritis (Prioritas Maksimal) — ✅ SELESAI

**Estimasi:** 2–3 hari | **Dependency:** Tidak ada (bisa langsung mulai)

| # | Item | Status | Verifikasi |
|---|---|:---:|---|
| R-1.1 | **Implementasi exception handler split (F-17, S-5, O-1)** | ✅ | `BaseExceptionHandler`(35), `SecurityExceptionHandler`(49), `ValidationExceptionHandler`(78), `BusinessExceptionHandler`(49), `InfrastructureExceptionHandler`(54) — semua <80 baris; `GlobalExceptionHandler` shim 20 baris; test pass |
| R-1.2 | **Apply `@Auditable` ke critical methods (F-24)** | ✅ | 7 metode di 5 kelas — `grep -rn "@Auditable" src/main/java/` ≥5 matches |
| R-1.3 | **Pindahkan UserRepository dari AuthController (D-1)** | ✅ | `AuthController` 4 deps; `grep "UserRepository" AuthController.java` = 0 (hanya di javadoc) |
| R-1.4 | **Update ErrorCode.java dan test javadoc** | ✅ | Javadoc `ErrorCode` menunjuk handler yang benar-benar ada |

### Phase R-2 — Fix Discrepancy Sedang & Cleanup — ✅ SELESAI

**Estimasi:** 0.5–1 hari | **Dependency:** R-1 selesai

| # | Item | Status | Verifikasi |
|---|---|:---:|---|
| R-2.1 | **Fix SpotBugs exclude** | ✅ | `com.oms.audit` = 0; blanket `dto.*` dipersempit ke `EI_EXPOSE_REP`/`EI_EXPOSE_REP2` |
| R-2.2 | **Apply atau remove `@Auditable`** | ✅ | Diputuskan: ada consumer (7 metode). Infrastructure dipertahankan |
| R-2.3 | **Hapus `AdminService.java` redundant** | ✅ | File dihapus; diganti `AdminDashboardService` + `AdminUserManagementService` + interfaces |
| R-2.4 | **Fix staging TracingConfig** | ✅ | `localhost:9411` dihapus dari `application-staging.yml` (hanya tinggal komentar javadoc default) |
| R-2.5 | **Hapus `AppConstants` facade (F-16 tuntas)** | ✅ | `AppConstants.java` dihapus; semua consumer migrasi ke 8 domain constant files |

### Phase R-3 — Partial Items Completion — ✅ SELESAI

**Estimasi:** 1–2 hari | **Dependency:** R-1, R-2 selesai

| # | Item | Status | Verifikasi |
|---|---|:---:|---|
| R-3.1 | **RateLimitInterceptor < 60 baris (S-7)** | ✅ | 57 baris saat Phase R (kini 59 — audit 20 Aug, drift +2, tetap <60); `IpResolver` + `RateLimitResponseBuilder` diekstrak |
| R-3.2 | **Hapus `User implements UserDetails` (L-2)** | ✅ | `grep "implements UserDetails" User.java` = 0; `UserDetailsAdapter` wrap `User` |
| R-3.3 | **Enterprise documentation** | ✅ | `docs/ops/SLO.md`, `DRP.md`, `IncidentResponse.md` ada |

### Phase R-CI — CI/CD Gate Completion — ✅ SELESAI

| # | Item | Status | Verifikasi |
|---|---|:---:|---|
| R-CI.1 | **Tambah step SonarCloud ke CI** | ✅ | Step `SonarCloud Analysis` di `ci-cd.yml` (guarded oleh `SONAR_TOKEN`); profile `-Psonar` di pom |
| R-CI.2 | **Trivy fs scan** | ✅ | Koreksi (V3-5): step sempat ter-comment di `ci-cd.yml` (ditemukan audit kesesuaian) — **di-re-enable Phase V3-5** dengan `ignore-unfixed: true` + `.trivyignore` deferral; simulasi gate lokal exit 0 |



### Phase V3-5 — Audit Alignment & Hardening Backlog — ✅ SELESAI (20 Agustus 2026)

> Eksekusi temuan audit kesesuaian kode-vs-dokumentasi. Prinsip: **kondisi aktual yang lebih baik dari klaim dokumen dipertahankan** (dokumen dikoreksi); kekurangan dieksekusi. Laporan: `07-final-report-phaseV3-5.md`.

| # | Item | Status | Bukti |
|---|---|:---:|---|
| V3-5.1 | `ErrorProperties` (`app.error.base-url`) — hapus hardcode `ERROR_BASE_URL`, hidupkan dead config | ✅ | `config/ErrorProperties.java` + `ErrorPropertiesConfig`; 4 handler constructor-inject; `ErrorPropertiesTest` 3 test |
| V3-5.2 | securityContext `k8s/postgres.yaml` & `redis.yaml` (KSV-0014/KSV-0118) | ✅ | Trivy misconfig repo = **0 CRITICAL/HIGH** (baseline 7 HIGH) |
| V3-5.3 | KSV-0109 configmap — 2 key policy dipindah ke `oms-secrets` | ✅ | ConfigMap clean; env var name verbatim → binding Spring tak berubah |
| V3-5.4 | Re-enable 2 step Trivy CI (R-CI.2 follow-up) | ✅ | `ci-cd.yml` + `.trivyignore` (37 temuan fixable di-defer: fix hanya di SB 3.3+/3.4+) |
| V3-5.5 | Spring Boot 3.2.5 → 3.2.12 | ✅ | BUILD SUCCESS; **314/314 test PASS**; JaCoCo **84.03%** (gate 0.80); vuln 43 → 37 |
| V3-5.6 | Koreksi dokumen (header, §1.2 JaCoCo 0.80, §1.4/§2.3 inventaris 116/63/7/16/5-job, §9.3 D-5) + `CHANGELOG.md` | ✅ | Diff ANALISIS + CHANGELOG fase ini |
| V3-5.7 | Hapus file sampah `0` di root | ✅ | `ls 0` = No such file |
| V3-5.8 | Catatan minor audit: rekonsiliasi command `grep DefaultTyping` (§V3-0 validation) + inventarisasi `.github/modernize/java-upgrade` (config otomatis GitHub App Modernize, bukan kode proyek — tidak perlu aksi) + header "3→4 discrepancy" | ✅ | Command dikoreksi; modernize dicatat; header line 4 sudah "bukan 3 seperti tulisan lama" |
| V3-5.9 | Re-audit §2 inventaris (presisi definisi): Repository = 6 file (5 interface + helper QueryDSL) · DTO = 13 file semuanya `public record` (audit "12" undercount) · K8s = 14 entri file aktif → 16 objek render; postgres/redis di blok komentar staging/prod, **tanpa overlays/** · LOC 6.742/7.494 & konstanta 183 baris live-verified persis | ✅ | `ls` + `grep -L 'public record'` (0 file non-record) + histogram `kubectl kustomize` (13×1 + 3 NetworkPolicy = 16) + `wc -l` |

### Phase R-4 — Deferred Items (F-14 Package Refactor)

**Estimasi:** 3–5 hari (sprint terpisah) | **Dependency:** R-1 s.d. R-3 selesai

| # | Item | File yang diubah | Deliverable | Verifikasi |
|---|---|---|---|---|
| R-4.1 | **Package by-layer → by-feature (F-14)** | Refactor ke `domain/order/`, `domain/product/`, `domain/auth/`, `domain/admin/`, `shared/` | Package structure domain-based | `find src/main/java/com/oms/domain -type d` returns domain packages |

### Matriks Dependensi

```text
R-1 (Discrepancy Kritis) ─── bisa langsung mulai
 ├── R-2 (Discrepancy Sedang) — tunggu R-1
 │    └── R-3 (Partial Items) — tunggu R-2
 │         └── R-4 (Package Refactor) — tunggu R-3
 └── Bisa paralel dengan R-4 planning
```

### Estimasi Total

| Phase | Effort | Prioritas | Output |
|---|:---:|:---:|---|
| **R-1** Fix Discrepancy Kritis | 2–3 hari | 🔴 Maksimal | Handler split, `@Auditable` consumer, AuthController DIP fix, javadoc |
| **R-2** Fix Discrepancy Sedang | 0.5–1 hari | 🟡 Tinggi | SpotBugs, AdminService removal, TracingConfig |
| **R-3** Partial Items | 1–2 hari | 🟢 Sedang | RateLimitInterceptor, User entity, enterprise docs |
| **R-4** Package Refactor | 3–5 hari | ⬜ Deferred | Package by-feature (F-14) |
| **Total** | **7–11 hari** | | **Semua discrepancy tertutup, compliance ~90%** |

### Success Criteria Post-Phase R — ✅ TERPENUHI

| Metrik | Target | Status Aktual |
|---|---|:---:|
| Discrepancy 🔴 kritis | 0 (semua tertutup) | ✅ 0 |
| `GlobalExceptionHandler` baris | Tidak ada / shim; 5 handler <80 baris | ✅ shim 20 baris + 5 handler <80 |
| `@Auditable` consumer | ≥ 5 metode production | ✅ 7 metode |
| `AuthController` → `UserRepository` | Tidak ada | ✅ 0 |
| SpotBugs `com.oms.audit.*` | Tidak ada | ✅ 0 |
| `RateLimitInterceptor` baris | < 60 | ✅ 57 |
| `User.java` implements `UserDetails` | Tidak ada | ✅ 0 |
| Enterprise docs (SLO, DRP, Incident) | Tersedia | ✅ 3 file |
| SonarCloud step di CI | Ada | ✅ ditambahkan |
| Trivy fs scan | Dijalankan; no critical baru | ✅ dijalankan lokal |
| Compliance aktual | ~90% dari 132 standar | ✅ ~90% |
| SOLID compliance | ~82% | ✅ ~82% |

---

*Dokumen ini adalah baseline analisis V3 dengan strategi Opsi D yang dihasilkan pada 14 Agustus 2026, diperbarui dengan re-verification pada 16 Agustus 2026, dan diperbarui lagi pada 20 Agustus 2026 (Phase V3-5: audit alignment & hardening backlog — koreksi inventaris/gate/klaim dokumen, Trivy CI re-enabled, Spring Boot 3.2.12, K8s securityContext hardening; lihat Section 13 dan `07-final-report-phaseV3-5.md`). Re-verification mengungkap 4 discrepancy (2 kritis) yang telah ditutup pada Phase R.*
