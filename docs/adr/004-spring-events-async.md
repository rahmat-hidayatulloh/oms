# ADR-004: Spring Application Events for Async Processing

- **Status:** Accepted
- **Date:** 2026-08-11 (V2 Phase 0)
- **Deciders:** Development Team

## Context

The Order creation flow triggers side effects: sending confirmation emails and updating analytics. These operations should not block the HTTP response to the client.

## Decision

Use **Spring `ApplicationEventPublisher`** with `@Async` listeners for decoupled, non-blocking side effects.

- `OrderCreatedEvent` is published after order persistence.
- `OrderCreatedEventListener` handles email notification asynchronously.
- Async executor is configured via `@EnableAsync` and `TaskConfig` (thread pool: 5 core, 10 max).

## Consequences

- Order creation response returns immediately; side effects happen in background.
- Event-driven architecture enables adding new listeners without modifying the order flow (OCP).
- Trade-off: Async failures are not visible to the caller; requires monitoring (SLF4J + Micrometer metrics).
- Trade-off: In monolith, events are in-process (not distributed); no persistence guarantees for events.

## Alternatives Considered

- **Synchronous email in request:** Rejected — adds latency, blocks thread.
- **Message queue (RabbitMQ/Kafka):** Overkill for current scale; Spring Events sufficient for monolith.
- **`@Async` without events:** Considered — events provide cleaner decoupling and testability.