# Onboarding Guide — OMS

Welcome to the OMS (Order Management System) project! This guide will help you set up your development environment and understand the codebase.

---

## Reading Order for New Joiners

Baca dokumentasi dalam urutan ini agar memahami role, aturan, dan workflow
sebelum menyentuh kode. Entry point resmi seluruh dokumentasi adalah
[`docs/index.md`](docs/index.md).

### Tahap 1 — Setup & orientasi (kerjakan lebih dulu)

1. **`ONBOARDING.md`** (dokumen ini) — prasyarat, cara build/test/run, struktur
   proyek, quality gates, dan code conventions.
2. **[`README.md`](README.md)** — gambaran fitur, tech stack, dan konteks produk.

### Tahap 2 — Aturan main (WAJIB sebelum commit/PR)

3. **[`CONTRIBUTING.md`](CONTRIBUTING.md)** — role dan ketentuan inti:
   konvensi commit, alur PR, dan bagian **Documentation Governance** (taksonomi
   status, aturan source-of-truth, satu-topik-satu-canonical, index sebagai kontrak,
   template & penegakan otomatis).
4. **[`.github/PULL_REQUEST_TEMPLATE.md`](.github/PULL_REQUEST_TEMPLATE.md)** —
   checklist konkret yang harus dicentang di setiap PR (code quality, security,
   dan sinkronisasi dokumentasi).

### Tahap 3 — Cara kerja berulang (saat mengerjakan tugas optimalisasi)

5. **[`docs/optimization-workflow.md`](docs/optimization-workflow.md)** — siklus
   baku 7 langkah per versi: BASELINE → PLAN → EXECUTE → REPORT → SYNC → VERIFY →
   CLOSE. Ini peta kerja utama.
6. **[`docs/_templates/README.md`](docs/_templates/README.md)** + keempat template —
   salin template yang sesuai saat membuat dokumen baru (baseline, laporan fase,
   ADR, current-state) agar format tetap konsisten.

### Tahap 4 — Referensi teknis (buka saat dibutuhkan, bukan dibaca berurutan)

7. **[`docs/current/`](docs/current/)** — dokumentasi current-state (arsitektur,
   API, fitur, testing, operasi, standar development). Standar 13 pilar yang
   menjadi acuan audit optimalisasi dan ringkasan **Code Conventions** di bawah
   ini ada di
   [`docs/current/development/standards/STANDAR-DEVELOPMENT-CODE.md`](docs/current/development/standards/STANDAR-DEVELOPMENT-CODE.md).
8. **[`docs/adr/`](docs/adr/)** — alasan di balik keputusan arsitektur.
9. **[`docs/history/`](docs/history/)** — audit trail per versi (V0–V5). Ini
   snapshot historis, **bukan** source of truth current-state.

### Aturan penjaga agar tidak keluar jalur

- **Source of truth = kode** (`src/`, `pom.xml`, `application*.yml`). Bila dokumen
  bertentangan dengan kode, kode yang menang — perbarui dokumennya.
- **Sebelum push, jalankan lokal:** `./mvnw clean verify` (kode) dan
  `python3 scripts/check-docs.py` (dokumentasi: link, metadata, placeholder). CI
  menjalankan keduanya pada setiap PR.
- **Setiap perubahan kode current-state wajib disertai update `docs/current/**`**
  pada PR yang sama (langkah SYNC di `optimization-workflow.md`).

---

## Prerequisites

- **Java 21** (Temurin or Oracle JDK)
- **Maven 3.9+** (or use `./mvnw` wrapper)
- **Docker** (for Testcontainers integration tests)
- **Redis** (local or via Docker)
- **IDE:** IntelliJ IDEA or VS Code with Java Extension Pack

## Quick Start

### 1. Clone and Build

```bash
git clone https://github.com/your-org/oms.git
cd oms
./mvnw clean compile -Pdev
```

### 2. Run Tests

```bash
# Unit tests only (no Docker required)
./mvnw test -Pci

# Full verification (including quality gates)
./mvnw clean verify -Pci

# Integration tests (requires Docker for Testcontainers)
./mvnw test -Pintegration
```

### 3. Run the Application

```bash
# Dev profile (H2 in-memory database)
./mvnw spring-boot:run -Pdev

# The app starts on http://localhost:8080
# API docs: http://localhost:8080/swagger-ui.html
# Actuator: http://localhost:8081/actuator/health
```

## Project Structure

```
oms/
├── src/main/java/com/oms/
│   ├── auth/            # Feature auth (controller, service, dto, entity, repository, security, validation)
│   ├── order/           # Feature order (controller, service, dto, entity, repository)
│   ├── product/         # Feature product (controller, service, dto, entity, repository)
│   ├── admin/           # Feature admin (controller, service, dto)
│   ├── audit/           # Feature audit (@Auditable, aspect, entity, repository, service)
│   ├── ratelimit/       # Feature rate limiting (interceptor, service, stores, properties)
│   ├── system/          # Endpoint app-level (CsrfController, VersionController)
│   ├── validation/      # Validasi lintas-fitur (SafeHtml, InputSanitizer)
│   ├── config/          # Spring configuration classes (lintas-fitur)
│   │   ├── SecurityFilterChainConfig.java
│   │   ├── RedisConfig.java
│   │   └── ...
│   ├── constant/        # Constants lintas-fitur
│   ├── exception/       # Exception handlers (domain-split)
│   ├── health/          # Custom health indicators
│   ├── entity/          # BaseEntity (shared persistence base)
│   └── OmsApplication.java
├── src/main/resources/
│   ├── application.yml          # Base config
│   ├── application-dev.yml      # Dev (H2)
│   ├── application-staging.yml  # Staging (PostgreSQL)
│   ├── application-prod.yml     # Production (PostgreSQL)
│   └── db/migration/            # Flyway migrations (V1–V7)
├── src/test/java/com/oms/      # 60+ test files (mirror struktur feature)
├── config/
│   ├── checkstyle/              # Checkstyle rules
│   └── spotbugs/                # SpotBugs excludes
├── k8s/                         # Kubernetes manifests (Kustomize)
├── docs/                        # Documentation (entry point: docs/index.md)
│   ├── index.md                 # Documentation entry point (start here)
│   ├── optimization-workflow.md # Per-version optimization cycle (BASELINE→…→CLOSE)
│   ├── _templates/              # Canonical doc templates (copy when creating docs)
│   ├── adr/                     # Architecture Decision Records (001–006)
│   ├── current/                 # Current-state docs (api, features, testing, ops, standards)
│   ├── ops/                     # Operational runbooks (SLO, DRP, Incident Response)
│   └── history/                 # Optimization history + snapshots (V0–V5)
└── .github/
    ├── workflows/ci-cd.yml     # CI/CD pipeline
    ├── PULL_REQUEST_TEMPLATE.md
    ├── ISSUE_TEMPLATE/
    └── CODEOWNERS
```

## Key Architecture Decisions

See [docs/adr/](docs/adr/) for architectural decisions:

1. **ADR-001:** JWT Authentication with Refresh Token Rotation
2. **ADR-002:** Database Per Profile (H2 Dev / PostgreSQL Staging+Prod)
3. **ADR-003:** QueryDSL for Dynamic Filtering
4. **ADR-004:** Spring Events for Async Processing
5. **ADR-005:** Resilience4j for Circuit Breaker and Retry
6. **ADR-006:** Package by-Feature (V5-P0 — domain code in `com.oms.{auth,order,product,admin,audit}`)

## Quality Gates

All PRs must pass these checks (enforced in CI):

| Gate | Command | Threshold |
|---|---|---|
| Checkstyle | `./mvnw checkstyle:check` | 0 violations |
| SpotBugs | `./mvnw spotbugs:check` | 0 findings |
| JaCoCo | `./mvnw jacoco:report jacoco:check` | ≥ 80% line coverage |
| Tests | `./mvnw clean verify` | 0 failures |

## Code Conventions

1. **Package by feature** — domain code lives in `com.oms.{auth,order,product,admin,audit,ratelimit}` with layer subpackages (`controller`, `service`, `dto`, `entity`, `repository`); cross-cutting infra stays in `config`, `exception`, `constant`, `health`, `validation` (ADR-006)
2. **Constructor injection only** — no `@Autowired` field injection
3. **DTOs as Java records** — immutable, thread-safe
4. **Service interfaces** — all services implement interfaces (DIP)
5. **Constants split by domain** — shared constants from `com.oms.constant.*`; feature-specific constants live in their feature package
6. **Exception handlers** — domain-specific `@RestControllerAdvice` classes
7. **Javadoc** on all public classes and methods
8. **No magic strings/numbers** — use constants

## Development Workflow

Alur singkat untuk perubahan sehari-hari (detail lengkap siklus optimalisasi per
versi ada di [`docs/optimization-workflow.md`](docs/optimization-workflow.md)):

1. Create a feature branch: `git checkout -b feature/TICKET-description`
2. Make changes following code conventions
3. Write/update tests
4. Sinkronkan dokumentasi: perbarui `docs/current/**` bila perilaku current-state
   berubah, dan `docs/index.md` bila ada dokumen ditambah/dipindah (langkah SYNC)
5. Run `./mvnw clean verify` dan `python3 scripts/check-docs.py` locally
6. Open a PR using the PR template dan centang seluruh checklist
7. Wait for CODEOWNERS review and CI pass
8. Merge after approval

## Common Issues

### Tests fail with "Connection refused"
Ensure Redis is running: `docker run -d -p 6379:6379 redis:7-alpine`

### Checkstyle violations
Run `./mvnw checkstyle:check` to see violations. The project uses Checkstyle with FileLength=500, MethodLength=50, LineLength=120.

### SpotBugs warnings
Run `./mvnw spotbugs:check` to see findings. Check `config/spotbugs/spotbugs-exclude.xml` for known Lombok/QueryDSL exclusions.

### Profile not activating
Use `-Dspring.profiles.active=dev` or the Maven profile `-Pdev`. Do not set `spring.profiles.active` in `application.yml` (F-22 fix).
