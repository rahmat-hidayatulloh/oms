# ADR-006: Restructure Package by-Layer menjadi by-Feature

- **Status:** Accepted
- **Date:** 2026-08-25 (V5 Phase V5-P0)
- **Deciders:** Development Team

## Context

Sejak awal proyek, kode domain disusun by-layer: `com.oms.{controller,service,dto,entity,repository,security}`.
Akibatnya kode satu fitur (mis. Order) tersebar di enam direktori, cohesion fitur sulit
dinilai, dan Standar Development pilar 1.12 (*Package by Feature*) tidak terpenuhi.
Temuan ini tercatat sebagai F-14 sejak audit V3/V4 dan di-defer dari Phase V4-P1
(sub-fase P1.5) karena lingkup besar dan berisiko regresi.

Faktor yang membuat restructure aman dilakukan pada V5:

- Tidak ada wildcard import (`import com.oms.*`) di seluruh `src/`.
- Tidak ada referensi FQN `com.oms.*` pada konfigurasi XML/YAML maupun manifest K8s.
- Pointcut AOP berbasis binding parameter (`@Around("@annotation(auditable)")`), bukan FQN string.
- `@RestControllerAdvice(basePackages = "com.oms")` dan Redis `allowIfSubType("com.oms.")`
  tetap valid karena feature package tetap sub-package dari `com.oms`.

## Decision

Domain codebase disusun ulang menjadi package per-feature dengan sub-layer di dalamnya
(sesuai pilar 1.12):

| Feature | Isi |
|---|---|
| `com.oms.auth` | AuthController, JWT (`auth/security`), register/refresh token (`auth/service`), User & Role (`auth/entity`), validator password (`auth/validation`) |
| `com.oms.order` | OrderController/Service, OrderItemFactory, event listener, email service, entitas Order |
| `com.oms.product` | ProductController/Service, Product & Category, QueryDSL predicates |
| `com.oms.admin` | AdminController, dashboard & user management, DTO admin |
| `com.oms.audit` | `@Auditable`, AuditLogAspect, AuditLog end-to-end |
| `com.oms.system` | Endpoint app-level: CsrfController, VersionController |
| `com.oms.validation` | SafeHtml, SafeHtmlValidator, InputSanitizer (lintas fitur) |

Infrastruktur lintas-fitur tetap pada package top-level yang sudah cohesive:
`config`, `exception`, `constant`, `health`, `ratelimit` (sudah feature-shaped),
dan `entity/BaseEntity` (shared persistence base). Test dimirror ke struktur feature
yang sama. Pola exclude JaCoCo (`pom.xml`) dan SpotBugs (`config/spotbugs/spotbugs-exclude.xml`)
diperbarui mengikuti layout baru tanpa melonggarkan cakupan bug-pattern.

## Consequences

- Cohesion per-fitur terlihat dalam satu direktori; boundary modul eksplisit.
- Dependensi antar-fitur kini tampak jelas lewat import (order → auth, order → product, admin → auth).
- Trade-off: diff besar sekali pakai (±99 file pindah via `git mv`, history file dipertahankan);
  dokumentasi contoh import di `docs/current/**` ikut disinkronkan pada PR yang sama.
- Feature baru selanjutnya mengikuti pola ini; konvensi di `ONBOARDING.md` diperbarui.

## Alternatives Considered

- **Feature-flat (tanpa sub-layer):** Rejected — kehilangan kesesuaian dengan rumusan
  pilar 1.12 (`controller/service/repository` di dalam feature) dan menyulitkan update
  pola exclude JaCoCo/SpotBugs.
- **Status quo (by-layer):** Rejected — F-14 akan tetap terbuka dan standar internal
  tidak pernah terpenuhi.
