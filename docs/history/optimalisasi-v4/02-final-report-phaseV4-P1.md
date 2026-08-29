# Final Report — Phase V4-P1: Code Quality & Environment Hardening

- **Tanggal:** 24 Agustus 2026
- **Tipe fase:** Implementation
- **Status:** ⚠️ Sebagian — kode P1.1–P1.4 selesai & build hijau; sinkronisasi dokumentasi current-state (Section 6) on-progress; P1.5 (F-14 restructure package by-feature) di-hold ke sub-fase tersendiri
- **Baseline:** Phase V4-P0 selesai (`01-final-report-phaseV4-P0.md`) — V4-03/V4-09 ditutup. State sebelum phase ini: `RateLimitService` 290 baris; `@Value` field injection di 4 kelas; actuator `env` ter-expose di staging; `SPRING_PROFILES_ACTIVE` hardcode di base ConfigMap K8s.
- **Quality gates:** ✅ Checkstyle 0, ✅ SpotBugs 0, ✅ JaCoCo gate `verify` lulus (≥ 80% LINE)
- **Tests:** 316 pass, 0 fail, 0 error (profil `ci`/`test`, integration tests dikecualikan — butuh Docker)
- **Branch:** `feature/oms-optimalisasi` — belum ada commit (menunggu keputusan user)

## 1. Ringkasan Eksekutif

| Temuan | Status | Deskripsi |
|---|---|---|
| **V4-01** | ✅ | `RateLimitService` 290 baris dipecah jadi facade tipis (93 baris) + kolaborator `BucketFactory`, `LocalBucketStore`, `DistributedBucketStore`, dan record top-level `RateLimitResult` |
| **V4-02** | ✅ | `@Value` field injection → constructor injection di `RedisConfig`, `TracingConfig`, `RefreshTokenService`, `ExternalAPIHealthIndicator` |
| **V4-04** | ✅ | `env` dihapus dari daftar `management.endpoints.web.exposure.include` staging |
| **V4-06** | ✅ | Base ConfigMap tidak lagi hardcode `SPRING_PROFILES_ACTIVE`; profil di-inject per-environment via Kustomize overlay (`k8s/overlays/{dev,staging,prod}`) |
| **F-14 / P1.5** | ⏸️ Ditunda | Restructure package by-layer → by-feature dipisah ke sub-fase tersendiri (keputusan user) |


## 2. Perubahan File

### 2.1 Dimodifikasi

| # | File | Temuan | Deskripsi |
|---|---|---|---|
| 1 | `src/main/java/com/oms/ratelimit/RateLimitService.java` | V4-01 | Ditulis ulang sebagai facade tipis (290 → 93 baris); mempertahankan API publik `tryConsume`, `initDistributedMode`, `isDistributedMode`. Constructor kini `(RateLimitProperties, LocalBucketStore, DistributedBucketStore)` |
| 2 | `src/main/java/com/oms/ratelimit/RateLimitInterceptor.java` | V4-01 | Import diperbarui ke `RateLimitResult` top-level |
| 3 | `src/main/java/com/oms/config/RedisConfig.java` | V4-02 | 5 field `@Value` → 5 constructor parameter `@Value` |
| 4 | `src/main/java/com/oms/config/TracingConfig.java` | V4-02 | 2 field `@Value` → constructor parameter |
| 5 | `src/main/java/com/oms/service/RefreshTokenService.java` | V4-02 | 2 field `@Value` → constructor parameter; import `@RequiredArgsConstructor` yang tak lagi dipakai dihapus |
| 6 | `src/main/java/com/oms/health/ExternalAPIHealthIndicator.java` | V4-02 | 2 field `@Value` → constructor parameter; import `@RequiredArgsConstructor` dihapus; field mutable `httpClient` (di-set via refleksi di test) dipertahankan |
| 7 | `src/main/resources/application-staging.yml` | V4-04 | `env` dihapus dari `exposure.include` (kini `health,info,metrics,circuitbreakers,circuitbreakerevents`), dengan komentar penjelas |
| 8 | `k8s/base/configmap.yaml` | V4-06 | `SPRING_PROFILES_ACTIVE: "staging"` dihapus; diganti komentar bahwa profil di-inject via overlay |
| 9 | `k8s/base/kustomization.yaml` | V4-06 | Blok dev/staging/prod yang di-comment dihapus; base kini environment-agnostic (tanpa datastore) |

Catatan: seluruh manifest K8s dipindah dari flat `k8s/*.yaml` ke `k8s/base/*.yaml` (via `git mv`), dan `postgres.yaml`/`redis.yaml` ke `k8s/components/datastore/`.

### 2.2 Dibuat Baru

| # | File | Temuan | Deskripsi |
|---|---|---|---|
| 1 | `src/main/java/com/oms/ratelimit/RateLimitResult.java` | V4-01 | Record top-level hasil ekstraksi dari inner record `RateLimitService.RateLimitResult` |
| 2 | `src/main/java/com/oms/ratelimit/BucketFactory.java` | V4-01 | Membangun Bucket4j `BucketConfiguration` per-endpoint dari `RateLimitProperties` |
| 3 | `src/main/java/com/oms/ratelimit/LocalBucketStore.java` | V4-01 | In-memory bucket store + cleanup sweep idle bucket (`purgeStaleBuckets`) |
| 4 | `src/main/java/com/oms/ratelimit/DistributedBucketStore.java` | V4-01 | Distributed bucket store berbasis Redis (`LettuceBasedProxyManager`) |
| 5 | `k8s/components/datastore/kustomization.yaml` | V4-06 | Komponen PostgreSQL + Redis, dilayer oleh overlay staging/prod |
| 6 | `k8s/overlays/dev/kustomization.yaml` | V4-06 | Overlay dev: base saja (H2), patch `SPRING_PROFILES_ACTIVE=dev` |
| 7 | `k8s/overlays/staging/kustomization.yaml` | V4-06 | Overlay staging: base + datastore, patch `SPRING_PROFILES_ACTIVE=staging` |
| 8 | `k8s/overlays/prod/kustomization.yaml` | V4-06 | Overlay prod: base + datastore, patch `SPRING_PROFILES_ACTIVE=prod` |

### 2.3 Test Dibuat/Diubah

| # | File | Temuan | Perubahan |
|---|---|---|---|
| 1 | `src/test/java/com/oms/ratelimit/RateLimitInterceptorTest.java` | V4-01 | Import `RateLimitResult` top-level |
| 2 | `src/test/java/com/oms/ratelimit/RateLimitServiceCleanupTest.java` | V4-01 | Diarahkan ke `LocalBucketStore` (via `BucketFactory`); `purgeStaleBuckets` kini di store |
| 3 | `src/test/java/com/oms/config/RedisConfigTest.java` | V4-02 | `new RedisConfig()` → `new RedisConfig("localhost", 6379, 0, "", Duration.ofSeconds(3600))` |
| 4 | `src/test/java/com/oms/health/ExternalAPIHealthIndicatorErrorTest.java` | V4-02 | 7 pemanggilan constructor kini menyuplai `paymentApiHealthUrl` + `healthCheckTimeout`; `setField` untuk 2 field final tersebut dihapus |

## 3. Validasi

### 3.1 Quality Gates

| Gate | Status | Bukti |
|---|---|---|
| Checkstyle | ✅ | 0 violations (fase `validate` pada `./mvnw clean verify -Pci`) |
| SpotBugs | ✅ | 0 findings (fase `compile`); `DistributedBucketStore.proxyManager` diinisialisasi eksplisit `= null` untuk menutup `UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR` |
| JaCoCo | ✅ | Gate `verify` lulus (≥ 80% LINE) |
| Unit tests | ✅ | 316/316 pass, 0 fail, 0 error |

### 3.2 Verifikasi Command

```bash
# V4-01: kelas ratelimit hasil split berukuran wajar; facade jauh < 290 baris
wc -l src/main/java/com/oms/ratelimit/*.java
# Validasi: ✅ PASS — tidak ada kelas ratelimit > ~150 baris

# V4-02: @Value hanya pada parameter constructor, bukan field
grep -rn "@Value" src/main/java/
# Validasi: ✅ PASS — RedisConfig/TracingConfig/RefreshTokenService/ExternalAPIHealthIndicator via constructor

# V4-04: env tidak lagi di exposure staging
grep -n "env" src/main/resources/application-staging.yml
# Validasi: ✅ PASS — tidak ada 'env' di daftar include

# V4-06: base tanpa literal profil; overlay meng-inject per-env
kubectl kustomize k8s/overlays/dev     | grep "SPRING_PROFILES_ACTIVE: dev"
kubectl kustomize k8s/overlays/staging | grep "SPRING_PROFILES_ACTIVE: staging"
kubectl kustomize k8s/overlays/prod    | grep "SPRING_PROFILES_ACTIVE: prod"
# Validasi: ✅ PASS — profil disuntik via patch overlay, base bebas profil

# Build + test penuh (profil ci/test)
./mvnw clean verify -Pci
# Hasil: BUILD SUCCESS — Tests run: 316, Failures: 0, Errors: 0
# Validasi: ✅ PASS

# Governance dokumentasi
python3 scripts/check-docs.py
# Hasil: PASSED — link OK, metadata OK, placeholder OK
# Validasi: ✅ PASS
```

## 4. Status Temuan

| Kode | Temuan | Status | Bukti |
|---|---|---|---|
| V4-01 | `RateLimitService` 290 baris | ✅ Selesai | Facade 93 baris + 4 kolaborator; API publik dipertahankan; `RateLimitInterceptorTest` & integration test hijau |
| V4-02 | `@Value` field injection di 4 kelas | ✅ Selesai | Keempat kelas kini constructor injection; `grep` tidak menemukan `@Value` di level field |
| V4-04 | Actuator `env` ter-expose di staging | ✅ Selesai | `application-staging.yml:75` tanpa `env` |
| V4-06 | ConfigMap hardcode profil | ✅ Selesai | Base tanpa literal `SPRING_PROFILES_ACTIVE`; 3 overlay build valid dengan profil masing-masing |

## 5. Exit Criteria

| Kriteria | Target | Aktual | Status |
|---|---|---|:---:|
| Kelas `ratelimit` hasil split | < ~150 baris | maks 144 (`LocalBucketStore`) | ✅ |
| `@Value` di level field | 0 | 0 | ✅ |
| `env` di exposure staging | Tidak ada | Tidak ada | ✅ |
| Literal profil di base ConfigMap | 0 | 0 | ✅ |
| `kubectl kustomize` tiap overlay | Valid + profil benar | dev/staging/prod valid | ✅ |
| Build + unit test | BUILD SUCCESS, 0 fail | BUILD SUCCESS, 316 pass 0 fail | ✅ |
| Checkstyle / SpotBugs | 0 / 0 | 0 / 0 | ✅ |
| Sinkronisasi docs current-state | Selesai | On-progress (lihat §6) | ⏳ |


## 6. Dokumentasi current-state yang disinkronkan

> **Status: ON-PROGRESS.** Perubahan kode sudah rampung, tetapi beberapa dokumen current-state masih menggambarkan struktur lama dan belum diperbarui pada fase ini. Item bertanda ⏳ adalah pekerjaan tersisa yang menjadi bagian penutupan (CLOSE) Phase V4-P1.

| Dokumen | Perubahan | Status |
|---|---|---|
| `docs/current/operations/deployment-guide.md` | `kubectl apply -k k8s/` → `kubectl apply -k k8s/overlays/<env>`; instruksi "update kustomization.yaml" diganti mekanisme overlay | ⏳ Belum |
| `docs/current/operations/65-oms-038-minikube.md` | Directory tree & tabel file diperbarui ke `k8s/base` + `k8s/overlays` + `k8s/components/datastore`; perintah apply per-overlay | ⏳ Belum |
| `docs/current/operations/66-oms-039-rate-limiting.md` | Deskripsi `RateLimitService` diperbarui: facade + `BucketFactory`/`LocalBucketStore`/`DistributedBucketStore` + `RateLimitResult` top-level | ⏳ Belum |
| `README.md` | Badge & Tech Stack Spring Boot `3.2.5` → `3.2.12`; blok deploy K8s ke overlay | ⏳ Belum |
| `docs/index.md` | Tambah tautan laporan Phase V4-P1 di bagian laporan fase optimalisasi V4 | ⏳ Belum |
| `docs/history/optimalisasi-v4/ANALISIS-PROYEK-OMS-V4.md` | Tandai V4-01/02/04/06 ✅ SELESAI di Section 13 & header status | ⏳ Belum |
| `CHANGELOG.md` | Tambah entri P1 (V4-01/02/04/06) di `[Unreleased]`; rapikan heading ganda | ⏳ Belum |

## 7. Catatan Teknis

1. **Facade + delegasi (V4-01)** — `RateLimitService` kini hanya mengatur mode (local vs distributed) dan mendelegasikan konsumsi token ke store yang aktif. `BucketFactory` memusatkan pembuatan `BucketConfiguration`; `LocalBucketStore` memegang `ConcurrentHashMap` bucket + sweep idle; `DistributedBucketStore` membungkus `LettuceBasedProxyManager`. API publik tidak berubah sehingga interceptor dan test lain tidak terdampak selain import `RateLimitResult`.
2. **`RateLimitResult` dipromosikan jadi top-level** — semua referensi `RateLimitService.RateLimitResult` diperbarui. Ini mempermudah reuse dan menghindari ketergantungan test pada inner class.
3. **Constructor injection (V4-02)** — `@Value` dipindah dari field ke parameter constructor (pola sama dengan `JwtTokenProvider` yang sudah benar sejak V3). `httpClient` di `ExternalAPIHealthIndicator` sengaja dibiarkan sebagai field mutable karena di-set via refleksi pada unit test.
4. **Kustomize base/overlay/component (V4-06)** — Struktur diubah agar base tak bisa membocorkan profil salah ke cluster produksi. Base bersifat H2-agnostic; datastore (PostgreSQL+Redis) dipisah ke komponen yang hanya dilayer overlay staging/prod. Profil disuntik lewat JSON-patch `add /data/SPRING_PROFILES_ACTIVE` pada ConfigMap `oms-config` (bukan `configMapGenerator`, agar tidak menambah name-suffix hash dan tetap merujuk ConfigMap base yang sama).
5. **SpotBugs** — `proxyManager` di `DistributedBucketStore` diinisialisasi eksplisit `= null` karena baru di-set saat `initDistributedMode`; tanpa ini SpotBugs menandai `UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR`.
6. **Peringatan `commonLabels` (V4-08)** — `kubectl kustomize` menampilkan warning deprecated `commonLabels`. Ini bukan bagian P1 melainkan lingkup Phase V4-P2 (V4-08) dan sengaja belum disentuh.

## 8. Rollback & Tindak Lanjut

- **Strategi rollback:** Code + config + manifest. Untuk K8s, restrukturisasi memakai `git mv` sehingga revert commit mengembalikan struktur flat lama. Untuk kode, revert 9 file main + 4 file test.
- **Monitoring setelah deploy:** Pantau (a) metrik rate-limit (429) tetap konsisten setelah split `RateLimitService`; (b) startup tiap environment memuat profil yang benar dari overlay (`SPRING_PROFILES_ACTIVE`); (c) actuator staging tidak lagi menyajikan `/actuator/env`.
- **Tindak lanjut:** Selesaikan Section 6 (SYNC docs) sebelum CLOSE; kemudian keputusan user untuk commit P1 dan penjadwalan sub-fase P1.5 (F-14) serta Phase V4-P2 (V4-05/07/08/10).

## 9. Deferred

| Item | Alasan | Rencana |
|---|---|---|
| F-14 (package by-layer → by-feature) | Lingkup besar & berisiko regresi; dipisah agar P1 tetap fokus quality+env hardening | Sub-fase P1.5 tersendiri (keputusan user) |
| Sinkronisasi docs current-state (§6) | Kode selesai; penyelarasan dokumen K8s/README/rate-limit menyusul di langkah CLOSE | Penutupan Phase V4-P1 |
| V4-05, V4-07, V4-08, V4-10 | Lingkup Phase V4-P2 (operational maturity) | Phase V4-P2 |
| Commit perubahan P1 | Kebijakan: menunggu keputusan user | Setelah §6 tuntas |

---

*Laporan ini disusun pada 24 Agustus 2026 mengikuti template `docs/_templates/phase-report-template.md` dan Section 13 `ANALISIS-PROYEK-OMS-V4.md`. Status ⚠️ Sebagian mencerminkan kode P1.1–P1.4 selesai & terverifikasi, dengan sinkronisasi dokumentasi current-state masih berjalan.*

