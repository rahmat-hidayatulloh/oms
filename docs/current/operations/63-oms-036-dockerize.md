# OMS-036: Dockerize Spring Boot Application

> **Status:** Current
> **Audience:** Operator, Maintainer
> **Owner:** Engineering
> **Last verified:** 2026-08-23 (screening kode aktual `src/`, `pom.xml`)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

> **Tanggal:** 7 Agustus 2026
> **Tujuan:** Membuat Dockerfile untuk aplikasi Spring Boot OMS dengan multi-stage build, layer caching, healthcheck, dan non-root user.
> **Status:** ✅ **COMPLETED**
> **Referensi:** `docs/current/overview/architecture.md`

---

## 1. RINGKASAN

Dockerfile dibuat untuk meng-containerize aplikasi Spring Boot OMS. Dockerfile menggunakan **multi-stage build** untuk menghasilkan image yang kecil dan optimal, dengan **layer caching** untuk mempercepat build, **healthcheck** untuk monitoring, dan **non-root user** untuk keamanan.

### Acceptance Criteria

| No | Kriteria | Status |
|----|----------|:---:|
| 1 | Multi-stage Dockerfile | ✅ |
| 2 | Base image: eclipse-temurin:21-jre | ✅ |
| 3 | Layer caching untuk dependency | ✅ |
| 4 | Healthcheck configuration | ✅ |
| 5 | Docker image bisa di-build dan di-run | ✅ |

---

## 2. FILES

| File | Deskripsi |
|------|-----------|
| `Dockerfile` | Multi-stage Dockerfile untuk build dan runtime |
| `.dockerignore` | File exclusion untuk build context |
| `docs/current/operations/63-oms-036-dockerize.md` | Dokumentasi ini |

---

## 3. DOCKERFILE

### 3.1 Struktur Multi-Stage

```
┌─────────────────────────────────────────────────────────────┐
│                    STAGE 1: BUILDER                          │
│  Base: eclipse-temurin:21-jdk-alpine                        │
│  ├── Copy Maven wrapper + pom.xml                           │
│  ├── Download dependencies (layer caching)                  │
│  ├── Copy source code + config                              │
│  ├── Build JAR (mvnw package -DskipTests)                   │
│  └── Extract Spring Boot layers                             │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    STAGE 2: RUNTIME                          │
│  Base: eclipse-temurin:21-jre-alpine                        │
│  ├── Install curl (healthcheck)                             │
│  ├── Create non-root user (oms)                             │
│  ├── Copy layers (dependencies, loader, snapshot, app)      │
│  ├── HEALTHCHECK configuration                              │
│  ├── EXPOSE 8080                                            │
│  └── ENTRYPOINT: java $JAVA_OPTS JarLauncher                │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 Dockerfile Content

```dockerfile
# ============================================================
# Dockerfile - OMS Application
# ============================================================
# Multi-stage build:
#   Stage 1: Build  (eclipse-temurin:21-jdk-alpine)
#   Stage 2: Runtime (eclipse-temurin:21-jre-alpine)
# ============================================================

# ---------- Stage 1: Build ----------
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper & pom.xml for dependency caching
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Download dependencies (layer caching)
# This layer is cached unless pom.xml changes
RUN ./mvnw dependency:go-offline -B -q

# Copy source code and config (checkstyle, spotbugs)
COPY src src
COPY config config

# Build the application
# Skip tests to keep image smaller; tests run in CI pipeline
RUN ./mvnw package -DskipTests -B -q

# Extract layers for Spring Boot layered JAR
RUN java -Djarmode=layertools -jar target/*.jar extract --destination extracted

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:21-jre-alpine AS runtime

# Add curl for healthcheck
RUN apk add --no-cache curl

# Create non-root user for security
RUN addgroup -S oms && adduser -S oms -G oms

WORKDIR /app

# Copy layers from builder stage (optimized for caching)
# Order matters: least frequently changed layers first
COPY --from=builder /build/extracted/dependencies/ ./
COPY --from=builder /build/extracted/spring-boot-loader/ ./
COPY --from=builder /build/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/extracted/application/ ./

# Create directory for logs
RUN mkdir -p /app/logs && chown -R oms:oms /app

# Switch to non-root user
USER oms:oms

# Healthcheck configuration
# Use /api/version endpoint (public, no auth required)
# Actuator on separate port 8081 has known Spring Security issues
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/version || exit 1

# Expose application port
EXPOSE 8080

# Default JVM options
ENV JAVA_OPTS="-XX:+UseZGC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
```

---

## 4. DOCKERIGNORE

### 4.1 Content

```dockerignore
# ============================================================
# .dockerignore - OMS Application
# ============================================================

# Build output
target/
*.class
*.jar
*.war

# IDE files
.idea/
*.iml
.project
.classpath
.settings/
.vscode/

# OS files
.DS_Store
Thumbs.db
*.swp
*~

# Logs
logs/
*.log
*.log.gz

# Environment / Sensitive files
.env
.env.*.local
application-local.yml

# Test reports
test-output/
surefire-reports/
jacoco.exec

# Temp files
tmp/
temp/
*.tmp
*.bak

# Git
.git/
.gitignore

# Docker
docker-compose*.yml
Dockerfile

# Node
node_modules/
package.json
package-lock.json

# Documentation (not needed at runtime)
docs/
```

### 4.2 Manfaat

- **Image lebih kecil** — file yang tidak dibutuhkan tidak masuk build context
- **Build lebih cepat** — Docker tidak perlu mengirim file yang di-ignore
- **Keamanan** — file sensitif (.env, application-local.yml) tidak masuk image
- **Cache lebih efektif** — perubahan file yang di-ignore tidak invalidate cache

---

## 5. LAYER CACHING

### 5.1 Strategi Layer Caching

```
Layer 1: .mvn + mvnw + pom.xml     → Cached (hanya berubah jika pom.xml berubah)
Layer 2: dependency:go-offline     → Cached (hanya berubah jika pom.xml berubah)
Layer 3: src + config               → Cached (hanya berubah jika source berubah)
Layer 4: mvnw package               → Cached (hanya berubah jika source berubah)
Layer 5: extract layers             → Cached (hanya berubah jika JAR berubah)
```

### 5.2 Spring Boot Layered JAR

Spring Boot 3.x mendukung **layered JAR** yang memisahkan:

| Layer | Isi | Frekuensi Perubahan |
|-------|-----|---------------------|
| `dependencies` | Library pihak ketiga | Jarang |
| `spring-boot-loader` | Spring Boot loader classes | Sangat jarang |
| `snapshot-dependencies` | Snapshot dependencies | Kadang-kadang |
| `application` | Kode aplikasi | Sering |

Dengan menyalin layer secara terpisah, Docker hanya perlu rebuild layer yang berubah.

---

## 6. HEALTHCHECK

### 6.1 Konfigurasi

```dockerfile
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/version || exit 1
```

### 6.2 Parameter

| Parameter | Nilai | Deskripsi |
|-----------|-------|-----------|
| `--interval` | 30s | Cek health setiap 30 detik |
| `--timeout` | 10s | Timeout 10 detik per cek |
| `--start-period` | 60s | Grace period 60 detik saat startup |
| `--retries` | 3 | 3 kali gagal sebelum dianggap unhealthy |

### 6.3 Endpoint Health

Healthcheck menggunakan endpoint `/api/version` (public, tanpa autentikasi):
- `200 OK` jika aplikasi sehat
- `Connection refused` jika aplikasi tidak sehat

> **Catatan:** Healthcheck menggunakan `/api/version` karena endpoint `/actuator/health` pada management port terpisah (8081) memiliki isu dengan Spring Security (`Failed to find servlet [dispatcherServletRegistration]`).

---

## 7. SECURITY

### 7.1 Non-Root User

```dockerfile
RUN addgroup -S oms && adduser -S oms -G oms
USER oms:oms
```

**Manfaat:**
- Aplikasi tidak berjalan sebagai root
- Mengurangi risiko jika container di-compromise
- Best practice untuk production

### 7.2 JVM Security

```dockerfile
ENV JAVA_OPTS="-XX:+UseZGC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
```

- `-XX:+UseZGC` — Garbage collector modern untuk low latency
- `-XX:MaxRAMPercentage=75.0` — Batasi penggunaan RAM container
- `-Djava.security.egd=file:/dev/./urandom` — Secure random yang lebih cepat

---

## 8. CARA MENGGUNAKAN

### 8.1 Build Image

```bash
# Build image
docker build -t oms:latest .

# Build dengan tag versi
docker build -t oms:1.0.0 .

# Build dengan build args
docker build \
  --build-arg JAR_FILE=target/oms-0.0.1-SNAPSHOT.jar \
  -t oms:latest .
```

### 8.2 Run Container

```bash
# Run dengan dev profile (H2 in-memory)
docker run -d \
  --name oms-dev \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e JWT_SECRET=your-dev-secret-min-32-chars \
  oms:latest

# Run dengan staging profile (PostgreSQL + Redis)
docker run -d \
  --name oms-staging \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=staging \
  -e DB_HOST=postgres \
  -e DB_PORT=5432 \
  -e DB_NAME=omsdb_staging \
  -e DB_USERNAME=sa_staging \
  -e DB_PASSWORD=your-staging-password \
  -e JWT_SECRET=your-staging-secret-min-32-chars \
  -e REDIS_HOST=redis \
  -e REDIS_PORT=6379 \
  oms:latest

# Run dengan prod profile
docker run -d \
  --name oms-prod \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_HOST=prod-db \
  -e DB_PORT=5433 \
  -e DB_NAME=omsdb_prod \
  -e DB_USERNAME=sa_prod \
  -e DB_PASSWORD=your-prod-password \
  -e JWT_SECRET=your-prod-secret-min-32-chars \
  -e REDIS_HOST=prod-redis \
  -e REDIS_PORT=6379 \
  oms:latest
```

### 8.3 Docker Compose

```yaml
# docker-compose.yml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: omsdb
      POSTGRES_USER: oms
      # Nilai dari environment/GitHub Actions secret, bukan hardcode
      POSTGRES_PASSWORD: ${DB_PASSWORD:?set DB_PASSWORD via GitHub Actions secret}
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U oms"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  mock-payment:
    image: ariefwara/mock-bi-snap
    ports:
      - "9090:8080"
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost:8080/health"]
      interval: 10s
      timeout: 5s
      retries: 5

  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: staging
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: omsdb
      DB_USERNAME: oms
      DB_PASSWORD: ${DB_PASSWORD:?set DB_PASSWORD via GitHub Actions secret}
      JWT_SECRET: ${JWT_SECRET:?set JWT_SECRET via GitHub Actions secret}
      REDIS_HOST: redis
      REDIS_PORT: 6379
      HEALTH_URL: http://localhost:9090/health
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
      mock-payment:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/version"]
      interval: 30s
      timeout: 10s
      retries: 3

volumes:
  postgres-data:
```

### 8.4 Verify Container

```bash
# Cek status container
docker ps

# Cek logs
docker logs oms-dev

# Cek health status
docker inspect --format='{{.State.Health.Status}}' oms-dev

# Test API
curl http://localhost:8080/api/version
```

---

## 9. ENVIRONMENT VARIABLES

### 9.1 Variables yang Didukung

| Variable | Default | Deskripsi |
|----------|---------|-----------|
| `SPRING_PROFILES_ACTIVE` | `dev` | Spring profile aktif |
| `DB_HOST` | `localhost` | Host database |
| `DB_PORT` | `5432` | Port database |
| `DB_NAME` | `omsdb` | Nama database |
| `DB_USERNAME` | `oms` | Username database |
| `DB_PASSWORD` | - | Password database |
| `JWT_SECRET` | - | Secret key untuk JWT (wajib) |
| `JWT_EXPIRATION` | `3600000` | Expiration access token (ms) |
| `REDIS_HOST` | `localhost` | Host Redis |
| `REDIS_PORT` | `6379` | Port Redis |
| `REDIS_PASSWORD` | - | Password Redis |
| `REDIS_DATABASE` | `0` | Database Redis index |
| `JAVA_OPTS` | `-XX:+UseZGC...` | JVM options |

### 9.2 Sesuai dengan env/omsdb-*.properties

| Profile | DB_NAME | DB_USERNAME | DB_PASSWORD | JWT_SECRET | REDIS_DATABASE |
|---------|---------|-------------|-------------|------------|----------------|
| **dev** | - (H2) | - | - | `<your-dev-secret>` | 0 |
| **staging** | `omsdb_staging` | `sa_staging` | `<your-staging-password>` | `<your-staging-secret>` | 1 |
| **prod** | `omsdb_prod` | `sa_prod` | `<your-prod-password>` | `<your-prod-secret>` | 2 |

---

## 10. BEST PRACTICES

### 10.1 Do's

- ✅ Gunakan **multi-stage build** untuk image yang kecil
- ✅ Gunakan **layer caching** untuk build yang cepat
- ✅ Gunakan **non-root user** untuk keamanan
- ✅ Gunakan **healthcheck** untuk monitoring
- ✅ Gunakan **Spring Boot layered JAR** untuk optimalisasi
- ✅ Gunakan **.dockerignore** untuk build context yang kecil
- ✅ Gunakan **environment variables** untuk konfigurasi
- ✅ Gunakan **JVM flags** yang optimal untuk container

### 10.2 Don'ts

- ❌ Jangan copy seluruh project ke image
- ❌ Jangan jalankan sebagai root
- ❌ Jangan hardcode credentials di Dockerfile
- ❌ Jangan skip healthcheck
- ❌ Jangan gunakan image yang tidak di-pin versinya

---

## 11. TROUBLESHOOTING

| Masalah | Penyebab | Solusi |
|---------|----------|--------|
| Build lambat | Dependency download | Gunakan layer caching, `dependency:go-offline` |
| Image besar | Tidak pakai multi-stage | Gunakan multi-stage build |
| Container tidak start | JWT_SECRET tidak di-set | Set `JWT_SECRET` environment variable |
| Healthcheck gagal | Aplikasi belum siap | Naikkan `--start-period` |
| Port conflict | Port 8080 sudah dipakai | Ganti port mapping `-p 8081:8080` |
| Database connection refused | DB container belum siap | Gunakan `depends_on` + healthcheck |
| Healthcheck 500 di 8081 | Actuator management port terpisah | Gunakan `/api/version` untuk healthcheck |

---

## 12. KESIMPULAN

Dockerfile untuk OMS telah berhasil dibuat dan diverifikasi dengan:

- ✅ **Multi-stage build** — builder (JDK) + runtime (JRE)
- ✅ **Base image** `eclipse-temurin:21-jre-alpine` — kecil dan aman
- ✅ **Layer caching** — dependency di-cache terpisah dari source
- ✅ **Healthcheck** — `/api/version` dengan interval 30s (verified healthy)
- ✅ **Non-root user** — `oms:oms` untuk keamanan
- ✅ **Spring Boot layered JAR** — optimalisasi layer
- ✅ **.dockerignore** — build context yang kecil
- ✅ **Environment variables** — konfigurasi per environment
- ✅ **Docker Compose** — orchestration dengan PostgreSQL + Redis
- ✅ **Build verified** — image berhasil di-build dan container berjalan healthy
- ✅ **API verified** — `curl /api/version` dan `POST /auth/login` berhasil

**Status:** ✅ **COMPLETED**

---

*Dokumen ini dibuat pada 7 Agustus 2026*
