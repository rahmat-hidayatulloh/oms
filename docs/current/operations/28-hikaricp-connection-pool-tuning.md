# OMS-025: Tuning HikariCP Connection Pool

> **Status:** Current
> **Audience:** Operator, Maintainer
> **Owner:** Engineering
> **Last verified:** 2026-08-28 (screening kode aktual `src/`, `pom.xml`)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## Tujuan
Optimasi database connection pool menggunakan HikariCP untuk meningkatkan performa aplikasi di setiap environment (dev, staging, production).

## Acceptance Criteria
- [x] Configure HikariCP di application-{profile}.yml
- [x] Profile dev: minimumIdle=2, maximumPoolSize=5
- [x] Profile staging: minimumIdle=5, maximumPoolSize=15
- [x] Profile prod: minimumIdle=10, maximumPoolSize=30
- [x] connectionTimeout=30000, idleTimeout=600000

## Detail Pekerjaan

### 1. Latar Belakang

HikariCP adalah connection pool default di Spring Boot 3.x yang sangat cepat, ringan, dan reliable. Dengan tuning yang tepat, kita bisa:

- **Mengurangi latency** koneksi database dengan menyediakan koneksi siap pakai
- **Mencegah connection leak** dengan timeout dan leak detection
- **Mengoptimalkan resource** sesuai kapasitas masing-masing environment
- **Menghindari overhead** pembuatan koneksi baru setiap request

### 2. Parameter HikariCP yang Di-tune

| Parameter | Nilai | Penjelasan |
|---|---|---|
| `minimumIdle` | 2/5/10 | Jumlah minimum koneksi idle yang dipertahankan |
| `maximumPoolSize` | 5/15/30 | Jumlah maksimum koneksi dalam pool |
| `connectionTimeout` | 30000ms | Waktu maksimum menunggu koneksi dari pool |
| `idleTimeout` | 600000ms (10 menit) | Waktu maksimum koneksi idle sebelum dihapus |
| `maxLifetime` | 1200000ms (20 menit) | Waktu maksimum koneksi sebelum di-recycle |
| `poolName` | Oms{Profile}Pool | Nama pool untuk identifikasi di log/metrics |
| `leakDetectionThreshold` | 300000/600000/60000 | Threshold deteksi connection leak (dev/staging/prod) |
| `connectionTestQuery` | SELECT 1 | Query untuk validasi koneksi |

### 3. Konfigurasi Per Profile

#### a. Dev Profile (application-dev.yml)

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 2
      maximum-pool-size: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1200000
      pool-name: OmsDevPool
      leak-detection-threshold: 300000
      connection-test-query: SELECT 1
```

**Rationale:**
- `minimumIdle=2`: Cukup untuk development single-user
- `maximumPoolSize=5`: Tidak perlu banyak koneksi untuk H2 in-memory
- `leakDetectionThreshold=300000` (5 menit): Deteksi dini connection leak saat development
- Database: H2 In-Memory (tidak perlu banyak koneksi)

#### b. Staging Profile (application-staging.yml)

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 5
      maximum-pool-size: 15
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1200000
      pool-name: OmsStagingPool
      leak-detection-threshold: 600000
      connection-test-query: SELECT 1
```

**Rationale:**
- `minimumIdle=5`: Siap melayani beberapa test user
- `maximumPoolSize=15`: Cukup untuk beban staging (beberapa concurrent user)
- `leakDetectionThreshold=600000` (10 menit): Masih perlu deteksi leak
- Database: PostgreSQL (Docker)

#### c. Production Profile (application-prod.yml)

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 10
      maximum-pool-size: 30
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1200000
      pool-name: OmsProdPool
      leak-detection-threshold: 60000
      connection-test-query: SELECT 1
```

**Rationale:**
- `minimumIdle=10`: Siap melayani traffic minimal
- `maximumPoolSize=30`: Optimal untuk beban production (berdasarkan formula: Tn * (Cm - 1) + 1)
- `leakDetectionThreshold=60000`: Deteksi dini connection leak di production (60 detik) — pantau false positive via monitoring metrics
- Database: PostgreSQL (Production)

### 4. Formula Menentukan Pool Size

```
Pool Size = Tn * (Cm - 1) + 1

Dimana:
- Tn = jumlah thread (core CPU)
- Cm = waktu koneksi / waktu proses

Contoh untuk production (8 core):
- Rata-rata query: 50ms
- Rata-rata proses: 5ms
- Cm = 50/5 = 10
- Pool Size = 8 * (10 - 1) + 1 = 73

Namun, maximumPoolSize=30 sudah cukup untuk sebagian besar kasus.
```

### 5. Perbandingan Profile

| Parameter | Dev | Staging | Production |
|---|---|---|---|
| **Database** | H2 In-Memory | PostgreSQL (Docker) | PostgreSQL (Production) |
| **minimumIdle** | 2 | 5 | 10 |
| **maximumPoolSize** | 5 | 15 | 30 |
| **connectionTimeout** | 30000ms | 30000ms | 30000ms |
| **idleTimeout** | 600000ms | 600000ms | 600000ms |
| **maxLifetime** | 1200000ms | 1200000ms | 1200000ms |
| **poolName** | OmsDevPool | OmsStagingPool | OmsProdPool |
| **leakDetectionThreshold** | 300000ms | 600000ms | 60000ms |
| **connectionTestQuery** | SELECT 1 | SELECT 1 | SELECT 1 |

### 6. Cara Verifikasi

#### a. Via Actuator Health Endpoint

```bash
# Cek status koneksi database
curl http://localhost:8080/actuator/health
```

Response:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "H2",
        "validationQuery": "SELECT 1"
      }
    }
  }
}
```

#### b. Via Actuator Metrics

```bash
# Cek metrics connection pool
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
curl http://localhost:8080/actuator/metrics/hikaricp.connections.idle
curl http://localhost:8080/actuator/metrics/hikaricp.connections.max
curl http://localhost:8080/actuator/metrics/hikaricp.connections.pending
```

#### c. Via Logging

Aktifkan DEBUG logging untuk HikariCP:
```yaml
logging:
  level:
    com.zaxxer.hikari: DEBUG
```

### 7. Best Practices

1. **Jangan set maximumPoolSize terlalu besar** - Lebih banyak koneksi tidak selalu lebih cepat. 30 sudah optimal untuk kebanyakan aplikasi.
2. **Set maxLifetime lebih kecil dari timeout database** - PostgreSQL default `tcp_keepalives_idle` biasanya 2 jam. Set maxLifetime ke 20 menit (1200000ms) untuk aman.
3. **Gunakan connectionTestQuery** - Pastikan koneksi valid sebelum digunakan. `SELECT 1` sangat ringan.
4. **Monitor metrics secara berkala** - Pantau `hikaricp.connections.active` dan `hikaricp.connections.pending` untuk mendeteksi bottleneck.
5. **Production memakai `leakDetectionThreshold=60000`** - Deteksi dini connection leak sebelum pool habis. Pantau false positive lewat metrics monitoring (`hikaricp.*`).

### 8. Troubleshooting

| Masalah | Penyebab | Solusi |
|---|---|---|
| `Connection is not available, request timed out` | Pool habis | Naikkan maximumPoolSize atau optimasi query |
| `Connection leak detected` | Koneksi tidak di-close | Cek transaction management, pastikan @Transactional digunakan dengan benar |
| `Too many connections` | Pool terlalu besar | Turunkan maximumPoolSize |
| `Connection reset` | maxLifetime > server timeout | Turunkan maxLifetime |

### 9. Cara Menjalankan

```bash
# Dev (default - H2)
./mvnw spring-boot:run

# Staging (Postgres)
# Semua nilai rahasia diambil dari environment / GitHub Actions secrets,
# jangan pernah hardcode di script atau docs.
DB_HOST=localhost DB_PORT=5432 DB_NAME=oms_staging \
DB_USERNAME=oms_user DB_PASSWORD=${DB_PASSWORD} \
JWT_SECRET=${JWT_SECRET} \
REDIS_HOST=localhost REDIS_PORT=6379 \
./mvnw spring-boot:run -Dspring-boot.run.profiles=staging

# Production (Postgres)
DB_HOST=prod-db.example.com DB_PORT=5432 DB_NAME=oms_prod \
DB_USERNAME=oms_admin DB_PASSWORD=${DB_PASSWORD} \
JWT_SECRET=${JWT_SECRET} \
REDIS_HOST=prod-redis.example.com REDIS_PORT=6379 REDIS_PASSWORD=${REDIS_PASSWORD} \
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```