# OMS-026: Implementasi Custom Health Indicator

> **Status:** Current
> **Audience:** Operator, Maintainer
> **Owner:** Engineering
> **Last verified:** 2026-08-29 (screening kode aktual `src/`, `pom.xml`)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## Tujuan
Membuat custom health check untuk memonitor ketersediaan dependency external aplikasi (database, Redis, external payment API) melalui endpoint `/actuator/health`.

## Acceptance Criteria
- [x] Implement HealthIndicator interface
- [x] DatabaseHealthIndicator (cek koneksi)
- [x] RedisHealthIndicator (ping Redis)
- [x] ExternalAPIHealthIndicator (mock external payment)
- [x] /actuator/health menampilkan semua komponen

## Detail Pekerjaan

### 1. Latar Belakang

Spring Boot Actuator menyediakan endpoint `/actuator/health` untuk mengecek kesehatan aplikasi. Secara default, Spring Boot menyediakan beberapa health indicator bawaan (db, diskSpace, ping). Namun, kita perlu:

- **DatabaseHealthIndicator**: Custom health check untuk database yang lebih detail (nama database, versi, URL)
- **RedisHealthIndicator**: Health check khusus Redis dengan PING command, hanya aktif saat Redis enabled
- **ExternalAPIHealthIndicator**: Health check untuk external payment API/microservice

### 2. Struktur File

```
src/main/java/com/oms/health/
├── DatabaseHealthIndicator.java
├── RedisHealthIndicator.java
└── ExternalAPIHealthIndicator.java
```

### 3. Implementasi Health Indicators

#### a. DatabaseHealthIndicator

**File:** `src/main/java/com/oms/health/DatabaseHealthIndicator.java`

Custom health indicator untuk mengecek koneksi database dengan detail:

- Menggunakan `DataSource.getConnection()` untuk mendapatkan koneksi
- `connection.isValid(5)` untuk validasi dengan timeout 5 detik
- `DatabaseMetaData` untuk mendapatkan nama database, versi, dan URL
- URL di-sanitasi untuk menyembunyikan credential (password=****)

**Response saat UP:**
```json
{
  "database": "H2",
  "version": "2.2.224 (2024-03-14)",
  "url": "jdbc:h2:mem:omsdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
  "validationQuery": "SELECT 1"
}
```

**Response saat DOWN:**
```json
{
  "reason": "Connection is not valid"
}
```

#### b. RedisHealthIndicator

**File:** `src/main/java/com/oms/health/RedisHealthIndicator.java`

Health indicator untuk Redis yang menggunakan PING command:

- Menggunakan `RedisConnectionFactory.getConnection()` 
- Execute `connection.ping()` - mengharapkan response "PONG"
- **Conditional**: Hanya aktif saat `spring.data.redis.enabled=true`
- Menggunakan `@ConditionalOnProperty` agar tidak error saat Redis disabled

**Response saat UP:**
```json
{
  "service": "Redis",
  "ping": "PONG"
}
```

**Response saat DOWN:**
```json
{
  "service": "Redis",
  "reason": "Connection refused: localhost:6379"
}
```

#### c. ExternalAPIHealthIndicator

**File:** `src/main/java/com/oms/health/ExternalAPIHealthIndicator.java`

Health indicator untuk external payment API (mock `mock-bi-snap`):

- Menggunakan `java.net.http.HttpClient` (Java 11+ native HTTP client)
- Melakukan HTTP GET ke `http://localhost:9090/health` (mock payment server, default `app.payment.api.health-url` / `HEALTH_URL`)
- Timeout 5 detik
- UP jika response HTTP 200
- Exception handling untuk `ConnectException`, `HttpTimeoutException`, dan general exception
- Dilindungi Circuit Breaker + Retry (`resilience4j`) agar kegagalan health check tidak mengganggu aplikasi

**Response saat UP:**
```json
{
  "service": "External Payment API",
  "url": "http://localhost:9090/health",
  "httpStatus": 200,
  "responseTime": "simulated"
}
```

**Response saat DOWN (circuit breaker fallback):**
```json
{
  "service": "External Payment API",
  "url": "http://localhost:9090/health",
  "httpStatus": 0,
  "responseTime": "N/A",
  "reason": "Circuit breaker fallback: Connection refused: ...",
  "circuitBreaker": "OPEN"
}
```

### 4. Konfigurasi Actuator Health

**File:** `src/main/resources/application.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,env
  endpoint:
    health:
      show-details: always
      show-components: always
      status:
        http-mapping:
          UP: 200
          DOWN: 503
          OUT_OF_SERVICE: 503
          UNKNOWN: 200
  health:
    status:
      order: DOWN,OUT_OF_SERVICE,UP,UNKNOWN
```

**Penjelasan:**
- `show-details: always`: Menampilkan detail semua komponen (tanpa autentikasi)
- `show-components: always`: Menampilkan sub-komponen health
- `status.http-mapping`: Mapping status health ke HTTP response code:
  - UP → 200 OK
  - DOWN → 503 Service Unavailable
  - OUT_OF_SERVICE → 503
  - UNKNOWN → 200
- `status.order`: Urutan prioritas penentuan status keseluruhan (jika ada satu DOWN, keseluruhan menjadi DOWN)

### 5. Cara Kerja

Saat endpoint `/actuator/health` diakses, Spring Boot akan:

1. Mengumpulkan semua bean yang mengimplementasikan `HealthIndicator`
2. Memanggil method `health()` pada masing-masing indicator
3. Menggabungkan hasil ke dalam response JSON
4. Menentukan status keseluruhan berdasarkan aggregasi (jika satu DOWN, semua DOWN)

### 6. Response Endpoint

#### Semua Komponen UP (Dev Profile - Redis Disabled)
```json
GET http://localhost:8080/actuator/health

{
  "status": "UP",
  "components": {
    "database": {
      "status": "UP",
      "details": {
        "database": "H2",
        "version": "2.2.224 (2024-03-14)",
        "url": "jdbc:h2:mem:omsdb",
        "validationQuery": "SELECT 1"
      }
    },
    "db": {
      "status": "UP",
      "details": {
        "database": "H2",
        "validationQuery": "SELECT 1"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500000000000,
        "free": 250000000000,
        "threshold": 10485760,
        "path": "/home/.",
        "exists": true
      }
    },
    "externalAPI": {
      "status": "UP",
      "details": {
        "service": "External Payment API",
        "url": "http://localhost:9090/health",
        "httpStatus": 200,
        "responseTime": "simulated"
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

#### Semua Komponen UP (Staging/Prod Profile - Redis Enabled)
```json
{
  "status": "UP",
  "components": {
    "database": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "version": "15.4",
        "url": "jdbc:postgresql://localhost:5432/oms_staging",
        "validationQuery": "SELECT 1"
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "service": "Redis",
        "ping": "PONG"
      }
    }
  }
}
```

#### Database DOWN
```json
{
  "status": "DOWN",
  "components": {
    "database": {
      "status": "DOWN",
      "details": {
        "reason": "Connection refused: localhost:5432"
      }
    }
  }
}
```

### 7. Cara Verifikasi

#### a. Via curl
```bash
# Health check lengkap
curl http://localhost:8080/actuator/health | jq .

# Cek komponen spesifik
curl http://localhost:8080/actuator/health/database
curl http://localhost:8080/actuator/health/redis
curl http://localhost:8080/actuator/health/externalAPI
```

#### b. Via browser
```
http://localhost:8080/actuator/health
```

#### c. Via Docker health check
```yaml
# docker-compose.yml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 3
```

### 8. Best Practices

1. **Gunakan timeout yang tepat** - Jangan biarkan health check blocking terlalu lama
2. **Conditional beans** - RedisHealthIndicator menggunakan `@ConditionalOnProperty` agar tidak aktif jika Redis disabled
3. **Logging yang informatif** - Log level WARN untuk failure, DEBUG untuk success
4. **Sanitasi informasi sensitif** - Database URL di-sanitasi untuk menyembunyikan password
5. **Jangan overload external API** - Health check jangan terlalu frequent ke external service
6. **HTTP Status mapping** - DOWN menghasilkan 503 agar load balancer bisa merespon dengan tepat

### 9. Troubleshooting

| Masalah | Penyebab | Solusi |
|---|---|---|
| Redis component tidak muncul | Redis disabled | Set `spring.data.redis.enabled=true` |
| External API selalu DOWN | Mock payment server tidak running | Jalankan mock: `docker run -d -p 9090:8080 ariefwara/mock-bi-snap` atau override `HEALTH_URL` |
| Health endpoint 404 | Endpoint tidak di-expose | Cek `management.endpoints.web.exposure.include` |
| Connection timeout | Database/Redis tidak running | Start service atau cek koneksi jaringan |