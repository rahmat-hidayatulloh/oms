# Siklus Kerja Optimalisasi — OMS

> **Status:** Current
> **Audience:** Developer, Maintainer
> **Owner:** Engineering
> **Last verified:** 2026-08-23 (screening struktur dokumentasi aktual)
> **Source of truth:** Konvensi ini + `CONTRIBUTING.md` (Documentation Governance)

Dokumen ini mendefinisikan siklus baku setiap versi optimalisasi (V0, V1, …).
Tujuannya menjaga konsistensi antara kode dan dokumentasi lintas versi, sehingga
`docs/current/` selalu mencerminkan kondisi sistem terkini dan `docs/history/`
menyimpan jejak audit tiap fase.

## Prinsip

1. **Current vs History.** `docs/current/**` = current-state (bagaimana sistem
   bekerja sekarang). `docs/history/**` = snapshot proses/laporan pada tanggalnya.
2. **Kode adalah source of truth.** Setiap klaim teknis harus dapat diverifikasi
   terhadap `src/`, `pom.xml`, dan `application*.yml`.
3. **Satu topik, satu dokumen kanonik.** Dokumen yang digantikan pindah ke
   `history/` dengan banner `> **Superseded by:** <path>`.
4. **Sinkronisasi bukan opsional.** Perubahan kode current-state wajib diikuti
   pembaruan `docs/current/**` pada PR yang sama.

## Alur per versi

```text
1. BASELINE   → docs/history/optimalisasi-vN/ANALISIS-PROYEK-OMS-VN.md
                (screening kode aktual; koreksi klaim versi sebelumnya;
                 matriks 13 pilar standar + SOLID; bukti praktik baik;
                 daftar temuan + severity)
                Template: docs/_templates/analysis-baseline-template.md

2. PLAN       → Section "Listing Phase" + matriks dependensi + decision gate
                + quality gates di file baseline
                (fase, penamaan file laporan, exit criteria, verifikasi)

3. EXECUTE    → per fase: implementasi kode + test

4. REPORT     → docs/history/optimalisasi-vN/0X-final-report-phase{ID}.md
                Template: docs/_templates/phase-report-template.md

5. SYNC       → WAJIB per fase:
                - update docs/current/** yang terdampak
                - update docs/index.md bila ada dokumen ditambah/dipindah
                - update CHANGELOG.md
                - buat/perbarui ADR bila ada keputusan arsitektur
                  (Template: docs/_templates/adr-template.md)

6. VERIFY     → ./mvnw clean verify
                + link-check & validasi metadata (CI docs-quality.yml)

7. CLOSE      → tandai temuan closed di file baseline;
                dokumen lama yang digantikan → history + "Superseded by"
```

Langkah **5 (SYNC)** adalah kontrol paling penting: inilah yang mencegah
`docs/current/` tertinggal dari kode. Checklist SYNC ditegakkan melalui
`.github/PULL_REQUEST_TEMPLATE.md`.

## Penamaan

| Artefak | Pola | Lokasi |
|---|---|---|
| Analisis baseline | `ANALISIS-PROYEK-OMS-V<N>.md` | `docs/history/optimalisasi-v<N>/` |
| Laporan fase | `XX-final-report-phase<ID>.md` (XX = urutan dua digit) | `docs/history/optimalisasi-v<N>/` |
| ADR | `00X-<slug>.md` (nomor berurutan) | `docs/adr/` |
| Dokumen current-state | `<nama-topik>.md` (deskriptif, tanpa nomor tiket untuk file baru) | `docs/current/**` |

## Gerbang otomatis (CI)

Workflow `.github/workflows/docs-quality.yml` menjalankan pada setiap PR yang
menyentuh `docs/**` atau file Markdown root:

- **Link check** — memastikan tidak ada tautan Markdown internal yang rusak.
- **Metadata validation** — memastikan setiap file di `docs/current/**` dan
  `docs/ops/**` memiliki blok metadata 5 baris wajib.

Lihat template dokumentasi di [`_templates/`](_templates/README.md).
