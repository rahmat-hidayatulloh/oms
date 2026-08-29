# OMS-027: Implementasi Structured Logging dengan MDC

> **Status:** Current
> **Audience:** Operator, Maintainer
> **Owner:** Engineering
> **Last verified:** 2026-08-29 (screening kode aktual `src/`, `pom.xml`)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## Tujuan
Mengimplementasikan structured logging dengan MDC (Mapped Diagnostic Context) untuk tracing request end-to-end, memudahkan debugging dan monitoring di environment production.

## Acceptance Criteria
- [x] Buat LoggingFilter (OncePerRequestFilter) untuk generate traceId
- [x] MDC.put("traceId", UUID.randomUUID().toString()) di awal request
- [x] MDC.put("userId", authenticated user jika ada)
- [x] MDC.put("method", request.getMethod())
- [x] MDC.put("path", request.getRequestURI())
- [x] MDC.clear() di akhir request
- [x] Konfigurasi logback-spring.xml untuk JSON format

## Detail Pekerjaan

### 1. Latar Belakang

MDC (Mapped Diagnostic Context) adalah fitur SLF4J yang memungkinkan kita menambahkan context information ke setiap log entry. Dengan structured logging:

- **Traceability**: Setiap log memiliki traceId yang unik per request
- **Auditability**: userId terekam di setiap log
- **Debugging**: method dan path membantu identifikasi endpoint yang bermasalah
- **Correlation**: traceId bisa di-pass ke service lain untuk distributed tracing

### 2. Struktur File

```
src/main/java/com/oms/config/
├── LoggingFilter.java              ← Filter MDC untuk setiap request

src/main/resources/
├── logback-spring.xml               ← Konfigurasi Logback dengan JSON encoder
```

### 3. LoggingFilter

**File:** `src/main/java/com/oms/config/LoggingFilter.java`

Filter yang berjalan di `HIGHEST_PRECEDENCE` (sebelum security filters) untuk memastikan MDC tersedia untuk semua logging downstream.

#### MDC Keys yang di-set:

| Key | Sumber | Contoh |
|---|---|---|
| `traceId` | `UUID.randomUUID()` | `a1b2c3d4e5f6...` |
| `userId` | `SecurityContextHolder` | `admin@example.com` |
| `method` | `request.getMethod()` | `GET`, `POST` |
| `path` | `request.getRequestURI()` | `/api/v1/products/search` |

#### Alur Filter:

```
Request Masuk
  ↓
LoggingFilter.doFilterInternal()
  ↓
MDC.put("traceId", UUID.randomUUID())
MDC.put("method", request.getMethod())
MDC.put("path", request.getRequestURI())
MDC.put("userId", authentication.getName())  ← Jika authenticated
  ↓
filterChain.doFilter(request, response)
  ↓
[Semua logging di layer lain punya MDC context]
  ↓
finally {
    MDC.clear();  ← Penting! Mencegah memory leak di thread pool
}
```

#### Code Flow:

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingFilter extends OncePerRequestFilter {

    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) {
        try {
            // 1. Generate traceId
            MDC.put("traceId", UUID.randomUUID().toString().replace("-", ""));

            // 2. Set HTTP context
            MDC.put("method", request.getMethod());
            MDC.put("path", request.getRequestURI());

            // 3. Set authenticated user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getPrincipal())) {
                MDC.put("userId", auth.getName());
            }

            // 4. Lanjutkan filter chain
            filterChain.doFilter(request, response);

        } finally {
            // 5. Bersihkan MDC (WAJIB!)
            MDC.clear();
        }
    }
}
```

### 4. Logback Configuration

**File:** `src/main/resources/logback-spring.xml`

#### Appenders

| Appender | Format | Profile |
|---|---|---|
| `CONSOLE` | Human-readable dengan MDC | dev, test, staging |
| `JSON_FILE` | JSON ke file (rolling) | staging, prod |
| `CONSOLE_JSON` | JSON ke stdout | prod |

#### Human-readable Pattern (Dev):
```
2026-07-26 08:30:00.123 [http-nio-8080-exec-1] DEBUG com.oms.order.service.OrderService
[traceId=a1b2c3d4e5f6] [userId=admin@example.com] [POST /api/orders]
- Created order OMS-20260726-1234 for user ...
```

#### JSON Format (Production):
```json
{
  "@timestamp": "2026-07-26T08:30:00.123+07:00",
  "level": "INFO",
  "logger": "com.oms.order.service.OrderService",
  "thread": "http-nio-8080-exec-1",
  "message": "Created order OMS-20260726-1234 for user ...",
  "application": "oms",
  "profile": "prod",
  "mdc": {
    "traceId": "a1b2c3d4e5f6",
    "userId": "admin@example.com",
    "method": "POST",
    "path": "/api/orders"
  }
}
```

#### Profile-specific Configuration:

```xml
<!-- Dev: Human-readable console -->
<springProfile name="dev">
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
    <logger name="com.oms" level="DEBUG"/>
</springProfile>

<!-- Staging: Console + JSON file -->
<springProfile name="staging">
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="JSON_FILE"/>
    </root>
</springProfile>

<!-- Production: JSON console + JSON file -->
<springProfile name="prod">
    <root level="WARN">
        <appender-ref ref="CONSOLE_JSON"/>
        <appender-ref ref="JSON_FILE"/>
    </root>
</springProfile>
```

### 5. Contoh Output Log

#### Dev Profile (Human-readable):
```
2026-07-26 08:30:00.001 [http-nio-8080-exec-1] DEBUG c.o.config.LoggingFilter
[traceId=abc123def456] [userId=-] [POST /api/orders]
- Incoming request: POST /api/orders [traceId=abc123def456]

2026-07-26 08:30:00.050 [http-nio-8080-exec-1] DEBUG c.o.service.OrderService
[traceId=abc123def456] [userId=customer@example.com] [POST /api/orders]
- Created order OMS-20260726-1234 for user ...

2026-07-26 08:30:00.100 [EmailExecutor-1] INFO c.o.service.EmailService
[traceId=-] [userId=-] [ - ]
- === SIMULATED EMAIL ===
```

#### Prod Profile (JSON):
```json
{"@timestamp":"2026-07-26T08:30:00.001+07:00","level":"DEBUG","logger":"com.oms.config.LoggingFilter","thread":"http-nio-8080-exec-1","message":"Incoming request: POST /api/orders [traceId=abc123def456]","mdc":{"traceId":"abc123def456","userId":"","method":"POST","path":"/api/orders"}}

{"@timestamp":"2026-07-26T08:30:00.050+07:00","level":"INFO","logger":"com.oms.order.service.OrderService","thread":"http-nio-8080-exec-1","message":"Created order OMS-20260726-1234 for user ...","mdc":{"traceId":"abc123def456","userId":"customer@example.com","method":"POST","path":"/api/orders"}}
```

### 6. Cara Verifikasi

#### a. Jalankan aplikasi dan lihat log console:
```bash
./mvnw spring-boot:run
# Di log akan terlihat: [traceId=xxx] [userId=xxx] [method path]
```

#### b. Cek JSON log file (staging/prod):
```bash
cat logs/oms-dev.json | jq '.'
```

#### c. Test traceId correlation:
```bash
# Buat request
curl -s http://localhost:8080/api/v1/products | head -1

# Cari traceId di log
grep "traceId=" logs/oms-dev.json
```

### 7. Files yang Diubah/Ditambahkan

| File | Tindakan | Deskripsi |
|---|---|---|
| `src/main/java/com/oms/config/LoggingFilter.java` | **Dibuat** | Filter MDC dengan HIGHEST_PRECEDENCE |
| `src/main/resources/logback-spring.xml` | **Dibuat** | Konfigurasi Logback dengan 3 appenders + JSON |
| `src/main/resources/application.yml` | **Diubah** | Hapus pattern console (diatur di logback) |
| `pom.xml` | **Diubah** | Tambah dependency logstash-logback-encoder |
| `docs/current/operations/30-structured-logging-mdc.md` | **Dibuat** | Dokumentasi |

### 8. Best Practices

1. **MDC.clear() di finally block** - Mencegah memory leak di thread pools
2. **@Order(HIGHEST_PRECEDENCE)** - Filter berjalan sebelum security filters
3. **traceId tanpa strip** - `UUID.randomUUID().toString().replace("-", "")` lebih pendek
4. **Handle anonymousUser** - Jangan set userId untuk request yang belum login
5. **Profile-specific logging** - Dev: human readable, Prod: JSON untuk log aggregation
6. **maxHistory 30 hari** - Retensi log 30 hari untuk compliance

### 9. Troubleshooting

| Masalah | Penyebab | Solusi |
|---|---|---|
| MDC context hilang di async thread | MDC tidak di-propagate | Gunakan `HystrixConcurrencyStrategy` atau `ThreadPoolTaskExecutor` dengan MDC |
| JSON encoder tidak ditemukan | Dependency missing | Pastikan `logstash-logback-encoder` ada di pom.xml |
| Log pattern tidak berubah | application.yml override | Hapus `logging.pattern.console` dari application.yml |
| traceId kosong di log | Filter tidak jalan | Cek `@Component` dan `@Order` annotations |

### 10. Integrasi dengan ELK Stack

JSON log yang dihasilkan bisa langsung dikirim ke Elasticsearch via Filebeat:

```yaml
# filebeat.yml
filebeat.inputs:
- type: log
  paths:
    - /path/to/logs/oms-*.json
  json.keys_under_root: true
  json.add_error_key: true

output.elasticsearch:
  hosts: ["localhost:9200"]
  index: "oms-logs-%{+yyyy.MM.dd}"
```