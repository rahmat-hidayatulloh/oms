<!--
TEMPLATE: analisis baseline versi optimalisasi
Salin ke docs/history/optimalisasi-vN/ANALISIS-PROYEK-OMS-VN.md
Hapus seluruh komentar <!-- ... --> sebelum commit.
-->
# Analisis Proyek OMS — V<N>

> **Status:** Baseline analysis (snapshot)
> **Audience:** Maintainer, Developer
> **Owner:** Engineering
> **Tanggal screening:** <YYYY-MM-DD>
> **Source of truth:** Repository source code (`src/`), `pom.xml`, konfigurasi runtime, artefak test

## 1. Ringkasan Eksekutif

<!-- Kondisi proyek pada awal versi ini; jumlah temuan per severity. -->

| Severity | Jumlah | Contoh |
|---|---|---|
| 🔴 Critical | N | ... |
| 🟠 High | N | ... |
| 🟡 Medium | N | ... |
| 🟢 Low | N | ... |

## 2. Koreksi vs Versi Sebelumnya (V<N-1>)

<!-- Klaim di analisis/laporan versi sebelumnya yang ternyata tidak akurat terhadap kode aktual. -->

| Klaim V<N-1> | Fakta aktual (kode) | Dampak |
|---|---|---|
| ... | ... | ... |

## 3. Metodologi Screening

<!-- File/direktori yang diperiksa, command verifikasi yang dijalankan. -->

## 4. Matriks Kesesuaian 13 Pilar Standar

<!-- Wajib: nilai proyek terhadap docs/current/development/standards/STANDAR-DEVELOPMENT-CODE.md.
     Status: Sesuai / Sebagian / Belum terbukti / Perlu optimalisasi.
     Pola ini konsisten dengan V2 §3, V3 §3, V4 §4. -->

| Pilar | Status | Bukti positif | Gap / optimalisasi |
|---|---|---|---|
| 1. Clean Code | Sebagian | ... | ... |
| 2. Optimal | ... | ... | ... |
| 3. Reusable | ... | ... | ... |
| 4. Environment-aware | ... | ... | ... |
| 5. Secure | ... | ... | ... |
| 6. Testable | ... | ... | ... |
| 7. Version Control | ... | ... | ... |
| 8. Documentation | ... | ... | ... |
| 9. CI/CD & Observability | ... | ... | ... |
| 10. Collaboration | ... | ... | ... |
| 11. Database | ... | ... | ... |
| 12. Enterprise | ... | ... | ... |
| 13. Additional | ... | ... | ... |

## 5. Matriks Compliance SOLID

<!-- Opsional tapi direkomendasikan (V3 §5.8, V4 §5). Skala 0–10 per prinsip. -->

| Prinsip | Skor | Catatan |
|---|:---:|---|
| SRP | N/10 | ... |
| OCP | N/10 | ... |
| LSP | N/10 | ... |
| ISP | N/10 | ... |
| DIP | N/10 | ... |

## 6. Temuan

### 6.1 <Kategori — mis. Security>

| Kode | Temuan | Severity | Bukti (path:line) |
|---|---|:---:|---|
| V<N>-01 | Deskripsi | 🔴 | `src/.../File.java:NN` |

<!-- Ulangi per kategori: Clean code, Performance, Operability, dst. -->

## 7. Bukti Praktik yang Sudah Baik

<!-- Wajib: hal-hal yang SUDAH sesuai standar, agar analisis seimbang dan tidak
     merekomendasikan perubahan yang justru menurunkan kualitas (V2 §5, V3 §5, V4 §6). -->

- <Praktik baik + bukti>

## 8. Listing Phase Tindakan Lanjutan

> Setiap fase yang diselesaikan WAJIB memiliki laporan sendiri di
> `docs/history/optimalisasi-v<N>/` mengikuti
> `docs/_templates/phase-report-template.md`.

### Penamaan File

| Fase | Nama File |
|---|---|
| V<N>-P0 | `01-final-report-phaseV<N>-P0.md` |
| V<N>-P1 | `02-final-report-phaseV<N>-P1.md` |

### Phase V<N>-P0 — <Judul> — ⏳ BELUM DIMULAI

**Estimasi:** <X> hari | **Dependency:** <none / phase sebelumnya>

| # | Item | Temuan | Status | Verifikasi |
|---|---|:---:|:---:|---|
| P0.1 | <Item> | V<N>-NN | ⏳ | `command` → hasil yang diharapkan |

### Matriks Dependensi

```text
V<N>-P0 ─── bisa langsung mulai
 └── V<N>-P1 — tunggu V<N>-P0
      └── V<N>-P2 — tunggu V<N>-P1
```

## 9. Decision Gate per Fase

<!-- Gerbang keputusan sebelum sebuah fase boleh ditutup (pola V2 §10.2). -->

| Gate | Pertanyaan keputusan | Jika "belum" | Output wajib sebelum lanjut |
|---|---|---|---|
| Triage | Temuan & dampak masih valid pada source target? | Perbarui/invalidasi temuan; jangan implementasi asumsi lama | Scope, owner, risiko, dependency |
| Design | Kontrak keamanan/data & strategi rollback disetujui? | Tahan implementasi | Acceptance criteria + rencana rollback |
| Build | Perubahan compile & quality gate relevan lulus? | Perbaiki/dokumentasikan; fase belum boleh "selesai" | Exit code, test, Checkstyle, SpotBugs, JaCoCo |
| Security | Negative/abuse path telah diuji? | Tambah test atau tandai fase "sebagian" | Bukti test token/rotation/replay/secret/scan |
| Deployment | Validasi manifest & perilaku staging tersedia? | Jangan klaim production-ready; tandai batasan | Kustomize/schema/policy result, smoke test, rollback proof |
| Documentation | `docs/current/**`, index, runbook sinkron? | Perbarui di PR yang sama atau jangan tutup temuan | Laporan fase + pembaruan index/CHANGELOG |

## 10. Quality Gates & Bukti Minimum

<!-- Command konkret untuk memverifikasi tiap area (pola V4 §10.3). Sesuaikan path. -->

| Area | Minimum | Command |
|---|---|---|
| Compile + test | Build clean | `./mvnw clean test -Pdev` |
| Full verify | Test + JaCoCo + verify | `./mvnw clean verify -Pdev` |
| Code style | 0 violation | `./mvnw checkstyle:check` |
| Static analysis | 0 finding | `./mvnw spotbugs:check` |
| Coverage | JaCoCo ≥ <target>% LINE | `./mvnw jacoco:report jacoco:check` |
| Security | No critical terbuka | Trivy / Gitleaks / OWASP output |
| K8s render | Valid | `kubectl kustomize k8s/` |
| Docs governance | Link + metadata + placeholder clean | `python3 scripts/check-docs.py` |

## 11. Checklist Sinkronisasi Dokumentasi

- [ ] `docs/current/**` diperbarui untuk setiap perubahan kode current-state.
- [ ] `docs/index.md` diperbarui bila ada dokumen ditambah/dipindah.
- [ ] `CHANGELOG.md` mencatat perubahan bermakna.
- [ ] Dokumen yang digantikan dipindah ke `history/` dengan banner `Superseded by`.
- [ ] Link-check, metadata, dan placeholder (CI `docs-quality.yml`) hijau.
