# Final Report — Phase V3-5: Audit Alignment & Hardening Backlog

- **Tanggal:** 20 Agustus 2026
- **Status:** ✅ Complete (Fix A–D + Docs E; R-4 package refactor tetap deferred)
- **Baseline:** Phase R (16 Agustus 2026) + kesesuaian scan read-only (20 Agustus 2026)
- **Quality gates:** ✅ Checkstyle 0 violations, ✅ SpotBugs 0 findings, ✅ JaCoCo 84.03% LINE (gate 0.80)
- **Tests:** 314 pass, 0 failures, 0 errors (+3 test baru `ErrorPropertiesTest`)

---

## 1. Ringkasan Eksekutif

Fase ini mengeksekusi seluruh temuan audit kesesuaian (kode aktual vs dokumentasi).
Item di mana **kode aktual lebih baik daripada klaim dokumen DIPERTAHANKAN** dan
dokumen yang dikoreksi; item di mana kode kurang DIPERBAIKI.

| Fix | Item | Status | Hasil |
|---|---|:---:|---|
| **A** | 2 step Trivy CI yang ter-comment di `ci-cd.yml` | ✅ | Di-un-comment + `.trivyignore` deferral terdokumentasi |
| **B** | `ERROR_BASE_URL` hardcoded; `app.error.base-url` dead config | ✅ | `ErrorProperties` + constructor injection di 4 handler + 3 test |
| **C** | K8s misconfig: KSV-0118 ×4, KSV-0014 ×2, KSV-0109 ×1 | ✅ | Trivy misconfig repo = **0 CRITICAL/HIGH** |
| **D** | Spring Boot 3.2.5 → 3.2.12 | ✅ | BUILD SUCCESS; 314/314 test; vuln 43 → 37 |
| **E** | Discrepancy dokumentasi (4) + inventaris basi | ✅ | ANALISIS §header/§1.2/§1.4/§2.3/§9.3/§13 dikoreksi |
| — | File sampah `0` di root (45 byte, artefak sesi putus) | ✅ | Dihapus |

## 2. Perubahan File

### 2.1 Dibuat baru (3 + 2 artefak)

| File | Deskripsi |
|---|---|
| `src/main/java/com/oms/config/ErrorProperties.java` | `@ConfigurationProperties("app.error")`, default `https://api.oms.com/errors` (`DEFAULT_BASE_URL`) |
| `src/main/java/com/oms/config/ErrorPropertiesConfig.java` | `@EnableConfigurationProperties(ErrorProperties.class)` (pola sama dengan `SecurityCorsConfig`) |
| `src/test/java/com/oms/config/ErrorPropertiesTest.java` | 3 test: default, override manual, binding via `ApplicationContextRunner` |
| `.trivyignore` | 37 advisories deferred (36 CVE + 1 GHSA `GHSA-r7wm-3cxj-wff9`; fix hanya tersedia di SB 3.3+/3.4+; 3.2.x EOL) — justifikasi lengkap di dalam file |
| `docs/history/optimalisasi-v3/06-final-report-phaseV3-5.md` | Laporan ini |

### 2.2 Dimodifikasi (13)

| File | Perubahan |
|---|---|
| `pom.xml` | `spring-boot-starter-parent` 3.2.5 → **3.2.12** |
| `.github/workflows/ci-cd.yml` | Un-comment step `Build JAR for Trivy scan` + `Run Trivy filesystem scan` (`ignore-unfixed: true`, severity CRITICAL,HIGH, exit-code 1 dipertahankan) |
| `k8s/postgres.yaml` | +pod securityContext (runAsNonRoot/runAsUser 70/fsGroup 70 — user postgres alpine), +container securityContext (allowPrivilegeEscalation false, readOnlyRootFilesystem, drop ALL, seccomp RuntimeDefault), `subPath: pgdata` → env `PGDATA=/var/lib/postgresql/data/pgdata` (subPath tidak menerima fsGroup), +emptyDir `/tmp` & `/var/run/postgresql` |
| `k8s/redis.yaml` | +pod securityContext (runAsUser/fsGroup 999 — user redis alpine), +container securityContext (setara deployment.yaml), +emptyDir `/data` & `/tmp` |
| `k8s/configmap.yaml` | Hapus `app.jwt.min-secret-length` & `app.refresh-token.expiration-days` (pindah ke Secret) |
| `k8s/secret.yaml` | +2 key policy (`app.jwt.min-secret-length: "32"`, `app.refresh-token.expiration-days: "7"`) dengan justifikasi — env var name verbatim, binding Spring tidak berubah |
| `src/main/java/com/oms/exception/BaseExceptionHandler.java` | Hapus konstanta `ERROR_BASE_URL`; constructor `(ErrorProperties, HttpServletRequest)`; `type` URI dari `errorProperties.getBaseUrl()` |
| `SecurityExceptionHandler.java` / `ValidationExceptionHandler.java` / `BusinessExceptionHandler.java` / `InfrastructureExceptionHandler.java` | Constructor menerima & meneruskan `ErrorProperties` ke super |
| `GlobalExceptionHandlerEdgeTest.java` / `GlobalExceptionHandlerGeneralTest.java` | Instantiate `new ErrorProperties()` untuk konstruktor baru; asersi `$.type` default tidak berubah |
| `CHANGELOG.md` | Entri Changed/Security/Added untuk fase ini |
| `docs/history/optimalisasi-v3/ANALISIS-PROYEK-OMS-V3.md` | Koreksi §header, §1.2, §1.4, §2.3, §9.3 (D-5), §13, footer |

## 3. Validasi Berlapis (command + hasil)

| # | Command | Hasil |
|---|---|---|
| 1 | `./mvnw clean verify -Pci` | **BUILD SUCCESS** (03:50 min) — compile + Checkstyle **0 violations** + SpotBugs check **lolos** + test + JaCoCo **"All coverage checks have been met"** |
| 2 | (dari log verify) `Tests run:` | **314, Failures: 0, Errors: 0** — regresi penuh pasca-bump SB 3.2.12 + refactor handler (baseline 311 → +3 `ErrorPropertiesTest`) |
| 3 | JaCoCo CSV parse | **LINE 84.03%** (covered 889 / total 1058) — di atas gate **0.80** |
| 4 | `kubectl kustomize k8s/` | exit **0**, **16 resource** (3 NetworkPolicy dalam 1 file) — sebelum & sesudah hardening |
| 5 | `trivy fs --scanners misconfig --severity CRITICAL,HIGH .` | **0 failures** — postgres/redis/configmap/secret semuanya clean (baseline: 7 HIGH) |
| 6 | `trivy fs --scanners vuln,secret --severity CRITICAL,HIGH --ignore-unfixed .` | **37 fixable** (5 CRITICAL/32 HIGH) tersisa vs baseline **43** — semua fix tersedia hanya di garis dependensi Spring Boot 3.3+/3.4+ |
| 7 | Gate CI simulation: command #6 + `--exit-code 1` + `.trivyignore` | exit **0** (hijau) — gate tetap menangkap CVE baru di luar daftar deferral |
| 8 | `grep -rn "ERROR_BASE_URL = " src/main/java/` | **0** — hardcode hilang (hanya env fallback `${ERROR_BASE_URL:...}` di application.yml yang kini benar-benar dibinding) |

## 4. Status Temuan Audit → Remediasi

| Temuan | Status | Bukti |
|---|:---:|---|
| JaCoCo gate 0.80 vs klaim dokumen 50% — aktual lebih baik | ✅ Dipertahankan | Dokumen dikoreksi (§1.2); gate 0.80 + aktual 84.03% |
| 2 interface granular Admin vs klaim `AdminServiceInterface` — aktual lebih baik | ✅ Dipertahankan | Dokumen dikoreksi (§9.3 D-5) |
| 2 step Trivy ter-comment (baris 152/155) | ✅ Fix A | Di-enable + `.trivyignore` |
| `ERROR_BASE_URL` hardcoded / `app.error.base-url` dead | ✅ Fix B | `ErrorProperties` + 4 handler + test |
| KSV-0118 ×4 + KSV-0014 ×2 (postgres/redis) | ✅ Fix C | securityContext lengkap; Trivy 0 |
| KSV-0109 (configmap `min-secret-length`/`expiration-days`) | ✅ Fix C | Key dipindah ke `oms-secrets`; binding tak berubah |
| Trivy vuln 43 CRIT+HIGH | 🔶 Fix D (partial by design) | 43 → 37 via SB 3.2.12; sisa didefer (§6) |
| 5 CI job vs klaim 4; 16 resource vs klaim 17 | ✅ Docs E | Inventaris dikoreksi |
| File sampah `0` | ✅ | `rm` |

## 5. Exit Criteria

| Kriteria | Target | Aktual | Status |
|---|---|---|:---:|
| Regresi penuh | semua test PASS | 314/314 PASS | ✅ |
| Coverage | ≥ 84% LINE (gate 0.80) | 84.03% | ✅ |
| Checkstyle/SpotBugs | 0 violation / 0 finding | 0 / 0 | ✅ |
| Trivy misconfig | 0 HIGH/CRITICAL | 0 | ✅ |
| Trivy vuln | turun dari 43 | 37 (deferral terdokumentasi) | ✅ |
| CI Trivy gate | enabled & hijau | enabled; simulasi lokal exit 0 | ✅ |
| Kustomize | render valid | 16 resource, exit 0 | ✅ |

## 6. Catatan Teknis

1. **KSV-0109 tidak bisa di-suppress inline.** Trivy melaporkan temuan ini
   resource-level tanpa nomor baris (`StartLine: None`); 4 varian penempatan
   `#trivy:ignore:KSV-0109`/`AVD-KSV-0109` (atas key, atas `data:`, atas `kind:`)
   dan 3 varian schema `trivy.yaml` (`misconfiguration.skip-checks` list AVD/pendek,
   `scan.skip-checks`) semuanya tidak berpengaruh di Trivy 0.74.0. Flag CLI
   `--skip-check` bekerja, tetapi tidak reliable lintas runner. Solusi final:
   pindahkan 2 key ke `oms-secrets` (deployment sudah `envFrom secretRef`) —
   tanpa suppress global, temuan hilang struktural. `trivy.yaml` sengaja tidak
   dipertahankan.
2. **postgres `subPath` → `PGDATA`.** Mount dengan `subPath` tidak menerima
   kepemilikan `fsGroup` (perilaku Kubernetes yang diketahui); postgres non-root
   (UID 70 alpine) tidak bisa menulis ke direktori subPath milik root. Ganti ke
   mount PVC penuh + `PGDATA=/var/lib/postgresql/data/pgdata` mempertahankan
   layout data lama secara persis (tidak ada migrasi data).
3. **Spring Boot 3.2.12 = rilis OSS terakhir 3.2.x.** Sisa 37 temuan fixable
   (mis. CVE-2025-24813 Tomcat, CVE-2026-22732 Spring Security) hanya difix di
   versi yang dibundel SB 3.3+/3.4+; garis 3.2.x EOL. `.trivyignore` mendokumentasikan
   deferral per CVE; gate CI tetap gagal untuk CVE baru.
4. **UID image**: postgres:16-alpine = 70/70; redis:7-alpine = 999/999
   (berbeda dari varian Debian 999/999 & 999/999 — postgres alpine memakai 70).

## 7. Sisa / Deferred

| Item | Alasan |
|---|---|
| **R-4 (F-14)** package by-feature | Tetap deferred (sprint terpisah, §13 ANALISIS) |
| **Spring Boot 3.4/3.5 upgrade** | Membersihkan 37 CVE deferred di `.trivyignore`; perlu sprint regresi terpisah |
| **Verifikasi runtime manifest** (UID postgres/redis di cluster nyata) | Butuh cluster; statis divalidasi terhadap Dockerfile resmi image |

## 8. Kronologi Commit Pasca-Phase R (rekonsiliasi ketentuan §9.7 ANALISIS)

Commit berikut terjadi setelah pelaporan Phase R (16 Aug) dan tidak tercatat di folder optimalisasi-v3 — temuan audit kesesuaian 18 Aug (§2 & §7.3); inilah yang menyebabkan drift inventaris 112→114 main / 60→62 test. Dicatat di sini sebagai laporan ad-hoc yang diminta §9.7:

| Commit | Tanggal | Pesan | Efek pada tree |
|---|---|---|---|
| `56508e7` | 2026-08-15 | cicd msg: Cross-Site Scripting (XSS) Fixes | +`security/SafeHtml.java` + `security/SafeHtmlValidator.java` (main 112→114); `util/InputSanitizer` diperluas; validasi `ProductRequest`/`RegisterRequest`; +2 test (60→62); masuk ke HEAD via merge PR #23/#26 |
| `1046344` | 2026-08-17 | git msg: disable trivy | 2 step Trivy di `ci-cd.yml` di-comment-out — **di-re-enable oleh V3-5.4** (Fix A) |
| `95f38ab` | 2026-08-18 | docker msg: install curl for mvnw | Dockerfile runtime: curl untuk Maven Wrapper |
| `4ca2b1f` | 2026-08-18 | cicd msg: docker permissions update | Permission Dockerfile/CI (posisi HEAD saat Phase V3-5 dimulai) |

Sumber audit: `Hasil-Analisa-Sebelumnya.md` (laporan verifikasi read-only, 18 Aug 2026).

---

*Phase V3-5 menutup seluruh temuan audit kesesuaian scan 20 Agustus 2026 (Fix A–D + Docs E). Item dengan kondisi aktual lebih baik dari klaim dokumen dipertahankan dan dokumennya dikoreksi. Validasi: BUILD SUCCESS 314/314, Checkstyle/SpotBugs 0, JaCoCo 84.03% (gate 0.80), kustomize 16 resource valid, Trivy misconfig 0, Trivy vuln 43→37 dengan deferral terdokumentasi.*

