# OMS Documentation

> **Status:** Current
> **Audience:** Developer, API Consumer, Operator, Maintainer
> **Owner:** Engineering
> **Last verified:** 2026-08-29 (struktur, tautan, dan screening kode aktual — V5-P0 selesai; semua dokumentasi current-state disinkronkan)
> **Source of truth:** Repository source code, `pom.xml`, konfigurasi runtime, dan artefak pengujian terkait

Indeks ini adalah entry point dokumentasi OMS selama migrasi **Skema C**. Dokumen aktif berada di `docs/current/`, sedangkan laporan fase dan audit lama berada di `docs/history/`. Nomor pada nama file dipertahankan sementara untuk menjaga audit trail.

> **Pekerja baru?** Mulai dari [`ONBOARDING.md`](../ONBOARDING.md) bagian *Reading Order for New Joiners* untuk urutan baca dokumentasi dan aturan workflow sebelum menyentuh kode.

## Quick navigation

### Developer

- [**Portfolio Highlights** — ringkasan teknis untuk reviewer](current/overview/PORTFOLIO-HIGHLIGHTS.md)
- [Arsitektur](current/overview/architecture.md)
- [Getting started](current/development/getting-started/01-inisialisasi-project.md)
- [Standar development](current/development/standards/STANDAR-DEVELOPMENT-CODE.md)
- [Clean code dan quality gates](current/development/standards/02-clean-code-quality-gates.md)
- [Testing dengan Testcontainers](current/development/testing/32-testcontainers-integration.md)

### API consumer

- [API index](current/api/api-index.md)
- [OpenAPI/Swagger](current/api/64-oms-037-swagger-openapi.md)
- [API versioning](current/api/31-api-versioning.md)
- [Postman collection](current/api/62-oms-034-postman-collection.md)

### Operator

- [Deployment guide](current/operations/deployment-guide.md)
- [Docker](current/operations/63-oms-036-dockerize.md)
- [Docker Compose](../docker-compose.yml)
- [Minikube/Kubernetes](current/operations/65-oms-038-minikube.md)
- [Rate limiting](current/operations/66-oms-039-rate-limiting.md)
- [Benchmark procedure](current/operations/67-oms-040-benchmark.md)
- [Benchmark evidence](current/operations/benchmark_results.md)
- [Service Level Objectives (SLO)](ops/SLO.md)
- [Disaster Recovery Plan (DRP)](ops/DRP.md)
- [Incident Response runbook](ops/IncidentResponse.md)

### Security

- [Security Policy](../SECURITY.md)
- [JWT Authentication](current/development/features/authentication-security/13-jwt-token-provider.md)
- [Security Config (Dual Filter Chain)](current/development/features/authentication-security/16-security-config.md)
- [Rate Limiting](current/development/features/cross-cutting/28-rate-limit.md)

### Architecture decisions (ADR)

- [ADR-001 — JWT authentication with refresh token rotation](adr/001-jwt-authentication.md)
- [ADR-002 — Database per profile](adr/002-database-per-profile.md)
- [ADR-003 — QueryDSL dynamic filtering](adr/003-querydsl-dynamic-filtering.md)
- [ADR-004 — Spring events async](adr/004-spring-events-async.md)
- [ADR-005 — Resilience4j patterns](adr/005-resilience4j-patterns.md)
- [ADR-006 — Package by-feature](adr/006-package-by-feature.md)

### Feature implementation

**Domain model**
- [Entity User & autentikasi](current/development/features/domain-model/05-entity-user-autentikasi.md)
- [Entity Product](current/development/features/domain-model/07-entity-product.md)
- [Entity Order](current/development/features/domain-model/08-entity-order.md)

**Authentication & security**
- [JWT Token Provider](current/development/features/authentication-security/13-jwt-token-provider.md)
- [Auth Controller](current/development/features/authentication-security/14-auth-controller.md)
- [JWT Authentication Filter](current/development/features/authentication-security/15-jwt-authentication-filter.md)
- [Security Config](current/development/features/authentication-security/16-security-config.md)
- [Refresh Token Redis](current/development/features/authentication-security/17-refresh-token-redis.md)

**Order & product**
- [Product Service](current/development/features/order-product/10-product-service.md)
- [Order Service](current/development/features/order-product/11-order-service.md)
- [Product Controller](current/development/features/order-product/12-product-controller.md)
- [QueryDSL Product Filter](current/development/features/order-product/20-querydsl-product-filter.md)
- [N+1 Order Items Fix](current/development/features/order-product/21-fix-n-plus-one-order-items.md)
- [Optimistic Locking Stock](current/development/features/order-product/22-optimistic-locking-stock.md)
- [Product Caching](current/development/features/order-product/26-product-caching.md)

**Cross-cutting**
- [Global Exception Handler (RFC 7807)](current/development/features/cross-cutting/19-global-exception-handler-rfc7807.md)
- [Soft Delete Semua Entity](current/development/features/cross-cutting/23-soft-delete-all-entities.md)
- [JPA Auditing](current/development/features/cross-cutting/24-jpa-auditing.md)
- [Redis Cache Config](current/development/features/cross-cutting/25-redis-cache-config.md)
- [Async Email Notification](current/development/features/cross-cutting/27-async-email-notification.md)
- [Rate Limit (Bucket4j)](current/development/features/cross-cutting/28-rate-limit.md)

## Historical documentation

- [Laporan analisis dan refactoring](history/reports/36-project-analysis-report.md)
### History & Audit Trail

- [Struktur proyek historis — V4 by-layer (superseded by ADR-006)](history/reports/37-project-structure-documentation.md)
- [Analisis baseline V0 — analisis awal (historical/superseded)](history/optimalisasi-v0/ANALISIS-PROYEK-OMS-V0.md)
- [Laporan fase implementasi V0](history/optimalisasi-v0/45-final-report.md)
- [Laporan fase V0 45–61](history/optimalisasi-v0/61-final-report-phase14.md)
- [Analisis baseline V1 — security finalization (historical/snapshot)](history/optimalisasi-v1/ANALISIS-PROYEK-OMS-V1.md)
- [Analisis baseline V2 — Phase 0–3 (historical/snapshot)](history/optimalisasi-v2/ANALISIS-PROYEK-OMS-V2.md)
- [Analisis baseline V3 — clean code, resilience, enterprise (historical/snapshot)](history/optimalisasi-v3/ANALISIS-PROYEK-OMS-V3.md)
- [Analisis baseline V4 — screening aktual kode; Phase V4-P0..P2 selesai (historical/snapshot)](history/optimalisasi-v4/ANALISIS-PROYEK-OMS-V4.md)
- [Analisis baseline V5 — restructure package by-feature / F-14; V5-P0 selesai / CLOSED (historical/snapshot)](history/optimalisasi-v5/ANALISIS-PROYEK-OMS-V5.md)

### Laporan fase optimalisasi V2

- [Fase 0 — Containment](history/optimalisasi-v2/01-final-report-phase0.md) (F-01, F-03, F-05, F-10)
- [Fase 1 — Auth & abuse prevention](history/optimalisasi-v2/02-final-report-phase1.md) (F-02, F-04)
- [Fase 2 — CI/CD & deployment hardening](history/optimalisasi-v2/03-final-report-phase2.md) (F-05, F-06, F-07)
- [Fase 3 — Quality & operability](history/optimalisasi-v2/04-final-report-phase3.md) (F-08, F-09, F-10)

### Laporan fase optimalisasi V3

- [Fase V3-0 — Security hardening](history/optimalisasi-v3/01-final-report-phase0.md)
- [Fase V3-1 — Resilience & reliability](history/optimalisasi-v3/02-final-report-phaseV3-1.md)
- [Fase V3-2 — Clean code, SOLID & structure](history/optimalisasi-v3/03-final-report-phaseV3-2.md)
- [Fase V3-3 — Governance & CI/CD enforcement](history/optimalisasi-v3/04-final-report-phaseV3-3.md)
- [Fase V3-4 — Enterprise readiness & final SOLID](history/optimalisasi-v3/05-final-report-phaseV3-4.md)
- [Fase R — Discrepancy closure & final compliance](history/optimalisasi-v3/06-final-report-phaseR.md)
- [Fase V3-5 — Audit alignment & hardening backlog](history/optimalisasi-v3/07-final-report-phaseV3-5.md)

### Laporan fase optimalisasi V4

- [Fase V4-P0 — Security hardening (V4-03, V4-09)](history/optimalisasi-v4/01-final-report-phaseV4-P0.md)
- [Fase V4-P1 — Code Quality & Environment Hardening (V4-01, V4-02, V4-04, V4-06)](history/optimalisasi-v4/02-final-report-phaseV4-P1.md)
- [Fase V4-P2 — Operational Maturity (V4-05, V4-07, V4-08, V4-10)](history/optimalisasi-v4/03-final-report-phaseV4-P2.md)

### Laporan fase optimalisasi V5

- [Analisis baseline V5 — restructure package by-feature / F-14 (historical/snapshot)](history/optimalisasi-v5/ANALISIS-PROYEK-OMS-V5.md)
- [Fase V5-P0 — Restructure Package by-Feature (F-14)](history/optimalisasi-v5/01-final-report-phaseV5-P0.md)

Dokumen historis dipertahankan untuk audit trail. Angka test, status issue, dan klaim teknis di dalamnya adalah snapshot pada tanggal masing-masing dan bukan source of truth current-state.

## Documentation governance

- [Siklus kerja optimalisasi (V0→VN)](optimization-workflow.md)
- [Template dokumentasi](_templates/README.md)
- [Contributing guide](../CONTRIBUTING.md)
- [Changelog](../CHANGELOG.md)
- [Rekomendasi struktur dokumentasi (historis, superseded)](history/reports/REKOMENDASI-STRUKTUR-DOKUMENTASI.md)

> **Catatan Skema C:** File historis sebagian masih memakai penomoran lama untuk menjaga audit trail. Dua dokumen yang telah digantikan versi current dipindahkan ke `docs/history/reports/` (Swagger `06`, exception handler `18`) dan ditandai `Superseded by`. Direktori arsip terpisah (`docs-archive/`) belum digunakan; seluruh dokumen historis tetap berada di `docs/history/`.