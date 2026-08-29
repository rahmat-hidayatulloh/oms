# Analisis Proyek OMS — V5

> **Status:** CLOSED — F-14 Restructure package by-layer → by-feature selesai pada Phase V5-P0 (lihat `01-final-report-phaseV5-P0.md`)
> **Audience:** Maintainer, Developer
> **Owner:** Engineering
> **Tanggal screening:** 2026-08-25
> **Source of truth:** Repository source code (`src/`), `pom.xml`, konfigurasi runtime, artefak test

## 1. Ringkasan Eksekutif

V5 adalah versi fokus-tunggal: menutup **F-14** satu-satunya temuan yang di-defer dari V4
(`02-final-report-phaseV4-P1.md` §9, item P1.5). Seluruh temuan V3/V4 lain telah tertutup
(lihat `ANALISIS-PROYEK-OMS-V4.md` — compliance ≥ 95%).

| Severity | Jumlah | Contoh |
|---|---|---|
| 🔴 Critical | 0 | — |
| 🟠 High | 0 | — |
| 🟡 Medium | 1 | V5-01: domain codebase terorganisir by-layer, bukan by-feature (F-14) |
| 🟢 Low | 1 | V5-02: pola exclude JaCoCo (`com/oms/dto/**`, `com/oms/entity/**`) menjadi stale setelah restructure |

## 2. Koreksi vs Versi Sebelumnya (V4)

| Klaim V4 | Fakta aktual (kode) | Dampak |
|---|---|---|
| Angka inventaris V4 (116 source / 341 tests / JaCoCo 84.03%) | Aktual pasca-fase V4: 120 source Java (6.855 baris), 63 test (7.516 baris), 316 unit test pass, JaCoCo 82% LINE | Sudah dikoreksi di `ANALISIS-PROYEK-OMS-V4.md` pada sinkronisasi pasca-verifikasi; tidak ada klaim teknis lain yang meleset |

## 3. Metodologi Screening

- Inventaris penuh `src/main/java/com/oms` (17 direktori, 120 file) dan `src/test/java/com/oms` (16 direktori, 63 file) via `find`.
- Analisis dependensi silang antar kelas untuk menentukan home feature (`grep -rln` per kunci: `UserRepository`, `EmailServiceInterface`, `SafeHtml`, `PasswordStrength`, `BaseEntity`, dsb.).
- Verifikasi tidak ada wildcard import (`import com.oms.*`) dan tidak ada FQN `com.oms.*` di file non-Java (`config/`, `src/main/resources`, `pom.xml`).
- Verifikasi string runtime yang menyebut package: pointcut AOP memakai binding `@annotation(auditable)` (bukan FQN), `@RestControllerAdvice(basePackages = "com.oms")` dan Redis `allowIfSubType("com.oms.")` tetap valid terhadap sub-package.

## 4. Matriks Kesesuaian 13 Pilar Standar

| Pilar | Status | Bukti positif | Gap / optimalisasi |
|---|---|---|---|
| 1. Clean Code | Sebagian | Constructor injection ✅, DTO record ✅, Checkstyle 0 ✅ | **V5-01**: organisasi package by-layer (Standar 1.12 belum terpenuhi) |
| 2. Optimal | Sesuai | HikariCP, Redis pooling per-env, `@EntityGraph`, rate limiting, CB+Retry | — |
| 3. Reusable | Sebagian | Service interfaces, factory, `@RateLimit` | Cohesion antar-fitur sulit dinilai karena layer memisahkan domain |
| 4. Environment-aware | Sesuai | Multi-profile, overlay Kustomize, `StartupConfigGuard` | — |
| 5. Secure | Sesuai | JWT hardening (V4-P0), actuator minimal, ingress whitelist | — |
| 6. Testable | Tinggi | 63 test file, Testcontainers, contract test, JaCoCo gate 0.80 | Struktur test by-layer ikut di-restructure agar mirror dengan main |
| 7–13 | Sesuai | Lihat `ANALISIS-PROYEK-OMS-V4.md` §4 (tidak berubah oleh V5) | — |

## 5. Matriks Compliance SOLID

| Prinsip | Skor | Catatan |
|---|:---:|---|
| SRP | 7/10 | Naik pasca-V4-01/02; package by-feature mempertegas boundary modul |
| OCP | 6/10 | Strategy via interface ✅ |
| LSP | 9/10 | `UserDetailsAdapter`, `NoOpRefreshTokenService` ✅ |
| ISP | 9/10 | 7 service interfaces ✅ |
| DIP | 8/10 | Constructor injection penuh pasca-V4-02 |

## 6. Temuan

### 6.1 Struktur Kode

| Kode | Temuan | Severity | Bukti (path) |
|---|---|:---:|---|
| V5-01 (= F-14) | Domain codebase disusun by-layer (`controller/`, `service/`, `dto/`, `entity/`, `repository/`); melanggar Standar 1.12 Package by Feature | 🟡 | `src/main/java/com/oms/{controller,service,dto,entity,repository}/` |
| V5-02 | Pola exclude JaCoCo di `pom.xml` (`com/oms/dto/**`, `com/oms/entity/**`) akan stale setelah restructure; harus diubah ke `com/oms/*/dto/**`, `com/oms/*/entity/**` agar gate coverage tetap setara | 🟢 | `pom.xml:371-375` |

## 7. Bukti Praktik yang Sudah Baik

- `ratelimit/` sudah berbentuk feature-cohesive (interceptor, service, store, factory, properties) — menjadi model untuk feature lain.
- Tidak ada wildcard import dan tidak ada referensi FQN `com.oms.*` di konfigurasi XML/YAML — restructure aman terhadap tooling.
- Pointcut AOP berbasis binding parameter (`@Around("@annotation(auditable)")`) sehingga pemindahan annotation tidak memerlukan perubahan expression.
- `git mv` tersedia untuk mempertahankan history file.

## 8. Listing Phase Tindakan Lanjutan

> Setiap fase yang diselesaikan WAJIB memiliki laporan sendiri di
> `docs/history/optimalisasi-v5/` mengikuti
> `docs/_templates/phase-report-template.md`.

### Penamaan File

| Fase | Nama File |
|---|---|
| V5-P0 | `01-final-report-phaseV5-P0.md` |

### Phase V5-P0 — Restructure Package by-Feature — ✅ SELESAI (25 Agustus 2026)

**Estimasi:** 1–2 hari | **Dependency:** tidak ada (V4 seluruhnya CLOSED)

| # | Item | Temuan | Status | Verifikasi |
|---|---|:---:|:---:|---|
| P0.1 | Pindah ±70 file main + ±36 file test ke feature `auth/order/product/admin/audit/system/validation` (sub-layer `controller/service/dto/entity/repository` mengikuti Standar 1.12) | V5-01 | ✅ | `find src -name '*.java'` → tidak ada lagi `controller/`, `service/`, `dto/`, `repository/`, `security/` generik untuk kode domain |
| P0.2 | Perbarui pola exclude JaCoCo di `pom.xml` | V5-02 | ✅ | Gate `jacoco:check` lulus dengan threshold LINE 0.80 |
| P0.3 | Quality gates penuh + SYNC dokumentasi current-state | V5-01 | ✅ | `./mvnw clean verify -Pci` BUILD SUCCESS; `python3 scripts/check-docs.py` PASSED |

### Matriks Dependensi

```text
V5-P0 ─── bisa langsung mulai (satu-satunya fase di V5)
 └── P0.1 → P0.2 → P0.3 (berurutan; P0.2 wajib sebelum VERIFY)
```

## 9. Decision Gate per Fase

| Gate | Pertanyaan keputusan | Jika "belum" | Output wajib sebelum lanjut |
|---|---|---|---|
| Triage | F-14 masih relevan pada struktur aktual? | — | Scope pemetaan feature (laporan fase §2) |
| Design | Pemetaan kelas → feature & strategi rollback (`git mv`) disetujui? | Tahan implementasi | Tabel pemetaan di laporan fase |
| Build | Compile + test + gates lulus pasca-restructure? | Perbaiki; fase belum boleh selesai | `./mvnw clean verify -Pci` hijau |
| Documentation | `docs/current/**`, ONBOARDING, index, CHANGELOG, ADR sinkron? | Perbarui di PR yang sama | Daftar SYNC di laporan fase §6 |

## 10. Quality Gates & Bukti Minimum

| Area | Minimum | Command |
|---|---|---|
| Compile + test | Build clean | `./mvnw clean test -Pdev` |
| Full verify | Test + JaCoCo + verify | `./mvnw clean verify -Pci` |
| Code style | 0 violation | `./mvnw checkstyle:check` |
| Static analysis | 0 finding | `./mvnw spotbugs:check` |
| Coverage | JaCoCo ≥ 80% LINE | `./mvnw jacoco:report jacoco:check` |
| Docs governance | Link + metadata + placeholder clean | `python3 scripts/check-docs.py` |

## 11. Checklist Sinkronisasi Dokumentasi

- [ ] `docs/current/**` diperbarui untuk setiap perubahan kode current-state (struktur package).
- [ ] `docs/index.md` diperbarui bila ada dokumen ditambah/dipindah.
- [ ] `CHANGELOG.md` mencatat perubahan bermakna.
- [ ] ADR baru untuk keputusan arsitektur package-by-feature.
- [ ] Link-check, metadata, dan placeholder (CI `docs-quality.yml`) hijau.

