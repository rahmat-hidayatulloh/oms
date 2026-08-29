# Service Level Objectives — OMS

> **Status:** Current
> **Audience:** Operator, Engineering Lead, SRE
> **Owner:** Engineering / Operations
> **Last verified:** 2026-08-29 (metric targets dan tooling dikonfirmasi sesuai konfigurasi aktual)
> **Source of truth:** `src/main/resources/application*.yml`, `k8s/base/`, Micrometer config

## Overview

This document defines the Service Level Indicators (SLIs) and Service Level Objectives (SLOs) for the OMS application.

## SLIs and SLOs

| Metric | SLI | SLO | Measurement |
|---|---|---|---|
| **Availability** | Uptime of API endpoints (excluding planned maintenance) | 99.9% monthly | `/actuator/health` probe success rate |
| **Latency (p95)** | 95th percentile response time for order creation | < 500ms | Micrometer timer on `OrderController` |
| **Latency (p99)** | 99th percentile response time | < 1000ms | Micrometer timer on `OrderController` |
| **Error Rate** | 5xx responses / total responses | < 0.1% | Structured access logs |
| **Durability** | Order data persistence | 99.999% | PostgreSQL replication + backups |

## Error Budget

- Monthly error budget: 43 minutes (0.1% of 30 days)
- When error budget is exhausted: freeze non-critical deploys, prioritize reliability

## Monitoring

| Tool | Purpose | Endpoint |
|---|---|---|
| Spring Actuator | Health check, metrics | `/actuator/health`, `/actuator/prometheus` |
| Micrometer | Custom business metrics | Order count, auth failures, cache hits |
| Structured Logging | Request tracing | MDC: traceId, userId, method, path |
| Circuit Breaker | Dependency health | `/actuator/circuitbreakers` |

## Alerting Rules

| Condition | Severity | Action |
|---|---|---|
| Availability < 99.5% (1hr window) | P1 — Critical | Page on-call, investigate immediately |
| p95 latency > 1s (5min window) | P2 — Warning | Investigate, may scale horizontally |
| Error rate > 1% (10min window) | P1 — Critical | Page on-call, check logs and traces |
| Circuit breaker OPEN > 30s | P2 — Warning | Check downstream dependency health |
| Disk usage > 80% | P3 — Info | Scale up or clean up |
