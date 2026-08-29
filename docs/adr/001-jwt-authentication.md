# ADR-001: JWT Authentication with Refresh Token Rotation

- **Status:** Accepted
- **Date:** 2026-08-11 (V2 Phase 0)
- **Deciders:** Development Team

## Context

The OMS application needs stateless authentication for a REST API consumed by a React frontend. Session-based auth does not fit the SPA + API architecture. Token management must support refresh without re-authentication and must prevent token reuse after logout.

## Decision

Adopt **JWT (JSON Web Token)** using the `jjwt` library (v0.12.5) with two token types:

1. **Access token** — short-lived (1 hour, `JWT_EXPIRATION:3600000` ms), carries user identity and role.
2. **Refresh token** — longer-lived (7 days, `JWT_REFRESH_EXPIRATION:604800000` ms), stored in Redis for lifecycle management.

Refresh token lifecycle:
- **Rotation:** Each refresh issues a new refresh token; the old one is atomically deleted.
- **Replay detection:** If a reused refresh token is detected, all tokens for that session are revoked.
- **Revocation on logout:** Refresh token is deleted from Redis.
- **Type enforcement:** `JwtAuthenticationFilter` rejects tokens where `token_type ≠ access`.

## Consequences

- Stateless access tokens eliminate session store for auth.
- Redis-backed refresh tokens enable distributed revocation across pods.
- Token-type enforcement prevents refresh tokens from being used as access tokens.
- Trade-off: JWT payload is opaque to the server; revocation of access tokens relies on short TTL.

## Alternatives Considered

- **Session-based auth (Spring Session):** Rejected — requires sticky sessions or shared session store; not ideal for SPA.
- **Opaque tokens only:** Rejected — stateless verification not possible; requires DB lookup per request.
- **Opaque access + refresh tokens:** Considered — JWT chosen for stateless verification and audit trail.