# Contributing to OMS

Thank you for considering contributing! This document outlines the process.

## Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/your-username/oms.git`
3. Create a feature branch: `git checkout -b feat/my-feature`

## Development Workflow

1. Make your changes
2. Write/update tests
3. Run tests: `./mvnw test -Dspring.profiles.active=dev`
4. Run quality checks: `./mvnw checkstyle:check spotbugs:check`
5. Commit with conventional commit message
6. Push and open a Pull Request

## Code Style

- Java 21 features encouraged (records, sealed classes, pattern matching)
- Follow existing code patterns
- Maximum method length: 50 lines (enforced by Checkstyle)
- Use Lombok for boilerplate reduction
- Write Javadoc for public APIs

## Commit Convention

We follow [Conventional Commits](https://www.conventionalcommits.org/):

- `feat:` - New feature
- `fix:` - Bug fix
- `refactor:` - Code refactoring
- `test:` - Adding/updating tests
- `docs:` - Documentation
- `chore:` - Maintenance tasks
- `perf:` - Performance improvements
- `security:` - Security fixes

## Pull Request Process

1. Ensure all tests pass
2. Ensure Checkstyle and SpotBugs pass
3. Update documentation if needed
4. Add CHANGELOG entry
5. Request review from maintainers

## Testing Requirements

- Unit tests for all new services and controllers
- Integration tests for API endpoints
- Minimum 80% coverage for new code
- Test edge cases and error scenarios

## Documentation Governance

Dokumentasi berada di `docs/` dengan pembagian area yang tegas:

| Area | Isi | Status |
|---|---|---|
| `docs/current/` | Dokumentasi current-state (kondisi aktual saat ini) | `Current` |
| `docs/history/` | Laporan fase, audit, dan snapshot per versi (V0–V4) | `Historical` / `Superseded` |
| `docs/adr/` | Architecture Decision Records | `Accepted` / `Superseded` |
| `docs/ops/` | Runbook operasional (SLO, DRP, Incident Response) | `Current` |
| `docs/index.md` | Entry point navigasi; wajib mencakup semua area top-level | `Current` |

Aturan:

1. **Metadata wajib** — setiap file di `docs/current/` dan `docs/ops/` diawali blok:
   ```
   > **Status:** Current
   > **Audience:** ...
   > **Owner:** Engineering
   > **Last verified:** YYYY-MM-DD (ruang lingkup verifikasi)
   > **Source of truth:** ...
   ```
2. **Source of truth** — nilai konfigurasi, path endpoint, dan angka test tidak di-hardcode ganda. Rujuk `src/`, `pom.xml`, dan `application*.yml`. Angka test snapshot hanya boleh di `docs/history/`.
3. **Status taksonomi** — `Current` (canonical), `Superseded` (ditandai `Superseded by:` + tautan pengganti, disimpan di `docs/history/`), `Historical`/snapshot (read-only, audit trail).
4. **Satu topik satu canonical** — jangan simpan dua dokumen topik sama di `docs/current/`. Versi lama dipindah ke `docs/history/` dengan header `Superseded by`.
5. **Index sebagai kontrak** — `docs/index.md` harus menautkan seluruh area top-level (current, history, adr, ops). Tambahkan tautan saat menambah dokumen baru.
6. **Penamaan** — dokumen `current/` sebaiknya berbasis topik; penomoran historis dipertahankan hanya di `docs/history/`. Hindari nomor urut yang bertabrakan.
7. **Verifikasi tautan** — sebelum PR, pastikan tidak ada broken link internal pada dokumen yang diubah.

### Template dan siklus kerja

- Gunakan template kanonik di [`docs/_templates/`](docs/_templates/README.md) saat membuat dokumen baru (current-state, laporan fase, ADR, analisis baseline).
- Ikuti siklus optimalisasi per versi di [`docs/optimization-workflow.md`](docs/optimization-workflow.md). Langkah **SYNC** (perbarui `docs/current/**`, `docs/index.md`, `CHANGELOG.md`) wajib pada setiap fase.

### Penegakan otomatis

- Workflow CI `.github/workflows/docs-quality.yml` menjalankan pemeriksaan tautan internal dan validasi metadata pada setiap PR yang menyentuh dokumentasi.
- Jalankan lokal sebelum push: `python3 scripts/check-docs.py`.



## Questions?

Open an issue for discussion before making significant changes.
