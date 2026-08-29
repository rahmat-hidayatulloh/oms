# ADR-002: Database Per Profile (H2 Dev / PostgreSQL Staging+Prod)

- **Status:** Accepted
- **Date:** 2026-08-11 (V2 Phase 0)
- **Deciders:** Development Team

## Context

The application needs fast local development without external dependencies, while staging and production require a production-grade relational database.

## Decision

Use **H2 in-memory database** for `dev` and `test` profiles, and **PostgreSQL 16** for `staging` and `prod` profiles.

- `application-dev.yml` → H2 (`jdbc:h2:mem:omsdb`)
- `application-staging.yml` → PostgreSQL (external)
- `application-prod.yml` → PostgreSQL (external)
- `application-test.yml` → H2 (`jdbc:h2:mem:testdb`)

Flyway migrations (`V1`–`V7`) use H2-compatible syntax (`CONSTRAINT pk_xxx PRIMARY KEY (id)`) to run identically across all profiles. Spring Data JPA `ddl-auto=validate` ensures schema consistency.

## Consequences

- Developers need zero setup — no Docker required for basic development.
- Flyway ensures schema parity across environments.
- Trade-off: H2 has minor SQL syntax differences from PostgreSQL (mitigated by using compatible syntax).
- Trade-off: H2 lacks some PostgreSQL-specific features (e.g., `LISTEN/NOTIFY`); integration tests use Testcontainers for PostgreSQL when needed.

## Alternatives Considered

- **PostgreSQL everywhere (via Docker Compose):** Rejected — adds friction for new developers.
- **Testcontainers for all profiles:** Considered — too slow for dev hot-reload cycle.