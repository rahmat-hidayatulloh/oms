# Task 9: Setup Flyway Migration untuk Semua Entity

> **Status:** Current
> **Audience:** Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-28 (screening kode aktual `src/`, `pom.xml`)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## Tujuan
Mengatur database migration menggunakan Flyway agar schema seluruh entity (users, products, orders, order_items) konsisten di semua environment, serta menyediakan seed user admin default.

## Acceptance Criteria
- [x] Dependency: flyway-core
- [x] V1__create_users_table.sql
- [x] V2__create_products_table.sql
- [x] V3__create_orders_table.sql
- [x] V4__create_order_items_table.sql
- [x] V5__insert_admin_user.sql
- [x] V6__add_indexes.sql
- [x] V7__create_audit_log.sql

## Detail Pekerjaan

### 1. Dependency

Menambahkan `flyway-core` ke `pom.xml`:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

### 2. Lokasi Migration Script

Semua script berada di `src/main/resources/db/migration/`:

```
src/main/resources/db/migration/
├── V1__create_users_table.sql
├── V2__create_products_table.sql
├── V3__create_orders_table.sql
├── V4__create_order_items_table.sql
├── V5__insert_admin_user.sql
├── V6__add_indexes.sql
└── V7__create_audit_log.sql
```

### 3. Skema per Tabel

| Versi | Tabel | Kolom Utama |
|---|---|---|
| V1 | `users` | id (UUID PK), username, email, password, role, flag akun, created_at, updated_at, deleted_at |
| V2 | `products` | id (UUID PK), name, description, price, stock, category, version (optimistic lock), audit columns |
| V3 | `orders` | id (UUID PK), order_number (unique), user_id, total_amount, status (default PENDING), audit columns |
| V4 | `order_items` | id (UUID PK), product_id, product_name, quantity, price, order_id (FK → orders ON DELETE CASCADE), audit columns |
| V6 | `(indexes)` | `idx_products_category`, `idx_orders_user_id`, `idx_orders_status`, `idx_order_items_product_id` — menyediakan indeks untuk query pencarian & pelaporan |
| V7 | `audit_log` | id (UUID PK), event_timestamp, user_id, action, entity_type, entity_id, ip_address, details (TEXT), created_at — tabel append-only untuk audit trail |

Catatan desain:
- Kolom audit (`created_at`, `updated_at`, `deleted_at`) disesuaikan dengan `BaseEntity` (auditing JPA).
- `orders.user_id` **tidak** dibuat FK keras agar riwayat order tetap utuh meski user di-soft-delete.
- `order_items.order_id` dibuat FK keras dengan `ON DELETE CASCADE` mengikuti `orphanRemoval = true` di entity `Order`.
- Tipe numerik mengikuti entity: `price` NUMERIC(10,2), `total_amount` NUMERIC(12,2).

### 4. Seed Admin User (V5)

V5 menyisipkan user `admin` dengan role `ADMIN`. Nilai password admin di-hash dengan **BCrypt strength 12**; nilai plaintext-nya diambil dari GitHub Actions secret `ADMIN_PASSWORD` (lihat komentar di `V5__insert_admin_user.sql`) — **tidak di-hardcode** di migration maupun dokumentasi.
UUID bersifat tetap (`00000000-0000-0000-0000-000000000001`) agar seed aman dijalankan berulang kali.

> Untuk environment non-lokal, nilailah `ADMIN_PASSWORD` via GitHub Actions secrets (`secrets.ADMIN_PASSWORD`)
> dan jangan pernah menuliskannya di docs/commit. Ganti password admin secara berkala untuk keamanan.

### 5. Konfigurasi Profile

Flyway diaktifkan di semua profile (`dev`, `staging`, `prod`) dengan:

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true
  jpa:
    hibernate:
      ddl-auto: validate   # Hibernate hanya memvalidasi, Flyway yang membuat schema
```

Perubahan penting: `ddl-auto` yang sebelumnya `create-drop` diubah menjadi `validate` agar tidak bentrok dengan Flyway.

### 6. Unit Test

File: `src/test/java/com/oms/migration/FlywayMigrationTest.java`

Test coverage:
- ✅ Keempat tabel terbuat setelah migrasi
- ✅ User admin berhasil di-seed (tepat 1 baris)
- ✅ `flyway_schema_history` mencatat ketujuh migrasi sukses

### 7. Hasil Verifikasi

```
BUILD SUCCESS ✅
├── Compile: OK
├── Checkstyle: 0 violations
├── SpotBugs: 0 bugs
├── Tests: passed
│   ├── OmsApplicationTest: 1/1
│   ├── OrderTest: 4/4
│   └── FlywayMigrationTest: 3/3
└── Sonar: Clean
```

### 8. Catatan
- Flyway menjalankan migrasi otomatis saat aplikasi startup (before JPA).
- `validate-on-migrate` mencegah drift antara SQL migration dan entity JPA.
- Untuk menambah tabel/column baru, buat `V8__*.sql` dan seterusnya — jangan ubah script yang sudah dijalankan.