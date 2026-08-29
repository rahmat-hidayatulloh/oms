# OMS — Portfolio Highlights

> **Status:** Current
> **Audience:** Recruiter, Technical Reviewer, Hiring Manager
> **Owner:** Engineering
> **Last verified:** 2026-08-29
> **Source of truth:** Repository source code, `docs/current/`, `docs/adr/`, `docs/ops/`

Dokumen ini adalah **ringkasan satu halaman** untuk reviewer yang ingin memahami
nilai teknis proyek OMS secara cepat, sebelum masuk ke dokumentasi detail.

---

## Apa Ini?

**OMS (Order Management System)** adalah REST API production-grade yang dibangun dengan
Spring Boot 3.2.12 + Java 21. Proyek ini mendokumentasikan **perjalanan engineering nyata**
dari inisialisasi (V0) hingga refactor enterprise (V5) — bukan sekadar CRUD tutorial.

---

## Keputusan Arsitektur Utama (ADR)

| ADR | Keputusan | Alasan |
|-----|-----------|--------|
| [ADR-001](../../adr/001-jwt-authentication.md) | JWT + Refresh Token Rotation | Stateless auth dengan replay detection via Redis |
| [ADR-002](../../adr/002-database-per-profile.md) | H2 (dev) / PostgreSQL (prod) | Zero-dependency dev, production parity |
| [ADR-003](../../adr/003-querydsl-dynamic-filtering.md) | QueryDSL untuk dynamic filter | Type-safe query tanpa raw SQL string |
| [ADR-004](../../adr/004-spring-events-async.md) | Spring Events + async executor | Decoupling domain events tanpa broker eksternal |
| [ADR-005](../../adr/005-resilience4j-patterns.md) | Resilience4j circuit breaker + retry | Fault tolerance terhadap downstream failure |
| [ADR-006](../../adr/006-package-by-feature.md) | Package-by-feature (V5) | Cohesion per domain, eliminasi cross-cutting import chaos |

---

## Fitur Teknis yang Menonjol

### 🔒 Security — Production-Hardened

- **JWT dengan `token_type` claim** — filter menolak refresh token di protected endpoint
- **Refresh token rotation + replay detection** — exact-match Redis, revoke on re-use
- **Rate Limiting (Bucket4j)** — token bucket per endpoint; distributed via `LettuceBasedProxyManager`, fallback in-memory
- **Dual filter chain** — chain terpisah untuk API (`/api/**`) dan management (`/actuator/**`)
- **Container hardening** — non-root user, `readOnlyRootFilesystem`, `capabilities.drop: ALL`, `seccompProfile: RuntimeDefault`
- **CI security** — OWASP Dependency-Check, Trivy image scan, Gitleaks secret scan, CycloneDX SBOM

### ⚡ Performance — Measured, Not Assumed

- **N+1 fix** — `@EntityGraph` + `JOIN FETCH` pada `OrderRepository` ([doc](../development/features/order-product/21-fix-n-plus-one-order-items.md))
- **Optimistic locking** — `@Version` + Resilience4j retry untuk concurrent stock update ([doc](../development/features/order-product/22-optimistic-locking-stock.md))
- **Write-through cache** — produk di-cache ke Redis; invalidasi saat update/delete ([doc](../development/features/cross-cutting/25-redis-cache-config.md))
- **HikariCP tuning** — pool size per-profile (dev/staging/prod) ([doc](../operations/28-hikaricp-connection-pool-tuning.md))
- **Benchmark terdokumentasi** — throughput cache ON vs OFF ([hasil](../operations/benchmark_results.md))

### 🏗️ Engineering Maturity

- **Package-by-feature** — `auth`, `order`, `product`, `admin`, `audit` masing-masing self-contained
- **Flyway migration** — V1–V7, fail-fast admin seed via environment variable
- **Structured logging (MDC)** — `traceId`, `userId`, `method`, `path` per request; JSON di prod ([doc](../operations/30-structured-logging-mdc.md))
- **RFC 7807 ProblemDetail** — response error standar industri ([doc](../development/features/cross-cutting/19-global-exception-handler-rfc7807.md))
- **Soft delete** — semua entity via `@SQLDelete` + `@SQLRestriction` ([doc](../development/features/cross-cutting/23-soft-delete-all-entities.md))
- **JPA Auditing** — `createdAt`, `updatedAt`, `createdBy`, `updatedBy` dari SecurityContext ([doc](../development/features/cross-cutting/24-jpa-auditing.md))
- **Quality gates** — Checkstyle + SpotBugs + JaCoCo wajib pass sebelum CI merge

### 🚀 Kubernetes-Native

- **Kustomize overlay** — `dev` / `staging` / `prod` dengan base + overlay pattern
- **HPA** — autoscale CPU 70% / Memory 80%
- **PDB** — min 1 pod selama disruption
- **NetworkPolicy** — default-deny; whitelist per path
- **RBAC** — dedicated ServiceAccount dengan `automountServiceAccountToken: false`
- **ResourceQuota + LimitRange** — namespace-level resource governance

### 📊 Observability & SRE

- **Custom health indicators** — Database, Redis, External API ([doc](../operations/29-custom-health-indicator.md))
- **Prometheus metrics** — via Micrometer + `/actuator/prometheus`
- **Distributed tracing** — OpenTelemetry + Zipkin exporter
- **SLO terdefinisi** — 99.9% availability, p95 < 500ms, error rate < 0.1% ([SLO](../../ops/SLO.md))
- **DRP terdokumentasi** — RTO < 15 menit, RPO < 1 jam ([DRP](../../ops/DRP.md))
- **Incident runbook** — P1–P4 severity, kubectl commands, post-mortem template ([IncidentResponse](../../ops/IncidentResponse.md))

---

## Evolusi Proyek (V0 → V5)

| Versi | Fokus | Highlight |
|-------|-------|-----------|
| V0 | Foundation | Spring Boot setup, JWT, basic CRUD |
| V1 | Security | Refresh token, rate limiting, Redis |
| V2 | CI/CD | GitHub Actions, Docker, Kustomize |
| V3 | Quality | Checkstyle, SpotBugs, SOLID refactor |
| V4 | Production Readiness | SLO/DRP/IncidentResponse, benchmark, kubeconform |
| V5 | Architecture | Package-by-feature refactor (ADR-006) |

Setiap fase didokumentasikan di [`docs/history/`](../../history/) sebagai audit trail.

---

## Entry Points untuk Reviewer

| Tujuan | Dokumen |
|--------|---------|
| Memahami arsitektur sistem | [architecture.md](architecture.md) |
| Melihat semua keputusan teknis | [docs/adr/](../../adr/) |
| Evaluasi security posture | [SECURITY.md](../../../SECURITY.md) |
| Melihat standar development | [STANDAR-DEVELOPMENT-CODE.md](../development/standards/STANDAR-DEVELOPMENT-CODE.md) |
| Menjalankan aplikasi | [deployment-guide.md](../operations/deployment-guide.md) |
| API reference | [api-index.md](../api/api-index.md) |
| SLO / DRP / Incident | [docs/ops/](../../ops/) |
| Full doc index | [docs/index.md](../../index.md) |

