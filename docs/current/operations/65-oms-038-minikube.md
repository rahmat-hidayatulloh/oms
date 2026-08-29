# OMS-038: Deploy ke Minikube (Kubernetes)

> **Status:** Current
> **Audience:** Operator, Maintainer
> **Owner:** Engineering
> **Last verified:** 2026-08-28 (screening kode aktual `src/`, `pom.xml`)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

> **Tanggal:** 7 Agustus 2026
> **Pembaruan terakhir:** 13 Agustus 2026
> **Tujuan:** Deploy aplikasi Spring Boot OMS ke Minikube dengan Kubernetes manifests hardened production-ready.
> **Status:** ✅ **COMPLETED**
> **Referensi:** `docs/current/operations/63-oms-036-dockerize.md`, `env/omsdb-*.properties`, `docs/current/overview/architecture.md`, `docs/history/optimalisasi-v2/ANALISIS-PROYEK-OMS-V2.md`

---

## 1. RINGKASAN

Membuat dan meng-hardening Kubernetes manifests untuk mendeploy aplikasi OMS ke Minikube. Mencakup Deployment, Service (ClusterIP), ConfigMap, Secret (placeholder), Ingress (TLS/HSTS), NetworkPolicy, RBAC, ResourceQuota, LimitRange, HPA, PDB, dan infrastruktur pendukung (PostgreSQL, Redis). Desain mendukung multi-environment via Kustomize. Semua manifest telah di-hardening sesuai Section 9 Analisis V2 (Fase 0–2).

### Acceptance Criteria

| No | Kriteria | Status |
|----|----------|:---:|
| 1 | Deployment.yaml | ✅ **DIBUAT** — 2 replicas, RollingUpdate, health probes, resource limits, container securityContext, ServiceAccount, volumeMounts |
| 2 | Service.yaml (ClusterIP) | ✅ **DIBUAT** — ClusterIP type, HTTP port 8080, Actuator port 8081; akses via Ingress |
| 3 | ConfigMap untuk environment | ✅ **DIBUAT** — app config, JWT expiry, Redis config, logging level |
| 4 | Secret untuk credential (placeholder) | ✅ **DIBUAT** — Placeholder `<CHANGE_ME>`; secret literal dihapus dari Git (F-03) |
| 5 | Ingress (TLS/HSTS) | ✅ **DIBUAT** — TLS, ssl-redirect, HSTS, whitelist-source-range |
| 6 | NetworkPolicy | ✅ **DIBUAT** — Default-deny ingress + allow dari ingress controller; egress terbatas (DNS, PostgreSQL, Redis) |
| 7 | RBAC (ServiceAccount/Role/RoleBinding) | ✅ **DIBUAT** — `oms-service-account` dengan `automountServiceAccountToken: false` |
| 8 | ResourceQuota + LimitRange | ✅ **DIBUAT** — Namespace-level resource caps dan default container limits |
| 9 | kubectl kustomize k8s/ valid | ✅ **DIVERIFIKASI** — 14 resource dirender (dev), 17 resource (staging/prod) |
| 10 | Aplikasi diakses via ingress/port-forward | ✅ **TERKONFIGURASI** — ClusterIP + Ingress (TLS) atau `kubectl port-forward` |

---

## 2. STRUKTUR FILE

### 2.1 Directory Structure

```
k8s/
├── base/                           # Environment-agnostic base manifests (V4-06)
│   ├── kustomization.yaml          # Kustomize base configuration
│   ├── namespace.yaml              # Namespace: oms-system
│   ├── configmap.yaml              # Environment configuration (non-sensitive)
│   ├── secret.yaml                 # Credentials: JWT, DB, Redis (PLACEHOLDER — no real values)
│   ├── serviceaccount.yaml         # ServiceAccount: oms-service-account (automountServiceAccountToken: false)
│   ├── role.yaml                   # Role: minimal RBAC (empty rules)
│   ├── rolebinding.yaml            # RoleBinding: bind Role to ServiceAccount
│   ├── deployment.yaml             # App deployment (2 replicas, health probes, container hardening)
│   ├── service.yaml                # ClusterIP service (port 8080 + 8081)
│   ├── ingress.yaml                # Ingress: TLS, HSTS, ssl-redirect, whitelist-source-range
│   ├── hpa.yaml                    # Horizontal Pod Autoscaler (CPU 70%)
│   ├── pdb.yaml                    # Pod Disruption Budget (min 1 available)
│   ├── networkpolicy.yaml          # NetworkPolicy: default-deny + allow ingress/egress
│   ├── resourcequota.yaml          # ResourceQuota: namespace-level resource caps
│   └── limitrange.yaml             # LimitRange: default container limits
├── components/
│   └── datastore/                  # PostgreSQL + Redis (layered by staging/prod overlays)
│       └── kustomization.yaml
└── overlays/
    ├── dev/                        # H2 in-memory, no external dependencies
    │   └── kustomization.yaml
    ├── staging/                    # PostgreSQL + Redis + staging profile
    │   └── kustomization.yaml
    └── prod/                       # PostgreSQL + Redis + prod profile
        └── kustomization.yaml
```itrange.yaml             # LimitRange: default container resource limits
├── postgres.yaml               # PostgreSQL deployment + service (optional)
└── redis.yaml                  # Redis deployment + service (optional)
```

### 2.2 File Descriptions

| File | Kind | Deskripsi |
|------|------|-----------|
| `namespace.yaml` | Namespace | `oms-system` dengan labels Kubernetes recommended |
| `configmap.yaml` | ConfigMap | 20+ konfigurasi aplikasi (app.*, JWT_EXPIRATION, REDIS_HOST, dll) |
| `secret.yaml` | Secret | Placeholder `<CHANGE_ME>` — secret literal dihapus dari Git; real values per environment via External Secrets/Vault |
| `serviceaccount.yaml` | ServiceAccount | `oms-service-account` + `automountServiceAccountToken: false` |
| `role.yaml` | Role | RBAC minimal (rules kosong; aplikasi tidak butuh K8s API access) |
| `rolebinding.yaml` | RoleBinding | Bind `oms-role` ke `oms-service-account` |
| `deployment.yaml` | Deployment | 2 replicas, RollingUpdate, 3 health probes, container securityContext, resource limits, volumeMounts |
| `service.yaml` | Service | ClusterIP: 8080 (HTTP), 8081 (Actuator) |
| `ingress.yaml` | Ingress | nginx ingress: TLS, ssl-redirect, HSTS, whitelist-source-range |
| `hpa.yaml` | HPA | Auto-scale: CPU 70%, min 2, max 10 replicas |
| `pdb.yaml` | PDB | minAvailable: 1 — garansi 1 pod aktif saat maintenance |
| `networkpolicy.yaml` | NetworkPolicy | Default-deny ingress; allow dari ingress-nginx namespace; egress ke DNS/PostgreSQL/Redis |
| `resourcequota.yaml` | ResourceQuota | Namespace: 4 CPU / 8Gi request; 8 CPU / 16Gi limit; max 20 pods |
| `limitrange.yaml` | LimitRange | Container default 256Mi/250m; max 2Gi/2 CPU; min 128Mi/100m |
| `postgres.yaml` | Deployment + Service | PostgreSQL 16 Alpine, pg_isready probes, emptyDir volume |
| `redis.yaml` | Deployment + Service | Redis 7 Alpine, redis-cli ping probes, ClusterIP |
| `kustomization.yaml` | Kustomization | Entry point, namespace injection, common labels, 14 resources (dev) / 17 (staging/prod) |

---

## 3. DETAIL MANIFESTS

### 3.1 Namespace

**File:** `k8s/base/namespace.yaml`

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: oms-system
  labels:
    app.kubernetes.io/name: oms
    app.kubernetes.io/part-of: oms
```

### 3.2 ConfigMap

**File:** `k8s/base/configmap.yaml`

Berisi 18 kunci konfigurasi non-sensitive. Key rate-limit memakai **nama property yang benar-benar dibaca** `RateLimitProperties` (`app.rate-limit.*`), bukan nama lama (`app.rate-limit.max-requests-per-minute` / `window-ms`) yang tidak ter-bind ke kode:

| Key | Value | Source |
|-----|-------|--------|
| `app.retry.max-attempts` | `3` | `application.yml` (`app.retry.*`) |
| `app.retry.backoff-delay-ms` | `100` | `application.yml` (`app.retry.*`) |
| `app.rate-limit.enabled` | `true` | `app.rate-limit.*` (RateLimitProperties) |
| `app.rate-limit.defaults.capacity` | `10` | `app.rate-limit.*` (RateLimitProperties) |
| `app.rate-limit.defaults.refill-tokens` | `10` | `app.rate-limit.*` (RateLimitProperties) |
| `app.rate-limit.defaults.refill-period` | `1m` | `app.rate-limit.*` (RateLimitProperties) |
| `JWT_EXPIRATION` | `3600000` | `application.yml` (1 jam) |
| `JWT_REFRESH_EXPIRATION` | `604800000` | `application.yml` (7 hari) |
| `REDIS_HOST` | `redis-service` | Service DNS name |
| `REDIS_PORT` | `6379` | Service DNS name |
| `REDIS_DATABASE` | `0` | Redis DB index |
| `logging.level.com.oms` | `INFO` | `application.yml` |
| `SPRING_PROFILES_ACTIVE` | `dev` / `staging` / `prod` | Di-inject via Kustomize overlay (V4-06) |

Key lain yang ikut di-ConfigMap: `app.payment.api.health-url`, `app.health-check.timeout`, `app.redis.blacklist-duration`, `app.redis.cache.ttl-default`, `app.email.thread-pool.core-size`, `app.email.thread-pool.max-size`, `app.email.queue.capacity`, `app.pagination.default-page-size`.

### 3.3 Secret

**File:** `k8s/base/secret.yaml`

Berisi placeholder — **bukan credential nyata** (F-03 remediasi):

| Key | Nilai default | Notes |
|-----|---------------|-------|
| `JWT_SECRET` | `<CHANGE_ME_MIN_32_CHARS>` | Minimal 32 karakter; ganti per environment |
| `DB_HOST` | `<CHANGE_ME>` | Service DNS name (mis. `postgres-service`) |
| `DB_PORT` | `<CHANGE_ME>` | Default PostgreSQL: `5432` |
| `DB_NAME` | `<CHANGE_ME>` | Database name (mis. `omsdb`) |
| `DB_USERNAME` | `<CHANGE_ME>` | PostgreSQL user |
| `DB_PASSWORD` | `<CHANGE_ME>` | PostgreSQL password |
| `REDIS_PASSWORD` | `<CHANGE_ME_OR_EMPTY>` | Redis password atau kosong |

> ⚠️ **Security Notice:** Nilai literal dihapus dari Git (F-03). Secret nyata harus diinject per environment via External Secrets Operator, HashiCorp Vault, Sealed Secrets, atau CI/CD secret manager. Jangan commit real values ke repository.

### 3.4 ConfigMap & Secret Injection Mechanism

Aplikasi menggunakan `envFrom` di Deployment untuk meng-inject ConfigMap dan Secret sebagai environment variable:

```yaml
envFrom:
  - configMapRef:
      name: oms-config      # → env var dari configmap.yaml
  - secretRef:
      name: oms-secrets      # → env var dari secret.yaml
```

Semua key di ConfigMap/Secret otomatis menjadi environment variable di container. Spring Boot via `application.yml` membaca env var dengan syntax `${NAMA_VARIABLE:default}`:

| application.yml | Sumber | Contoh Nilai |
|----------------|--------|-------------|
| `${JWT_SECRET}` | Secret `JWT_SECRET` | `<CHANGE_ME_MIN_32_CHARS>` |
| `${JWT_EXPIRATION:3600000}` | ConfigMap `JWT_EXPIRATION` | `3600000` |
| `${DB_HOST:localhost}` | Secret `DB_HOST` | `<CHANGE_ME>` (mis. `postgres-service`) |
| `${REDIS_HOST:localhost}` | ConfigMap `REDIS_HOST` | `redis-service` |

### 3.5 Deployment

**File:** `k8s/base/deployment.yaml`

| Parameter | Value | Keterangan |
|-----------|-------|------------|
| **Replicas** | 2 | High availability |
| **Strategy** | RollingUpdate | maxSurge=1, maxUnavailable=0 |
| **Image** | `oms:latest` | Build dari Dockerfile; production: pin by digest |
| **Pull Policy** | `IfNotPresent` | Local image preferred |
| **ServiceAccount** | `oms-service-account` | Bukan `default`; automountServiceAccountToken: false |
| **Pod SecurityContext** | runAsNonRoot, runAsUser 1001, fsGroup 1001 | Non-root user |
| **Container SecurityContext** | allowPrivilegeEscalation: false, readOnlyRootFilesystem: true, capabilities.drop: [ALL], seccompProfile: RuntimeDefault | Hardened sesuai F-05/Fase 2 |
| **Resources Requests** | 256Mi RAM, 250m CPU | Minimum guarantee |
| **Resources Limits** | 512Mi RAM, 500m CPU | Maximum allowed |
| **Liveness Probe** | GET /api/version, 60s delay | Restart if unhealthy |
| **Readiness Probe** | GET /api/version, 30s delay | Traffic if healthy |
| **Startup Probe** | GET /api/version, 10s delay, 30 failures | Allow slow startup |
| **Volume Mounts** | `/tmp` (emptyDir), `/app/logs` (emptyDir) | Diperlukan karena readOnlyRootFilesystem |

### 3.6 Service

**File:** `k8s/base/service.yaml`

```yaml
apiVersion: v1
kind: Service
metadata:
  name: oms-service
  namespace: oms-system
spec:
  type: ClusterIP
  selector:
    app.kubernetes.io/name: oms
  ports:
    - name: http
      port: 8080
      targetPort: 8080
      protocol: TCP
    - name: actuator
      port: 8081
      targetPort: 8081
      protocol: TCP
```

> **Catatan (F-05/Fase 0):** Service diubah dari NodePort ke ClusterIP untuk mencegah paparan actuator/metrics pada node network. Akses ke aplikasi dilakukan melalui Ingress (TLS) atau `kubectl port-forward`.

### 3.7 PostgreSQL & Redis

**Files:** `k8s/components/datastore/postgres.yaml`, `k8s/components/datastore/redis.yaml`

Untuk staging/prod profile, PostgreSQL dan Redis dideploy sebagai service internal:

- **PostgreSQL**: `postgres:16-alpine`, port 5432, ClusterIP service `postgres-service`
- **Redis**: `redis:7-alpine`, port 6379, ClusterIP service `redis-service`

Keduanya memiliki health probes (pg_isready, redis-cli ping) dan resource limits.
---

## 4. KUSTOMIZATION

### 4.1 Dev Profile (Default)

**File:** `k8s/base/kustomization.yaml`

```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

namespace: oms-system

# V4-08: `labels` (Kustomize v5+) replaces deprecated `commonLabels`.
# Unlike `commonLabels`, `labels` does NOT add to `spec.selector`, avoiding
# immutable-field errors on Deployment/StatefulSet updates.
labels:
  - pairs:
      app.kubernetes.io/name: oms
      app.kubernetes.io/part-of: oms

resources:
  - namespace.yaml
  - configmap.yaml
  - secret.yaml
  - serviceaccount.yaml
  - role.yaml
  - rolebinding.yaml
  - deployment.yaml
  - service.yaml
  - hpa.yaml
  - pdb.yaml
  - ingress.yaml
  - networkpolicy.yaml
  - resourcequota.yaml
  - limitrange.yaml
```

Dev profile menggunakan H2 in-memory database (tanpa PostgreSQL/Redis), dengan 14 resource.

### 4.2 Staging/Prod Profile

Untuk staging/prod, tambahkan PostgreSQL dan Redis (total 17 resource):

```yaml
resources:
  - namespace.yaml
  - configmap.yaml
  - secret.yaml
  - serviceaccount.yaml
  - role.yaml
  - rolebinding.yaml
  - deployment.yaml
  - service.yaml
  - hpa.yaml
  - pdb.yaml
  - ingress.yaml
  - networkpolicy.yaml
  - resourcequota.yaml
  - limitrange.yaml
  - postgres.yaml
  - redis.yaml
configMapGenerator:
  - name: oms-config
    behavior: merge
    literals:
      - SPRING_PROFILES_ACTIVE=staging
```

---

## 5. CARA DEPLOY

### 5.1 Prerequisites

```bash
# 1. Start Minikube
minikube start --cpus=4 --memory=8192

# 2. Enable Minikube Docker daemon (use local Docker images)
eval $(minikube docker-env)

# 3. Build Docker image
cd /home/workplace-pc/Documents/workplace-project/oms
docker build -t oms:latest .

# 4. Verify image exists
docker images | grep oms
```

### 5.2 Deploy with Dev Profile (H2)

```bash
# Validate manifests first (recommended)
kubectl kustomize k8s/
kubectl apply -k k8s/ --dry-run=client

# Deploy all resources
kubectl apply -k k8s/

# Verify deployment
kubectl get all -n oms-system

# Wait for pods to be ready
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=oms -n oms-system --timeout=120s

# Verify hardened resources
kubectl get networkpolicy -n oms-system
kubectl get resourcequota -n oms-system
kubectl get limitrange -n oms-system
kubectl get role,rolebinding -n oms-system
```

### 5.3 Deploy with Staging Profile (PostgreSQL + Redis)

```bash
# Edit kustomization.yaml to include postgres.yaml and redis.yaml
# Then deploy
kubectl apply -k k8s/

# Deploy PostgreSQL and Redis first, wait for them
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=postgres -n oms-system --timeout=120s
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=redis -n oms-system --timeout=60s

# Then deploy the app
kubectl apply -k k8s/
```

### 5.4 Verify Deployment

```bash
# Check all resources
kubectl get all -n oms-system

# Check logs
kubectl logs -n oms-system -l app.kubernetes.io/name=oms --tail=50

# Check pod status
kubectl get pods -n oms-system -w

# Describe pod for details
kubectl describe pod -n oms-system -l app.kubernetes.io/name=oms

# Check ConfigMap
kubectl get configmap oms-config -n oms-system -o yaml

# Check Secret
kubectl get secret oms-secrets -n oms-system -o yaml
```

### 5.5 Access Application

> **Catatan:** Service menggunakan ClusterIP (bukan NodePort). Akses ke aplikasi dilakukan melalui **Ingress** atau **port-forward**.

```bash
# Option 1: Port-forward (recommended for local dev)
kubectl port-forward -n oms-system service/oms-service 8080:8080

# Option 2: Via Ingress (requires nginx ingress controller)
# Enable minikube ingress addon: minikube addons enable ingress
# Then access via: https://oms.local/api/version
# (Requires TLS secret — see ingress.yaml)

# Test the API
curl http://localhost:8080/api/version
# ADMIN_PASSWORD diambil dari Kubernetes Secret / GitHub Actions secret
curl http://localhost:8080/api/v1/auth/login \
  -X POST \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"${ADMIN_PASSWORD}\"}"

# Check actuator (via port-forward on 8081)
kubectl port-forward -n oms-system service/oms-service 8081:8081 &
curl http://localhost:8081/actuator/health
```

### 5.6 Scale & Update

```bash
# Scale replicas
kubectl scale deployment oms -n oms-system --replicas=3

# Rolling update (after building new image)
docker build -t oms:latest .
kubectl rollout restart deployment oms -n oms-system

# Check rollout status
kubectl rollout status deployment oms -n oms-system

# Rollback if needed
kubectl rollout undo deployment oms -n oms-system
```

### 5.7 Cleanup

```bash
# Delete all resources
kubectl delete -k k8s/

# Or delete namespace (removes everything)
kubectl delete namespace oms-system

# Stop Minikube
minikube stop
```
---

## 6. ENVIRONMENT VARIABLES MAPPING

### 6.1 From env/omsdb-*.properties to Kubernetes

| Properties File | ConfigMap/Secret Key | Deployment Env |
|----------------|---------------------|----------------|
| `JWT_SECRET` | `secret: JWT_SECRET` | `JWT_SECRET` |
| `JWT_EXPIRATION` | `configmap: JWT_EXPIRATION` | `JWT_EXPIRATION` |
| `DB_HOST` | `secret: DB_HOST` | `DB_HOST` |
| `DB_PORT` | `secret: DB_PORT` | `DB_PORT` |
| `DB_NAME` | `secret: DB_NAME` | `DB_NAME` |
| `DB_USERNAME` | `secret: DB_USERNAME` | `DB_USERNAME` |
| `DB_PASSWORD` | `secret: DB_PASSWORD` | `DB_PASSWORD` |
| `REDIS_HOST` | `configmap: REDIS_HOST` | `REDIS_HOST` |
| `REDIS_PORT` | `configmap: REDIS_PORT` | `REDIS_PORT` |
| `REDIS_PASSWORD` | `secret: REDIS_PASSWORD` | `REDIS_PASSWORD` |
| `REDIS_DATABASE` | `configmap: REDIS_DATABASE` | `REDIS_DATABASE` |

---

## 7. SECURITY CONSIDERATIONS

### 7.1 Secrets Management

- **Dev**: Secret placeholder `<CHANGE_ME>` di `k8s/base/secret.yaml` — isi secara manual per environment
- **Staging/Prod**: Gunakan External Secrets Operator atau HashiCorp Vault
- **Never commit** production secrets ke git (F-03 remediasi — semua literal values dihapus dari Git)

### 7.2 Pod Security Context

```yaml
# Pod level
securityContext:
  runAsNonRoot: true
  runAsUser: 1001
  fsGroup: 1001

# Container level (hardened — F-05/Fase 2)
securityContext:
  allowPrivilegeEscalation: false
  readOnlyRootFilesystem: true
  capabilities:
    drop:
      - ALL
  seccompProfile:
    type: RuntimeDefault
```

### 7.3 Resource Limits

CPU/Memory limits mencegah resource starvation (dikontrol juga oleh LimitRange dan ResourceQuota):

```yaml
resources:
  requests:
    memory: "256Mi"
    cpu: "250m"
  limits:
    memory: "512Mi"
    cpu: "500m"
```

### 7.4 ServiceAccount & RBAC

- `oms-service-account` dengan `automountServiceAccountToken: false` — pod tidak otomatis mount K8s API token
- Role `oms-role` dengan rules kosong — aplikasi tidak membutuhkan akses K8s API
- RoleBinding mengikat Role ke ServiceAccount

### 7.5 NetworkPolicy

Default-deny ingress + egress; hanya izinkan:
- **Ingress:** dari namespace `ingress-nginx` ke port 8080
- **Egress:** DNS (kube-dns:53), PostgreSQL (5432), Redis (6379)

### 7.6 Ingress Security

- **TLS** dengan certificate
- **ssl-redirect: "true"**
- **HSTS** (max-age 31536000, includeSubDomains)
- **whitelist-source-range**: `0.0.0.0/0` (default di base; staging/prod di-override
  via Kustomize patch ke CIDR internal `10.0.0.0/8` — V4-07)

---

## 8. MONITORING

### 8.1 Health Endpoints

| Probe | Endpoint | Port | Delay |
|-------|----------|:----:|:-----:|
| Liveness | `/api/version` | 8080 | 60s |
| Readiness | `/api/version` | 8080 | 30s |
| Startup | `/api/version` | 8080 | 10s |

### 8.2 Actuator Endpoints

| Endpoint | Port | Description |
|----------|:----:|-------------|
| `/actuator/health` | 8081 | Health check |
| `/actuator/info` | 8081 | Application info |
| `/actuator/metrics` | 8081 | Application metrics |
| `/actuator/prometheus` | 8081 | Prometheus metrics |

---

## 9. TROUBLESHOOTING

| Masalah | Penyebab | Solusi |
|---------|----------|--------|
| Pod CrashLoopBackOff | JWT_SECRET tidak di-set | Verifikasi secret: `kubectl get secret -n oms-system` |
| ImagePullBackOff | Image tidak ada di Minikube | Jalankan `eval $(minikube docker-env)` lalu build ulang |
| Probe failed | Aplikasi belum siap | Naikkan `initialDelaySeconds` |
| Connection refused | Service selector mismatch | Cek labels di deployment dan service |
| PostgreSQL not reachable | Pod belum ready | `kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=postgres` |
| 403 Forbidden | RunAsUser mismatch | Pastikan `runAsUser: 1001` sesuai dengan Dockerfile |

---

## 10. KESIMPULAN

Kubernetes manifests untuk OMS telah berhasil dibuat, di-hardening, dan diverifikasi:

- ✅ **Deployment.yaml** — 2 replicas, RollingUpdate, 3 health probes, container securityContext hardened, ServiceAccount, resource limits, volumeMounts
- ✅ **Service.yaml** — ClusterIP 8080 (HTTP) + 8081 (Actuator); akses via Ingress/port-forward
- ✅ **Ingress.yaml** — TLS, ssl-redirect, HSTS, whitelist-source-range
- ✅ **NetworkPolicy.yaml** — Default-deny ingress + egress; allow dari ingress-nginx, ke DNS/PostgreSQL/Redis
- ✅ **ServiceAccount/Role/RoleBinding** — `oms-service-account`, automount false, RBAC minimal
- ✅ **ResourceQuota + LimitRange** — Namespace-level caps dan default container limits
- ✅ **ConfigMap.yaml** — ~19 kunci konfigurasi aplikasi (retry, rate-limit, JWT expiry, Redis, email, pagination, dll)
- ✅ **Secret.yaml** — Placeholder `<CHANGE_ME>`; literal values dihapus dari Git
- ✅ **HPA + PDB** — Auto-scale CPU 70%; min 1 pod available during disruptions
- ✅ **PostgreSQL + Redis** — Manifest untuk staging/prod (optional)
- ✅ **Kustomization** — Multi-environment: 14 resource (dev), 17 resource (staging/prod)
- ✅ **Dokumentasi** — Deploy, verify, access, scale, troubleshoot

**Status:** ✅ **COMPLETED**

---

*Dokumen ini dibuat pada 7 Agustus 2026*
*Diperbarui pada 13 Agustus 2026 — hardening K8s manifests sesuai Fase 0–2 (F-03, F-05, F-06)*
