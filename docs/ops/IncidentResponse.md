# Incident Response Runbook — OMS

> **Status:** Current
> **Audience:** On-call Engineer, SRE, Engineering Lead
> **Owner:** Engineering / Operations
> **Last verified:** 2026-08-29 (severity levels, kubectl commands, dan alert thresholds dikonfirmasi)
> **Source of truth:** `docs/ops/SLO.md`, `docs/ops/DRP.md`, `k8s/`, Micrometer alert config

## Severity Levels

| Level | Description | Response Time | Examples |
|---|---|---|---|
| **P1 — Critical** | Service down, data loss, security breach | < 15 min | API returning 500s, DB unreachable, auth bypass |
| **P2 — High** | Major feature degraded, significant user impact | < 1 hour | Payment API failures, slow response times |
| **P3 — Medium** | Minor feature issue, workaround available | < 4 hours | Cache miss causing stale data, non-critical endpoint errors |
| **P4 — Low** | Cosmetic issue, no user impact | Next business day | Log noise, minor UI glitches |

## Incident Response Flow

```
1. DETECT    → Monitoring alert or user report
2. TRIAGE    → Assess severity, assign incident commander
3. MITIGATE  → Stop the bleeding (rollback, scale, circuit breaker)
4. INVESTIGATE → Root cause analysis
5. RESOLVE   → Deploy fix
6. REVIEW    → Post-mortem within 48 hours
```

## Common Scenarios and Runbooks

### Scenario: API Returning 500 Errors

1. **Check:** Application logs for stack traces
   ```bash
   kubectl logs -l app=oms --tail=100 --since=5m
   ```
2. **Check:** Database connectivity
   ```bash
   curl http://localhost:8081/actuator/health
   ```
3. **Check:** Circuit breaker state
   ```bash
   curl http://localhost:8081/actuator/circuitbreakers
   ```
4. **Mitigate:** If database issue, check connection pool
5. **Mitigate:** If Redis issue, check Redis connectivity
6. **Escalate:** If unresolvable in 15 minutes

### Scenario: High Latency / Slow Responses

1. **Check:** p95 latency on Grafana
2. **Check:** Thread pool saturation
   ```bash
   curl http://localhost:8081/actuator/metrics/jvm.threads.live
   ```
3. **Check:** Database slow queries (HikariCP metrics)
4. **Mitigate:** Scale horizontally (kubectl scale deployment)
5. **Mitigate:** Check for N+1 queries or missing indexes

### Scenario: Circuit Breaker OPEN

1. **Check:** Which dependency is failing
   ```bash
   curl http://localhost:8081/actuator/circuitbreakers
   ```
2. **Check:** Downstream service health
3. **Mitigate:** If downstream is down, wait for recovery (automatic retry after 10s)
4. **Mitigate:** If persistent, disable circuit breaker temporarily (emergency only)

### Scenario: Authentication Failures Spike

1. **Check:** Security logs for failed login attempts
2. **Check:** Rate limiting metrics
3. **Mitigate:** If brute force, increase rate limit for affected IPs
4. **Escalate:** If credential stuffing, force password reset for affected accounts

## Escalation Matrix

| Level | Contact | Method |
|---|---|---|
| L1 | On-call engineer | PagerDuty |
| L2 | Engineering lead | Slack DM |
| L3 | CTO / VP Engineering | Phone call |

## Post-Mortem Template

```markdown
## Incident: [Title]
- **Date:** YYYY-MM-DD
- **Duration:** X minutes
- **Severity:** P1/P2/P3/P4
- **Incident Commander:** Name

### Timeline
- HH:MM — Alert triggered
- HH:MM — Investigation started
- HH:MM — Root cause identified
- HH:MM — Fix deployed
- HH:MM — Full recovery confirmed

### Root Cause
[Description of root cause]

### Impact
- Users affected: X
- Data loss: Yes/No
- Revenue impact: $X

### Resolution
[What was done to fix]

### Action Items
- [ ] Action 1 (Owner, Due date)
- [ ] Action 2 (Owner, Due date)

### Lessons Learned
- What went well:
- What could be improved:
```
