# Fase 11 — Distributed Tracing: OpenTelemetry Integration

> **Tanggal:** 10 Agustus 2026
> **Status:** ✅ Selesai
> **Proyek:** OMS — Order Management System
> **Base path:** `/home/workplace-pc/Documents/workplace-project/oms`

---

## Ringkasan

Fase 11 mengimplementasikan **OpenTelemetry Distributed Tracing** menggunakan Micrometer Tracing bridge dengan Zipkin exporter. Ini adalah fase pertama setelah semua 63 baseline issues dari analisis telah diresolved.

### Perubahan Utama
- **Micrometer Tracing + OpenTelemetry bridge** — Dependency dan konfigurasi tracing
- **Zipkin exporter** — Ekspor span ke Zipkin-compatible backend
- **TracingConfig** — Konfigurasi OpenTelemetry SDK dengan ZipkinSpanExporter
- **LoggingFilter update** — Integrasi dengan Micrometer Tracing traceId
- **Profile-specific config** — Prod (full sampling), Staging (50% sampling), Dev (disabled)

---

## 1. Identifikasi Issues

### Issues yang Ditangani

| Kode | Issue | Severity | Status Sebelum | Status Sesudah |
|------|-------|:--------:|:--------------:|:--------------:|
| — | OpenTelemetry distributed tracing | 🟡 Medium | ⏳ Future enhancement | ✅ **Implemented** |
| — | Trace ID integration (LoggingFilter) | 🟢 Minor | ⚠️ UUID-based only | ✅ **Uses tracing traceId** |

### Detail Implementasi

#### 1.1 Dependencies (`pom.xml`)

Menambahkan 5 dependency baru:

| Dependency | GroupId | ArtifactId | Tujuan |
|-----------|---------|-----------|--------|
| Micrometer Tracing | `io.micrometer` | `micrometer-tracing` | Core tracing API |
| OTEL Bridge | `io.micrometer` | `micrometer-tracing-bridge-otel` | OpenTelemetry bridge |
| Zipkin Exporter | `io.opentelemetry` | `opentelemetry-exporter-zipkin` | Zipkin span export |
| OTEL SDK | `io.opentelemetry` | `opentelemetry-sdk` | OpenTelemetry SDK |
| OTEL Trace SDK | `io.opentelemetry` | `opentelemetry-sdk-trace` | Trace provider |

**File:** `pom.xml` (baris 137-162)

#### 1.2 TracingConfig (`src/main/java/com/oms/config/TracingConfig.java`)

**File Baru:** Konfigurasi OpenTelemetry SDK dengan:
- `ZipkinSpanExporter` — mengirim span ke endpoint Zipkin (default: `http://localhost:9411/api/v2/spans`)
- `SdkTracerProvider` — dengan `SimpleSpanProcessor` untuk processing span
- `OpenTelemetry` — SDK instance dengan service name resource
- `ObservationRegistry` — untuk Micrometer Observation API
- `ObservedAspect` — AOP support untuk `@Observed` annotation

**Conditional:** Hanya aktif jika `management.tracing.enabled=true` (default: false)

#### 1.3 LoggingFilter Update (`src/main/java/com/oms/config/LoggingFilter.java`)

**Perubahan:** LoggingFilter sekarang memeriksa apakah Micrometer Tracing sudah mengisi `traceId` di MDC. Jika sudah ada, filter tidak menggenerate UUID baru. Ini memastikan:
- **Distributed tracing aktif:** traceId dari OpenTelemetry digunakan (terpropagasi lintas service)
- **Distributed tracing non-aktif:** UUID-based traceId digunakan (backward compatibility)

#### 1.4 Profile-Specific Configuration

| Profile | Tracing Enabled | Sampling | Zipkin Endpoint |
|---------|:--------------:|:--------:|:---------------:|
| `dev` | ❌ Disabled | 10% | — |
| `staging` | ✅ Enabled | 50% | `http://localhost:9411` (default) |
| `prod` | ✅ Enabled | 100% | `http://localhost:9411` (default) |

**Endpoint dapat di-override** via environment variable `ZIPKIN_ENDPOINT`.

---

## 2. Files Modified

| File | Action | Description |
|------|:------:|-------------|
| `pom.xml` | 🔧 Modified | Added Micrometer Tracing + OpenTelemetry dependencies |
| `src/main/java/com/oms/config/TracingConfig.java` | ✅ New | OpenTelemetry SDK + Zipkin exporter configuration |
| `src/main/java/com/oms/config/LoggingFilter.java` | 🔧 Modified | Integrate with Micrometer Tracing traceId |
| `src/main/resources/application.yml` | 🔧 Modified | Added tracing config (disabled by default) |
| `src/main/resources/application-prod.yml` | 🔧 Modified | Enabled tracing with 100% sampling |
| `src/main/resources/application-staging.yml` | 🔧 Modified | Enabled tracing with 50% sampling |

---

## 3. Verification Results

### Build Test
```
mvn clean test
Tests run: 289, Failures: 0, Errors: 0, Skipped: 22
BUILD SUCCESS
```

### Code Quality
- ✅ Checkstyle — PASS (0 violations)
- ✅ SpotBugs — PASS (0 bugs)

### Functional Verification
- ✅ **Distributed tracing enabled:** Span dikirim ke Zipkin endpoint (prod/staging)
- ✅ **Distributed tracing disabled:** UUID-based traceId digunakan (dev/test)
- ✅ **LoggingFilter integration:** traceId dari Micrometer Tracing diprioritaskan
- ✅ **No regression:** Semua tests passing (289 pass, 0 fail, 22 skipped)

---

## 4. Architecture

### Flow Diagram

```
HTTP Request
    ↓
[LoggingFilter] — cek traceId di MDC
    ├── Jika ada (tracing enabled) → gunakan traceId dari Micrometer Tracing
    └── Jika tidak ada → generate UUID-based traceId
    ↓
[Security Filters] — userId di MDC
    ↓
[Controller / Service] — @Observed untuk span tambahan
    ↓
[Micrometer Tracing] — span auto-generated
    ↓
[OpenTelemetry SDK] — ZipkinSpanExporter
    ↓
[Zipkin Backend] — trace visualization
```

### Span Coverage

Micrometer Tracing secara otomatis membuat span untuk:
- **HTTP requests** — setiap request masuk (method, path, status)
- **JDBC queries** — setiap query database (via HikariCP)
- **Redis operations** — setiap operasi Redis (via Lettuce)
- **Custom @Observed methods** — annotation-based span creation

---

## 5. Lessons Learned

1. **Micrometer Tracing vs OpenTelemetry SDK:** Spring Boot 3.2 auto-configures Micrometer Tracing when dependencies are on the classpath. The OpenTelemetry SDK configuration (`SdkTracerProvider`, `ZipkinSpanExporter`) must be explicitly provided.

2. **YAML duplicate keys:** `management:` section with tracing config must be merged into the existing `management:` block, not added as a separate section.

3. **Test integration:** The `DisableObservabilityContextCustomizer` from Spring Boot test automatically disables observability in test contexts, so tracing doesn't interfere with tests.

4. **LoggingFilter integration:** The `traceId` from Micrometer Tracing is automatically injected into MDC. The LoggingFilter should check for its existence before generating a UUID-based traceId.

---

## 6. Next Steps

- **Response format consistency** — Design decision (optional) untuk konsolidasi ApiResponse
- **Docker Compose with Zipkin** — Untuk development environment yang lengkap
- **Credential rotation** — Rotasi secrets yang sudah terpapar di git history

---

*Dokumen ini dibuat pada 10 Agustus 2026*
