# OMS — Deployment Guide

> **Status:** Current guide; production assumptions require environment verification
> **Audience:** Operator, Developer, Maintainer
> **Owner:** Engineering / Operations
> **Last verified:** 2026-08-29 (path dan konfigurasi repository dikonfirmasi; `docker-compose.yml` ditambahkan)
> **Source of truth:** `Dockerfile`, `docker-compose.yml`, `k8s/`, `env/`, `src/main/resources/`, dan `pom.xml`
> **Version:** 1.1
> **Last Updated:** 29 Agustus 2026

---

## Prerequisites

- **Java 21+** (Temurin/OpenJDK recommended)
- **Maven** (or use `./mvnw` wrapper)
- **Docker** (for containerized deployment)
- **Kubernetes** (minikube for local cluster)
- **Redis 7+** (for caching in staging/prod)

---

## Local Development

### Quick Start

```bash
git clone <repo-url>
cd oms
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Application starts at `http://localhost:8080`.

### With Redis (Staging)

```bash
docker run -d -p 6379:6379 redis:7-alpine
./mvnw spring-boot:run -Dspring-boot.run.profiles=staging
```

### Run Tests

```bash
./mvnw test -Dspring.profiles.active=dev
./mvnw checkstyle:check spotbugs:check
./mvnw jacoco:report
```

---

## Docker Deployment

### Build & Run

```bash
docker build -t oms .
docker run -p 8080:8080 -e SPRING_PROFILES_ACTIVE=dev oms
```

### Docker Compose

```yaml
version: '3.8'
services:
  app:
    build: .
    ports: ["8080:8080"]
    environment:
      - SPRING_PROFILES_ACTIVE=staging
      - JWT_SECRET=${JWT_SECRET}
      - DB_HOST=postgres
      - DB_PASSWORD=${DB_PASSWORD}
      - REDIS_HOST=redis
    depends_on:
      postgres: { condition: service_healthy }
      redis: { condition: service_healthy }
  postgres:
    image: postgres:16-alpine
    environment:
      - POSTGRES_DB=omsdb
      - POSTGRES_USER=oms
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes: [pgdata:/var/lib/postgresql/data]
    healthcheck: { test: ["CMD","pg_isready","-U","oms"], interval: 10s, timeout: 5s, retries: 5 }
  redis:
    image: redis:7-alpine
    healthcheck: { test: ["CMD","redis-cli","ping"], interval: 10s, timeout: 5s, retries: 5 }
volumes: { pgdata: }
```

---

## Kubernetes Deployment (Minikube)

```bash
minikube start --cpus=4 --memory=8192
eval $(minikube docker-env)
docker build -t oms:latest .

# Deploy via Kustomize overlay per environment (V4-06)
kubectl apply -k k8s/overlays/dev       # H2 in-memory, no external deps
kubectl apply -k k8s/overlays/staging   # PostgreSQL + Redis
kubectl apply -k k8s/overlays/prod      # PostgreSQL + Redis
```

> **Catatan:** Service menggunakan ClusterIP. Akses via `kubectl port-forward` atau Ingress (TLS).
> Base manifest ada di `k8s/base/`; overlay di `k8s/overlays/<env>/` meng-inject `SPRING_PROFILES_ACTIVE` dan melayer datastore untuk staging/prod.

### Verify

```bash
kubectl get pods -n oms-system
kubectl logs -n oms-system deployment/oms
kubectl get hpa -n oms-system
kubectl get ingress -n oms-system
kubectl get networkpolicy -n oms-system
kubectl get resourcequota -n oms-system
```

---

### Production Considerations

#### Secrets (GitHub Actions)

Semua secret dikelola via **GitHub Actions secrets** (`Settings → Secrets and variables → Actions`):

| Secret | Digunakan untuk |
|--------|-----------------|
| `ADMIN_PASSWORD` | Admin user seed (Flyway V5, di-hash BCrypt) |
| `DB_PASSWORD` | Database password (PostgreSQL) |
| `JWT_SECRET` | JWT signing key (≥32 chars) |
| `REDIS_PASSWORD` | Redis password (staging/prod) |

Workflow `.github/workflows/ci-cd.yml` mereferensikan `secrets.DB_PASSWORD`, `secrets.JWT_SECRET`,
`secrets.REDIS_PASSWORD`, dst. di job deploy. Untuk job yang menjalankan `seed-data.sh` / Newman,
referensikan juga `secrets.ADMIN_PASSWORD` (dan `secrets.CUSTOMER_PASSWORD`) — nilai secret
tidak pernah ditulis liter di repo/dokumentasi.

### Security
- JWT Secret: ≥32 chars, store in Kubernetes Secret
- Database: Strong passwords, rotate regularly
- TLS/SSL: Configure ingress with certificate
- Secrets: Use Vault for production

### High Availability
- HPA: CPU 70%, Memory 80% auto-scaling
- PDB: Min 1 pod during disruptions
- Replicas: Min 2 for production

### Monitoring
- Health: `/actuator/health`
- Metrics: Prometheus at `/actuator/metrics`
- Logging: Structured JSON logs

---

## Environment Variables

| Variable | Required | Default | Description |
|----------|:--------:|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | ❌ | `dev` | Active profile |
| `JWT_SECRET` | ✅ | — | JWT signing key (≥32 chars) |
| `JWT_EXPIRATION` | ❌ | 3600000 | JWT token TTL (ms) |
| `DB_HOST` | ❌ | localhost | Database host |
| `DB_PORT` | ❌ | 5432 | Database port |
| `DB_NAME` | ❌ | omsdb | Database name |
| `DB_USERNAME` | ❌ | oms | Database user |
| `DB_PASSWORD` | ✅ | — | Database password |
| `REDIS_HOST` | ❌ | localhost | Redis host |
| `REDIS_PORT` | ❌ | 6379 | Redis port |
| `REDIS_PASSWORD` | ❌ | — | Redis password (staging/prod) |
| `RATE_LIMIT_DISTRIBUTED` | ❌ | false | Distributed rate limiting via Redis (requires Redis) |
| `HEALTH_URL` | ❌ | `http://localhost:9090/health` | Payment API health URL (default: local mock server `ariefwara/mock-bi-snap`) |
| `ADMIN_PASSWORD` | ✅ (seed) | — | Password admin yang di-hash oleh Flyway V5. **Jangan di-hardcode**; set via `secrets.ADMIN_PASSWORD` di GitHub Actions / env |

> **Sekuriti:** Semua nilai rahasia (`DB_PASSWORD`, `JWT_SECRET`, `REDIS_PASSWORD`, `ADMIN_PASSWORD`) disuntikkan dari
> **GitHub Actions secrets** (`secrets.*`) saat CI/CD, atau dari environment/secret manager saat deploy manual.
> Tidak ada secret plaintext yang boleh dicantumkan di dalam repository/dokumentasi.

---

## Health Checks

| Endpoint | Description | Auth |
|----------|-------------|:----:|
| `/actuator/health` | Overall status | ❌ |
| `/actuator/info` | App info | ❌ |
| `/actuator/metrics` | Prometheus metrics | ✅ |

Custom indicators: DatabaseHealthIndicator, RedisHealthIndicator, ExternalAPIHealthIndicator.
`ExternalAPIHealthIndicator` memeriksa payment API di `HEALTH_URL` (default `http://localhost:9090/health`,
server mock `ariefwara/mock-bi-snap`).

---

## Troubleshooting

### App Won't Start
- Check Java 21+: `java -version`
- Check port 8080: `lsof -i :8080`
- Check Redis (staging): `redis-cli ping`

### Tests Fail
- Use `-Dspring.profiles.active=dev` for H2
- Ensure Redis not required for dev tests

### Kubernetes Issues
- `minikube status`
- `kubectl logs -n oms-system <pod-name>`
- `kubectl get pvc -n oms-system`