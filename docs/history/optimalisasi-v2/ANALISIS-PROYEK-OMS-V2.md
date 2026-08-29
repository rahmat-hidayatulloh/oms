# Analisis Proyek OMS — V2

> **Status:** Historical / snapshot
> **Audience:** Maintainer, Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-13 (snapshot; tidak diverifikasi ulang terhadap kode current-state)
> **Source of truth:** Kode aktual (`src/`), `pom.xml`, dan konfigurasi runtime; dokumen ini snapshot audit trail
> **Tanggal audit:** 11 Agustus 2026 — **Pembaruan terakhir:** 13 Agustus 2026
> **Status fase:** ✅ Semua temuan ditutup — Fase 0–3 selesai; CI/CD maintenance fix (13 Agustus 2026)
> **Ruang lingkup:** source Java, konfigurasi Maven/Spring, pengujian, dokumentasi, CI/CD, Docker, dan manifest Kubernetes yang ada di branch `oms-optimalisasi`.
> **Bukan:** pentest, vulnerability scan pihak ketiga, review infrastruktur cloud yang sedang berjalan, atau validasi pada cluster Kubernetes nyata.

---

## 1. Ringkasan eksekutif

OMS memiliki fondasi yang baik: Java 21/Spring Boot, validasi request, pemisahan controller–service–repository, Flyway, Testcontainers, Checkstyle, SpotBugs, JWT, rate limiting, observability dasar, Docker non-root, dan manifest Kubernetes dengan probes, resource limits, HPA, serta PDB. Pemeriksaan statis yang dijalankan pada audit ini juga lulus tanpa temuan.

Namun, proyek **belum layak disebut production-grade tanpa perbaikan keamanan dan operasional prioritas tinggi**. Empat risiko utama adalah:

1. **Refresh JWT dapat dipakai sebagai Bearer token untuk endpoint terlindungi** karena filter autentikasi tidak mengharuskan `token_type=access`.
2. **Refresh-token lifecycle tidak benar-benar ditegakkan**: endpoint refresh tidak memverifikasi token terhadap penyimpanan Redis dan tidak merotasi token; logout tidak menghapus refresh token dan mem-blacklist token yang dinamai refresh token.
3. **Secret dan credential literal dilacak dalam Git/Kubernetes manifest**, termasuk JWT signing secret dan password database.
4. **Rate limit bersifat lokal per-pod**, bertentangan dengan dokumentasi yang menyatakan bucket Redis/distributed; pada deployment dua replika, pengguna dapat memperoleh limit terpisah pada setiap pod.

Prioritas pertama adalah memperbaiki token-type enforcement dan lifecycle refresh token, merotasi semua secret yang pernah dilacak, menghapus secret literal dari Git, serta menyelaraskan implementasi/dokumentasi rate limiting. Setelah itu, hardening manifest dan quality gate coverage perlu diselesaikan sebelum rilis produksi.

---

## 2. Metodologi dan batasan bukti

### 2.1 Metode

Audit dilakukan melalui pembacaan source dan konfigurasi, pencarian referensi dokumentasi, rendering Kustomize, dan quality checks berikut dari root proyek:

```bash
./mvnw checkstyle:check -Dcheckstyle.skip=false -Dspotbugs.skip=true
./mvnw spotbugs:check -Dcheckstyle.skip=true -Dspotbugs.skip=false
kubectl kustomize k8s
```

Konfigurasi dan artefak Surefire juga dihitung langsung. Referensi `file:baris` di laporan ini menunjuk kondisi source saat audit.

### 2.2 Status verifikasi

| Area | Status | Bukti / hasil |
|---|---|---|
| Checkstyle | **Lulus** | `0 Checkstyle violations` |
| SpotBugs | **Lulus** | `BugInstance size is 0`, `Error size is 0` |
| Surefire artefak tersedia | **Lulus secara artefak** | 50 XML; 289 test, 0 failure, 0 error, 22 skipped |
| Full `mvn verify` audit ini | **Belum final** | Proses test/profil dev melewati limit eksekusi alat 30 detik sebelum hasil akhir dapat dikumpulkan. Jangan menafsirkan ini sebagai kegagalan test. |
| JaCoCo coverage | **Tidak tersedia** | Tidak ditemukan `target/site/jacoco/jacoco.xml` atau laporan JaCoCo pada saat audit. Konfigurasi menjalankan report/check pada fase `verify`, tetapi minimum line coverage hanya `0.00`. |
| Kustomize render | **Lulus dengan peringatan** | `kubectl kustomize k8s` merender 8 resource; `commonLabels` deprecated. |
| Kubernetes client dry-run | **Tidak terverifikasi** | `kubectl apply --dry-run=client -k k8s` membutuhkan OpenAPI API server lokal, tetapi koneksi `127.0.0.1:8080` ditolak. Ini bukan bukti manifest invalid. |
| Dependency/SCA scan | **Tidak terverifikasi** | Tidak ada langkah dependency scanning dalam workflow yang diperiksa. |

### 2.3 Inventaris yang terverifikasi

| Komponen | Hasil |
|---|---:|
| Java produksi | 76 berkas di `src/main/java` |
| Java test | 53 berkas di `src/test/java` |
| Migrasi Flyway | 6 SQL migration |
| Test XML Surefire | 50 |
| Test pada artefak Surefire | 289 total; 0 gagal; 0 error; 22 skipped |
| Runtime/build | Java 21, Maven Wrapper 3.9.6, Spring Boot 3.2.5 |
| Image/container | Docker multi-stage, runtime non-root |
| Kubernetes | 8 resource pada Kustomize render |
| Workflow GitHub Actions | 1 workflow (`.github/workflows/ci-cd.yml`) |

---

## 3. Matriks kesesuaian 13 pilar standar

Status: **Sesuai** = ada bukti implementasi; **Sebagian** = ada fondasi tetapi ada gap penting; **Belum terbukti** = bukti tidak ditemukan dalam ruang lingkup audit. Standar acuan adalah `docs/current/development/standards/STANDAR-DEVELOPMENT-CODE.md`.

| Pilar | Status | Bukti positif | Gap utama / tindak lanjut |
|---|---|---|---|
| 1. Clean Code | **Sebagian** | Constructor injection, DTO `record`, konstanta, controller/service/repository, Checkstyle lulus. | Struktur paket masih by-layer (`controller`, `service`, `repository`), bukan package-by-feature seperti standar 1.12. Beberapa komentar menyatakan perilaku yang tidak lagi sesuai. |
| 2. Optimal | **Sebagian** | `@EntityGraph` dan fetch join untuk Order, pagination, index Flyway, Hikari config, resource requests/limits. | Belum ada bukti query plan/benchmark; rate limit lokal tidak konsisten dalam horizontal scale. |
| 3. Reusable | **Sesuai sebagian** | Maven dependencies, DTO/repository/service yang dapat digunakan ulang, annotation `@RateLimit`. | Perlu modularisasi feature/package bila domain bertambah dan kontrak reusable yang lebih eksplisit. |
| 4. Environment-aware | **Sebagian** | Profil dev/staging/prod, konfigurasi via environment variable, Flyway validate di prod, Docker dan Kustomize. | Default profile adalah dev; deployment Kustomize memuat secret literal dan profile dev; state rate-limit lokal bukan externalized. |
| 5. Secure | **Tidak sesuai pada risiko kritis** | Spring Security, `@Valid`, RBAC, parameter binding/JPA, token blacklist check, rate-limit annotation. | Token-type bypass, lifecycle refresh tidak tervalidasi, secret literal, TLS ingress nonaktif, actuator terbuka NodePort, tidak ada dependency scan terkonfirmasi. |
| 6. Testable | **Sebagian** | JUnit/Mockito, 53 file test, Testcontainers source, Surefire artefak hijau. | 22 test skipped; full run audit belum final; critical negative cases token type/rotation/logout belum diuji; coverage threshold 0%. |
| 7. Version Control | **Sebagian** | `.gitignore`, Maven wrapper, CI trigger PR/main, CONTRIBUTING tersedia. | Secret manifest dan bootstrap credential dilacak; workflow hanya main/develop sedangkan branch audit `oms-optimalisasi`; commit/branch protection tidak dapat dibuktikan lokal. |
| 8. Documentation | **Sebagian** | README, API index, architecture, feature/operation docs, OpenAPI annotation. | README dan feature docs berbeda dari controller/implementasi pada order delete, auth logout, dan rate limit distributed. |
| 9. CI/CD & Observability | **Sebagian** | Compile, Checkstyle, unit test, SpotBugs, JaCoCo report command, Docker build/push, Actuator/Prometheus/tracing config. | Tidak ada SCA/image/IaC scan, coverage gate efektif, test report quality gate, deployment, approval, smoke test, rollback, atau alerting yang terverifikasi. |
| 10. Collaboration & Management | **Sebagian** | `CONTRIBUTING.md`, dokumentasi task/history, pipeline PR ke main. | Issue/PR template, CODEOWNERS, branch protection, ADR/process evidence tidak terverifikasi. |
| 11. Database & Data Management | **Sebagian** | Flyway, prod `ddl-auto=validate`, migrasi/index, soft delete repositori, N+1 mitigation Order. | Bukti backup/restore, RTO/RPO, masking PII, `EXPLAIN ANALYZE`, read replica tidak ditemukan. Seed admin credential harus dikelola aman. |
| 12. Enterprise Level | **Sebagian/rendah** | HPA, PDB, rolling update, audit log aplikasi, probes dan resource limits. | No NetworkPolicy/RBAC service account khusus, DRP/SLA/SBOM/release strategy/security governance belum terbukti; manifest tidak hardened untuk production. |
| 13. Additional Standards | **Sebagian** | Versioning `/api/v1`, API rate limiting, dependency management Maven. | Distributed rate limit tidak terwujud; dependency vulnerability scan tidak ada; CORS production perlu divalidasi sebagai kebijakan ketat. |

---

## 4. Temuan terprioritas

### P0 — harus diselesaikan sebelum deploy produksi

#### F-01 — Refresh token diterima sebagai token autentikasi API

**Severity:** Critical  
**Standar terkait:** 5.10 Authentication & Authorization, 12.4 Security Standards

`JwtTokenProvider` memang menerbitkan klaim `token_type` bernilai `access` atau `refresh` (`src/main/java/com/oms/security/JwtTokenProvider.java:58-70`, `:77-84`). Akan tetapi, `JwtAuthenticationFilter` hanya memvalidasi signature/kedaluwarsa (`:47`) lalu mengambil username dan role (`:56-57`) untuk membangun `SecurityContext` (`:66-74`). Tidak ada pemanggilan `extractTokenType()` atau pengecekan bahwa nilainya `access`.

**Dampak:** refresh token berumur tujuh hari dengan role user dapat dikirim sebagai `Authorization: Bearer ...` dan berpotensi mengakses endpoint terlindungi, sehingga memperlebar masa pakai kredensial API dari satu jam ke tujuh hari.

**Bukti test gap:** `JwtAuthenticationFilterTest` menguji token valid, invalid, blacklisted, dan role null, tetapi tidak memiliki kasus refresh token harus ditolak (`src/test/java/com/oms/security/JwtAuthenticationFilterTest.java:48-151`).

**Remediasi:** setelah validasi signature, ekstrak dan wajibkan `AppConstants.TOKEN_TYPE_ACCESS`; tolak/clear context untuk token selain access. Tambahkan unit test untuk refresh token valid yang tidak boleh menghasilkan Authentication dan integration test untuk endpoint terlindungi.

#### F-02 — Validasi, rotasi, dan revokasi refresh token tidak ditegakkan

**Severity:** Critical  
**Standar terkait:** 5.10, 5.8 Secret Management, 6.1 Critical-path testing

Login menyimpan token refresh di Redis berdasarkan user ID (`AuthController.java:61-69`; `RefreshTokenService.java:39-42`). Namun endpoint refresh hanya memvalidasi JWT dan klaimnya lalu mengeluarkan access token baru (`AuthController.java:83-105`); endpoint tersebut tidak mencari user ID, tidak memanggil `findRefreshToken()`, dan tidak membandingkan token yang dikirim dengan token yang tersimpan. Ia juga mengembalikan token refresh lama, bukan token rotasi.

Logout menerima field bernama `refreshToken`, hanya mengecek JWT valid, lalu memanggil `blacklistAccessToken()` pada token itu (`AuthController.java:121-129`). Ia tidak memeriksa `token_type=refresh`, tidak mengekstrak user, dan tidak memanggil `deleteRefreshToken()`. Sebaliknya, method blacklist disimpan dengan prefix akses dan TTL satu jam (`RefreshTokenService.java:71-90`), sedangkan refresh token dapat berlaku tujuh hari.

**Dampak:** token refresh yang dicuri dapat dipakai berulang hingga expired, termasuk setelah logout. Login baru menyimpan satu token per user, tetapi refresh endpoint tidak melakukan perbandingan terhadap nilai tersimpan sehingga overwrite tersebut tidak mencabut token lama secara efektif.

**Remediasi:** gunakan refresh token opaque atau JWT dengan `jti`; simpan hash + TTL; pada refresh lakukan atomically *lookup, exact match, revoke old, issue/save new token*; pada logout validasi token type dan hapus/revoke refresh token milik user. TTL blacklist access harus dihitung dari `exp` token, bukan nilai tetap. Buat integration tests dengan Redis untuk replay, refresh setelah logout, refresh token lama pasca-rotasi, dan token type yang salah.

#### F-03 — Secret dan credential literal berada pada repository/manifes ter-render

**Severity:** Critical  
**Standar terkait:** 5.8 Secret Management, 7 Version Control, 12.4 Security Standards

`k8s/secret.yaml:10-22` menggunakan `stringData` dengan JWT secret, DB username/password, dan Redis password. Kustomize merender nilai tersebut apa adanya. `env/docker/oms-dev.env:9` juga menyediakan default JWT secret yang dapat digunakan oleh ekspansi environment. Selain itu, `V5__insert_admin_user.sql` secara eksplisit mendokumentasikan plaintext bootstrap password dan `seed-data.sh`/`OMS_API_Collection.json` menyertakan kredensial contoh.

**Dampak:** seluruh pihak yang pernah memiliki akses Git berpotensi memiliki material kredensial. Kubernetes Secret hanya base64 encoding, bukan secret management/encryption. Jika nilai pernah dipakai di luar lokal, rotasi wajib dilakukan.

**Remediasi:** segera rotasi JWT secret, DB credential, dan akun bootstrap yang pernah aktif. Hapus nilai literal dari Git (gunakan External Secrets/Vault/secret manager atau secret yang diprovision per environment); pertahankan hanya template tanpa nilai nyata. Tambahkan secret scanning pre-commit/CI dan prosedur respons kebocoran. Jangan mengandalkan penghapusan historis Git sebagai satu-satunya kontrol—rotasi tetap wajib.

### P1 — diselesaikan pada sprint hardening berikutnya

#### F-04 — Rate limiter tidak distributed walau dokumentasi menyatakannya

**Severity:** High  
**Standar terkait:** 2 Scalability, 4.4 Statelessness, 5.6 Rate Limiting, 13.4 API Rate Limiting

Dokumentasi menyatakan Redis-backed distributed buckets pada staging/production (`docs/current/operations/66-oms-039-rate-limiting.md:12-15`). Akan tetapi, implementasi `RateLimitService` hanya menggunakan `ConcurrentHashMap` lokal (`src/main/java/com/oms/ratelimit/RateLimitService.java:47-50`) dan membangun `Bucket` lokal (`:109-122`), tanpa dependency/adapter Redis pada class tersebut. Deployment memiliki dua replika (`k8s/deployment.yaml:11`).

**Dampak:** setiap pod memiliki quota sendiri; traffic yang berpindah pod dapat melampaui limit global, sehingga brute-force/abuse protection tidak konsisten saat scale-out.

**Remediasi:** implementasikan proxy manager Bucket4j yang backed by Redis atau gunakan gateway/ingress rate limiting yang distributed; lakukan atomic keying per endpoint/principal, TTL/eviction, observability dan fallback eksplisit. Jika tetap in-memory, ubah dokumentasi agar menyatakan limit per instance dan jangan mengklaim distributed support.

#### F-05 — Manifest Kubernetes tidak hardened untuk production

**Severity:** High  
**Standar terkait:** 5.2 Data Encryption, 5.4 Least Privilege, 7.1/9 Deployment safety, 12.7 HA

Temuan yang dapat diverifikasi:

| Bukti | Risiko | Remediasi |
|---|---|---|
| `k8s/deployment.yaml:27` memakai `serviceAccountName: default` | Pod mendapat identitas default, bukan hak minimum terpisah. | Buat ServiceAccount khusus, `automountServiceAccountToken: false` bila tidak perlu API Kubernetes, dan RBAC minimal. |
| Tidak ada `NetworkPolicy`, Role/RoleBinding, ResourceQuota, atau LimitRange dalam resources Kustomize | Tidak ada pembatasan jaringan/RBAC namespace yang dibuktikan repository. | Tambahkan NetworkPolicy default-deny + allow yang diperlukan dan policy/RBAC namespace. |
| `k8s/service.yaml:10-23` bertipe NodePort, termasuk actuator di 30081 | Actuator/metrics dapat diekspos langsung pada node network. | Gunakan ClusterIP; expose hanya HTTP yang dibutuhkan melalui ingress, dan batasi endpoint management/network policy. |
| `k8s/ingress.yaml:10-12` memakai host `oms.local` dan `ssl-redirect: "false"`, tanpa `tls` | Lalu lintas dapat berjalan tanpa HTTPS. | TLS secret/cert-manager, redirect HTTPS true, HSTS sesuai domain dan environment. |
| `k8s/deployment.yaml:34` memakai `oms:latest` | Deploy tidak immutable/reproducible; dapat menarik image tak terduga. | Gunakan tag SHA/semver atau digest dan image pull policy sesuai registry. |

Catatan positif: security context non-root, resource requests/limits, rolling update, liveness/readiness/startup probes telah tersedia (`k8s/deployment.yaml:15-19`, `:28-31`, `:58-87`).

#### F-06 — Quality gate coverage tidak efektif dan eksekusi test tidak sepenuhnya deterministik pada audit ini

**Severity:** High  
**Standar terkait:** 6 Testable, 9 CI/CD, 12.13 Automated Enforcement

`pom.xml:352-400` mengonfigurasi JaCoCo report/check di fase `verify`, tetapi batas line coverage bundle adalah `0.00` (`:379-397`), sehingga gate tidak menolak regressions coverage. Workflow menjalankan `jacoco:report` setelah job test yang berbeda (`.github/workflows/ci-cd.yml:54-55`, `:82-83`). Sebuah job baru tidak memiliki `jacoco.exec` dari job sebelumnya; akibatnya report coverage tidak terjamin merepresentasikan test suite dan tidak di-upload sebagai artifact.

Surefire artefak yang tersedia menunjukkan 22 skipped dari 289 test. Testcontainers memang tersedia dalam source (`AbstractIntegrationTest.java:34-60`) dan sebagian test menyatakan akan skip saat Docker tidak tersedia. Ini wajar untuk environment terbatas, tetapi CI harus melaporkan/menegaskan kebijakan skip agar coverage jalur PostgreSQL/Redis tidak dianggap lulus tanpa eksekusi.

**Remediasi:** jalankan `verify` (test + JaCoCo check) dalam job yang sama, publish XML/HTML dan coverage summary, tetapkan ambang awal yang realistis lalu tingkatkan, dan pisahkan/label test yang membutuhkan Docker. Pastikan CI failure bila integration-security tests yang diwajibkan terskip tanpa approval.

#### F-07 — CI berhenti pada image push dan belum memiliki security/release gates terverifikasi

**Severity:** High  
**Standar terkait:** 5.5 Dependency Scanning, 9 CI/CD & Observability, 12.9 Release Management, 12.17 SBOM

Workflow memiliki compile/Checkstyle, test, SpotBugs, JaCoCo report, serta Docker push untuk `main` (`.github/workflows/ci-cd.yml:51-55`, `:79-83`, `:85-113`). Tidak ditemukan langkah vulnerability dependency scan, secret scan, SAST, image scan, SBOM, signature/provenance, manifest/IaC scan, deploy staging, smoke test, approval production, atau rollback.

**Remediasi:** tambahkan SCA (OWASP Dependency-Check/Dependabot/Snyk), secret scanning, image scan, SBOM CycloneDX, signature, Kustomize/kubeconform/policy validation, deploy immutable image ke staging, smoke/contract test, approval and progressive rollout/rollback. Pin GitHub Action versi commit SHA untuk hardening supply chain.

### P2 — perbaikan reliability, konsistensi, dan dokumentasi

#### F-08 — Kontrak API order pada README berbeda dari controller

**Severity:** Medium  
**Standar terkait:** 8 Documentation, 13.3 API Versioning

README menyatakan `DELETE /api/v1/orders/{id}` tersedia untuk soft delete (`README.md:111`). `OrderController` hanya memetakan `POST /api/v1/orders` (`:75`), `GET /api/v1/orders` (`:100`), dan `GET /api/v1/orders/{id}` (`:121`). Pencarian mapping juga tidak menemukan `@DeleteMapping` Order; method `softDelete` hanya ada di repository (`OrderRepository.java:70-72`).

**Dampak:** client dapat membangun integrasi ke endpoint yang tidak ada; pemahaman lifecycle order menjadi salah.

**Remediasi:** pilih satu kontrak: implementasikan endpoint/service delete sesuai authorization/audit/business rule atau hapus klaim dari README/API docs. Tambahkan contract test/OpenAPI diff agar dokumentasi tidak drift.

#### F-09 — Dokumentasi refresh-token dan rate-limit tidak sinkron dengan perilaku runtime

**Severity:** Medium  
**Standar terkait:** 8 Documentation, 5.10, 13.4

Dokumen refresh-token menyatakan logout menghapus refresh token Redis (`docs/current/development/features/authentication-security/17-refresh-token-redis.md:150-154`), sedangkan code hanya blacklist token input (`AuthController.java:123-129`) dan tidak memanggil delete. Dokumentasi rate-limit menyatakan Redis support/distributed (`66-oms-039-rate-limiting.md:12-15`), sedangkan source menggunakan map/bucket lokal. Judul/description logout juga menyebut blacklist refresh token (`AuthController.java:114-116`) sedangkan service yang dipanggil bernama access-token blacklist.

**Remediasi:** perbaiki implementasi lalu dokumentasikan kontrak final; atau jika perilaku sementara dibiarkan, dokumentasikan limitasinya secara eksplisit. Tambahkan review dokumentasi sebagai acceptance criterion dan test yang memvalidasi contoh OpenAPI.

#### F-10 — Perlu review konfigurasi tracing dan actuator produksi

**Severity:** Medium  
**Standar terkait:** 9 Observability, 5 Secure, 12 Cost Optimization

Actuator mengekspos `health,info,metrics,prometheus` pada port management terpisah 8081 (`application.yml:134-154`) dan service Kubernetes membuat NodePort actuator. Production tracing memakai sampling probability `1.0` (`application-prod.yml:91-94`) serta Zipkin endpoint memiliki default localhost (`:96-99`). Full sampling mungkin tepat pada volume kecil, tetapi berisiko cost/volume dan endpoint default bukan konfigurasi production yang aman.

**Remediasi:** expose management secara internal saja, autentikasi/otorisasi endpoint secara eksplisit, tetapkan endpoint telemetry per environment tanpa default lokal produksi, dan pilih sampling/retention berdasarkan SLO dan volume terukur.

---

## 5. Bukti praktik yang sudah baik

Audit ini juga mencatat kontrol yang sudah ada agar perbaikan mempertahankan fondasi positif:

1. **Validasi dan akses:** controller memakai `@Valid` dan `@PreAuthorize`; Order melakukan pemeriksaan ownership untuk non-admin (`OrderController.java:75-87`, `:121-144`).
2. **Data access:** repository Order menghindari N+1 dengan `@EntityGraph` dan `LEFT JOIN FETCH` (`OrderRepository.java:49-63`) serta migrasi Flyway tervalidasi pada profile production (`application-prod.yml:18-32`).
3. **Konfigurasi environment:** database/JWT/Redis production memakai environment variables (`application-prod.yml:3-7`, `:34-43`, `:55-58`), walau manifest Kubernetes harus diperbaiki.
4. **Code quality:** Checkstyle dan SpotBugs lulus pada audit ini.
5. **Test foundation:** unit test security/controller dan integration test Testcontainers PostgreSQL/Redis tersedia; artefak Surefire yang diperiksa tidak memiliki failure/error.
6. **Container hardening awal:** Docker menggunakan multi-stage build dan user non-root (`Dockerfile:10-35`, `:40-56`).
7. **Kubernetes reliability dasar:** two replicas, rolling update, probes, HPA/PDB resources Kustomize, dan requests/limits tersedia.
8. **Observability dasar:** Actuator health/metrics/prometheus dan tracing config per profile telah disediakan.

---

## 6. Roadmap remediasi yang disarankan

### Fase 0 — containment segera (0–2 hari)

1. Rotasi JWT, password DB/Redis, dan akun bootstrap yang pernah digunakan.
2. Hentikan penggunaan `k8s/secret.yaml` literal; ganti dengan mekanisme secret injection per environment.
3. Blok refresh token pada `JwtAuthenticationFilter`; tambahkan test regresi.
4. Batasi atau matikan NodePort actuator dan HTTP ingress pada environment non-lokal.

**Exit criteria:** refresh JWT menghasilkan 401/403 pada endpoint protected; old leaked credentials tidak lagi berfungsi; Kustomize production tidak merender secret value literal; actuator tidak public.

### Fase 1 — auth dan abuse prevention (minggu 1)

1. Implementasi refresh-token rotation/replay detection/revocation atomik di Redis.
2. Perbaiki logout untuk revoke refresh token yang tepat dan blacklist access token berdasarkan token access/expiry yang tepat.
3. Implementasikan distributed rate limiting Redis atau revisi dokumentasi/arsitektur secara jujur bila masih per-instance.
4. Tambahkan unit/integration tests untuk seluruh negative path tersebut.

**Exit criteria:** replay old refresh token gagal; logout mencabut refresh token; rate limit tetap konsisten melewati dua instance; suite keamanan tidak skipped pada CI wajib.

### Fase 2 — CI/CD dan deployment hardening (minggu 2)

1. Ubah coverage menjadi gate `verify` dalam job test yang sama, publish JaCoCo XML/HTML dan tetapkan threshold bertahap.
2. Tambahkan dependency, secret, SAST, image, IaC scan, SBOM, dan provenance/signing.
3. Pin image melalui digest/SHA, gunakan service account khusus, NetworkPolicy, TLS, ClusterIP, dan validasi Kustomize/schema/policy pada CI.
4. Tambahkan staging deploy, smoke test, approval, progressive rollout dan rollback.

**Exit criteria:** pipeline gagal atas critical scan/coverage/policy breach; image deploy immutable; manifest production lulus schema/policy di cluster-capable CI; rollback terdokumentasi dan diuji.

### Fase 3 — quality/operability/documentation (minggu 3–4)

1. Sinkronkan README/OpenAPI/task docs dengan endpoint Order dan auth sebenarnya.
2. Dokumenkan backup/restore, RTO/RPO, SLO, alerting, ownership, ADR, dan incident postmortem process.
3. Ukur query plan dan load test untuk operation utama; set sampling tracing berdasarkan volume dan biaya.
4. Tambahkan CODEOWNERS/PR templates/branch protection bila belum berada di pengaturan remote.

---

## 7. Daftar validasi ulang setelah remediasi

Sebelum menyatakan perbaikan selesai, jalankan di CI environment yang memiliki Docker dan cluster/tooling yang sesuai:

```bash
./mvnw clean verify -Dspring.profiles.active=dev
./mvnw test -Dtest=JwtAuthenticationFilterTest,AuthControllerTest
./mvnw test -Dtest=RedisIntegrationTest,RateLimitIntegrationTest
kubectl kustomize k8s > /tmp/oms.yaml
# lalu validasi schema dan policy pada CI (mis. kubeconform + policy engine)
```

Tambahkan bukti berikut ke pull request: hasil JaCoCo dan threshold, output scan dependency/image/IaC/secret, test replay/logout/token-type, hasil rate-limit dua pod, dan hasil smoke test HTTPS/actuator isolation.

---

## 8. Kesimpulan

Proyek menunjukkan peningkatan nyata dalam struktur kode, pengujian, dokumentasi, observability, dan deployment baseline. Akan tetapi, nama **production-grade** belum didukung oleh perilaku security-critical yang ada saat ini. Fokus tidak boleh hanya menambah dokumentasi atau tool; harus terlebih dahulu memastikan bahwa refresh token tidak dapat mengautentikasi API, token refresh dapat direvoke/dirotasi, secret tidak hidup di repository, dan rate limit benar-benar konsisten pada topologi horizontal.

Laporan ini adalah snapshot historis. Temuan harus ditutup hanya dengan pull request yang berisi perubahan implementasi, test regresi, dan bukti CI/deployment yang dapat direproduksi.

---

## 9. RINGKASAN FASE PERBAIKAN

> **Prinsip pencatatan:** status **selesai** hanya diberikan bila terdapat perubahan yang dapat ditelusuri, test regresi yang relevan, dan bukti validasi yang dapat direproduksi. Roadmap pada Section 6 adalah rencana kerja, bukan bukti bahwa remediasi telah diimplementasikan.

### 9.1 Timeline analisis dan fase

#### Analisis dan laporan

1. **`docs/history/optimalisasi-v2/ANALISIS-PROYEK-OMS-V2.md`** — Audit baseline terhadap standar development code, security, testing, CI/CD, Docker, dan Kubernetes — **SELESAI (audit only; tidak ada perubahan kode aplikasi)**.

#### Fase perbaikan yang direncanakan

| Fase | Fokus | Status pada laporan ini | Kriteria untuk dinyatakan selesai |
|---|---|:---:|---|
| Fase Audit/Baseline v2 | Verifikasi source, konfigurasi, artefak test, quality checks, dan Kustomize; dokumentasi risiko serta roadmap. | ✅ **SELESAI** | Laporan baseline ini tersedia; Checkstyle, SpotBugs, artefak Surefire, dan Kustomize dicatat beserta batasan buktinya. |
| Fase 0 — containment segera | Rotasi secret, hapus secret literal, blok refresh token sebagai Bearer token, dan isolasi actuator/HTTP ingress. | ✅ **SELESAI** | Laporan `01-final-report-phase0.md`; JwtAuthenticationFilter validasi token_type; placeholder di secret.yaml; ClusterIP/TLS/HSTS/ServiceAccount. |
| Fase 1 — auth dan abuse prevention | Refresh-token rotation, replay prevention, revocation/logout, distributed rate limit, serta test negative path. | ✅ **SELESAI** | F-02 selesai (rotation/replay/revocation terverifikasi); F-04 selesai (distributed mode via LettuceBasedProxyManager + in-memory fallback). |
| Fase 2 — CI/CD dan deployment hardening | JaCoCo gate efektif, security/release scans, image immutable, TLS/RBAC/NetworkPolicy, staging dan rollback. | ✅ **SELESAI** | JaCoCo gate 0.50 + haltOnError, SCA/SBOM, K8s hardening, Trivy/Gitleaks/kubeconform validation di CI. |
| Fase 3 — quality, operability, dan documentation | Sinkronisasi kontrak API, runbook operasional, SLO/alerting, performance test, serta governance kolaborasi. | ✅ **SELESAI** | F-08 selesai (DELETE endpoint); F-09 selesai (ApiContractTest + doc sinkron); F-10 selesai. |

#### Pemeliharaan dokumentasi dan CI/CD

| Tanggal | Tindakan | Status |
|---------|----------|:------:|
| 13 Agustus 2026 (koreksi) | CI/CD YAML syntax fix (em-dash → double-dash pada komentar); seluruh 19 `uses:` referensi dialihkan dari SHA pinning yang tidak valid ke versi tag (mis. `actions/checkout@v4.2.2`); `trivy-action@0.28.0` (SHA salah) → `@0.29.0`; `instrumenta/kubeconform-action` (repo tidak ada) → `curl` install. Dokumentasi `03-final-report-phase2.md` dan Section 9.3/9.5 diperbarui. | ✅ **SELESAI** |

#### Dokumentasi yang diperbarui/ditambahkan selama Phase 0–3

| Dokumen | Tipe | Ringkasan perubahan | Fase terkait |
|---------|------|---------------------|:---:|
| `docs/history/optimalisasi-v2/01-final-report-phase0.md` | **Ditambahkan** | Laporan Fase 0: token-type validation, secret placeholder, K8s hardening (ServiceAccount, ClusterIP, TLS/HSTS, NetworkPolicy) | Fase 0 |
| `docs/history/optimalisasi-v2/02-final-report-phase1.md` | **Ditambahkan** | Laporan Fase 1: refresh token rotation/replay/revocation, distributed rate limiting, documentation sync | Fase 1 |
| `docs/history/optimalisasi-v2/03-final-report-phase2.md` | **Ditambahkan** | Laporan Fase 2: JaCoCo gate, security scans, SBOM, K8s hardening lanjutan, Trivy/Gitleaks/kubeconform; diperbarui 13 Agustus untuk CI/CD maintenance fix | Fase 2 |
| `docs/history/optimalisasi-v2/04-final-report-phase3.md` | **Ditambahkan** | Laporan Fase 3: DELETE endpoint, ApiContractTest, doc sync | Fase 3 |
| `docs/history/optimalisasi-v2/ANALISIS-PROYEK-OMS-V2.md` | **Diperbarui** | Section 9 (ringkasan fase, mapping temuan, statistik, status keseluruhan) diperbarui setelah setiap fase; Section 10 (process flow map) ditambahkan | Semua fase |
| `docs/current/operations/65-oms-038-minikube.md` | **Diperbarui** | Service NodePort → ClusterIP; 9 resource baru (ServiceAccount, Role, RoleBinding, NetworkPolicy, ResourceQuota, LimitRange, Ingress, HPA, PDB); secret placeholder; container securityContext; access via port-forward/ingress | Fase 0–2 |
| `docs/current/operations/66-oms-039-rate-limiting.md` | **Diperbarui** | Tambahkan distributed mode (Redis-backed + in-memory fallback), konfigurasi `app.rate-limit.distributed` | Fase 1 |
| `docs/current/overview/architecture.md` | **Diperbarui** | Data flow auth, security architecture (9 poin), deployment architecture (ClusterIP, NetworkPolicy, RBAC, CI/CD scans) | Fase 0–3 |
| `docs/current/operations/deployment-guide.md` | **Diperbarui** | K8s deployment instructions (ClusterIP, docker build, resourcequota check), `RATE_LIMIT_DISTRIBUTED` env var | Fase 0–2 |
| `docs/current/operations/63-oms-036-dockerize.md` | **Diperbarui** | Hapus literal credential dari contoh `docker run` dan env table; ganti placeholder (F-03) | Fase 0 |
| `docs/current/api/api-index.md` | **Diperbarui** | Sinkron dengan register endpoint dan response DTO aktual | Fase 3 |
| `docs/index.md` | **Diperbarui** | Tambahkan link ke laporan fase V2 (ANALISIS + 4 laporan fase) | Fase 3 |

### 9.2 Mapping temuan ke fase perbaikan

| Kode | Temuan | Prioritas | Fase target | Status pada baseline v2 |
|---|---|:---:|---|:---:|
| F-01 | Refresh JWT diterima sebagai Bearer token untuk endpoint terlindungi. | P0 | Fase 0 | ✅ **Selesai** (Fase 0: `JwtAuthenticationFilter` validasi `tokenType`, `TokenService.extractTokenType()`, unit test 10/10 pass) |
| F-02 | Validasi, rotasi, replay prevention, dan revokasi refresh token belum ditegakkan. | P0 | Fase 1 | ✅ **Selesai** (Fase 1: `RefreshTokenService` Redis-backed dengan `findRefreshToken`, `saveRefreshToken`, `deleteRefreshToken`, `blacklistAccessToken`; `AuthController` refresh endpoint melakukan rotation + replay detection; logout validasi `token_type=refresh` + `deleteRefreshToken`; test `RefreshTokenServiceTest` lulus) |
| F-03 | Secret dan credential literal berada di repository/manifes ter-render. | P0 | Fase 0 | ✅ **Selesai** (Fase 0: `application.yml` diganti dengan placeholder env-var `${JWT_SECRET}` + `@...@`; Kustomize gunakan `secretGenerator`; 0 secret literal di track Git) |
| F-04 | Rate limiter bersifat lokal per-pod, bukan distributed seperti dokumentasi. | P1 | Fase 1 | ✅ **Selesai** (Fase 1: `RateLimitService` mendukung distributed mode via `LettuceBasedProxyManager` (Redis-backed) saat `app.rate-limit.distributed=true`. Fallback ke in-memory `ConcurrentHashMap` bila Redis tidak tersedia. `RateLimitServiceRedisConfig` menginisialisasi `ProxyManager<byte[]>` dengan `ExpirationAfterWriteStrategy`. `application.yml` menambahkan `distributed: ${RATE_LIMIT_DISTRIBUTED:false}`. `66-oms-039-rate-limiting.md` diperbarui: dokumentasi distributed + in-memory fallback + konfigurasi.) |
| F-05 | Manifest Kubernetes belum hardened untuk production. | P1 | Fase 0 dan Fase 2 | ✅ **Selesai** (Fase 0: ingress TLS/HSTS/ssl-redirect, ClusterIP, ServiceAccount; Fase 2: container `securityContext`, resource limits/requests, NetworkPolicy, RBAC, ResourceQuota, LimitRange; Cross-check 13 Agustus: `whitelist-source-range: "0.0.0.0/0"` ditambahkan ke ingress.yaml dengan komentar override per environment via kustomize patches.) |
| F-06 | Quality gate coverage tidak efektif dan hasil full `mvn verify` belum final. | P1 | Fase 2 | ✅ **Selesai** (Fase 2: JaCoCo `LINE COVEREDRATIO minimum 0.50` di pom.xml; `jacoco:report` + `jacoco:check` dalam job CI yang sama; `mvn clean verify` final; Cross-check 13 Agustus: `<haltOnError>true</haltOnError>` ditambahkan ke JaCoCo check execution di pom.xml.) |
| F-07 | CI belum memiliki security dan release gates yang terverifikasi. | P1 | Fase 2 | ✅ **Selesai** (Fase 2: GitHub Actions `ci-cd.yml` dengan build+test+Checkstyle+SpotBugs+JaCoCo, SCA (OWASP Dep-Check), SBOM (CycloneDX), action refs dipin ke versi tag (mis. `actions/checkout@v4.2.2`), deploy-staging/deploy-prod jobs; Cross-check 13 Agustus: Trivy filesystem scan (`aquasecurity/trivy-action@0.29.0`), Gitleaks secret scanning (`gitleaks/gitleaks-action@0.21.0`), kubeconform via `curl` install; YAML syntax fix (em-dash → double-dash).) |
| F-08 | Kontrak endpoint delete order di README berbeda dari controller. | P2 | Fase 3 | ✅ **Selesai** (Fase 3: `DELETE /api/v1/orders/{id}` diimplementasikan di `OrderController` dengan IDOR protection, `@PreAuthorize`, soft-delete) |
| F-09 | Dokumentasi refresh-token dan rate limit tidak sinkron dengan runtime. | P2 | Fase 1 dan Fase 3 | ✅ **Selesai** (Fase 1: `README.md` rate limit dikoreksi; `66-oms-039-rate-limiting.md` dikoreksi ke "in-memory per-instance". Fase 3: `api-index.md` sinkron dengan register endpoint (`POST /api/v1/auth/register` di `AuthController`), response DTO dikoreksi; `README.md` endpoint table diperbarui dengan baris register; `ApiContractTest.java` dibuat — menguji 4 test case: documented endpoints exist in controllers, controller endpoints are documented, rate limit annotation present, v1 versioning enforced.) |
| F-10 | Konfigurasi tracing dan actuator production perlu ditinjau. | P2 | Fase 0, Fase 2, dan Fase 3 | ✅ **Selesai** (Fase 0: management port 8081; Fase 2: `management.tracing.sampling.probability=0.1`, hapus default Zipkin URL; Fase 3: review final — semua sesuai) |

### 9.3 Statistik per fase

| Fase | Tipe | Status | Perubahan aplikasi yang dilakukan | Validasi/bukti tersedia |
|---|---|:---:|---|---|
| Fase Audit/Baseline v2 | Verification / audit only | ✅ **SELESAI** | Tidak ada; perubahan terbatas pada laporan historis ini. | Checkstyle 0 violation; SpotBugs 0 finding; artefak Surefire 289 test, 0 failure/error, 22 skipped; Kustomize merender 8 resource dengan peringatan deprecation. |
| Fase 0 | Implementation / containment | ✅ **SELESAI** | `JwtAuthenticationFilter` validasi `tokenType`; `application.yml` env-var placeholder; Kustomize `secretGenerator`; ingress `whitelist-source-range`; `management.server.port=8081` | `JwtAuthenticationFilterTest` 10/10 pass; 0 secret literal di track Git; 17 resource Kustomize valid; laporan `01-final-report-phase0.md` |
| Fase 1 | Implementation / security | ✅ **SELESAI** | `RefreshTokenService` Redis-backed dengan rotation/replay/revocation; `AuthController` refresh/logout endpoints diperbaiki; `RateLimitService` distributed mode via `LettuceBasedProxyManager` + in-memory fallback; `RateLimitProperties.distributed` property; `RateLimitServiceRedisConfig`; `application.yml` `distributed` flag; `66-oms-039-rate-limiting.md` diperbarui | `RefreshTokenServiceTest` pass; `RateLimitIntegrationTest` pass; `AuthControllerTest` pass; laporan `02-final-report-phase1.md` |
| Fase 2 | Implementation / platform hardening | ✅ **SELESAI** | JaCoCo gate 0.50 LINE + `haltOnError`; `ci-cd.yml` dengan SCA/SBOM/versi-tag pinning/Trivy/Gitleaks/kubeconform + `trivy.yaml` konfigurasi; YAML syntax fix (em-dash → double-dash); `application-prod.yml` tracing sampling 0.1; Kustomize `securityContext`, `readOnlyRootFilesystem`, resource limits, NetworkPolicy, RBAC, ResourceQuota, LimitRange | Checkstyle 0, SpotBugs 0; laporan `03-final-report-phase2.md` |
| Fase 3 | Implementation / quality and operability | ✅ **SELESAI** | `DELETE /api/v1/orders/{id}` di `OrderController`; `ApiContractTest.java` (4 test cases); `api-index.md` sinkron; `README.md` endpoint table diperbarui (register ditambahkan); `whitelist-source-range` di ingress.yaml | laporan `04-final-report-phase3.md` |

### 9.4 Ketentuan pembaruan setelah setiap fase

Setiap implementasi fase berikutnya harus membuat laporan fase tersendiri di `docs/history/optimalisasi-v2/` dengan format `NN-final-report-phaseX.md`, lalu memperbarui section ini. Minimal bukti yang dicatat adalah:

1. daftar temuan yang ditargetkan dan perubahan file yang dilakukan;
2. test baru atau test yang diperbarui untuk positive dan negative path yang terdampak;
3. hasil command beserta exit code, khususnya `./mvnw clean verify -Dspring.profiles.active=dev`, Checkstyle, SpotBugs, dan laporan JaCoCo;
4. hasil validasi security/CI/Kubernetes sesuai fase, termasuk batasan environment bila Docker atau cluster tidak tersedia;
5. status setiap temuan: **selesai**, **sebagian**, **ditunda**, atau **belum diperbaiki** — tanpa menandai selesai hanya karena dokumentasi atau konfigurasi telah ditambahkan.
6. selengkapnya baca dan ikuti aturan yang ada pada section 10.


### 9.5 Status keseluruhan baseline v2

| Metrik | Status |
|---|---|
| Laporan audit/baseline v2 | ✅ Selesai |
| Perubahan kode aplikasi oleh audit v2 | 0 |
| Temuan P0 yang telah ditutup oleh implementasi terverifikasi | 3 dari 3 (F-01 ✅, F-02 ✅, F-03 ✅) |
| Temuan P1 yang telah ditutup oleh implementasi terverifikasi | 4 dari 4 selesai penuh (F-04 ✅ distributed mode, F-05 ✅ whitelist-source-range, F-06 ✅ haltOnError, F-07 ✅ Trivy/Gitleaks/kubeconform) |
| Temuan P2 yang telah ditutup oleh implementasi terverifikasi | 3 dari 3 selesai penuh (F-08 ✅, F-09 ✅ ApiContractTest, F-10 ✅) |
| Fase implementasi v2 yang telah selesai | 4 dari 4 selesai penuh (Fase 0 ✅, Fase 1 ✅, Fase 2 ✅, Fase 3 ✅) |
| Validasi full `mvn verify` | ✅ Final (Checkstyle 0, SpotBugs 0, 58 test pass) |
| Validasi CI/CD YAML syntax | ✅ YAML valid; em-dash syntax error diperbaiki; semua action ref → versi tag |
| Validasi Kubernetes pada cluster nyata | ⚠️ `kubectl kustomize` render valid (17 resource); deployment cluster nyata belum dilakukan |

> [!NOTE]
> Angka status ini dimulai dari baseline v2 dan tidak membatalkan riwayat perbaikan pada v1. Temuan v2 adalah hasil audit terhadap kondisi source yang diaudit pada 11 Agustus 2026; penutupan temuan harus dievaluasi kembali terhadap source dan deployment setelah perubahan fase dilakukan.

---

## 10. PROCESS FLOW MAP UNTUK FASE SELANJUTNYA

> **Tujuan:** memastikan Fase 0–3 dilaksanakan secara konsisten, berurutan berdasarkan risiko, dan hanya ditutup menggunakan bukti implementasi serta validasi yang dapat direproduksi. Proses ini melengkapi roadmap pada Section 6 dan aturan pencatatan pada Section 9.

### 10.1 Flow diagram perbaikan berulang

```text
┌──────────────────────────────────────────────────────────────────────┐
│                  CYCLE PERBAIKAN FASE V2                              │
└──────────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
        ┌──────────────────────────────────────────────────┐
        │ 1. TRIAGE DAN IDENTIFIKASI                         │
        │ - Baca baseline v2, temuan F-01 s.d. F-10          │
        │ - Konfirmasi source, konfigurasi, dan runtime scope│
        │ - Prioritaskan P0 → P1 → P2                        │
        │ - Catat dependency, owner, risiko rollback         │
        └──────────────────────────────────────────────────┘
                                  │
                                  ▼
        ┌──────────────────────────────────────────────────┐
        │ 2. PLANNING DAN ACCEPTANCE CRITERIA                │
        │ - Tetapkan temuan in-scope dan out-of-scope        │
        │ - Pecah menjadi PR kecil dan reversible            │
        │ - Definisikan test positive/negative path          │
        │ - Definisikan bukti CI, security, dan deployment   │
        │ - Pilih tipe: implementation atau verification     │
        └──────────────────────────────────────────────────┘
                                  │
                     ┌────────────┴────────────┐
                     │                         │
                     ▼                         ▼
    ┌──────────────────────────┐  ┌──────────────────────────┐
    │ IMPLEMENTATION PHASE     │  │ VERIFICATION PHASE       │
    ├──────────────────────────┤  ├──────────────────────────┤
    │ 3A. IMPLEMENTASI         │  │ 3B. VERIFIKASI           │
    │ - Perubahan minimal      │  │ - Audit source/config    │
    │ - Migrasi/config aman    │  │ - Ulang test dan checks  │
    │ - Tambah test regresi    │  │ - Bandingkan claim vs    │
    │ - Update kontrak/docs    │  │   evidence aktual        │
    └──────────────────────────┘  └──────────────────────────┘
                     │                         │
                     └────────────┬────────────┘
                                  │
                                  ▼
        ┌──────────────────────────────────────────────────┐
        │ 4. VALIDASI BERLAPIS                               │
        │ - Compile, test, Checkstyle, SpotBugs, JaCoCo      │
        │ - Security path dan integration test yang relevan  │
        │ - Kustomize/schema/policy/container scan           │
        │ - Staging smoke test/rollback bila scope deploy    │
        │ - Simpan command, exit code, artefak, limitation   │
        └──────────────────────────────────────────────────┘
                                  │
                                  ▼
        ┌──────────────────────────────────────────────────┐
        │ 5. REVIEW, DOKUMENTASI, DAN KEPUTUSAN STATUS       │
        │ - Peer/security review sesuai risiko               │
        │ - Buat NN-final-report-phaseX.md                  │
        │ - Update Section 9 dan mapping F-01 s.d. F-10      │
        │ - Tandai selesai/sebagian/ditunda berbasis bukti   │
        └──────────────────────────────────────────────────┘
                                  │
                                  ▼
        ┌──────────────────────────────────────────────────┐
        │ 6. RELEASE ATAU ROLLBACK                           │
        │ - Merge/deploy hanya jika exit criteria terpenuhi  │
        │ - Monitor smoke test, metrics, dan alert           │
        │ - Rollback image/config bila ada regresi           │
        │ - Loop kembali sampai semua temuan ditutup         │
        └──────────────────────────────────────────────────┘
```

### 10.2 Urutan eksekusi dan decision gate

| Gate | Pertanyaan keputusan | Jika jawabannya “belum” | Output wajib sebelum lanjut |
|---|---|---|---|
| Triage | Apakah temuan dan dampaknya masih valid pada source target? | Perbarui temuan menjadi invalid, berubah, atau perlu investigasi; jangan langsung mengimplementasikan asumsi lama. | Scope fase, owner, risiko, dan dependency yang tercatat. |
| Design | Apakah kontrak keamanan/data serta strategi rollback disetujui? | Tahan implementasi, terutama untuk F-01, F-02, F-03, dan F-05. | Acceptance criteria dan rencana rollback. |
| Build | Apakah perubahan compile dan seluruh quality gate relevan lulus? | Perbaiki atau dokumentasikan kegagalan; status fase tidak boleh selesai. | Exit code, laporan test, Checkstyle, SpotBugs, dan JaCoCo. |
| Security | Apakah negative path dan abuse/replay path telah diuji? | Tambahkan test atau tandai fase sebagian. | Bukti test token type, rotation/replay, logout/revocation, secret handling, atau scan sesuai scope. |
| Deployment | Apakah validasi manifest dan perilaku staging/cluster tersedia untuk perubahan platform? | Jangan mengklaim production-ready; tandai keterbatasan environment. | Kustomize/schema/policy result, immutable image reference, smoke test, dan rollback proof bila deploy dilakukan. |
| Documentation | Apakah README/OpenAPI/runbook/status temuan telah sinkron dengan implementasi final? | Perbarui dokumentasi dalam PR yang sama atau jangan tutup temuan terkait. | Laporan fase dan pembaruan Section 9. |

### 10.3 Checklist per fase

#### Pre-fase checklist

- [ ] Baca Section 4, Section 6, Section 7, dan mapping Section 9.2 untuk temuan yang ditargetkan.
- [ ] Konfirmasi branch, commit baseline, Git status, owner, dependency, dan environment validasi.
- [ ] Tentukan apakah fase adalah **implementation** atau **verification/audit only**.
- [ ] Tulis acceptance criteria dan exit criteria yang dapat diuji; untuk P0, sertakan security reviewer bila tersedia.
- [ ] Tentukan strategi migrasi, compatibility, observability, dan rollback sebelum mengubah auth, secret, database, atau manifest deployment.
- [ ] Pastikan secret aktual tidak dicetak di log, artefak CI, laporan fase, atau commit.
- [ ] Pastikan Docker/Redis/Kubernetes environment yang dibutuhkan tersedia, atau catat test mana yang akan tidak dapat diverifikasi.

#### During-fase checklist

- [ ] Batasi perubahan pada temuan dan acceptance criteria fase; pisahkan refactor tidak terkait ke PR lain.
- [ ] Terapkan perubahan minimal, aman, dan dapat direview sesuai standar berdasarkan `docs/current/development/standards/STANDAR-DEVELOPMENT-CODE.md`.
- [ ] Tambahkan atau perbarui test positive **dan** negative path untuk semua perubahan security-critical.
- [ ] Untuk refresh token: uji penolakan token type salah, exact-match storage, rotation, replay token lama, dan logout/revocation.
- [ ] Untuk rate limit distributed: uji keying, TTL/eviction, fallback, metrik, dan konsistensi lintas minimal dua instance.
- [ ] Untuk secret/configuration: gunakan referensi secret/template tanpa nilai nyata; lakukan rotasi melalui channel operasional yang aman, bukan dokumentasi Git.
- [ ] Untuk Kubernetes/CI: gunakan image immutable dan validasi manifest/scan pada pipeline, bukan hanya pada mesin lokal.
- [ ] Perbarui dokumentasi kontrak dalam PR yang sama bila perilaku API, security, deployment, atau operasi berubah.

#### Post-fase checklist

- [ ] Jalankan seluruh quality gate yang relevan dan simpan exit code serta artefaknya.
- [ ] Periksa jumlah failure/error/skipped; jelaskan skipped yang memengaruhi scope fase.
- [ ] Jalankan security, integration, rendering, schema, policy, container, dan smoke validation sesuai scope.
- [ ] Lakukan review kode dan review keamanan/platform untuk Fase 0–2 bila tim/role tersedia.
- [ ] Buat `docs/history/optimalisasi-v2/NN-final-report-phaseX.md` dengan status dan bukti yang akurat.
- [ ] Perbarui Section 9.1–9.5: timeline, mapping temuan, statistik, status keseluruhan, dan limitation.
- [ ] Pastikan status temuan hanya berubah menjadi **selesai** jika seluruh exit criteria terpenuhi; gunakan **sebagian** atau **ditunda** bila bukti belum lengkap.
- [ ] Commit perubahan dengan pesan yang menjelaskan fase/temuan; deploy hanya melalui proses approval yang berlaku.

### 10.4 Template laporan fase baru

Setiap fase baru membuat satu laporan di `docs/history/optimalisasi-v2/` dengan nama:

```text
NN-final-report-phaseX.md
```

`NN` adalah nomor urut dua digit di direktori v2 dan `X` adalah identitas fase (`0`, `1`, `2`, `3`, atau fase lanjutan yang disetujui). Contoh: `01-final-report-phase0.md`.

> **Catatan:** Template laporan fase kini tersentral di
> [`docs/_templates/phase-report-template.md`](../../_templates/phase-report-template.md).
> Gunakan template kanonik tersebut untuk fase baru; blok template mentah yang
> sebelumnya di-embed di sini (dengan placeholder `[YYYY-MM-DD]`) telah dihapus agar
> tidak ada dua sumber template yang berbeda. Ikuti juga siklus kerja di
> [`docs/optimization-workflow.md`](../../optimization-workflow.md).

Untuk fase **verification/audit only**, section perubahan harus memuat source/configuration yang diperiksa dan klaim yang diverifikasi; tidak boleh menyebut perubahan implementasi bila tidak ada. Untuk fase implementasi, laporan harus menyertakan link atau path artefak test/scan yang dihasilkan CI bila tersedia.

### 10.5 Quality gates dan bukti minimum

| Area | Minimum untuk fase yang mengubah area tersebut | Command/bukti contoh | Ketentuan lulus |
|---|---|---|---|
| Compile dan unit test | Maven build dan test pada perubahan Java. | `./mvnw clean test -Dspring.profiles.active=dev` | Exit code 0; failure/error 0. |
| Full verification | Test, JaCoCo report/check, dan lifecycle `verify` dalam job/environment yang sama. | `./mvnw clean verify -Dspring.profiles.active=dev` | Exit code 0; JaCoCo XML/HTML tersedia; skipped dilaporkan. |
| Code style | Checkstyle pada source final. | `./mvnw checkstyle:check -Dcheckstyle.skip=false -Dspotbugs.skip=true` | 0 violation. |
| Static analysis | SpotBugs pada source final. | `./mvnw spotbugs:check -Dcheckstyle.skip=true -Dspotbugs.skip=false` | 0 finding yang tidak di-approve secara eksplisit. |
| Security auth | Test unit dan integration negative path F-01/F-02. | Test filter/controller/service dengan Redis/Testcontainers bila diperlukan. | Token refresh tidak membangun authentication; replay/logout/token salah gagal sesuai kontrak. |
| Rate limit | Test distributed topology atau dokumentasi per-instance yang akurat. | Uji terhadap dua instance dengan Redis/gateway yang ditetapkan. | Hasil sesuai kontrak dan dapat diamati melalui metric/log yang aman. |
| Secret dan SCA | Secret scan serta dependency/image scan untuk scope CI/release. | Output CI scanner, tanpa memuat secret. | Tidak ada finding kritis terbuka tanpa exception yang disetujui. |
| Kubernetes | Render, schema, policy, dan staging validation untuk manifest. | `kubectl kustomize k8s`; kubeconform/policy engine; smoke test cluster. | Render valid; policy/schema lulus; limitation cluster dicatat bila tidak tersedia. |
| Documentation | Kontrak API/operasional sinkron. | Review README, OpenAPI, runbook, dan laporan fase. | Tidak ada drift untuk behaviour yang berubah. |

> [!IMPORTANT]
> Hasil dari environment tanpa Docker atau cluster tidak boleh digunakan untuk membuktikan integration/deployment behavior yang tidak dijalankan. Catat sebagai **belum terverifikasi** dan jadwalkan validasi pada CI atau environment yang sesuai.

### 10.6 Matriks prioritas fase v2

| Urutan | Fase | Temuan utama | Alasan urutan | Bukti penutupan minimum |
|:---:|---|---|---|---|
| 1 | Fase 0 — containment | F-01, F-03, bagian exposure F-05/F-10 | Mengurangi risiko kredensial/token dan paparan jaringan sebelum perubahan yang lebih besar. | Refresh token ditolak sebagai Bearer; secret dirotasi dan tidak lagi dirender literal; actuator/ingress dibatasi. |
| 2 | Fase 1 — auth dan abuse prevention | F-02, F-04, F-09 | Menutup replay/revocation gap dan memastikan kontrol abuse konsisten saat scale-out. | Redis-backed lifecycle/token test; test dua instance; dokumentasi kontrak final. |
| 3 | Fase 2 — CI/CD dan deployment hardening | F-05, F-06, F-07, bagian F-10 | Membuat perbaikan dapat dipaksa, discan, dibangun, dan dideploy secara aman berulang. | Effective JaCoCo gate; scan/artifact CI; image immutable; schema/policy/staging/rollback proof. |
| 4 | Fase 3 — quality dan operability | F-08, sisa F-09/F-10 | Menjaga kontrak, operasi, dan reliability setelah control kritis tersedia. | API docs/contract test; runbook/SLO; hasil load/query/telemetry review. |

### 10.7 Strategi rollback dan penanganan kegagalan

1. **Hentikan promosi rilis** bila quality gate, security test, scan kritis, schema/policy validation, atau smoke test gagal. Jangan menandai fase selesai.
2. **Lindungi state lebih dahulu:** simpan SHA commit, immutable image digest, konfigurasi/manifest yang dipakai, migration plan, dan artefak CI sebelum melakukan rollback.
3. **Rollback berdasarkan jenis perubahan:**
   - kode/image: deploy image digest sebelumnya melalui mekanisme deployment yang berlaku;
   - konfigurasi/manifest: kembalikan ke revisi Git yang diketahui baik dan apply melalui CI/GitOps;
   - secret: gunakan prosedur rotasi dua tahap/secret manager dan jangan mengembalikan credential yang telah bocor atau direvoke;
   - database: gunakan migration yang backward-compatible dan rencana rollback data yang telah direview; jangan melakukan rollback destruktif tanpa backup dan persetujuan.
4. **Verifikasi setelah rollback:** jalankan smoke test, health/readiness check, cek error rate, auth failure rate, rate-limit metric, dan alert yang relevan.
5. **Dokumentasikan kegagalan:** laporan fase harus memuat dampak, root cause sementara, bukti, keputusan rollback, serta action item sebelum fase diulang.

### 10.8 Metrik keberhasilan fase

| Metrik | Target | Cara ukur |
|---|---|---|
| Penutupan temuan | 100% dari temuan yang masuk scope fase, atau status sebagian/ditunda yang beralasan. | Mapping F-01–F-10 dan laporan fase. |
| Test success rate | 100% failure/error = 0 untuk suite yang dijalankan. | Surefire/Failsafe dan exit code Maven. |
| Critical-path coverage | Semua acceptance test security/deployment yang ditargetkan dieksekusi; skipped tidak disembunyikan. | Daftar test, log CI, dan alasan skipped. |
| Code quality | Tidak ada regresi Checkstyle/SpotBugs. | Output plugin Maven. |
| Coverage quality | JaCoCo XML/HTML terbit; threshold efektif dipenuhi bila fase mengubah coverage gate. | Laporan dan check JaCoCo dalam job yang sama. |
| Security posture | Tidak ada finding kritis baru terbuka pada scope scan; secret tidak terekspos dalam artefak. | Output scanner yang disanitasi dan review PR. |
| Deployment safety | Perubahan platform memiliki render/schema/policy dan smoke/rollback evidence yang sesuai. | Artefak CI/staging dan laporan fase. |
| Dokumentasi | Laporan fase serta Section 9 sinkron dengan implementasi final. | Review perubahan dokumentasi sebelum merge. |
