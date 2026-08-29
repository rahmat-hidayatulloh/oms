# Task 3: Multi-Profile Configuration

> **Status:** Current
> **Audience:** Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-28 (screening kode aktual `src/`, `pom.xml`)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## Tujuan
Membuat konfigurasi untuk profile dev (H2), staging (Postgres), dan parent config.

## Acceptance Criteria
- [x] application.yml (parent)
- [x] application-dev.yml (H2 in-memory)
- [x] application-staging.yml (Postgres Docker)
- [x] JWT secret dan expiration di config
- [x] Logging pattern untuk development

## Detail Pekerjaan

### 1. Struktur File Konfigurasi
```
src/main/resources/
├── application.yml              ← Parent config (shared)
├── application-dev.yml          ← H2 In-Memory (development)
├── application-staging.yml      ← PostgreSQL Docker (staging)
└── (application-test.yml di test/resources)  ← H2 for testing
```

### 2. Parent Config (application.yml)
Berisi konfigurasi shared yang digunakan oleh semua profile:

| Konfigurasi | Nilai | Keterangan |
|---|---|---|
| `server.port` | 8080 | Port aplikasi |
| `spring.profiles.active` | dev | Default profile |
| `spring.jpa.open-in-view` | false | Mencegah LazyInitializationException |
| `spring.jpa.properties.hibernate.jdbc.batch_size` | 50 | Batch processing untuk performa (`order_inserts`/`order_updates` = true) |
| `jwt.secret` | `${JWT_SECRET:?required}` | Wajib via env pada non-dev (fail-fast; V4-09); dev memakai secret khusus ≥32 char (HS256) |
| `jwt.expiration` | `${JWT_EXPIRATION:3600000}` | 1 jam default (refresh token 7 hari via `JWT_REFRESH_EXPIRATION`) |
| `management.endpoints.web.exposure.include` | health,info,metrics,prometheus,circuitbreakers,circuitbreakerevents | Actuator endpoints (`management.server.port` = 8081) |
| `spring.jackson.time-zone` | Asia/Jakarta | Timezone |

### 3. Dev Profile (application-dev.yml)
Untuk development lokal dengan H2 in-memory database:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:omsdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: validate     # Schema dikelola Flyway (create-drop ditinggalkan)
    show-sql: true
  # Flyway manages the schema; Hibernate only validates it.
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true

  # H2 Console (hanya untuk dev)
  h2:
    console:
      enabled: true
      path: /h2-console

  # Redis disabled for dev
  data:
    redis:
      enabled: false

logging:
  level:
    com.oms: DEBUG
    org.hibernate.SQL: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %highlight(%-5level) %cyan(%logger{36}) - %msg%n"
```

**Fitur khusus dev:**
- H2 Console: `http://localhost:8080/h2-console`
- DDL auto: `validate` (schema dibuat oleh Flyway, bukan Hibernate)
- Logging: DEBUG dengan warna (highlight, cyan)
- Redis: disabled
- JWT secret: hardcoded untuk dev

### 4. Staging Profile (application-staging.yml)
Untuk staging dengan PostgreSQL via Docker:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate       # Validasi schema saja
    show-sql: false

logging:
  file:
    name: logs/oms-staging.log
    max-size: 10MB
    max-history: 7
```

**Fitur khusus staging:**
- Database: PostgreSQL via environment variables
- DDL auto: `validate` (tidak mengubah schema)
- Redis: enabled via env
- Logging: INFO level + file rotation
- JWT secret: wajib dari environment variable

### 5. Perbandingan Profile

| Fitur | Dev | Staging |
|---|---|---|
| **Database** | H2 In-Memory | PostgreSQL |
| **DDL Auto** | validate (Flyway) | validate (Flyway) |
| **Redis** | Disabled | Enabled (via env) |
| **H2 Console** | ✅ Enabled | ❌ Disabled |
| **JWT Secret** | Hardcoded | Environment variable |
| **Logging Level** | DEBUG + SQL TRACE | INFO |
| **Logging Output** | Console (colorized) | Console + File |
| **Connection Pool** | 5 max | 15 max |

**Prod:** `minimumIdle=10`, `maximumPoolSize=30`, Redis enabled (Lettuce pool `max-active=16`, `min-idle=4`), logging `com.oms=WARN`, JWT strict via `JWT_SECRET` — lihat `application-prod.yml`.

### 6. Cara Menjalankan

```bash
# Dev (default - H2)
./mvnw spring-boot:run

# Dev (eksplisit)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Staging (Postgres)
# Nilai rahasia (DB_PASSWORD, JWT_SECRET, REDIS_PASSWORD) diambil dari
# environment / GitHub Actions secrets — jangan pernah hardcode di docs.
DB_HOST=localhost DB_PORT=5432 DB_NAME=oms_staging \
DB_USERNAME=oms_user DB_PASSWORD=${DB_PASSWORD} \
JWT_SECRET=${JWT_SECRET} \
REDIS_HOST=localhost REDIS_PORT=6379 \
./mvnw spring-boot:run -Dspring-boot.run.profiles=staging
```