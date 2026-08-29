# Security Policy — OMS

> **Status:** Current
> **Audience:** Security Researcher, Developer, Operator
> **Owner:** Engineering / Security
> **Last verified:** 2026-08-29
> **Source of truth:** Repository codebase, `.github/workflows/ci-cd.yml`, `docs/ops/`

## Supported Versions

| Version | Supported |
|---------|:---------:|
| `main` (latest) | ✅ |
| `develop` | ✅ (pre-release) |
| `< v5.0` | ❌ |

---

## Reporting a Vulnerability

**Jangan buat GitHub Issue publik untuk kerentanan keamanan.** Laporan publik dapat
dieksploitasi sebelum patch tersedia.

### Cara melaporkan

1. **GitHub Private Security Advisory** *(preferred)*  
   Buka tab **Security → Advisories → New draft security advisory** di repository ini.  
   Ini memberi akses langsung ke maintainer tanpa eksposur publik.

2. **Email** *(alternatif)*  
   Kirim ke: `security@oms-project.example.com` (ganti dengan email maintainer aktual)  
   Subject: `[OMS SECURITY] Deskripsi singkat kerentanan`

### Informasi yang harus disertakan

```
- Deskripsi kerentanan (jenis: IDOR, SSRF, JWT bypass, dll.)
- Langkah reproduksi (PoC minimal)
- Dampak potensial (data yang terekspos, escalasi privilege, dll.)
- Versi / branch yang terpengaruh
- Saran mitigasi (opsional)
```

---

## Response Timeline

| Tahap | Target Waktu |
|-------|:------------:|
| Konfirmasi penerimaan laporan | 48 jam |
| Triase & penilaian severitas | 5 hari kerja |
| Patch tersedia (P1-Critical) | ≤ 14 hari |
| Patch tersedia (P2-High) | ≤ 30 hari |
| Disclosure publik (coordinated) | Setelah patch di-release |

---

## Scope

### In-scope (prioritas tinggi)

- **Authentication bypass** — JWT validation, refresh token, logout blacklist
- **Authorization escalation** — RBAC bypass, `@PreAuthorize` circumvention
- **Injection** — SQL injection (via QueryDSL), NoSQL injection, Header injection
- **Rate limit bypass** — distributed atau in-memory rate limiting
- **CSRF** — state-changing requests tanpa CSRF token
- **Information disclosure** — credentials, PII, internal stack traces di response
- **Kubernetes manifest** — misconfigured RBAC, NetworkPolicy, ServiceAccount
- **Secrets in code** — plaintext credentials di source atau dokumentasi

### Out-of-scope

- Serangan yang membutuhkan akses fisik ke server
- Social engineering / phishing terhadap kontributor
- Denial-of-service volumetrik (infrastruktur-level)
- Kerentanan di third-party dependency (laporkan ke upstream langsung)
- Scanner noise tanpa PoC yang jelas (misalnya Nikto false positives)

---

## Security Features yang Sudah Diimplementasikan

| Layer | Mekanisme |
|---|---|
| **Authentication** | JWT HS256, refresh token rotation, replay detection (Redis), blacklist pada logout |
| **Authorization** | Spring Security `@PreAuthorize`, dual filter chain (API + management) |
| **Rate Limiting** | Bucket4j token bucket, distributed via Redis (`LettuceBasedProxyManager`), in-memory fallback |
| **Input Validation** | `@Valid` (Bean Validation), custom SQL-injection sanitizer |
| **CSRF** | Token-based CSRF untuk state-changing operations |
| **Secrets Management** | Semua secret via GitHub Actions `secrets.*` atau env-var; tidak ada plaintext di repo |
| **Dependency Scanning** | OWASP Dependency-Check (CI), Trivy image scan, Gitleaks secret scan |
| **SBOM** | CycloneDX SBOM di-generate setiap build CI |
| **Container Hardening** | Non-root user, `readOnlyRootFilesystem`, `allowPrivilegeEscalation: false`, `capabilities.drop: ALL` |
| **Network Policy** | Default-deny Kubernetes NetworkPolicy; whitelist per path |
| **Audit Logging** | Admin operations tercatat via structured MDC logging |

---

## Security Contacts

- **Primary**: Maintainer (via GitHub Security Advisory)
- **Reference docs**: [`docs/ops/IncidentResponse.md`](docs/ops/IncidentResponse.md)
- **Runbook P1**: [`docs/ops/IncidentResponse.md#scenario-authentication-failures-spike`](docs/ops/IncidentResponse.md)

---

## Acknowledgments

Terima kasih kepada semua peneliti keamanan yang bertanggung jawab melaporkan kerentanan.
Kontribusi Anda membantu menjaga keamanan proyek ini.
