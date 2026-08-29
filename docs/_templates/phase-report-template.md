<!--
TEMPLATE: laporan fase optimalisasi
Salin ke docs/history/optimalisasi-vN/0X-final-report-phase{ID}.md
Penamaan: XX-final-report-phase{ID}.md (XX = urutan dua digit; ID = mis. V5-P0)
Hapus seluruh komentar <!-- ... --> sebelum commit.
-->
# Final Report — Phase <VN-ID>: <Subtitle>

<!-- Tipe fase: pilih salah satu.
       - Implementation  : ada perubahan kode/konfigurasi.
       - Verification (Audit Only) : hanya audit source/config, tidak ada perubahan kode.
     Untuk verification phase, Section 2 memuat source/config yang diperiksa & klaim
     yang diverifikasi, bukan perubahan implementasi. -->

- **Tanggal:** <DD Bulan YYYY>
- **Tipe fase:** Implementation / Verification (Audit Only)
- **Status:** ✅ Selesai / ⚠️ Sebagian / ⏸️ Ditunda / ❌ Gagal
- **Baseline:** <Deskripsi state sebelum phase; commit SHA / URL PR bila ada>
- **Quality gates:** ✅ Checkstyle 0, ✅ SpotBugs 0, ✅ JaCoCo <X>%
- **Tests:** <N> pass, <M> fail

## 1. Ringkasan Eksekutif

| Temuan | Status | Deskripsi |
|---|---|---|
| **<VN-ID>** | ✅ | Deskripsi singkat |

## 2. Perubahan File

### 2.1 Dimodifikasi

| # | File | Temuan | Deskripsi |
|---|---|---|---|
| 1 | `path/to/File.java` | <VN-ID> | Deskripsi perubahan |

### 2.2 Dibuat Baru

| # | File | Temuan | Deskripsi |
|---|---|---|---|
| 1 | `path/to/NewFile.java` | <VN-ID> | Deskripsi |

### 2.3 Test Dibuat Baru

| # | File | Temuan | Jumlah Test |
|---|---|---|---|
| 1 | `path/to/NewTest.java` | <VN-ID> | N test |

## 3. Validasi

### 3.1 Quality Gates

| Gate | Status | Bukti |
|---|---|---|
| Checkstyle | ✅ | 0 violations |
| SpotBugs | ✅ | 0 findings |
| Unit tests | ✅ | N/N pass |

### 3.2 Verifikasi Command

```bash
# Command 1: Deskripsi
grep -n "pattern" path/to/file.java
# Hasil: Lines X, Y
# Validasi: ✅ PASS — penjelasan
```

## 4. Status Temuan

| Kode | Temuan | Status | Bukti |
|---|---|---|---|
| <VN-ID> | Deskripsi temuan | ✅ Selesai | Bukti teknis |

## 5. Exit Criteria

| Kriteria | Target | Aktual | Status |
|---|---|---|:---:|
| Kriteria 1 | Target value | Aktual value | ✅ |

## 6. Dokumentasi current-state yang disinkronkan

<!-- WAJIB: daftar file docs/current/** dan docs/index.md yang diperbarui akibat phase ini. -->

| Dokumen | Perubahan |
|---|---|
| `docs/current/.../<file>.md` | Deskripsi sinkronisasi |

## 7. Catatan Teknis

1. **Judul Catatan** — Penjelasan teknis.

## 8. Rollback & Tindak Lanjut

<!-- Untuk fase yang menyentuh deployment/infra/data. Hapus bila tidak relevan (V2 §10.1 langkah 6). -->

- **Strategi rollback:** <image / config / code / data>
- **Monitoring setelah deploy:** <metric, alert, periode observasi>
- **Tindak lanjut:** <temuan/risiko tersisa yang di-carry ke fase berikutnya>

## 9. Deferred

| Item | Alasan | Rencana |
|---|---|---|
| Item ditunda | Alasan | Phase berikutnya |

---

*Laporan ini disusun pada <date> mengikuti template `docs/_templates/phase-report-template.md`.*
