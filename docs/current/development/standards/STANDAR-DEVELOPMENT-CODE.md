# 📋 STANDAR DEVELOPMENT CODE PROFESIONAL

> **Status:** Current
> **Audience:** Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-29 (screening kode aktual `src/`, `pom.xml`)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## Panduan Lengkap untuk Tim Pengembangan Perangkat Lunak (Java Backend Edition)

**Versi:** 3.0  
**Tanggal:** 10 Agustus 2026  
**Status:** Aktif

---

## Daftar Isi

1. [🧼 Clean Code](#1-clean-code-kebersihan--keterbacaan-kode)
2. [⚡ Optimal](#2-optimal-kinerja--efisiensi)
3. [♻️ Reusable](#3-reusable-modular--dapat-digunakan-ulang)
4. [🌍 Environment-aware](#4-environment-aware-sadar-lingkungan--konfigurasi)
5. [🔒 Secure](#5-secure-keamanan)
6. [🧪 Testable](#6-testable-pengujian)
7. [🔀 Version Control](#7-version-control-manajemen-kode-dengan-git)
8. [📚 Documentation](#8-documentation-dokumentasi)
9. [🚀 CI/CD & Observability](#9-cicd--observability-otomatisasi--pemantauan)
10. [👥 Collaboration & Management](#10-collaboration--management-kolaborasi--manajemen-proyek)
11. [🗄️ Database & Data Management](#11-database--data-management)
12. [🏢 Enterprise Level](#12-enterprise-level-governance-security--resilience)
13. [🌐 Additional Standards](#13-additional-standards-standar-tambahan)

---

## 1. 🧼 CLEAN CODE (Kebersihan & Keterbacaan Kode)

| No | Standar | Praktik Spesifik | Contoh Penerapan |
|:---:|:---|:---|:---|
| 1.1 | Self-Documenting Names | Nama variabel/fungsi/kelas harus deskriptif dan menjelaskan tujuan | `getActiveUsers()` ✅ vs `getData()` ❌ |
| 1.2 | Consistent Formatting | Indentasi, spasi, dan panjang baris harus seragam | Prettier, Black, gofmt, ESLint, atau **Google Java Style Guide** |
| 1.3 | Meaningful Comments | Komentar untuk menjelaskan "mengapa", bukan "apa" | `// Retry karena API sering timeout di jam sibuk` |
| 1.4 | Small Functions & Classes | Satu fungsi/kelas hanya melakukan SATU hal (SRP) | `validateEmail()` terpisah dari `sendEmail()` |
| 1.5 | No Magic Numbers/Strings | Semua konstanta wajib diberi nama | `private static final int MAX_RETRY = 3;` ✅ vs `if (retry > 3)` ❌ |
| 1.6 | DRY (Don't Repeat Yourself) | Hindari duplikasi kode; ekstrak ke fungsi/modul reusable | Buat `CurrencyFormatter` sekali, panggil di banyak tempat |
| 1.7 | KISS (Keep It Simple, Stupid) | Prioritaskan solusi sederhana daripada yang kompleks | Pilih loop biasa daripada pattern yang over-engineered |
| 1.8 | Single Responsibility Principle | Setiap modul punya satu alasan untuk berubah | Pisahkan business logic dari controller/route |
| 1.9 | Favor Immutability | Utamakan pembuatan objek immutable menggunakan `record` dan `final` | `public record User(Long id, String name) {}` daripada class dengan setter |
| 1.10 | Use Modern Java Features | Adopsi fitur Java 17+ (Records, Pattern Matching, Switch Expressions, Sealed Classes, Virtual Threads) | `switch (obj) { case String s -> ...; case Integer i -> ...; }` |
| 1.11 | Constructor Injection | Gunakan constructor injection untuk Dependency Injection | `@AllArgsConstructor` (Lombok) atau constructor manual, hindari `@Autowired` field injection |
| 1.12 | Package by Feature | Organisasi package berdasarkan fitur, bukan teknologi | `com.example.order` berisi `controller`, `service`, `repository` untuk Order |

---

## 2. ⚡ OPTIMAL (Kinerja, Efisiensi & Skalabilitas)

| No | Standar | Praktik Spesifik | Contoh Penerapan |
|:---:|:---|:---|:---|
| 2.1 | Algorithm Complexity | Pilih struktur data & algoritma yang efisien sesuai use case | Pakai `HashMap` untuk O(1) lookup, hindari nested loop O(n²), gunakan `Collections.binarySearch()` untuk data terurut |
| 2.2 | Caching Strategy | Terapkan multi-level caching untuk mengurangi beban backend | **L1 Cache**: Caffeine/Guava Cache (in-memory) <br> **L2 Cache**: Redis/Memcached <br> **L3 Cache**: CDN untuk asset statis |
| 2.3 | Database Query Optimization | Optimasi query database secara menyeluruh | Hindari N+1 query di JPA/Hibernate (`@EntityGraph`), gunakan Projections (DTO), SELECT spesifik, gunakan `EXPLAIN` untuk analisis |
| 2.4 | Lazy Loading & On-Demand | Muat data/resource hanya saat benar-benar dibutuhkan | Hibernate `FetchType.LAZY`, paginasi API (Pageable), streaming response |
| 2.5 | Memory Management | Kelola memori dengan baik, cegah memory leak | Bersihkan resource di `finally` atau try-with-resources, hindari `ThreadLocal` tanpa cleanup, perhatikan referensi yang tidak terpakai |
| 2.6 | Avoid Premature Optimization | Optimasi berdasarkan data, bukan asumsi | Tulis kode benar dulu, gunakan profiler (JProfiler, YourKit, async-profiler), baru optimasi di bottleneck |
| 2.7 | Connection Pooling | Kelola koneksi database dengan pool yang tepat | HikariCP dengan `maximumPoolSize` sesuai beban (default 10) |
| 2.8 | Asynchronous & Non-Blocking | Manfaatkan async/non-blocking untuk operasi I/O | Spring WebFlux, CompletableFuture, Virtual Threads (Java 21+) untuk parallel processing |
| 2.9 | Payload Size Optimization | Minimalkan ukuran data yang dikirim melalui jaringan | Gunakan kompresi (gzip/brotli), paginasi, field selection (GraphQL), Protobuf, atau MessagePack |
| 2.10 | Resource Throttling | Batasi resource yang digunakan per request/user | **Rate limiting** per endpoint, **Circuit Breaker** (Resilience4j), **Bulkhead pattern** untuk isolasi resource |
| 2.11 | Cold Start Optimization | Kurangi waktu startup aplikasi (terutama untuk serverless/container) | Minimalkan dependency, gunakan lazy initialization, AppCDS, Spring AOT (Native), optimasi container image (multi-stage build) |
| 2.12 | Horizontal Scalability | Desain aplikasi agar bisa scale out dengan mudah | Pastikan stateless, gunakan distributed cache (Redis), shared storage (S3, RDS), dan session externalization |
| 2.13 | Background Processing | Pindahkan task berat ke background job | Gunakan queue (RabbitMQ, Kafka, SQS) dengan Spring Batch atau task scheduler untuk: email, report, image processing, batch data |
| 2.14 | Performance Budget | Tetapkan batas performa untuk setiap fitur kritis | API response time < 200ms (p95), Startup < 5 detik, Heap < 1GB, GC pause < 100ms |
| 2.15 | CDN & Edge Computing | Manfaatkan CDN dan edge untuk latensi rendah | Serve static asset via CDN (Cloudflare, AWS CloudFront), API di edge (Cloudflare Workers) |
| 2.16 | Database Indexing Strategy | Buat index yang tepat berdasarkan pola query | Index composite untuk query multi-kolom, partial index, rutin analisis query menggunakan `pg_stat_statements` |
| 2.17 | JVM & Garbage Collection Tuning | Pilih dan konfigurasi Garbage Collector sesuai profil beban kerja | **G1GC** untuk throughput tinggi, **ZGC** untuk latensi sangat rendah (<1ms), **Shenandoah** untuk heap besar |
| 2.18 | Application Class-Data Sharing (AppCDS) | Manfaatkan AppCDS untuk meningkatkan waktu startup dan mengurangi footprint memori | Gunakan AppCDS di container untuk mempercepat cold start di platform cloud/serverless |
| 2.19 | Virtual Threads | Gunakan Virtual Threads (Java 21+) untuk aplikasi dengan banyak operasi I/O | `Thread.startVirtualThread()` atau `Executors.newVirtualThreadPerTaskExecutor()` untuk handling request scale tinggi |
| 2.20 | Reactive Programming | Untuk aplikasi dengan lalu lintas tinggi, gunakan arsitektur non-blocking | Spring WebFlux, Quarkus, Micronaut untuk mengurangi jumlah thread dan mengoptimasi resource |

---

## 3. ♻️ REUSABLE (Modular & Dapat Digunakan Ulang)

| No | Standar | Praktik Spesifik | Contoh Penerapan |
|:---:|:---|:---|:---|
| 3.1 | Modularization | Pisahkan kode ke dalam modul/fungsi independen | Folder `/common`, `/shared`, modul Maven/Gradle terpisah, Java `module-info.java` |
| 3.2 | Composition over Inheritance | Gunakan komposisi daripada turunan kelas yang dalam | Dependency Injection, Strategy Pattern, Decorator Pattern |
| 3.3 | Generic Utilities | Buat utility class yang generik & tidak terikat domain | `DateUtils`, `StringUtils`, `NumberUtils` dengan method `static` |
| 3.4 | Parameterization | Hindari hardcode; gunakan parameter/konfigurasi | `@Value("${app.feature.enabled}")` untuk konfigurasi |
| 3.5 | Shared Library | Bangun library internal yang dipakai banyak proyek | Multi-module Maven, internal artifact di Nexus/Artifactory |
| 3.6 | Interface/Contract First | Definisikan interface sebelum implementasi | OpenAPI spec, GraphQL schema, atau Java Interface (`@FeignClient`) |
| 3.7 | Scalable Dependency Management | Kelola dependensi dengan baik untuk menghindari konflik | Maven `dependencyManagement`, Gradle `platform`, perhatikan transitive dependency |

---

## 4. 🌍 ENVIRONMENT-AWARE (Sadar Lingkungan & Konfigurasi)

| No | Standar | Praktik Spesifik | Contoh Penerapan |
|:---:|:---|:---|:---|
| 4.1 | Externalize Configuration | Semua konfigurasi via environment variables atau property file | `@ConfigurationProperties`, `application-{profile}.yml`, `.env` |
| 4.2 | Graceful Degradation | Tetap berjalan walau service pendukung mati | Jika Redis error, fallback ke cache in-memory |
| 4.3 | Feature Toggles | Aktif/nonaktifkan fitur tanpa deploy ulang | Spring Cloud Config, LaunchDarkly, atau `@ConditionalOnProperty` |
| 4.4 | Statelessness | Aplikasi stateless agar mudah di-scale horizontal | JWT session di client, gunakan externalized session (Redis) |
| 4.5 | Environment Detection | Hindari logika berbasis nama env; pakai konfigurasi boolean | `@Profile("debug")` ✅ vs `if (env == "staging")` ❌ |
| 4.6 | 12-Factor App Principles | Ikuti prinsip 12-factor app untuk portabilitas maksimal | Codebase tunggal, dependency explicit, config terpisah |
| 4.7 | Environment Abstraction | Gunakan alat untuk memastikan lingkungan konsisten | Testcontainers untuk testing, Infrastructure as Code (IaC) untuk environment |

---

## 5. 🔒 SECURE (Keamanan)

| No | Standar | Praktik Spesifik | Contoh Penerapan |
|:---:|:---|:---|:---|
| 5.1 | Input Validation | Validasi semua input user (pakai whitelist, bukan blacklist) | `@Valid` (Jakarta Bean Validation), Joi, Yup, atau Zod |
| 5.2 | Data Encryption | Enkripsi data sensitif (password, PII) di rest dan transit | Bcrypt/Argon2 untuk password, TLS/HTTPS untuk transfer |
| 5.3 | SQL/NoSQL Injection | Selalu gunakan parameterized query atau ORM | JPA `@Query` dengan parameter binding ✅ vs raw string query ❌ |
| 5.4 | Least Privilege | Beri akses seminimal mungkin ke database/API | `@PreAuthorize`, `@Secured`, Read-only user untuk query report |
| 5.5 | Dependency Scanning | Scan library pihak ketiga untuk kerentanan | `mvn dependency-check`, Snyk, Dependabot |
| 5.6 | Rate Limiting & Throttling | Lindungi backend dari brute-force/traffic berlebih | Resilience4j RateLimiter, Bucket4j, 100 request/menit per IP |
| 5.7 | Security Headers | Pasang header keamanan standar | Spring Security headers, CSP, X-Frame-Options |
| 5.8 | Secret Management | Jangan simpan secret di kode; gunakan vault | AWS Secrets Manager, HashiCorp Vault, Doppler, `@Value` + env |
| 5.9 | Secure Serialization | Hindari serialisasi Java standar. Gunakan JSON/Protobuf | Gunakan Jackson/Protobuf daripada `ObjectOutputStream`, terapkan Serialization Filter jika terpaksa |
| 5.10 | Authentication & Authorization | Implementasikan autentikasi dan otorisasi yang robust | Spring Security + JWT, OAuth2, OIDC, RBAC |
| 5.11 | CORS Configuration | Konfigurasi CORS yang ketat | `@CrossOrigin(origins = {"https://example.com"})` |

---

## 6. 🧪 TESTABLE (Pengujian)

| No | Standar | Praktik Spesifik | Contoh Penerapan |
|:---:|:---|:---|:---|
| 6.1 | Unit Test | Tulis tes untuk setiap fungsi/logika kritis | JUnit 5 + Mockito, AssertJ, dengan pola AAA (Arrange-Act-Assert) |
| 6.2 | Integration Test | Tes interaksi antar modul/database/API | @SpringBootTest + Testcontainers, @DataJpaTest |
| 6.3 | E2E Test | Tes alur end-to-end dari UI ke backend | Cypress, Selenium, Playwright, atau RestAssured |
| 6.4 | Mocking | Mock dependency eksternal (API, DB) agar tes stabil | Mockito, WireMock untuk API mock, Testcontainers untuk DB mock |
| 6.5 | Code Coverage | Target minimal coverage (misal >80%) | JaCoCo, Cobertura, SonarQube |
| 6.6 | TDD (Optional) | Tulis tes gagal dulu, baru kode, lalu refactor | Red-Green-Refactor cycle |
| 6.7 | Test Naming Convention | Nama test harus menjelaskan skenario dan ekspektasi | `shouldReturn401WhenTokenIsInvalid()` |
| 6.8 | Testcontainers | Gunakan Testcontainers untuk integration test yang realistis | PostgreSQL container, Redis container, Kafka container di test |

---

## 7. 🔀 VERSION CONTROL (Manajemen Kode dengan Git)

| No | Standar | Praktik Spesifik | Contoh Penerapan |
|:---:|:---|:---|:---|
| 7.1 | Branching Strategy | Gunakan strategi branching yang terstruktur | **Git Flow** (master/dev/feature/hotfix) atau **GitHub Flow** |
| 7.2 | Commit Message | Tulis pesan commit terstruktur & informatif | **Conventional Commits**: `feat: add login endpoint` |
| 7.3 | Atomic Commits | Satu commit untuk SATU perubahan logis | Pisahkan `fix: typo` dan `feat: add dark mode` |
| 7.4 | Pull/Merge Request | Wajib PR/MR untuk setiap perubahan, minimal 1 reviewer | Review checklist: cek kode, cek test, cek dokumentasi |
| 7.5 | Pre-commit Hooks | Jalankan linter & formatter otomatis sebelum commit | Husky + lint-staged, Maven spotless plugin |
| 7.6 | Semantic Versioning | Versi rilis mengikuti format MAJOR.MINOR.PATCH | 2.1.0 (breaking change.minor fix.bug fix) |
| 7.7 | Sign-off & GPG | Tandai commit dengan GPG untuk verifikasi identitas | `git commit -S -m "feat: add feature"` |

---

## 8. 📚 DOCUMENTATION (Dokumentasi)

| No | Standar | Praktik Spesifik | Contoh Penerapan |
|:---:|:---|:---|:---|
| 8.1 | README.md | Setiap repositori wajib punya README yang jelas | Cara install, cara run, env vars, cara test, struktur folder |
| 8.2 | Inline Docs (Docstring) | Dokumentasi API/function dengan format baku | Javadoc untuk Java, Docstring Python, JSDoc untuk JS |
| 8.3 | ADR (Architecture Decision Record) | Dokumentasikan keputusan arsitektur penting | Folder `/docs/adr/` berisi `adr-001-use-postgres.md` |
| 8.4 | API Documentation | Dokumentasi endpoint API interaktif | Swagger/OpenAPI, Springdoc OpenAPI, Postman Collection |
| 8.5 | Changelog | Catat setiap perubahan signifikan per versi rilis | `CHANGELOG.md` format **Keep a Changelog** |
| 8.6 | Onboarding Guide | Dokumen panduan untuk anggota baru | File `ONBOARDING.md` di root repositori |
| 8.7 | Internal Wiki | Pusatkan semua dokumentasi di satu tempat | Confluence, Notion, atau GitBook |

---

## 9. 🚀 CI/CD & OBSERVABILITY (Otomatisasi & Pemantauan)

| No | Standar | Praktik Spesifik | Contoh Penerapan |
|:---:|:---|:---|:---|
| 9.1 | Continuous Integration (CI) | Setiap push/PR wajib otomatis menjalankan test & linter | GitHub Actions, GitLab CI, Jenkins, CircleCI |
| 9.2 | Build Automation | Proses build otomatis dan reproducible | Maven `mvn clean package`, Gradle `./gradlew build`, docker build |
| 9.3 | Continuous Delivery (CD) | Deploy otomatis ke staging setelah test passing | Auto-deploy ke staging tiap merge ke `develop` |
| 9.4 | Zero-downtime Deployment | Strategi deploy tanpa mematikan service | Blue-Green Deployment, Canary Release, Rolling Update di Kubernetes |
| 9.5 | Rollback Strategy | Mekanisme rollback cepat (< 5 menit) jika deploy gagal | Git revert + auto-deploy ulang, Kubernetes rollback |
| 9.6 | Structured Logging | Log dalam format terstruktur (JSON) agar mudah dicari | Logback + JSON encoder, Log4j2, ELK Stack |
| 9.7 | Distributed Tracing | Lacak request yang melintasi banyak service | Jaeger, Zipkin, AWS X-Ray, Micrometer Tracing |
| 9.8 | Metrics & Dashboard | Pantau health aplikasi (CPU, memory, error rate, latency) | Prometheus + Grafana, Micrometer, Datadog, New Relic |
| 9.9 | Alerting | Kirim notifikasi otomatis jika metrik melewati threshold | Alert via Slack, PagerDuty, Opsgenie |
| 9.10 | SLI/SLO | Tentukan target performa (latency < 200ms untuk 99% request) | Service Level Indicator & Service Level Objective |
| 9.11 | JVM Metrics | Monitor JVM-specific metrics | GC metrics, heap usage, thread count, class loading via Micrometer + Prometheus |

---

## 10. 👥 COLLABORATION & MANAGEMENT (Kolaborasi & Manajemen Proyek)

| No | Standar | Praktik Spesifik | Contoh Penerapan |
|:---:|:---|:---|:---|
| 10.1 | Code Review Checklist | Setiap PR dicek dengan daftar kriteria minimal | Mudah dipahami? Ada test? Ada dokumentasi? Aman? |
| 10.2 | Small PRs | Batasi PR maksimal 300-400 baris agar review efektif | Pecah fitur besar jadi beberapa PR kecil |
| 10.3 | Reviewer Assignment | Tugaskan 1-2 reviewer kompeten di area tersebut | Assign ke senior engineer atau owner modul |
| 10.4 | Constructive Feedback | Kritik spesifik, sopan, dan menawarkan solusi | "Saran: pindahkan logika ini ke service terpisah" ✅ |
| 10.5 | Issue Tracking | Setiap task/bug/feature wajib punya tiket | Jira, Linear, Trello, GitHub Issues |
| 10.6 | Definition of Done (DoD) | Kriteria "selesai" yang jelas sebelum task ditutup | Kode ✅, test ✅, review ✅, deploy staging ✅ |
| 10.7 | Sprint Planning & Retro | Ikuti siklus Agile (Scrum/Kanban) konsisten | Sprint 2 minggu: planning, review, retro |
| 10.8 | Estimasi Task | Beri estimasi waktu/effort untuk setiap task | Fibonacci (1,2,3,5,8,13) untuk story points |
| 10.9 | Daily Standup | Update harian singkat (maks 15 menit) | Apa yang dikerjakan, hambatan, rencana selanjutnya |
| 10.10 | Pair Programming | Untuk task kompleks, kerjakan berdua | Driver-navigator, bergantian tiap 30 menit |
| 10.11 | Tech Talk / Showcase | Presentasikan solusi/teknologi baru ke tim | Share session tiap 2 minggu |

---

## 11. 🗄️ DATABASE & DATA MANAGEMENT

| No | Standar | Praktik Spesifik | Contoh Penerapan |
|:---:|:---|:---|:---|
| 11.1 | Schema Migration | Semua perubahan struktur DB wajib pakai migration | Flyway, Liquibase, Alembic, atau Hibernate `ddl-auto=validate` di production |
| 11.2 | Data Seeding | Sediakan data dummy untuk development & testing | `data.sql`, `import.sql`, atau seeder class di test |
| 11.3 | Query Optimization | Wajib pakai `EXPLAIN`/`EXPLAIN ANALYZE` untuk query kompleks | Cek apakah query memakai index atau full table scan |
| 11.4 | Backup & Recovery | Backup rutin dan bisa dipulihkan (RTO/RPO) | Backup harian ke S3, uji restore di staging |
| 11.5 | Data Privacy | PII wajib di-masking/anonymized di non-prod | `faker.js`, Java Faker, atau database view dengan masking |
| 11.6 | Database Indexing | Buat index yang tepat untuk query yang sering dipakai | `@Index` di JPA, index pada kolom yang sering dipakai di `WHERE` |
| 11.7 | Read Replica | Pisahkan query baca dan tulis untuk scaling | Gunakan replica untuk reporting/analytics, `@Transactional(readOnly = true)` |
| 11.8 | N+1 Query Prevention | Hindari N+1 query di JPA/Hibernate | `@EntityGraph`, `fetch join`, atau `@NamedEntityGraph` |

---

## 12. 🏢 ENTERPRISE LEVEL (Governance, Security & Resilience)

| No | Standar | Praktik Spesifik | Contoh Penerapan |
|:---:|:---|:---|:---|
| 12.1 | Architecture Framework | Gunakan standar arsitektur formal (ISO/IEC/IEEE 42010) | Architecture Description (AD) dengan viewpoints terstruktur |
| 12.2 | Process Framework | Terapkan proses siklus hidup (ISO/IEC/IEEE 12207) | Proses formal untuk akuisisi, pengembangan, operasi, pemeliharaan |
| 12.3 | DevOps Capability | Acu model kapabilitas DevOps terstandar (ISO/IEC NP 33203) | Domain manajemen proyek, peningkatan proses, manajemen infrastruktur |
| 12.4 | Security Standards | Terapkan standar keamanan ketat (OWASP Top 10) | Validasi input & encoding output sebagai aturan yang ditegakkan |
| 12.5 | Compliance Roadmap | Patuhi regulasi (SOC 2, ISO 27001, GDPR, HIPAA) | Audit internal berkala, dokumentasi kebijakan keamanan |
| 12.6 | Enterprise SSO | Dukung SAML, OIDC, dan SCIM untuk otomatisasi user | Spring Security + Okta, Azure AD, Google Workspace |
| 12.7 | High Availability (HA) | Redundansi lintas region & availability zone | Failover otomatis, health check berkelanjutan, Kubernetes multi-zone |
| 12.8 | Disaster Recovery Plan (DRP) | Dokumen DRP dengan RTO & RPO jelas, uji rutin | Prosedur pemulihan teruji secara berkala |
| 12.9 | Release Management | Strategi rilis matang (canary, staged rollouts) | Deployment terkontrol, minim risiko |
| 12.10 | Multi-Tenant Management | Konsol admin untuk manajemen organisasi | Kelola anggota, peran, penggunaan, penagihan |
| 12.11 | Audit Log Terpusat | Catat semua kejadian kritis di audit log immutable | Login, perubahan konfigurasi, akses API, @Auditable |
| 12.12 | AI Coding Guidelines | Panduan eksplisit untuk agen AI (Claude, Copilot) | `INDEX.md` dengan keputusan arsitektur & aturan spesifik |
| 12.13 | Automated Enforcement | Standar ditegakkan otomatis via pre-commit & CI | Pipeline gagal jika ada pelanggaran standar koding (checkstyle, spotbugs) |
| 12.14 | SLA (Service Level Agreement) | Komitmen uptime & performa ke pengguna | 99.9% uptime = down maksimal ~43 menit/bulan |
| 12.15 | Cost Optimization (FinOps) | Pantau biaya cloud & optimasi resource | Matikan instance tidak terpakai, pilih storage class tepat |
| 12.16 | Post-Mortem (Incident Review) | Laporan blameless jika terjadi insiden | Kronologi, dampak, akar masalah, action item |
| 12.17 | Software Bill of Materials (SBOM) | Hasilkan SBOM untuk keamanan rantai pasok | Maven `dependency-list` plugin, `cyclonedx-maven-plugin`, `syft` |

---

## 13. 🌐 ADDITIONAL STANDARDS (Standar Tambahan)

| No | Standar | Praktik Spesifik | Contoh Penerapan |
|:---:|:---|:---|:---|
| 13.1 | Accessibility (a11y) | Aplikasi bisa diakses penyandang disabilitas | WAI-ARIA, semantic HTML, contrast ratio |
| 13.2 | Internationalization (i18n) | Kode siap mendukung multi-bahasa | i18next, `MessageSource` di Spring, tidak boleh ada teks hardcode di UI |
| 13.3 | API Versioning | API publik wajib punya versi | `/api/v1/`, `/api/v2/` agar tidak merusak client lama |
| 13.4 | API Rate Limiting | Batasi request per user/ip | 100 request/menit per IP |
| 13.5 | CORS Policy | Konfigurasi CORS yang ketat | Hanya izinkan domain tertentu, bukan `*` di production |
| 13.6 | Dependency Management | Kelola library pihak ketiga dengan aman | Gunakan lockfile, scan vulnerability rutin, Maven `dependencyManagement` |

---

## 📊 RINGKASAN STATISTIK

| **Pilar** | **Jumlah Standar** |
|:---|:---:|
| 1. Clean Code | 12 |
| 2. Optimal | 20 |
| 3. Reusable | 7 |
| 4. Environment-aware | 7 |
| 5. Secure | 11 |
| 6. Testable | 8 |
| 7. Version Control | 7 |
| 8. Documentation | 7 |
| 9. CI/CD & Observability | 11 |
| 10. Collaboration & Management | 11 |
| 11. Database & Data Management | 8 |
| 12. Enterprise Level | 17 |
| 13. Additional Standards | 6 |
| **TOTAL** | **132** |

---

## 🔧 TOOLS PENDUKUNG PER PILAR

| **Pilar** | **Tools Populer** |
|:---|:---|
| Clean Code | Checkstyle, PMD, SpotBugs, SonarQube, Google Java Style Guide |
| Optimal | JProfiler, YourKit, async-profiler, New Relic, pg_stat_statements, Redis, Memcached, Cloudflare CDN, AWS CloudFront, RabbitMQ, Kafka, SQS |
| Reusable | Maven, Gradle, Nexus, Artifactory, Storybook (UI) |
| Environment-aware | Spring Cloud Config, dotenv, Docker, Kubernetes, Consul, Vault, Testcontainers |
| Secure | Snyk, Dependabot, OWASP ZAP, Bcrypt, Spring Security, JWT |
| Testable | JUnit 5, Mockito, AssertJ, Testcontainers, RestAssured, JaCoCo, SonarQube |
| Version Control | Git, GitHub, GitLab, Bitbucket |
| Documentation | Swagger, Springdoc OpenAPI, Redoc, Confluence, Notion, MkDocs |
| CI/CD | GitHub Actions, GitLab CI, Jenkins, CircleCI, ArgoCD, Kubernetes |
| Observability | Prometheus, Grafana, Micrometer, Datadog, New Relic, ELK Stack, Jaeger, Zipkin |
| Database | Flyway, Liquibase, Hibernate, jOOQ, PostgreSQL, MySQL, Oracle |
| Enterprise | Okta, Azure AD, AWS, GCP, HashiCorp Vault, Kubernetes |

---

## 📝 REVISI DOKUMEN

| **Versi** | **Tanggal** | **Perubahan** | **Author** |
|:---:|:---|:---|:---|
| 1.0 | 01-01-2026 | Initial document | Tim Engineering |
| 2.0 | 10-08-2026 | Penambahan Enterprise Level & Additional Standards | Tim Engineering |
| 2.1 | 10-08-2026 | Perkayaan bagian Optimal (dari 7 menjadi 16 standar) | Tim Engineering |
| 3.0 | 10-08-2026 | Penambahan poin spesifik Java Backend (Clean Code +4, Optimal +4, Secure +3, Testable +1, Database +1, Enterprise +1) | Tim Engineering |

---

**📧 Kontak:** Untuk pertanyaan atau saran terkait dokumen ini, silakan hubungi tim Engineering Lead.

---

*Dokumen ini adalah panduan hidup (living document) yang akan terus diperbarui sesuai dengan perkembangan teknologi dan kebutuhan tim.*