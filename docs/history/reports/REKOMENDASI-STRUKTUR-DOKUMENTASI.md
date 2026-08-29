# Rekomendasi Struktur Dokumentasi — OMS

> **Superseded by:** [`docs/index.md`](../../index.md), [`../../../CONTRIBUTING.md`](../../../CONTRIBUTING.md) (Documentation Governance), dan [`docs/optimization-workflow.md`](../../optimization-workflow.md).
>
> **Status:** Historical / Superseded — snapshot screening 10 Agustus 2026. Reorganisasi yang direkomendasikan **sudah dieksekusi**; statistik (mis. "89 file", `docs-optimalisasi/`, `.continue/`) dan pernyataan "reorganisasi belum dilakukan" di bawah ini tidak lagi mencerminkan struktur repository saat ini. Disimpan untuk audit trail.

> **Tujuan:** Menganalisis seluruh file Markdown yang ada di repository, mengklasifikasikan dokumen berdasarkan isi dan kegunaannya, mendeteksi inkonsistensi, kemudian merekomendasikan penataan serta penyesuaian isi — **tanpa mengubah file dokumentasi lain**.
>
> **Tanggal screening:** 10 Agustus 2026  
> **Scope:** seluruh file `*.md` di repository, kecuali direktori `.git/`  
> **Status dokumen ini:** hasil screening dan rekomendasi; bukan instruksi bahwa reorganisasi sudah dilakukan.

---

## Daftar Isi

1. [Metodologi dan Batasan](#1-metodologi-dan-batasan)
2. [Ringkasan Temuan](#2-ringkasan-temuan)
3. [Inventaris Lengkap dan Klasifikasi](#3-inventaris-lengkap-dan-klasifikasi)
4. [Analisis Kualitas Isi](#4-analisis-kualitas-isi)
5. [Tiga Skema Struktur yang Dipertimbangkan](#5-tiga-skema-struktur-yang-dipertimbangkan)
6. [Struktur Target yang Direkomendasikan](#6-struktur-target-yang-direkomendasikan)
7. [Mapping Lengkap File ke Struktur Target](#7-mapping-lengkap-file-ke-struktur-target)
8. [Referensi Internal dan Inkonsistensi yang Perlu Diperbaiki](#8-referensi-internal-dan-inkonsistensi-yang-perlu-diperbaiki)
9. [Rekomendasi Penyesuaian Isi](#9-rekomendasi-penyesuaian-isi)
10. [Rencana Eksekusi Bertahap](#10-rencana-eksekusi-bertahap)
11. [Checklist Validasi](#11-checklist-validasi)

---

## 1. Metodologi dan Batasan

Screening dilakukan terhadap:

- daftar file `*.md` aktual dari filesystem;
- status tracking Git untuk membedakan file yang sudah dilacak dan file baru;
- judul, heading, metadata tanggal/status, daftar isi, code fence, dan tautan Markdown;
- referensi path dokumen yang ditulis sebagai teks biasa;
- kesesuaian antar dokumen analisis, API, arsitektur, deployment, standar, dan laporan fase;
- keberadaan target tautan internal pada filesystem.

### Batasan interpretasi

- Analisis ini tidak menilai kebenaran setiap klaim implementasi terhadap seluruh source code secara end-to-end. Klaim teknis yang saling berbeda ditandai sebagai **perlu verifikasi terhadap source code/configuration sebagai source of truth**.
- Tidak semua teks `docs/...` adalah tautan Markdown. Sebagian merupakan contoh konfigurasi, URL dokumentasi pihak ketiga, atau teks literal. Daftar masalah di bawah membedakan tautan Markdown yang benar-benar rusak dari referensi path yang perlu ditinjau.
- File `/.continue/rules/new-rule.md` dihitung sebagai Markdown repository, tetapi bukan dokumentasi produk. File tersebut adalah konfigurasi/rule tooling dan sebaiknya tidak dicampur dengan dokumentasi OMS.
- File rekomendasi ini berada di `docs-generate/` dan pada saat screening berstatus file baru/untracked. Karena itu, jumlah file aktual dan jumlah file tracked berbeda.

---

## 2. Ringkasan Temuan

### 2.1 Statistik aktual

| Aspek | Hasil screening |
|---|---|
| Total file Markdown aktual | **89 file** |
| Markdown tracked Git | **88 file** |
| Markdown untracked | **1 file**, yaitu dokumen rekomendasi ini |
| File dokumentasi produk/engineering utama | **87 file tracked**, tidak termasuk rule `.continue` |
| Root project | 4 file: `README.md`, `CONTRIBUTING.md`, `CHANGELOG.md`, `benchmark_results.md` |
| `.continue/` | 1 file tooling rule |
| `docs/` | **70 file** |
| `docs-optimalisasi/` dan `v1/` | **13 file**: 2 file utama + 11 laporan fase |
| `docs-generate/` | 1 file rekomendasi screening |
| Nomor dokumen di `docs/` | 01–43, **gap 44**, lalu 45–67; total 66 file bernomor |
| Dokumen `docs/` tanpa nomor | 4 file: analisis utama, API index, architecture, deployment guide |
| Laporan optimalisasi V0 | 20 artefak: analisis V0, laporan pendahuluan 42–43, serta laporan/analisis 45–61; 53–54 bukan laporan fase murni |
| Laporan fase optimalisasi v1 | 11 file, fase 1–11 |
| Broken Markdown links terdeteksi | 10 target: 1 `LICENSE` + 9 tautan relatif di 2 dokumen |
| Referensi path dokumen tidak ditemukan | 10 pola/path unik utama, termasuk file 44, file 43 yang tidak ada, dan placeholder path |

### 2.2 Masalah struktural utama

1. **Flat structure di `docs/`:** task, feature specification, testing, API, deployment, analisis, dan laporan historis bercampur dalam satu folder.
2. **Nomor urut tidak lagi menjadi taxonomy:** nomor 01–67 merepresentasikan urutan pekerjaan historis, bukan kategori yang membantu navigasi. Nomor 44 juga hilang.
3. **Tidak ada entry point dokumentasi:** README belum menyediakan indeks tautan menuju keseluruhan dokumentasi teknis; `docs/api-index.md` hanya menjadi indeks API.
4. **Dua sumber analisis historis besar:** `docs/history/optimalisasi-v0/ANALISIS-PROYEK-OMS-V0.md` dan `docs/history/optimalisasi-v1/ANALISIS-PROYEK-OMS-V1.md` memiliki tujuan yang beririsan, sedangkan current-state diringkas oleh `docs/index.md` dan dokumen pada `docs/current/`.
5. **Status historis saling bertentangan:** beberapa snapshot menyatakan 37/42 issue selesai atau 230/234 test, sedangkan dokumen yang lebih baru menyatakan status lain seperti 54 issue selesai dan jumlah test berbeda.
6. **Referensi silang rapuh:** ada referensi ke `docs/44-...`, `docs/43-analisis-struktur-dan-flow-bisnis.md`, beberapa typo `v1`, dan placeholder `NN-final-report-phaseX.md` yang terbaca seperti path aktual.
7. **Isi teknis berpotensi usang:** versi Spring Boot, jumlah test, status rate limiting, endpoint metrics, dan strategi retry perlu diverifikasi terhadap source code/configuration terbaru.
8. **Format tidak seragam:** sebagian dokumen berbahasa Indonesia, sebagian Inggris, sebagian memakai emoji, sebagian memakai format `Task N`, sebagian `OMS-NNN`, dan sebagian berupa laporan tanpa metadata standar.
9. **Heading tidak konsisten:** sejumlah dokumen mempunyai beberapa heading level-1 karena contoh/template atau bagian lampiran. Ini mengganggu navigasi otomatis dan pembuatan TOC.
10. **Dokumentasi historis dan dokumentasi operasional bercampur:** laporan fase lama masih berada sejajar dengan panduan yang seharusnya dibaca pengguna/developer saat ini.

### 2.3 Kesimpulan screening

Dokumentasi proyek kaya secara teknis, tetapi lebih menyerupai **log evolusi implementasi** daripada knowledge base yang terkurasi. Rekomendasi utama adalah mempertahankan histori untuk auditability, namun memisahkannya dari dokumentasi normatif/current-state dan menyediakan satu indeks navigasi sebagai entry point.

---

## 3. Inventaris Lengkap dan Klasifikasi

Klasifikasi di bawah menggunakan **peran utama**. Sebuah file dapat memiliki peran sekunder, tetapi saat reorganisasi harus dipindahkan hanya ke satu lokasi utama dan dirujuk dari kategori lain.

### 3.1 Root project — meta, onboarding, dan evidence

| File | Klasifikasi utama | Observasi |
|---|---|---|
| `README.md` | Onboarding / project overview | Entry point proyek, quick start, API ringkas, konfigurasi, deployment, testing. Perlu menjadi hub menuju `docs/`. |
| `CONTRIBUTING.md` | Governance kontribusi | Workflow kontribusi, quality checks, testing, commit convention. |
| `CHANGELOG.md` | Release history | Menyebut tiga dokumen current-state sebagai Added, tetapi path masih berupa teks biasa. |
| `benchmark_results.md` | Performance evidence | Hasil benchmark aktual; sebaiknya menjadi ringkasan/evidence yang ditautkan ke prosedur benchmark. |

### 3.2 Tooling dan artefak screening

| File | Klasifikasi | Rekomendasi |
|---|---|---|
| `.continue/rules/new-rule.md` | Tooling rule | Keluarkan dari inventaris dokumentasi produk. Lengkapi atau hapus hanya melalui keputusan terpisah karena isinya masih placeholder. |
| `docs-generate/REKOMENDASI-STRUKTUR-DOKUMENTASI.md` | Documentation governance | Pertahankan sebagai artefak screening/rencana; jangan dicampur dengan panduan runtime. |

### 3.3 `docs/` — 70 file

| Kelompok isi | File |
|---|---|
| Getting started dan repository setup | `docs/01-inisialisasi-project.md`, `docs/03-multi-profile-configuration.md`, `docs/04-gitignore.md`, `docs/09-flyway-migration.md` |
| Engineering standards dan quality gates | `docs/02-clean-code-quality-gates.md` |
| Domain model dan autentikasi dasar | `docs/05-entity-user-autentikasi.md`, `docs/07-entity-product.md`, `docs/08-entity-order.md`, `docs/10-product-service.md`, `docs/11-order-service.md`, `docs/12-product-controller.md`, `docs/13-jwt-util.md`, `docs/14-auth-controller.md`, `docs/15-jwt-authentication-filter.md`, `docs/16-security-config.md`, `docs/17-refresh-token-redis.md`, `docs/18-global-exception-handler.md` |
| API, error handling, dan data/performance features | `docs/06-swagger-openapi.md`, `docs/19-global-exception-handler-rfc7807.md`, `docs/20-querydsl-product-filter.md`, `docs/21-fix-n-plus-one-order-items.md`, `docs/22-optimistic-locking-stock.md`, `docs/23-soft-delete-all-entities.md`, `docs/24-jpa-auditing.md`, `docs/25-redis-cache-config.md`, `docs/26-product-caching.md`, `docs/27-async-email-notification.md`, `docs/28-hikaricp-connection-pool-tuning.md`, `docs/29-custom-health-indicator.md`, `docs/30-structured-logging-mdc.md`, `docs/31-api-versioning.md` |
| Testing | `docs/32-testcontainers-integration.md`, `docs/33-unit-test-product-service.md`, `docs/34-product-controller-test.md`, `docs/35-repository-test-product.md`, `docs/38-oms-033-order-flow-integration-test.md` |
| Architecture, project analysis, dan refactoring analysis | `docs/36-project-analysis-report.md`, `docs/37-project-structure-documentation.md`, `docs/39-errorcode-refactoring-and-null-safety.md`, `docs/40-refactoring-summary.md`, `docs/41-refactoring-vs-analysis.md` |
| Current-state reference yang belum bernomor | `docs/index.md`, `docs/current/api/api-index.md`, `docs/current/overview/architecture.md`, `docs/current/operations/deployment-guide.md` |
| Optimalisasi V0 | `docs/history/optimalisasi-v0/ANALISIS-PROYEK-OMS-V0.md`, `docs/history/optimalisasi-v0/42-final-execution-report.md`, `docs/history/optimalisasi-v0/43-laporan-perbaikan-implementasi.md`, dan laporan 45–61 di folder yang sama |
| API testing artifact | `docs/62-oms-034-postman-collection.md` |
| DevOps, platform, security control, dan performance task | `docs/63-oms-036-dockerize.md`, `docs/64-oms-037-swagger-openapi.md`, `docs/65-oms-038-minikube.md`, `docs/66-oms-039-rate-limiting.md`, `docs/67-oms-040-benchmark.md` |

### 3.4 `docs-optimalisasi/` — 13 file

| Kelompok isi | File |
|---|---|
| Baseline audit dan source-of-practice | `docs/history/optimalisasi-v1/ANALISIS-PROYEK-OMS-V1.md`, `docs/current/development/standards/STANDAR-DEVELOPMENT-CODE.md` |
| Laporan fase optimalisasi V1 | `docs/history/optimalisasi-v1/01-final-report-phase1.md` sampai `11-final-report-phase11.md` |

### 3.5 Kategori yang perlu dibedakan saat reorganisasi

| Tipe | Makna | Perlakuan |
|---|---|---|
| **Normative/current-state** | Cara menjalankan, kontrak API, arsitektur aktual, standar yang berlaku | Harus akurat, ringkas, punya owner dan tanggal verifikasi. |
| **How-to/implementation note** | Penjelasan fitur atau keputusan teknis tertentu | Pertahankan jika masih berguna; tambahkan status dan relasi ke current-state. |
| **Historical report** | Catatan fase, keputusan, hasil refactoring, audit masa lalu | Arsipkan secara terstruktur; jangan dijadikan sumber status terkini. |
| **Evidence** | Benchmark, test result, coverage, quality result | Simpan bersama tanggal, environment, command, dan artefak hasil. |
| **Tooling** | Rule `.continue` dan artefak internal generator | Pisahkan dari docs publik/engineering. |

---

## 4. Analisis Kualitas Isi

### 4.1 Kekuatan yang perlu dipertahankan

- README sudah mencakup quick start, tech stack, endpoint ringkas, konfigurasi, deployment, dan testing.
- `docs/api-index.md`, `docs/architecture.md`, dan `docs/deployment-guide.md` mempunyai format current-state yang lebih cocok untuk pembaca dibanding sebagian besar task report.
- Dokumen `docs/history/optimalisasi-v1/ANALISIS-PROYEK-OMS-V1.md` sudah memakai status verifikasi eksplisit: terverifikasi, sebagian terverifikasi, belum terverifikasi, dan tidak akurat/berlebihan. Pola ini layak dijadikan standar.
- Dokumen standar development cukup komprehensif dan telah memiliki versi, tanggal, status, TOC, statistik, serta riwayat revisi.
- Laporan fase menyimpan konteks keputusan, perubahan, hasil test, dan quality gate yang berguna untuk audit.
- Dokumen API dan arsitektur memakai contoh request/response serta flow diagram yang dapat dijadikan basis dokumentasi operasional.

### 4.2 Masalah isi dan rekomendasi spesifik

| Area | Bukti screening | Rekomendasi |
|---|---|---|
| Source of truth | V0 memiliki banyak referensi tekstual historis; V1 juga menjadi baseline audit tersendiri | Pertahankan V0/V1 sebagai historical, dan gunakan `docs/index.md` serta dokumen pada `docs/current/` sebagai source of truth current-state. |
| Version drift | `README.md` menyebut Spring Boot 3.3, sedangkan beberapa analisis/snapshot menyebut Spring Boot 3.2.5 | Verifikasi terhadap `pom.xml`; gunakan satu metadata versi yang dihasilkan/ditinjau berkala. Jangan mengedit klaim sebelum source of truth dipastikan. |
| Test count drift | Snapshot menyebut 230, 234, 255, dan 280 test pada konteks berbeda | Setiap angka harus memiliki tanggal, command, profile, dan status skipped. Current-state hanya boleh menyimpan hasil verifikasi terbaru. |
| Historical status | `41-refactoring-vs-analysis.md` dan `42-final-execution-report.md` menyatakan status parsial, sementara laporan setelahnya mengklaim penyelesaian yang lebih tinggi | Tambahkan banner `Historical Snapshot` dan pindahkan ke archive. Link dari current-state hanya sebagai histori, bukan status aktif. |
| API contract | `api-index.md` mendokumentasikan endpoint dan response, tetapi belum menjadi indeks ke OpenAPI/Postman dan tidak mencantumkan semua detail auth/role/limit secara konsisten | Jadikan OpenAPI sebagai contract utama; API index menjadi overview; Postman menjadi testing artifact yang direferensikan. Tambahkan status endpoint dan contoh error per kelompok endpoint. |
| Metrics endpoint | `api-index.md` dan `deployment-guide.md` menyebut `/actuator/metrics` sebagai Prometheus metrics, sedangkan analisis menyebut `/actuator/prometheus` | Verifikasi endpoint aktual pada konfigurasi Actuator. Dokumentasikan `/actuator/metrics` dan `/actuator/prometheus` secara terpisah bila keduanya tersedia. |
| Rate limiting | `docs/66-oms-039-rate-limiting.md` menyebut Redis/Bucket4j; analisis V1 menyatakan implementasi rate limit saat ini in-memory per pod | Selaraskan judul, teknologi, scope distribusi, limit, header response, dan multi-pod limitation berdasarkan implementasi aktual. |
| Retry/concurrency | Analisis V1 menyatakan retry dan optimistic locking belum terbukti end-to-end; dokumen lain dapat terdengar lebih final | Pisahkan `implemented`, `verified`, dan `design intent`; sertakan test concurrency yang dapat direproduksi. |
| Benchmark | `benchmark_results.md` menyimpan hasil wrk dengan H2, Redis/rate limiting disabled, error order 64.6%, dan P95/P99 estimated | Labeli sebagai baseline non-production; simpan script, commit, seed data, command, warm-up, dan alasan error. Jangan menjadikan angka tersebut SLO produksi. |
| Struktur proyek | `docs/37-project-structure-documentation.md` menyebut dirinya hybrid snapshot dan dapat sudah tidak sinkron | Jadikan dokumen historical atau regenerasi dari struktur aktual; current-state architecture harus menjadi satu-satunya ringkasan struktur yang dipakai onboarding. |
| Standards | `docs/02-clean-code-quality-gates.md` dan `STANDAR-DEVELOPMENT-CODE.md` beririsan | Jadikan dokumen standar panjang sebagai policy; dokumen quality gate menjadi quick reference/checklist yang menaut ke policy. |
| Bahasa dan istilah | Bahasa Indonesia dan Inggris bercampur; istilah `Task`, `OMS-NNN`, `Final Report`, dan `Laporan` tidak seragam | Pilih bahasa utama (disarankan Indonesia untuk narrative, English untuk nama teknis), lalu tetapkan format judul dan istilah. |
| Heading/TOC | Sebagian file mempunyai multiple H1 dan TOC manual; ada code sample yang terlihat seperti heading | Terapkan satu H1 per file, heading bertingkat, TOC otomatis/terverifikasi, dan fenced code block yang benar. |
| Metadata | Tidak semua file memiliki tanggal, status, audience, owner, atau last verified | Tambahkan front matter atau blok metadata minimal pada dokumen normative dan technical guide. |

---

## 5. Tiga Skema Struktur yang Dipertimbangkan

### Skema A — `docs/` berbasis kategori dengan archive terpisah (**direkomendasikan sebagai target**)

```text
oms/
├── README.md
├── CONTRIBUTING.md
├── CHANGELOG.md
├── benchmark_results.md
├── docs/
│   ├── index.md
│   ├── 00-overview/
│   ├── 01-getting-started/
│   ├── 02-standards/
│   ├── 03-api/
│   ├── 04-features/
│   ├── 05-testing/
│   ├── 06-operations/
│   └── 07-architecture-decisions/
├── docs-archive/
│   ├── reports/
│   └── optimalisasi-v1/
└── docs-generate/
    └── REKOMENDASI-STRUKTUR-DOKUMENTASI.md
```

**Kelebihan:** mudah dinavigasi, membedakan current-state dari histori, kompatibel dengan tooling Markdown umum.  
**Kekurangan:** memerlukan pemetaan path dan pembaruan tautan; perlu disiplin agar archive tidak menjadi tempat dokumen aktif.

### Skema B — Prefix eksplisit `docs-*`

```text
docs-overview/
docs-getting-started/
docs-standards/
docs-api/
docs-features/
docs-testing/
docs-operations/
docs-archive/
```

**Kelebihan:** tujuan setiap folder jelas dari root dan cocok bila banyak domain dokumentasi berdiri sendiri.  
**Kekurangan:** nama path panjang dan fragmentasi lebih besar; `docs-optimalisasi/` menjadi pola yang perlu dimigrasikan.

### Skema C — Pertahankan `docs/`, tambah subfolder secara minimal (**direkomendasikan sebagai tahap transisi**)

```text
docs/
├── index.md
├── current/
│   ├── overview/
│   ├── api/
│   ├── operations/
│   └── development/
└── history/
    ├── reports/
    └── optimalisasi-v1/
```

**Kelebihan:** perubahan path paling kecil dan risiko migrasi lebih rendah.  
**Kekurangan:** nama `current/` menambah satu level dan file lama perlu diberi aturan deprecation agar tidak ada dua sumber aktif.

### Keputusan yang disarankan

Gunakan **Skema C selama migrasi**, lalu menuju **Skema A** setelah tautan dan current-state content stabil. Jangan langsung memindahkan 89 file sekaligus. Prioritaskan indeks, source of truth, dan repair link sebelum rename/move.

---

## 6. Struktur Target yang Direkomendasikan

### 6.1 Prinsip penataan

1. Folder menunjukkan **tujuan pembaca**, bukan urutan pengerjaan.
2. Satu dokumen hanya memiliki satu lokasi canonical.
3. Dokumen current-state tidak boleh bergantung pada laporan fase sebagai satu-satunya sumber fakta.
4. Histori tetap dipertahankan, tetapi diberi label tanggal/status dan tidak masuk ke quick-start navigation.
5. Nama file baru menggunakan `kebab-case` deskriptif; nomor historis dipertahankan selama masa transisi untuk menjaga audit trail.
6. Semua dokumen yang dapat berubah wajib mencantumkan `Status`, `Owner`, `Last verified`, dan `Source of truth`.
7. API contract, configuration reference, dan operational runbook harus dapat diverifikasi dengan command atau artefak.

### 6.2 Struktur target

```text
docs/
├── index.md                         # Satu pintu masuk seluruh dokumentasi
├── 00-overview/
│   ├── project-overview.md          # Ringkasan dari README, bukan duplikasi penuh
│   ├── architecture.md              # Current-state architecture
│   └── project-structure.md         # Struktur aktual yang diverifikasi
├── 01-getting-started/
│   ├── initialize-project.md
│   ├── configuration-profiles.md
│   ├── gitignore.md
│   └── flyway-migration.md
├── 02-standards/
│   ├── development-code-standard.md
│   └── clean-code-quality-gates.md
├── 03-api/
│   ├── api-index.md
│   ├── openapi.md
│   ├── api-versioning.md
│   └── postman-collection.md
├── 04-features/
│   ├── domain-model/
│   ├── authentication-security/
│   ├── order-product/
│   └── cross-cutting/
├── 05-testing/
│   ├── strategy.md
│   ├── integration-testing.md
│   └── test-case-guides/
├── 06-operations/
│   ├── deployment-guide.md
│   ├── docker.md
│   ├── kubernetes-minikube.md
│   ├── rate-limiting.md
│   ├── observability.md
│   └── performance-benchmark.md
└── 07-architecture-decisions/
    ├── decisions-index.md
    └── individual-decisions/

docs-archive/
├── analysis/
├── phase-reports/
└── optimalisasi-v1/
```

`docs-optimalisasi/` sebaiknya menjadi kompatibilitas sementara. Setelah semua referensi diperbaiki, isinya dapat dipindahkan ke `docs-archive/optimalisasi-v1/` tanpa menghapus histori.

---

## 7. Mapping Lengkap File ke Struktur Target

Mapping berikut adalah **rencana lokasi**, bukan perubahan yang sudah dilakukan. Pada tahap pertama, nama file dapat dipertahankan; rename hanya dilakukan setelah link checker lulus.

### 7.1 Root dan governance

| File saat ini | Target utama | Perlakuan |
|---|---|---|
| `README.md` | Root | Tetap; tambahkan link ke `docs/index.md`. |
| `CONTRIBUTING.md` | Root | Tetap; tambahkan aturan perubahan dokumentasi. |
| `CHANGELOG.md` | Root | Tetap; ubah path teks menjadi link Markdown saat target stabil. |
| `benchmark_results.md` | `docs/06-operations/performance-benchmark.md` atau evidence archive | Ringkas di current-state, simpan raw evidence/versi terperinci. |
| `.continue/rules/new-rule.md` | `.continue/rules/` | Tetap di tooling; jangan dimasukkan ke docs index. |
| `docs-generate/REKOMENDASI-STRUKTUR-DOKUMENTASI.md` | `docs-generate/` | Tetap sebagai governance artifact. |

### 7.2 Overview, getting started, standards, API, dan testing

| File saat ini | Target utama |
|---|---|
| `docs/history/optimalisasi-v0/ANALISIS-PROYEK-OMS-V0.md` | `docs/history/optimalisasi-v0/` | Historical V0; tidak digunakan sebagai current-state analysis canonical. |
| `docs/architecture.md` | `docs/00-overview/architecture.md` |
| `docs/37-project-structure-documentation.md` | `docs/00-overview/project-structure.md` sebagai snapshot yang direvisi; versi lama ke archive |
| `docs/01-inisialisasi-project.md` | `docs/01-getting-started/initialize-project.md` |
| `docs/03-multi-profile-configuration.md` | `docs/01-getting-started/configuration-profiles.md` |
| `docs/04-gitignore.md` | `docs/01-getting-started/gitignore.md` |
| `docs/09-flyway-migration.md` | `docs/01-getting-started/flyway-migration.md` |
| `docs/02-clean-code-quality-gates.md` | `docs/02-standards/clean-code-quality-gates.md` |
| `docs-optimalisasi/STANDAR-DEVELOPMENT-CODE.md` | `docs/02-standards/development-code-standard.md` |
| `docs/api-index.md` | `docs/03-api/api-index.md` |
| `docs/06-swagger-openapi.md` | `docs/03-api/openapi.md` sebagai how-to awal |
| `docs/64-oms-037-swagger-openapi.md` | `docs/03-api/openapi.md` sebagai implementation detail/current update |
| `docs/62-oms-034-postman-collection.md` | `docs/03-api/postman-collection.md` |
| `docs/31-api-versioning.md` | `docs/03-api/api-versioning.md` |
| `docs/32-testcontainers-integration.md` | `docs/05-testing/integration-testing.md` |
| `docs/33-unit-test-product-service.md` | `docs/05-testing/test-case-guides/product-service-unit-test.md` |
| `docs/34-product-controller-test.md` | `docs/05-testing/test-case-guides/product-controller-test.md` |
| `docs/35-repository-test-product.md` | `docs/05-testing/test-case-guides/product-repository-test.md` |
| `docs/38-oms-033-order-flow-integration-test.md` | `docs/05-testing/test-case-guides/order-flow-integration-test.md` |

### 7.3 Feature, security, data, performance, dan operations

| File saat ini | Target utama |
|---|---|
| `docs/05-entity-user-autentikasi.md` | `docs/04-features/domain-model/user-authentication.md` |
| `docs/07-entity-product.md` | `docs/04-features/domain-model/product.md` |
| `docs/08-entity-order.md` | `docs/04-features/domain-model/order.md` |
| `docs/10-product-service.md` | `docs/04-features/order-product/product-service.md` |
| `docs/11-order-service.md` | `docs/04-features/order-product/order-service.md` |
| `docs/12-product-controller.md` | `docs/04-features/order-product/product-controller.md` |
| `docs/13-jwt-util.md` | `docs/04-features/authentication-security/jwt-token.md` |
| `docs/14-auth-controller.md` | `docs/04-features/authentication-security/auth-controller.md` |
| `docs/15-jwt-authentication-filter.md` | `docs/04-features/authentication-security/jwt-filter.md` |
| `docs/16-security-config.md` | `docs/04-features/authentication-security/security-config.md` |
| `docs/17-refresh-token-redis.md` | `docs/04-features/authentication-security/refresh-token.md` |
| `docs/18-global-exception-handler.md` | `docs/04-features/cross-cutting/exception-handling.md` |
| `docs/19-global-exception-handler-rfc7807.md` | `docs/04-features/cross-cutting/problem-detail-rfc7807.md` |
| `docs/20-querydsl-product-filter.md` | `docs/04-features/order-product/querydsl-product-filter.md` |
| `docs/21-fix-n-plus-one-order-items.md` | `docs/04-features/order-product/order-query-performance.md` |
| `docs/22-optimistic-locking-stock.md` | `docs/04-features/order-product/optimistic-locking-stock.md` |
| `docs/23-soft-delete-all-entities.md` | `docs/04-features/cross-cutting/soft-delete.md` |
| `docs/24-jpa-auditing.md` | `docs/04-features/cross-cutting/jpa-auditing.md` |
| `docs/25-redis-cache-config.md` | `docs/04-features/cross-cutting/redis-cache.md` |
| `docs/26-product-caching.md` | `docs/04-features/order-product/product-caching.md` |
| `docs/27-async-email-notification.md` | `docs/04-features/cross-cutting/async-email-notification.md` |
| `docs/28-hikaricp-connection-pool-tuning.md` | `docs/06-operations/database-connection-pool.md` |
| `docs/29-custom-health-indicator.md` | `docs/06-operations/health-indicators.md` |
| `docs/30-structured-logging-mdc.md` | `docs/06-operations/observability.md` |
| `docs/deployment-guide.md` | `docs/06-operations/deployment-guide.md` |
| `docs/63-oms-036-dockerize.md` | `docs/06-operations/docker.md` |
| `docs/65-oms-038-minikube.md` | `docs/06-operations/kubernetes-minikube.md` |
| `docs/66-oms-039-rate-limiting.md` | `docs/06-operations/rate-limiting.md` |
| `docs/67-oms-040-benchmark.md` | `docs/06-operations/performance-benchmark.md` |

### 7.4 Analysis, refactoring, dan seluruh laporan historis

| File saat ini | Target utama | Status yang disarankan |
|---|---|---|
| `docs/36-project-analysis-report.md` | `docs-archive/analysis/project-analysis-report.md` | Historical; canonical facts dipindahkan/ringkas ke overview. |
| `docs/39-errorcode-refactoring-and-null-safety.md` | `docs-archive/analysis/errorcode-null-safety.md` | Implementation report. |
| `docs/40-refactoring-summary.md` | `docs-archive/analysis/refactoring-summary.md` | Historical summary. |
| `docs/41-refactoring-vs-analysis.md` | `docs-archive/analysis/refactoring-vs-analysis.md` | Historical snapshot, status lama harus tetap diberi tanggal. |
| `docs/history/optimalisasi-v0/42-final-execution-report.md` dan `43-laporan-perbaikan-implementasi.md` | `docs-archive/optimalisasi-v0/` pada target Skema A | Historical V0; dipertahankan bersama baseline V0 selama Skema C. |
| `docs/history/optimalisasi-v0/45-final-report.md` sampai `61-final-report-phase14.md` | `docs-archive/optimalisasi-v0/` pada target Skema A | Historical V0; pertahankan nomor fase dan tanggal. `53`–`54` adalah analisis/laporan pendukung dalam rangkaian V0, bukan fase murni. |
| `docs/history/optimalisasi-v1/ANALISIS-PROYEK-OMS-V1.md` | `docs/history/optimalisasi-v1/` | Baseline/audit versioned; bukan current-state tanpa verifikasi ulang. |
| `docs/history/optimalisasi-v1/01-final-report-phase1.md` sampai `11-final-report-phase11.md` | `docs/history/optimalisasi-v1/` | Historical v1; urutan fase dipertahankan dan path sudah dinormalisasi. |

---

## 8. Referensi Internal dan Inkonsistensi yang Perlu Diperbaiki

### 8.1 Tautan Markdown yang terdeteksi rusak

| Sumber | Target | Masalah | Perbaikan yang direkomendasikan |
|---|---|---|---|
| `README.md` | `LICENSE` | File `LICENSE` tidak ditemukan pada filesystem saat screening. | Putuskan apakah license file harus ditambahkan atau link dihapus; jangan biarkan link placeholder. |
| `docs/35-repository-test-product.md` | `../07-entity-product.md` | Dari folder `docs/`, path mengarah ke root dan tidak menemukan file. | Jika tetap di `docs/`, gunakan `07-entity-product.md`; setelah migrasi gunakan link canonical. |
| `docs/35-repository-test-product.md` | `../20-querydsl-product-filter.md` | Masalah relatif yang sama. | Ganti ke path canonical. |
| `docs/35-repository-test-product.md` | `../22-optimistic-locking-stock.md` | Masalah relatif yang sama. | Ganti ke path canonical. |
| `docs/35-repository-test-product.md` | `../23-soft-delete-all-entities.md` | Masalah relatif yang sama. | Ganti ke path canonical. |
| `docs/35-repository-test-product.md` | `../24-jpa-auditing.md` | Masalah relatif yang sama. | Ganti ke path canonical. |
| `docs/35-repository-test-product.md` | `../repository/OrderRepositoryTest.md` | Target tidak tersedia pada repository. | Cari nama test aktual; bila tidak ada, hapus link atau tandai sebagai artefak yang belum tersedia. |
| `docs/36-project-analysis-report.md` | `../repository/ProductRepositoryTest.md` | Target tidak tersedia. | Tautkan ke file test aktual atau hapus referensi. |
| `docs/36-project-analysis-report.md` | `../02-clean-code-quality-gates.md` | Masalah relatif dari direktori `docs/`. | Gunakan `02-clean-code-quality-gates.md` sebelum migrasi atau target canonical setelah migrasi. |
| `docs/36-project-analysis-report.md` | `../32-testcontainers-integration.md` | Masalah relatif dari direktori `docs/`. | Gunakan path canonical. |

### 8.2 Referensi path yang tidak ditemukan atau bersifat placeholder

Sebelum reorganisasi, lakukan keputusan eksplisit untuk setiap item berikut:

- `docs/44-laporan-perbaikan-implementasi.md` — dirujuk beberapa laporan, tetapi file 44 tidak ada; jangan membuat file baru hanya untuk menutupi link tanpa mengetahui isi yang dimaksud.
- `docs/history/optimalisasi-v0/43-analisis-struktur-dan-flow-bisnis.md` — dirujuk dari `docs/history/optimalisasi-v0/43-laporan-perbaikan-implementasi.md`, tetapi tidak ada.
- Path V1 lama dengan variasi kapitalisasi `V1`/`v1` — sudah dinormalisasi ke `docs/history/optimalisasi-v1/ANALISIS-PROYEK-OMS-V1.md`.
- Path V1 yang sebelumnya dirujuk seolah berada di dalam folder laporan — sudah dinormalisasi ke `docs/history/optimalisasi-v1/`.
- `docs-optimalisasi/v157-final-report-phase10.md`, `v159-final-report-phase12.md`, `v160-final-report-phase13.md`, `v161-final-report-phase14.md` — path tampak merupakan hasil concatenation/typo; target historis yang tersedia berada di `docs/history/optimalisasi-v0/`.
- `docs/history/optimalisasi-v1/NN-final-report-phaseX.md` dan `docs/NN-final-report-phaseX.md` — placeholder dokumentasi yang terdeteksi sebagai path; ubah menjadi inline code generik atau hapus dari daftar link/path.

### 8.3 Referensi yang sebaiknya diubah menjadi link navigable

Referensi teks berikut sering menjadi hub tetapi saat ini tidak semuanya merupakan tautan Markdown:

- `docs/history/optimalisasi-v0/ANALISIS-PROYEK-OMS-V0.md`;
- `docs/history/optimalisasi-v1/ANALISIS-PROYEK-OMS-V1.md`;
- laporan fase 45–61;
- `docs/api-index.md`, `docs/architecture.md`, dan `docs/deployment-guide.md`.

Setelah target final diputuskan, tambahkan link relatif canonical dan jalankan link checker pada CI. Jangan mengandalkan path teks biasa karena tidak tervalidasi oleh renderer atau tooling.

---

## 9. Rekomendasi Penyesuaian Isi

### 9.1 Metadata minimum per dokumen

Untuk dokumen current-state dan guide, gunakan format seragam seperti berikut:

```markdown
> **Status:** Current | Historical | Draft | Deprecated
> **Audience:** Developer | Operator | API Consumer | Maintainer
> **Owner:** Engineering / nama tim
> **Last verified:** YYYY-MM-DD
> **Source of truth:** source file, `pom.xml`, manifest, OpenAPI, atau command tertentu
```

Untuk laporan historis, tambahkan juga:

```markdown
> **Snapshot date:** YYYY-MM-DD
> **Scope:** fase/commit/environment
> **Superseded by:** link ke current-state atau laporan pengganti
```

### 9.2 Template isi guide teknis

Setiap guide sebaiknya mengikuti urutan:

1. Tujuan dan audience;
2. prasyarat;
3. konsep/arsitektur singkat;
4. konfigurasi atau langkah implementasi;
5. contoh command/request/response;
6. verifikasi hasil;
7. troubleshooting;
8. security/operational considerations;
9. referensi canonical;
10. metadata status dan tanggal verifikasi.

### 9.3 Template isi laporan fase

Laporan fase sebaiknya memiliki:

- phase ID, tanggal, commit atau baseline;
- tujuan dan scope;
- issue/requirement yang ditangani;
- file/perubahan yang relevan;
- hasil verifikasi dengan command lengkap;
- test count, skipped, environment, dan limitation;
- risiko/remaining issue;
- link ke dokumen current-state yang diperbarui;
- label `Historical` setelah fase selesai.

### 9.4 Penyesuaian isi per dokumen penting

| Dokumen | Penyesuaian prioritas |
|---|---|
| `README.md` | Tambahkan `Documentation Index`, link ke API/architecture/deployment/testing/standards, serta bedakan quick start dari detail teknis. Verifikasi badge, license link, versi Spring Boot, endpoint, dan environment variables. |
| `docs/api-index.md` | Jadikan overview API; referensikan OpenAPI/Swagger dan Postman. Verifikasi response register/login, role/authorization, pagination, CSRF, rate limit, actuator, error schema, dan endpoint metrics. |
| `docs/architecture.md` | Tambahkan dependency/source reference, diagram boundary, data stores, trust boundary, failure modes, deployment assumptions, serta tanggal verifikasi. |
| `docs/deployment-guide.md` | Pisahkan local, Docker, Minikube, dan production. Tambahkan secret handling, rollback, readiness/liveness, resource requirements, persistence, backup, observability, dan troubleshooting yang dapat dijalankan. |
| `docs/history/optimalisasi-v0/ANALISIS-PROYEK-OMS-V0.md` | Pertahankan sebagai baseline/audit V0 dengan banner historical; jangan gunakan sebagai current-state. |
| `docs/history/optimalisasi-v1/ANALISIS-PROYEK-OMS-V1.md` | Pertahankan sebagai baseline/audit V1; koreksi klaim status atau rujukan fase hanya melalui audit historis yang terpisah. |
| `docs/37-project-structure-documentation.md` | Jangan biarkan status hybrid tanpa owner. Pilih antara regenerasi menjadi current-state atau arsipkan penuh. |
| `docs/history/reports/41-refactoring-vs-analysis.md` dan `docs/history/optimalisasi-v0/42-final-execution-report.md` | Pertahankan angka lama sebagai snapshot audit, tetapi tambahkan banner superseded dan link ke status terbaru. |
| `docs/66-oms-039-rate-limiting.md` | Selaraskan judul dan implementasi aktual: Redis/Bucket4j vs in-memory per pod, limit, trusted proxy, header, dan multi-pod limitation. |
| `benchmark_results.md` dan `docs/67-oms-040-benchmark.md` | Tentukan satu canonical benchmark report; bedakan procedure, raw result, interpretation, dan recommendation. Tandai estimated percentile sebagai estimasi. |
| `docs/02-clean-code-quality-gates.md` dan `STANDAR-DEVELOPMENT-CODE.md` | Hapus duplikasi normatif; buat quick checklist yang menaut ke standar lengkap. |
| seluruh task `01`–`18` | Tambahkan status `implemented/verified/historical`, tanggal, dan link ke file source/test; pisahkan acceptance criteria dari hasil aktual. |
| seluruh laporan fase | Normalisasi metadata, status, test evidence, dan link. Jangan menulis placeholder path yang menyerupai file nyata. |

### 9.5 Aturan gaya dokumentasi

- Satu H1 per file; gunakan H2 untuk section utama dan H3 untuk subsection.
- Gunakan bahasa Indonesia sebagai bahasa narasi utama, dengan istilah teknis/API tetap dalam English bila itu nama resmi.
- Hindari emoji pada judul canonical; emoji boleh dipertahankan di laporan historis bila dibutuhkan audit trail.
- Gunakan `kebab-case` untuk file baru; jangan menambah nomor urut global.
- Tanggal gunakan ISO 8601 (`YYYY-MM-DD`) dan selalu sertakan timezone bila hasil benchmark/runtime penting.
- Semua command harus menyebut working directory, profile, dependency/service yang diperlukan, dan expected result.
- Jangan menulis secret, token nyata, password, atau nilai environment sensitif dalam contoh.
- Pisahkan fakta terverifikasi, asumsi, rekomendasi, dan future enhancement dengan label yang jelas.

---

## 10. Rencana Eksekusi Bertahap

### Fase 0 — Freeze dan baseline

1. Simpan hasil screening ini sebagai baseline.
2. Pastikan tidak ada perubahan tidak terkait pada working tree.
3. Tetapkan owner dokumentasi dan keputusan current-state source of truth.

### Fase 1 — Navigasi tanpa memindahkan file

1. Buat `docs/index.md`.
2. Tautkan index dari README.
3. Kelompokkan link berdasarkan audience: user/API consumer, developer, operator, maintainer, historical.
4. Tambahkan label status pada dokumen current-state yang sudah diverifikasi.

### Fase 2 — Repair referensi

1. Perbaiki 10 broken Markdown links yang terdeteksi.
2. Putuskan status file 44, file repository test yang tidak ada, dan file license.
3. ✅ Normalisasi kapitalisasi `ANALISIS-PROYEK-OMS-V1` dan path `v1` ke `docs/history/optimalisasi-v1/`.
4. Ubah placeholder `NN-final-report-phaseX` menjadi code span atau template yang tidak dianggap path.
5. Jalankan link checker setelah setiap kelompok perbaikan.

### Fase 3 — Konsolidasi current-state

1. Tetapkan versi framework, jumlah test, status issues, endpoint API, rate limiting, dan metrics dari source of truth.
2. Jadikan architecture, API index, deployment guide, standards quick reference, dan project status sebagai dokumen aktif.
3. Tandai dokumen yang hanya snapshot dengan `Historical` dan `Superseded by`.

### Fase 4 — Migrasi struktur

1. Terapkan Skema C terlebih dahulu agar path transition tidak terlalu besar.
2. Pindahkan laporan fase ke `docs-archive/` setelah semua referensi canonical siap.
3. Gabungkan `docs/history/optimalisasi-v1/` ke archive versioned bila tidak lagi menjadi working area aktif.
4. Rename file bernomor hanya bila nilai audit trail tidak hilang dan seluruh inbound link sudah dipetakan.

### Fase 5 — Quality automation

Tambahkan pemeriksaan di CI untuk:

- broken relative links;
- duplicate/multiple H1 pada dokumen current-state;
- heading hierarchy yang melompat;
- file tanpa metadata wajib;
- path referensi yang tidak ada;
- Markdown lint dan format tabel;
- pemeriksaan secret/token pada code block;
- freshness review berdasarkan `Last verified`.

### Prioritas

| Prioritas | Item | Alasan |
|:---:|---|---|
| P0 | Tetapkan source of truth dan repair broken links | Mencegah pembaca mengikuti instruksi yang tidak dapat dinavigasi atau status yang salah. |
| P1 | Buat `docs/index.md`, current-state matrix, dan archive boundary | Dampak terbesar pada discoverability tanpa perubahan isi besar. |
| P1 | Selaraskan API, deployment, rate limiting, metrics, dan test evidence | Area yang berpengaruh langsung pada penggunaan dan operasi sistem. |
| P2 | Normalisasi metadata, heading, bahasa, dan nama file | Meningkatkan konsistensi dan kemampuan automation. |
| P3 | Rename/move seluruh file serta portal/tooling dokumentasi | Dilakukan setelah struktur dan link stabil. |

---

## 11. Checklist Validasi

### Inventaris dan struktur

- [ ] `find . -name '*.md'` menghasilkan jumlah yang diketahui dan dijelaskan.
- [ ] Semua file Markdown memiliki satu klasifikasi utama.
- [ ] `docs/index.md` menjadi entry point dan ditautkan dari README.
- [ ] Current-state dan historical report berada pada boundary yang jelas.
- [ ] `docs-optimalisasi/` memiliki status transisi atau sudah dipindahkan secara terencana.
- [ ] `.continue/rules/` tidak tercampur dalam indeks dokumentasi produk.

### Referensi

- [ ] Tidak ada broken Markdown link.
- [ ] Tidak ada referensi ke `docs/44-...` tanpa keputusan eksplisit.
- [ ] Tidak ada referensi ke file test/repository yang tidak ada.
- [ ] Variasi kapitalisasi `V1`/`v1` konsisten.
- [ ] Placeholder `NN-final-report-phaseX` tidak diperlakukan sebagai path aktual.
- [ ] Semua link inbound ke file yang dipindah telah diperbarui.

### Isi dan akurasi

- [ ] Versi framework sesuai `pom.xml` dan source of truth yang disepakati.
- [ ] Jumlah test, skipped test, quality gate, dan tanggal hasil dapat direproduksi.
- [ ] Endpoint API, auth/role, error schema, rate limit, dan metrics diverifikasi.
- [ ] Benchmark mencantumkan environment, command, seed data, commit, dan limitations.
- [ ] Dokumen historical memiliki snapshot date dan link ke current-state.
- [ ] Secret/token nyata tidak tercantum.

### Format dan maintenance

- [ ] Dokumen current-state memiliki status, audience, owner, last verified, dan source of truth.
- [ ] Satu H1 per file dan heading hierarchy valid.
- [ ] TOC cocok dengan heading aktual.
- [ ] Bahasa, istilah, tanggal, code fence, tabel, dan nama file konsisten.
- [ ] CI menjalankan link check dan Markdown lint.
- [ ] Ada proses review freshness dan owner untuk setiap kelompok dokumen.

---

## Rekomendasi Akhir

Jangan melakukan bulk move atau rename sebagai langkah pertama. Urutan yang paling aman adalah:

1. tetapkan current-state source of truth;
2. buat indeks dokumentasi;
3. perbaiki referensi rusak dan status yang kontradiktif;
4. normalkan metadata dan struktur isi;
5. arsipkan laporan historis;
6. baru terapkan struktur kategori target.

Dengan pendekatan tersebut, histori 88 file tracked tetap dapat diaudit, sementara pembaca baru memperoleh jalur dokumentasi yang singkat, dapat ditemukan, dan lebih dapat dipercaya.

> **Catatan penting:** Screening ini tidak mengubah isi file dokumentasi lain. Rekomendasi di atas harus dieksekusi melalui perubahan terpisah, dengan validasi link dan review teknis setelah setiap fase.