# OMS — Order Management System

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=java)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.12-6DB33F?logo=spring)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring_Security-6-6DB33F?logo=springsecurity)](https://spring.io/projects/spring-security)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis)](https://redis.io/)
[![Flyway](https://img.shields.io/badge/Flyway-managed-CC0200?logo=flyway)](https://flywaydb.org/)

[![Docker](https://img.shields.io/badge/Docker-ready-2496ED?logo=docker)](https://www.docker.com/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-k8s-326CE5?logo=kubernetes)](https://kubernetes.io/)
[![CI/CD](https://github.com/rahmat-hidayatulloh/oms/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/rahmat-hidayatulloh/oms/actions)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-3-85EA2D?logo=swagger)](docs/current/api/64-oms-037-swagger-openapi.md)
[![JUnit](https://img.shields.io/badge/JUnit-5-25A162?logo=junit5)](https://junit.org/junit5/)
[![Secret scanning](https://img.shields.io/badge/secret_scanning-gitleaks-4C4C4C)](.github/workflows/ci-cd.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Security Policy](https://img.shields.io/badge/Security-Policy-red.svg)](SECURITY.md)

A production-grade **Order Management System (OMS)** built with Spring Boot 3.2.12,
featuring rate limiting, caching, health checks, and Kubernetes-native deployment.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Quick Start](#quick-start)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)
- [Configuration](#configuration)
- [Deployment](#deployment)
- [Testing](#testing)
- [Contributing](#contributing)
- [Security](#security)
- [License](#license)

## Overview

OMS provides RESTful APIs for managing orders and products, with
built-in security, rate limiting, Redis caching, and comprehensive monitoring.
It follows a **package-by-feature architecture** ([ADR-006](docs/adr/006-package-by-feature.md))
with clear separation of concerns — each feature (`auth`, `order`, `product`, `admin`, `audit`)
owns its own `controller`, `service`, `dto`, `entity`, and `repository`.

> 📋 **Technical reviewers:** See [PORTFOLIO-HIGHLIGHTS](docs/current/overview/PORTFOLIO-HIGHLIGHTS.md)
> for a concise summary of engineering decisions, security posture, and SRE docs.
> Full doc index at [docs/index.md](docs/index.md).

## Features

- **Order Management**: Create, read, soft-delete orders with stock validation
- **Product Management**: CRUD with dynamic filtering, sorting, pagination (QueryDSL)
- **Rate Limiting**: Per-endpoint rate limiting with trusted proxy support
- **Caching**: Redis-based caching for products and authentication tokens
- **Security**: JWT-based auth, refresh tokens, input sanitization
- **Monitoring**: Actuator health checks, custom metrics, Prometheus integration
- **Resilience**: Retry with backoff for optimistic locking conflicts
- **Audit Logging**: Track all admin operations
- **API Versioning**: URL-based versioning (`/api/v1/`)

## Tech Stack

| Category         | Technology                                     |
|------------------|------------------------------------------------|
| **Runtime**      | Java 21, Spring Boot 3.2.12, Spring Security 6  |
| **Database**     | PostgreSQL (prod/staging), H2 (dev)            |
| **Cache**        | Redis (Lettuce client)                         |
| **ORM**          | Hibernate 6 + QueryDSL 5                       |
| **Migration**    | Flyway                                         |
| **API Docs**     | OpenAPI/Swagger (springdoc-openapi)            |
| **Testing**      | JUnit 5, Mockito, Testcontainers               |
| **Quality**      | Checkstyle, SpotBugs, JaCoCo                   |
| **Deployment**   | Docker, Kubernetes, Kustomize                  |
## Quick Start

### Option A: Docker Compose (Recommended — app + PostgreSQL + Redis)

```bash
# 1. Copy environment config & isi nilai secret
cp .env.example .env
# Edit .env: JWT_SECRET, DB_PASSWORD, ADMIN_PASSWORD

# 2. Build & start all services
docker-compose up -d

# 3. Verify health
curl http://localhost:8080/api/version
```

> See [docker-compose.yml](docker-compose.yml) for full config.

### Option B: Local Dev (H2 in-memory, no external services)

### Prerequisites

- Java 21+
- Docker & Docker Compose (optional, for Redis)

### Run Locally (Dev Profile)

```bash
# Clone the repository
git clone <repo-url>
cd oms

# (Opsional) Salin template environment — isi nilai rahasia dari secret manager
cp .env.example .env

# Build and run with H2 in-memory database
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The application starts at `http://localhost:8080`.

### Run with Redis

```bash
# Start Redis
docker run -d -p 6379:6379 redis:7-alpine

# Start mock payment server (untuk /actuator/health ExternalAPIHealthIndicator)
docker run -d -p 9090:8080 ariefwara/mock-bi-snap

# Run with staging profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=staging
```

## API Documentation

Dokumentasi lengkap tersedia di [Documentation Index](docs/index.md), dengan pintasan berikut:

- [API index](docs/current/api/api-index.md)
- [Architecture](docs/current/overview/architecture.md)
- [Deployment guide](docs/current/operations/deployment-guide.md)
- [Testing guides](docs/current/development/testing/32-testcontainers-integration.md)
- [Development standards](docs/current/development/standards/STANDAR-DEVELOPMENT-CODE.md)

### Base URL

```
http://localhost:8080/api/v1
```

### Endpoints

| Method | Path                          | Description              | Auth Required |
|--------|-------------------------------|--------------------------|:-------------:|
| POST   | `/api/v1/auth/register`       | Register new account     | ❌            |
| POST   | `/api/v1/auth/login`          | Authenticate             | ❌            |
| POST   | `/api/v1/auth/refresh`        | Refresh access token     | ❌            |
| POST   | `/api/v1/auth/logout`         | Invalidate refresh token | ✅            |
| GET    | `/api/v1/auth/csrf`           | CSRF token               | ❌            |
| POST   | `/api/v1/orders`              | Create order             | ✅            |
| GET    | `/api/v1/orders/{id}`         | Get order by ID          | ✅            |
| GET    | `/api/v1/orders`              | List orders (paginated)  | ✅ (ADMIN)    |
| DELETE | `/api/v1/orders/{id}`         | Soft-delete order        | ✅            |
| POST   | `/api/v1/products`            | Create product           | ✅ (ADMIN)    |
| GET    | `/api/v1/products/{id}`       | Get product by ID        | ✅            |
| GET    | `/api/v1/products`            | List all products        | ✅            |
| PUT    | `/api/v1/products/{id}`       | Update product           | ✅ (ADMIN)    |
| DELETE | `/api/v1/products/{id}`       | Soft-delete product      | ✅ (ADMIN)    |
| GET    | `/api/v1/products/search`     | Search products          | ✅            |
| GET    | `/api/v1/admin/dashboard`     | Dashboard stats          | ✅ (ADMIN)    |
| GET    | `/api/v1/admin/users`         | List users               | ✅ (ADMIN)    |
| GET    | `/api/version`                | App version              | ❌            |
| GET    | `/actuator/health`            | Spring Actuator health   | ❌            |

### Error Responses

All errors follow **RFC 7807 Problem Details** format:

```json
{
  "type": "https://api.oms.com/errors/404",
  "title": "Not Found",
  "status": 404,
  "detail": "Order not found with id: 123e4567-e89b-12d3-a456-426614174000",
  "instance": "/api/v1/orders/123e4567-e89b-12d3-a456-426614174000",
  "timestamp": "2026-08-10T09:00:00"
}
```
## Project Structure

```
src/
├── main/java/com/oms/
│   ├── auth/                # Feature auth (controller, service, dto, entity, repository, security, validation)
│   ├── order/               # Feature order (controller, service, dto, entity, repository)
│   ├── product/             # Feature product (controller, service, dto, entity, repository)
│   ├── admin/               # Feature admin (controller, service, dto)
│   ├── audit/               # Feature audit (@Auditable, aspect, entity, repository, service)
│   ├── ratelimit/           # Feature rate limiting (interceptor, service, stores, properties)
│   ├── system/              # Endpoint app-level (CsrfController, VersionController)
│   ├── validation/          # Validasi lintas-fitur (SafeHtml, InputSanitizer)
│   ├── config/              # Spring configuration classes (lintas-fitur)
│   ├── constant/            # Constants lintas-fitur
│   ├── exception/           # Exception handlers (domain-split) & ErrorCode
│   ├── health/              # Custom health indicators
│   ├── entity/              # BaseEntity (shared persistence base)
│   └── OmsApplication.java
├── main/resources/
│   ├── db/migration/        # Flyway migration scripts
│   └── application*.yml     # Environment-specific configs
└── test/java/com/oms/       # Unit & integration tests (mirror struktur feature)
k8s/                         # Kubernetes manifests (Kustomize)
.github/workflows/           # CI/CD pipeline (GitHub Actions)
```

## Configuration

### Environment Variables

Template untuk semua environment variable tersedia di [`.env.example`](.env.example) — salin ke `.env` dan isi nilai rahasia hanya dari environment/secret manager, **jangan commit nilai asli**.

| Variable            | Required | Default    | Description              |
|---------------------|:--------:|------------|--------------------------|
| `JWT_SECRET`        | ✅       | —          | JWT signing key (≥32 chars) |
| `JWT_EXPIRATION`    | ❌       | 3600000    | JWT token TTL (ms)       |
| `DB_HOST`           | ❌       | localhost  | Database host            |
| `DB_PORT`           | ❌       | 5432       | Database port            |
| `DB_NAME`           | ❌       | omsdb      | Database name            |
| `DB_USERNAME`       | ❌       | oms        | Database user            |
| `DB_PASSWORD`       | ❌       | —          | Database password        |
| `REDIS_HOST`        | ❌       | localhost  | Redis host               |
| `REDIS_PORT`        | ❌       | 6379       | Redis port               |
| `REDIS_PASSWORD`    | ❌       | (kosong)   | Redis password           |
| `HEALTH_URL`        | ❌       | `http://localhost:9090/health` | Payment API health URL (default: mock server `ariefwara/mock-bi-snap`) |
| `ADMIN_PASSWORD`    | ✅       | —          | Admin user seed (Flyway V5). Set via `secrets.ADMIN_PASSWORD` — jangan hardcode |

> **Secrets:** Nilai rahasia (`ADMIN_PASSWORD`, `DB_PASSWORD`, `JWT_SECRET`, `REDIS_PASSWORD`, dst.)
> dikelola lewat **GitHub Actions secrets** (`secrets.*`) di CI/CD, atau environment/secret manager saat deploy manual.
> Jangan pernah menuliskan nilai secret di dokumentasi/commit.

### Profiles

| Profile    | Database     | Redis | Caching |
|------------|-------------|:-----:|:-------:|
| `dev`      | H2 (in-mem) | ❌    | ❌      |
| `staging`  | PostgreSQL  | ✅    | ✅      |
| `prod`     | PostgreSQL  | ✅    | ✅      |

## Deployment

### Docker

```bash
# Build image
docker build -t oms .

# Run container (secret via environment/GitHub Actions secret, jangan hardcode)
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=staging \
  -e JWT_SECRET=$JWT_SECRET \
  -e DB_PASSWORD=$DB_PASSWORD \
  oms
```

### Kubernetes (Minikube)

```bash
# Start minikube
minikube start

# Deploy via Kustomize overlay per environment (V4-06)
kubectl apply -k k8s/overlays/dev       # H2 in-memory
kubectl apply -k k8s/overlays/staging   # PostgreSQL + Redis
kubectl apply -k k8s/overlays/prod      # PostgreSQL + Redis

# Access the application
minikube service oms-service -n oms-system
```

### Operational Docs

| Doc | Link |
|-----|------|
| Deployment Guide | [docs/current/operations/deployment-guide.md](docs/current/operations/deployment-guide.md) |
| Docker | [docs/current/operations/63-oms-036-dockerize.md](docs/current/operations/63-oms-036-dockerize.md) |
| Kubernetes | [docs/current/operations/65-oms-038-minikube.md](docs/current/operations/65-oms-038-minikube.md) |
| **SLO** (99.9% availability target) | [docs/ops/SLO.md](docs/ops/SLO.md) |
| **Disaster Recovery Plan** (RTO <15min) | [docs/ops/DRP.md](docs/ops/DRP.md) |
| **Incident Response Runbook** | [docs/ops/IncidentResponse.md](docs/ops/IncidentResponse.md) |
| Benchmark Results | [docs/current/operations/benchmark_results.md](docs/current/operations/benchmark_results.md) |

## Testing

```bash
# Unit tests
./mvnw test -Dspring.profiles.active=dev

# Code quality checks
./mvnw checkstyle:check spotbugs:check

# Coverage report
./mvnw jacoco:report
open target/site/jacoco/index.html
```

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feat/my-feature`
3. Commit changes: `git commit -am 'feat: add my feature'`
4. Push: `git push origin feat/my-feature`
5. Open a Pull Request

### Commit Convention

We follow [Conventional Commits](https://www.conventionalcommits.org/):

- `feat:` — new feature
- `fix:` — bug fix
- `refactor:` — code refactoring
- `test:` — adding/updating tests
- `docs:` — documentation
- `chore:` — maintenance tasks

See [CONTRIBUTING.md](CONTRIBUTING.md) for full guidelines including documentation governance.

## Security

OMS implements layered security: JWT authentication with refresh token rotation, Bucket4j
rate limiting (distributed via Redis), Spring Security RBAC, CSRF protection, input
sanitization, and container hardening (non-root, `capabilities.drop: ALL`).

**To report a vulnerability,** please read [SECURITY.md](SECURITY.md) and use
**GitHub Private Security Advisories** — do not open a public Issue for security bugs.

CI pipeline includes OWASP Dependency-Check, Trivy image scan, Gitleaks secret scan,
and CycloneDX SBOM generation on every build.

## License

This project is licensed under the **MIT License** — see [LICENSE](LICENSE) for full text.

Copyright © 2026 OMS Contributors.

