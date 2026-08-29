# Disaster Recovery Plan — OMS

> **Status:** Current
> **Audience:** Operator, Engineering Lead, On-call Engineer
> **Owner:** Engineering / Operations
> **Last verified:** 2026-08-29 (prosedur recovery dikonfirmasi sesuai konfigurasi Kubernetes aktual)
> **Source of truth:** `k8s/`, `docs/ops/SLO.md`, `docs/ops/IncidentResponse.md`

## Recovery Time Objective (RTO) and Recovery Point Objective (RPO)

| Metric | Target | Strategy |
|---|---|---|
| **RTO** (Recovery Time Objective) | < 15 minutes | Automated failover, health checks, Kubernetes self-healing |
| **RPO** (Recovery Point Objective) | < 1 hour | Hourly PostgreSQL backups + WAL archiving |

## Backup Strategy

### Database (PostgreSQL)
- **Full backup:** Daily at 02:00 UTC
- **WAL archiving:** Continuous (enables point-in-time recovery)
- **Backup retention:** 30 days
- **Backup location:** S3-compatible storage with cross-region replication
- **Restore test:** Monthly — restore to staging and verify data integrity

### Redis
- **Strategy:** Redis is used for ephemeral data (rate limits, refresh tokens, cache)
- **Recovery:** Redis rebuilds from application state on restart
- **No backup required** — refresh tokens re-issued, rate limits reset, cache warms up

### Application Configuration
- **Strategy:** All configuration stored in Git (infrastructure as code)
- **Recovery:** Rebuild and deploy from Git

## Recovery Procedures

### Scenario 1: Single Pod Failure
- **Detection:** Kubernetes liveness/readiness probe failure
- **Recovery:** Kubernetes auto-restarts the pod (typically < 30s)
- **Action:** Monitor; escalate if recurring

### Scenario 2: Database Failure
- **Detection:** Application health check fails, PostgreSQL unavailable
- **Recovery:**
  1. If replica available: promote replica to primary
  2. If no replica: restore from latest backup
  3. Verify application connectivity
  4. Resume traffic
- **Max downtime:** 15 minutes (RTO)

### Scenario 3: Full Cluster Failure
- **Detection:** All pods down, load balancer returning 502/503
- **Recovery:**
  1. Provision new cluster (IaC via Kustomize manifests)
  2. Restore database from latest backup
  3. Redeploy application
  4. Verify data integrity
- **Max downtime:** 30 minutes (for full cluster recovery)

### Scenario 4: Data Corruption
- **Detection:** Anomalous data patterns, customer reports
- **Recovery:**
  1. Stop writes (scale down write-serving pods)
  2. Identify corruption scope via audit logs
  3. Restore to point-in-time before corruption
  4. Replay valid transactions from application logs
  5. Verify and resume

## Communication Plan

| Stakeholder | Notification Channel | Timing |
|---|---|---|
| On-call engineer | PagerDuty | Immediately |
| Engineering lead | Slack #incidents | Within 5 minutes |
| Product owner | Email | Within 15 minutes |
| Affected customers | Status page | Within 30 minutes |

## Testing

- **DR drill:** Quarterly
- **Backup restore test:** Monthly
- **Failover test:** Monthly (promote replica)
- **Chaos engineering:** Bi-weekly (kill random pods)
