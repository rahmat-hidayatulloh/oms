<!--
TEMPLATE: current-state documentation
Salin file ini ke docs/current/<kategori>/<nama-topik>.md lalu isi.
Hapus seluruh komentar <!-- ... --> sebelum commit.
Aturan:
  - Deskripsikan BAGAIMANA sistem bekerja SEKARANG.
  - JANGAN pakai "Acceptance Criteria" checkbox, angka test, atau "BUILD SUCCESS".
  - Narasi proses/verifikasi milik laporan fase di docs/history/.
-->
# <Judul Topik — deskriptif, bukan nomor tiket>

> **Status:** Current
> **Audience:** <Developer | API Consumer | Operator | Maintainer>
> **Owner:** Engineering
> **Last verified:** <YYYY-MM-DD> (screening kode aktual `src/`, `pom.xml`)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## Ringkasan

<!-- 2–4 kalimat: apa komponen ini, perannya dalam sistem. -->

## Cara kerja saat ini

<!-- Deskripsi current-state: alur, komponen, dependensi. Diagram/ASCII bila perlu. -->

## Konfigurasi terkait

<!-- Properti application*.yml yang relevan + nilai default aktual. Contoh: -->

| Properti | Profile | Nilai aktual | Keterangan |
|---|---|---|---|
| `contoh.property` | `dev/staging/prod` | `value` | Deskripsi |

## Kontrak / Endpoint

<!-- Hanya jika ini dokumen API. Hapus section bila tidak relevan. -->

```http
<METHOD> /api/v1/<path>
```

## Referensi kode

<!-- Path file sumber yang mengimplementasikan topik ini. -->

- `src/main/java/.../<File>.java`
- `src/main/resources/application.yml`

## Tautan terkait

- [<Dokumen terkait>](<relative-path>.md)
