# Laporan Final Fase 0 — Containment Segera

> **Tanggal:** 2026-08-11
> **Tipe fase:** Implementation
> **Tujuan:** Containment segera — menutup risiko kritis F-01, F-03, F-05 (parsial), F-10 (parsial)
> **Status:** ✅ SELESAI
> **Referensi:** `docs/history/optimalisasi-v2/ANALISIS-PROYEK-OMS-V2.md`

---

## 1. RINGKASAN DAN SCOPE

- **Temuan target:** F-01, F-03, F-05 (parsial), F-10 (parsial)
- **In scope:** Blok refresh token sebagai Bearer; hapus secret literal; ClusterIP + SSL redirect + HSTS + ServiceAccount khusus; isolasi actuator
- **Out of scope:** F-02 → Fase 1; F-04 → Fase 1; F-05 NetworkPolicy, RBAC, image digest → Fase 2; F-06, F-07, F-08, F-09, sisa F-10 → Fase 2/3

## 2. PERUBAHAN

| File | Perubahan | Fase |
|---|---|---|
| `JwtAuthenticationFilter.java:48-55` | Tambah cek `token_type=access`; tolak token selain access | F-01 |
| `JwtAuthenticationFilterTest.java` | +2 test negative: `shouldRejectRefreshTokenAsBearer`, `shouldRejectTokenWithNullTokenType`; mock `extractTokenType` di 6 existing test | F-01 |
| `k8s/secret.yaml` | Literal values → placeholder `<CHANGE_ME_...>` + security notice | F-03 |
| `env/docker/oms-dev.env` | Hapus literal fallback `omsDevSuperSecretKey...`, ganti placeholder | F-03 |
| `k8s/service.yaml` | `NodePort` → `ClusterIP`, hapus `nodePort` | F-05/F-10 |
| `k8s/ingress.yaml` | `ssl-redirect: "true"`, TLS block, HSTS annotations | F-05 |
| `k8s/serviceaccount.yaml` (BARU) | `oms-service-account` + `automountServiceAccountToken: false` | F-05 |
| `k8s/deployment.yaml:27` | `serviceAccountName: default` → `oms-service-account` | F-05 |
| `k8s/kustomization.yaml` | Tambah `serviceaccount.yaml` ke resources | F-05 |

## 3. TEST DAN VALIDASI

| Validasi | Command | Hasil |
|---|---|---|
| JwtAuthenticationFilterTest | `./mvnw test -Dtest=JwtAuthenticationFilterTest` | **10 tests, 0 failure, 0 error, 0 skipped** ✅ |
| AuthControllerTest | `./mvnw test -Dtest=AuthControllerTest` | **7 tests, 0 failure, 0 error, 0 skipped** ✅ |
| JwtTokenProviderTest | `./mvnw test -Dtest=JwtTokenProviderTest` | **22 tests, 0 failure, 0 error, 0 skipped** ✅ |
| RateLimit tests | `./mvnw test -Dtest=RateLimitInterceptorTest,RateLimitServiceTest` | **6 tests, 0 failure, 0 error, 0 skipped** ✅ |
| Checkstyle | `./mvnw checkstyle:check` | **0 violations** ✅ |
| SpotBugs | `./mvnw spotbugs:check` | **0 finding** ✅ |
| Kustomize render | `kubectl kustomize k8s` | **9 resources rendered** ✅ |

## 4. STATUS TEMUAN

| Temuan | Status | Bukti |
|---|---|---|
| **F-01** — Refresh JWT sebagai Bearer token | ✅ SELESAI | Filter cek `token_type=access`; 2 negative test memvalidasi |
| **F-03** — Secret literal di repository | ✅ SELESAI | Semua placeholder; ⚠️ secret wajib dirotasi via channel aman |
| **F-05** — Manifest tidak hardened | ⚠️ SEBAGIAN | ClusterIP/SSL/HSTS/SA ✅; NetworkPolicy/RBAC/digest → Fase 2 |
| **F-10** — Tracing/actuator config | ⚠️ SEBAGIAN | Actuator NodePort dihilangkan; sampling/tracing → Fase 2/3 |

## 5. ROLLBACK DAN TINDAK LANJUT

- **Rollback:** Revert kode `JwtAuthenticationFilter`, revert K8s service/ingress/deployment, hapus SA
- **Monitoring:** Pantau auth failure rate, 401/403 response rate
- **Tindak lanjut:**
  - Rotasi JWT secret + DB credentials melalui channel operasional aman
  - Fase 1: F-02 (refresh token lifecycle) + F-04 (distributed rate limiting)
  - Fase 2: NetworkPolicy, RBAC, image digest, coverage gate, CI scans

## 6. KESIMPULAN

Fase 0 menyelesaikan 2 dari 3 temuan P0 (F-01, F-03) dan kontribusi parsial F-05/F-10.
Semua quality gate lulus. Perubahan backward-compatible. Prasyarat Fase 1 terpenuhi.