# Laporan Fase V3-0 — Security Hardening

> **Tanggal:** 14 Agustus 2026  
> **Fokus:** Redis serializer, CORS env-aware, hapus default dev profile, ObjectMapper dedup  
> **Temuan ditutup:** F-11, F-12, F-22, F-18 + SOLID S-3 (partial), S-4, O-2  
> **Status:** ✅ **SELESAI**

---

## 1. Ikhtisar

Fase V3-0 menutup 3 temuan P0 (F-11, F-12, F-22) dan 1 temuan P2 (F-18), beserta 3 temuan SOLID (S-3 partial, S-4, O-2). Total: 4 temuan finding + 3 SOLID = 7 item tertutup.

---

## 2. Perubahan File

### 2.1 Dimodifikasi

| # | File | Temuan | Deskripsi |
|---|---|---|---|
| 1 | `src/main/java/com/oms/config/RedisConfig.java` | F-11, F-18, S-4 | Ganti `DefaultTyping.NON_FINAL` open validator dengan `BasicPolymorphicTypeValidator` whitelist `com.oms.*`, `java.util.*`, `java.lang.*`. Ekstrak shared `@Bean ObjectMapper redisObjectMapper()`. |
| 2 | `src/main/java/com/oms/config/SecurityConfig.java` | F-12, S-3, O-2 | Inject `CorsProperties` via constructor. `@EnableConfigurationProperties(CorsProperties.class)`. CORS dari properties, bukan hardcoded. |
| 3 | `src/main/resources/application.yml` | F-22 | Hapus `spring.profiles.active: dev`. Profile harus via `SPRING_PROFILES_ACTIVE` env var. |
| 4 | `src/main/resources/application-dev.yml` | F-12 | Tambah `app.cors.allowed-origins: [localhost:3000, 127.0.0.1:3000, https://localhost:3000]` |
| 5 | `src/main/resources/application-staging.yml` | F-12 | Tambah `app.cors.allowed-origins: [https://staging.oms.com]` |
| 6 | `src/main/resources/application-prod.yml` | F-12 | Tambah `app.cors.allowed-origins: [https://oms.com]` |
| 7 | `k8s/configmap.yaml` | F-22 | `SPRING_PROFILES_ACTIVE: "dev"` → `"staging"` |

### 2.2 Dibuat Baru

| # | File | Temuan | Deskripsi |
|---|---|---|---|
| 1 | `src/main/java/com/oms/config/CorsProperties.java` | F-12, O-2 | `@ConfigurationProperties(prefix = "app.cors")` — default: `localhost:3000` (dev-safe). |

### 2.3 Test Dibuat Baru

| # | File | Temuan | Jumlah Test |
|---|---|---|---|
| 1 | `src/test/java/com/oms/config/RedisConfigTest.java` | F-11, F-18 | 4 test |
| 2 | `src/test/java/com/oms/config/CorsConfigTest.java` | F-12 | 5 test |
| 3 | `src/test/java/com/oms/config/SecurityConfigCorsTest.java` | F-12, O-2 | 4 test |

---

## 3. Hasil Validasi

### 3.1 Validation Commands (Section 6)

```bash
# Command 1: DefaultTyping di RedisConfig
grep -n "DefaultTyping" src/main/java/com/oms/config/RedisConfig.java
# Hasil: Lines 53 (Javadoc), 60 (method call), 66 (enum)
# Catatan: String "DefaultTyping" muncul karena nama method Jackson API
# `activateDefaultTyping()`. Yang diperbaiki adalah type validator —
# sekarang pakai BasicPolymorphicTypeValidator whitelist (bukan validator terbuka).
# Validasi: ✅ PASS — tidak ada open validator.

# Command 2: localhost:3000 di SecurityConfig
grep -n "localhost:3000" src/main/java/com/oms/config/SecurityConfig.java
# Hasil: (exit code 1 — tidak ditemukan)
# Validasi: ✅ PASS

# Command 3: profiles.active di application.yml
grep -n "profiles.active" src/main/resources/application.yml
# Hasil: Line 12 — `${spring.profiles.active}` di config.import placeholder
# Catatan: Ini property placeholder, bukan default active profile.
# `spring.profiles.active: dev` sudah dihapus.
# Validasi: ✅ PASS
```

### 3.2 Quality Gates

| Gate | Command | Hasil |
|---|---|---|
| Checkstyle | `./mvnw checkstyle:check` | **0 violations** ✅ |
| SpotBugs | `./mvnw spotbugs:check` | **0 findings** ✅ |
| Tests (V3-0 + regression) | 6 test classes | **32 tests, 0 failures** ✅ |

---

## 4. Status Temuan

| Kode | Temuan | Status | Bukti |
|---|---|---|---|
| F-11 | Redis serializer `DefaultTyping.NON_FINAL` | ✅ Selesai | `BasicPolymorphicTypeValidator` whitelist |
| F-12 | CORS hardcoded localhost | ✅ Selesai | `CorsProperties` + per-profile YAML |
| F-22 | Default dev profile berisiko | ✅ Selesai | Hapus dari `application.yml`, K8s → staging |
| F-18 | Duplicated ObjectMapper | ✅ Selesai | Shared `@Bean redisObjectMapper()` |
| SOLID S-3 | SecurityConfig mixed concerns | ✅ Selesai (partial) | CORS di-extract; full split di V3-2 |
| SOLID S-4 | RedisConfig duplicated ObjectMapper | ✅ Selesai | Shared bean |
| SOLID O-2 | CORS requires code change per env | ✅ Selesai | CorsProperties + YAML profiles |

---

## 5. Exit Criteria Verification

| Kriteria | Status | Bukti |
|---|---|---|
| Redis serializer aman (whitelist only) | ✅ | `BasicPolymorphicTypeValidator` — `com.oms.*`, `java.util.*`, `java.lang.*` |
| CORS per profile | ✅ | dev: localhost:3000, staging: staging.oms.com, prod: oms.com |
| Tidak ada default dev profile | ✅ | `spring.profiles.active: dev` dihapus dari `application.yml` |
| ObjectMapper tidak duplikat | ✅ | Satu shared `@Bean redisObjectMapper()` |

---

## 6. Catatan

1. **Validasi `grep DefaultTyping`** — Command Section 6 mengharuskan "should return nothing", tetapi `activateDefaultTyping()` adalah method name Jackson API yang diwajibkan. Yang diperbaiki adalah type validator (open → whitelist). Validation command idealnya: `grep -n "getPolymorphicTypeValidator" RedisConfig.java` (should return nothing).

2. **CorsProperties default** — Default `localhost:3000` memastikan aplikasi tetap berjalan di dev tanpa tambahan config (safe for local development).

3. **Test approach** — Unit test instantiate config langsung tanpa Spring context, sehingga tidak terpengaruh `redis.enabled=false` di test profile.

---

*Laporan ini disusun pada 14 Agustus 2026 sesuai Section 9.5 ANALISIS-PROYEK-OMS-V3.md.*
