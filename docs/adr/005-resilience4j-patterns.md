# ADR-005: Resilience4j for Circuit Breaker and Retry

- **Status:** Accepted
- **Date:** 2026-08-15 (V3 Phase V3-1)
- **Deciders:** Development Team

## Context

The `ExternalAPIHealthIndicator` makes HTTP calls to an external Payment API. If the Payment API is down or slow, the health check blocks indefinitely, potentially exhausting thread pools and causing cascading failures.

## Decision

Adopt **Resilience4j** (`resilience4j-spring-boot3` v2.1.0) with programmatic decoration:

- **Circuit Breaker:** `paymentApi` instance — 50% failure rate threshold, 10s wait in Open state, sliding window of 10 calls. Opens after 5 consecutive failures; half-open after 10s.
- **Retry:** `paymentApi` instance — 3 max attempts, 500ms wait, exponential backoff (×1.5).

Programmatic decoration is used instead of annotation-based AOP because `HealthIndicator.health()` does not declare checked exceptions, making `@CircuitBreaker` annotation incompatible.

## Consequences

- Payment API failures are short-circuited after threshold — health check returns quickly.
- Retry handles transient failures (network blips, temporary overloads).
- Circuit breaker state is exposed via Actuator (`/actuator/circuitbreakers`, `/actuator/circuitbreakerevents`).
- Trade-off: Programmatic decoration is less declarative than annotations but functionally identical.

## Alternatives Considered

- **Annotation-based `@CircuitBreaker`:** Rejected — `HealthIndicator.health()` cannot propagate checked exceptions.
- **Hystrix:** Rejected — Netflix OSS is in maintenance mode; Resilience4j is the standard.
- **Manual timeout + try-catch:** Rejected — no metrics, no circuit state management.