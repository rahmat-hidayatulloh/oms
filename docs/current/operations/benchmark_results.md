# Benchmark Results - OMS API Performance

> **Status:** Current
> **Audience:** Operator, Maintainer
> **Owner:** Engineering
> **Last verified:** 2026-08-29 (re-run benchmark dengan auth via `ADMIN_PASSWORD` env)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

> **Original date:** 7 Agustus 2026
> **Re-run:** 29 Agustus 2026 (setup identik; password admin dari env `ADMIN_PASSWORD` / GitHub Actions secret)
> **Tool:** wrk 4.1.0
> **Environment:** Dev profile (H2 in-memory database)
> **Configuration:** Redis disabled, Rate limiting disabled
> **Seed Data:** 50 products with mixed categories (`stock = i*10+50`)

---

## 1. Baseline Benchmark (Cache OFF)

### 1.1 GET /api/v1/products/{id}

| Metric | Value |
|--------|-------|
| Threads | 50 |
| Duration | 10s |
| Connections | 1000 |
| **Requests/sec** | **855.99** |
| **Avg Latency** | **954.34ms** |
| Std Dev Latency | 647.01ms |
| Max Latency | 2.00s |
| P95 Latency (est.) | ~2,000ms* |
| P99 Latency (est.) | ~2,000ms* |
| Total Requests | 8,645 |
| Data Transferred | 5.61MB |
| Socket Errors | 679 timeouts |
| **Error Rate** | **7.85%** |

*Banyak request menyentuh batas socket-timeout 2s, sehingga ekor latensi ter-capp di 2.00s.*

### 1.2 GET /api/v1/products?page=0&size=20

| Metric | Value |
|--------|-------|
| Threads | 50 |
| Duration | 10s |
| Connections | 1000 |
| **Requests/sec** | **1,282.54** |
| **Avg Latency** | **753.49ms** |
| Std Dev Latency | 457.91ms |
| Max Latency | 2.00s |
| P95 Latency (est.) | ~1,506ms |
| P99 Latency (est.) | ~1,818ms |
| Total Requests | 12,950 |
| Data Transferred | 187.45MB |
| Socket Errors | 1 timeout |
| **Error Rate** | **0.01%** |

### 1.3 POST /api/v1/orders

| Metric | Value |
|--------|-------|
| Threads | 50 |
| Duration | 10s |
| Connections | 1000 |
| **Requests/sec** | **797.33** |
| **Avg Latency** | **1.15s** |
| Std Dev Latency | 249.15ms |
| Max Latency | 2.00s |
| P95 Latency (est.) | ~1,558ms |
| P99 Latency (est.) | ~1,728ms |
| Total Requests | 8,053 |
| Successful Requests | ±60 (stock produk pertama = 60 unit) |
| Non-2xx (400 Insufficient stock) | 7,993 |
| Socket Errors | 44 timeouts |
| **Error Rate** | **99.8%** (stock habis setelah ±60 order sukses) |

---

## 2. Cache ON vs Cache OFF Comparison (GET /api/v1/products/{id})

| Metric | Cache OFF | Cache ON (Simple) | Improvement |
|--------|:---------:|:------------------:|:-----------:|
| Requests/sec | 855.99 | **1,380.86** | **+61.3%** |
| Avg Latency | 954.34ms | **649.59ms** | **-31.9%** |
| Std Dev Latency | 647.01ms | 466.83ms | -27.8% |
| Max Latency | 2.00s | 2.00s | - |
| P95 Latency (est.) | ~2,000ms | ~1,417ms | **-29.2%** |
| P99 Latency (est.) | ~2,000ms | ~1,735ms | **-13.3%** |
| Total Requests | 8,645 | 13,946 | +61.3% |
| Timeouts | 679 | 186 | -72.6% |
| **Error Rate** | 7.85% | 1.33% | **-6.5pp** |

---

## 3. Summary Table

| Endpoint | Cache | Req/s | Avg Latency | P95 | P99 | Error Rate |
|----------|:-----:|:-----:|:-----------:|:---:|:---:|:----------:|
| GET /products/{id} | OFF | 856 | 954ms | ~2,000ms* | ~2,000ms* | 7.85% |
| GET /products/{id} | **ON** | **1,381** | **650ms** | **~1,417ms** | **~1,735ms** | 1.33% |
| GET /products?page=0&size=20 | OFF | 1,283 | 753ms | ~1,506ms | ~1,818ms | 0.01% |
| POST /orders | OFF | 797 | 1.15s | ~1,558ms | ~1,728ms | 99.8% |

---

## 4. Analysis

### 4.1 Cache Impact (GET /products/{id})
- **+61.3% throughput** with cache enabled (1,381 vs 856 req/s)
- **-31.9% latency reduction** (650ms vs 954ms avg)
- **-72.6% socket timeouts** (679 → 186), error rate turun 7.85% → 1.33%
- Cache reduces database load significantly for repeated product lookups

### 4.2 List vs Single Endpoint (Cache OFF)
- Pada re-run ini list endpoint (*pagination*) **lebih stabil** dari single fetch:
  1,283 req/s dengan 0.01% error vs single fetch 856 req/s dengan 7.85% timeout
- List endpoint transfers **33.4x more data** (187.45MB vs 5.61MB)
- Variasi antar-run cukup besar untuk workload read (dipengaruhi kondisi mesin/H2), sehingga
  perbandingan relatif antar endpoint lebih bermakna daripada angka absolut tunggal

### 4.3 Write Endpoint (POST /orders)
- **Throttle** pada ~797 req/s (expected untuk write-heavy operation + validasi stock + optimistic locking)
- **Error rate tinggi (99.8%) karena stock exhaustion**: seed memberi `stock = i*10+50`,
  produk pertama (target benchmark) ber-stock **60 unit**, habis setelah ±60 order sukses;
  request berikutnya ditolak `400 Insufficient stock`
- 7,993 non-2xx (400) + 44 timeout dari 8,053 total request (≈60 sukses)

### 4.4 Bottlenecks Identified
1. **Socket/latency cap 2s** — sebagian besar timeout berasal dari koneksi yang mengantre melewati batas 2s
2. **Stock exhaustion** — benchmark write perlu produk ber-stock besar atau payload round-robin
   agar mengukur throughput sebenarnya (bukan rejection rate)
3. **Optimistic locking + validasi stock** — write ops inherently slower than reads
4. **H2 vs PostgreSQL** — H2 adalah in-memory single-process; PostgreSQL menangani concurrency lebih baik

---

## 5. Recommendations

1. **Enable caching** for GET endpoints (+61% throughput improvement pada re-run)
2. **Seed stock lebih besar** (mis. 10.000) sebelum benchmark POST /orders agar hasil mencerminkan
   throughput write, bukan rejection rate karena stock habis
3. **Increase connection pool size** for production (currently 5, should be 20+)
4. **Use PostgreSQL** in production (H2 is dev-only, PostgreSQL handles concurrency better)
5. **Consider read replicas** for product listing queries
6. **Monitor optimistic locking** - retry rate should be tracked in production

---

## 6. Test Environment

| Component | Configuration |
|-----------|---------------|
| **wrk** | 4.1.0 (debian) |
| **Application** | OMS (Spring Boot 3.2.12, Java 21) |
| **Database** | H2 In-Memory (dev profile) |
| **Cache** | Disabled (OFF) / Simple (ON) |
| **Redis** | Disabled |
| **Rate Limiting** | Disabled |
| **Auth** | Login admin via `POST /api/v1/auth/login` dengan password dari env `ADMIN_PASSWORD` (GitHub Actions secret) |

### Run Parameters
```
wrk -t50 -d10s -c1000 -H "Authorization: Bearer <token>" <url>
```

### Reproducing the benchmark
```bash
export ADMIN_PASSWORD=<value-from-github-secret>   # jangan di-hardcode
bash seed-data.sh                                  # login + seed 50 produk
source /tmp/oms-benchmark-env.sh
wrk -t50 -d10s -c1000 -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/products/$FIRST_PRODUCT_ID
```

---

*Generated on 7 Agustus 2026, re-run 29 Agustus 2026*
*Tool: wrk 4.1.0*