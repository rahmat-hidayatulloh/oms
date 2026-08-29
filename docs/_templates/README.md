# Documentation Templates

> **Status:** Current
> **Audience:** Developer, Maintainer
> **Owner:** Engineering
> **Last verified:** 2026-08-23 (screening struktur dokumentasi aktual)
> **Source of truth:** Konvensi governance di `CONTRIBUTING.md` dan alur di `docs/optimization-workflow.md`

Folder ini berisi template kanonik untuk seluruh dokumentasi OMS. Salin
template yang sesuai saat membuat dokumen baru agar struktur, metadata, dan gaya
tetap konsisten lintas versi optimalisasi.

## Daftar template

| Template | Gunakan untuk | Lokasi target dokumen |
|---|---|---|
| [`current-doc-template.md`](current-doc-template.md) | Dokumentasi current-state fitur, API, atau operasi | `docs/current/**` |
| [`phase-report-template.md`](phase-report-template.md) | Laporan hasil satu fase optimalisasi | `docs/history/optimalisasi-vN/` |
| [`adr-template.md`](adr-template.md) | Architecture Decision Record | `docs/adr/` |
| [`analysis-baseline-template.md`](analysis-baseline-template.md) | Analisis baseline satu versi optimalisasi | `docs/history/optimalisasi-vN/ANALISIS-PROYEK-OMS-VN.md` |

## Aturan pemakaian

1. Blok metadata (5 baris: `Status`, `Audience`, `Owner`, `Last verified`,
   `Source of truth`) wajib pada semua dokumen di `docs/current/**` dan
   `docs/ops/**`. Validasi otomatis dijalankan CI (`docs-quality.yml`).
2. Dokumen di `docs/current/**` mendeskripsikan **kondisi sistem saat ini** —
   tanpa `Acceptance Criteria` berbentuk checkbox, tanpa angka test/`BUILD SUCCESS`,
   dan tanpa narasi proses. Semua narasi proses/verifikasi masuk ke laporan fase
   di `docs/history/`.
3. Source of truth selalu kode aktual (`src/`), `pom.xml`, dan konfigurasi
   runtime (`application*.yml`). Klaim yang tidak dapat diverifikasi terhadap kode
   harus ditandai eksplisit.
4. Satu topik = satu dokumen kanonik. Dokumen lama yang digantikan dipindahkan ke
   `docs/history/` dengan banner `> **Superseded by:** <path>`.
5. Template `analysis-baseline-template.md` mengabadikan struktur "flow map" yang
   selama ini ditulis ulang manual di tiap `ANALISIS-PROYEK-OMS-V<N>.md` (matriks
   13 pilar standar, matriks SOLID, bukti praktik baik, listing phase + matriks
   dependensi, decision gate, quality gate). Ikuti section wajibnya agar analisis
   antar-versi tetap sebanding.

Lihat siklus kerja lengkap di [`docs/optimization-workflow.md`](../optimization-workflow.md).
