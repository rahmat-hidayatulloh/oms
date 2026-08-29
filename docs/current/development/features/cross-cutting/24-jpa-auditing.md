# OMS-021: Implementasi Auditing dengan Spring Data JPA

> **Status:** Current
> **Audience:** Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-28 (screening kode aktual `src/`, `pom.xml`)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## 📋 Deskripsi

Mengimplementasikan **JPA Auditing** untuk otomatis mengisi field `createdAt`, `createdBy`, `updatedAt`, dan `updatedBy` pada semua entity. Auditor diambil dari `SecurityContext` (authenticated user) atau fallback ke `"system"` jika tidak ada user yang login.

## 🎯 Acceptance Criteria

| No | Kriteria | Status |
|----|----------|--------|
| 1 | `@EnableJpaAuditing` di main class atau config | ✅ |
| 2 | Abstract `BaseEntity` dengan auditing fields | ✅ |
| 3 | `@CreatedDate`, `@LastModifiedDate` | ✅ |
| 4 | `@CreatedBy`, `@LastModifiedBy` (ambil dari SecurityContext) | ✅ |
| 5 | `AuditorAware` implementation | ✅ |

## 🔍 Masalah yang Diselesaikan

### Tanpa Auditing

```java
Product product = new Product();
product.setName("Laptop");
product.setPrice(new BigDecimal("15000000"));
// Developer harus manual set:
product.setCreatedAt(LocalDateTime.now());
product.setCreatedBy("admin");
product.setUpdatedAt(LocalDateTime.now());
product.setUpdatedBy("admin");
productRepository.save(product);
```

**Masalah:**
- Repetitif dan error-prone
- Inconsistent timestamps
- Tidak ada tracking siapa yang mengubah data

### Dengan JPA Auditing

```java
Product product = new Product();
product.setName("Laptop");
product.setPrice(new BigDecimal("15000000"));
productRepository.save(product);
// ✅ createdAt, createdBy, updatedAt, updatedBy otomatis diisi!
```

## 🏗️ Struktur File yang Diubah/Dibuat

```
src/main/java/com/oms/
├── config/
│   ├── JpaAuditingConfig.java          ← [SUDAH ADA] @EnableJpaAuditing
│   └── AuditorAwareConfig.java         ← [BARU] AuditorAware bean
├── entity/
│   └── BaseEntity.java                 ← [DIUBAH] Tambah @CreatedBy, @LastModifiedBy
└── service/
    └── [TIDAK DIUBAH] — Auditing otomatis via EntityListeners

src/test/java/com/oms/
└── service/
    └── AuditingIntegrationTest.java    ← [BARU] Integration test auditing

src/main/resources/db/migration/
├── V1__create_users_table.sql          ← [DIUBAH] Tambah created_by, updated_by
├── V2__create_products_table.sql       ← [DIUBAH] Tambah created_by, updated_by
├── V3__create_orders_table.sql         ← [DIUBAH] Tambah created_by, updated_by
├── V4__create_order_items_table.sql    ← [DIUBAH] Tambah created_by, updated_by
└── V5__insert_admin_user.sql           ← [DIUBAH] Tambah created_by, updated_by

docs/
└── 24-jpa-auditing.md                  ← [BARU] Dokumentasi ini
```

## 🔧 Implementasi

### 1. BaseEntity — Auditing Fields

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false, length = 100)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now(ZoneId.of("Asia/Jakarta"));
    }
}
```

**Annotation Explanation:**
- `@CreatedDate` — Otomatis diisi saat entity pertama kali dibuat
- `@CreatedBy` — Otomatis diisi dengan nama user yang membuat
- `@LastModifiedDate` — Otomatis diisi saat entity di-update
- `@LastModifiedBy` — Otomatis diisi dengan nama user yang mengubah
- `@EntityListeners(AuditingEntityListener.class)` — Hook yang memicu auditing

### 2. JpaAuditingConfig — Enable Auditing

```java
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
```

**Cara kerja:**
- `@EnableJpaAuditing` mengaktifkan JPA auditing untuk seluruh aplikasi
- Mencari `AuditorAware` bean di context untuk mendapatkan current auditor

### 3. AuditorAwareConfig — Auditor Provider

```java
@Configuration
public class AuditorAwareConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null
                    || !authentication.isAuthenticated()
                    || authentication instanceof AnonymousAuthenticationToken) {
                return Optional.of("system");
            }

            return Optional.ofNullable(authentication.getName());
        };
    }
}
```

**Cara kerja:**
1. Ambil `Authentication` dari `SecurityContextHolder`
2. Jika tidak ada authenticated user → return `"system"`
3. Jika ada authenticated user → return username (`authentication.getName()`)

### 4. Database Schema — Migration Scripts

**V1__create_users_table.sql:**
```sql
CREATE TABLE users (
    ...
    created_at   TIMESTAMP    NOT NULL,
    created_by   VARCHAR(100) NOT NULL,
    updated_at   TIMESTAMP,
    updated_by   VARCHAR(100),
    deleted_at   TIMESTAMP,
    ...
);
```

**V2__create_products_table.sql:**
```sql
CREATE TABLE products (
    ...
    created_at   TIMESTAMP     NOT NULL,
    created_by   VARCHAR(100)  NOT NULL,
    updated_at   TIMESTAMP,
    updated_by   VARCHAR(100),
    deleted_at   TIMESTAMP
);
```

**V3__create_orders_table.sql:**
```sql
CREATE TABLE orders (
    ...
    created_at   TIMESTAMP    NOT NULL,
    created_by   VARCHAR(100) NOT NULL,
    updated_at   TIMESTAMP,
    updated_by   VARCHAR(100),
    deleted_at   TIMESTAMP,
    ...
);
```

**V4__create_order_items_table.sql:**
```sql
CREATE TABLE order_items (
    ...
    created_at   TIMESTAMP    NOT NULL,
    created_by   VARCHAR(100) NOT NULL,
    updated_at   TIMESTAMP,
    updated_by   VARCHAR(100),
    deleted_at   TIMESTAMP,
    ...
);
```

**V5__insert_admin_user.sql:**
```sql
INSERT INTO users (
    id,
    username,
    email,
    password,
    role,
    ...
    created_at,
    created_by,
    updated_at,
    updated_by
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin',
    'admin@oms.local',
    '$2a$10$...',
    'ADMIN',
    ...
    CURRENT_TIMESTAMP,
    'system',
    CURRENT_TIMESTAMP,
    'system'
);
```

## 🧪 Test Coverage

### AuditingIntegrationTest (3 tests)

| Test Case | Verifikasi |
|-----------|------------|
| `shouldSetCreatedByAndCreatedAtOnCreate` | `createdBy` dan `createdAt` diisi saat create |
| `shouldSetUpdatedByAndUpdatedAtOnUpdate` | `updatedBy` dan `updatedAt` diisi saat update |
| `shouldNotChangeCreatedByAndCreatedAtOnUpdate` | `createdBy` dan `createdAt` tidak berubah saat update |

### Test Results

```
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 ✅
```

### Example Test Output

```java
@Test
void shouldSetCreatedByAndCreatedAtOnCreate() {
    Product saved = productRepository.save(product);
    
    // ✅ createdBy = "system" (no authenticated user in test)
    assertThat(saved.getCreatedBy()).isEqualTo("system");
    
    // ✅ createdAt = current timestamp
    assertThat(saved.getCreatedAt()).isNotNull();
}
```

## 📊 Auditing Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Application Start                                        │
│    @EnableJpaAuditing activates auditing                    │
└─────────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. Entity Save/Update                                       │
│    productRepository.save(product)                          │
└─────────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. AuditingEntityListener triggered                         │
│    @PrePersist → set createdAt, createdBy                   │
│    @PreUpdate → set updatedAt, updatedBy                    │
└─────────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. AuditorAwareProvider called                              │
│    SecurityContextHolder.getContext().getAuthentication()    │
└─────────────────────────────────────────────────────────────┘
                        ↓
              ┌─────────┴─────────┐
              ↓                   ↓
    ┌─────────────────┐  ┌──────────────────┐
    │ User logged in   │  │ No user logged in│
    │ → return username│  │ → return "system"│
    └─────────────────┘  └──────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ 5. Fields populated                                         │
│    product.setCreatedBy("admin")                            │
│    product.setCreatedAt(LocalDateTime.now())                 │
│    product.setUpdatedBy("admin")                            │
│    product.setUpdatedAt(LocalDateTime.now())                 │
└─────────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────────┐
│ 6. SQL INSERT/UPDATE executed                               │
│    INSERT INTO products (..., created_by, created_at, ...)  │
│    VALUES (..., 'admin', '2026-07-23 12:00:00', ...)        │
└─────────────────────────────────────────────────────────────┘
```

## 🚀 Running Tests

```bash
# Run auditing tests only
./mvnw test -Dtest=AuditingIntegrationTest

# Run all service tests
./mvnw test -Dtest="ProductServiceTest,OrderServiceTest,AuditingIntegrationTest"

# Run all tests
./mvnw test
```

## ⚠️ Catatan Penting

### 1. AuditorAware di Test Environment

Di test environment (tanpa authenticated user), `AuditorAware` akan return `"system"`:

```java
@Test
void shouldSetCreatedByAndCreatedAtOnCreate() {
    Product saved = productRepository.save(product);
    assertThat(saved.getCreatedBy()).isEqualTo("system"); // ✅
}
```

Jika ingin test dengan specific user, gunakan `@WithMockUser`:

```java
@Test
@WithMockUser(username = "admin")
void shouldSetCreatedByWithMockUser() {
    Product saved = productRepository.save(product);
    assertThat(saved.getCreatedBy()).isEqualTo("admin"); // ✅
}
```

### 2. @CreatedBy dan @LastModifiedBy di Seed Data

Untuk data yang di-seed via Flyway (misal: admin user), kita harus explicitly set `created_by` dan `updated_by`:

```sql
INSERT INTO users (
    id, username, email, password, role,
    created_at, created_by, updated_at, updated_by
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin',
    'admin@oms.local',
    '$2a$10$...',
    'ADMIN',
    CURRENT_TIMESTAMP,
    'system',
    CURRENT_TIMESTAMP,
    'system'
);
```

### 3. Performance Impact

JPA Auditing memiliki overhead minimal:
- Hanya trigger pada `@PrePersist` dan `@PreUpdate`
- Tidak ada additional query (hanya set field values)
- `AuditorAware` dipanggil synchronous (tidak blocking)

### 4. Timezone

`createdAt` dan `updatedAt` menggunakan `LocalDateTime` yang disimpan sebagai `TIMESTAMP` di database. Pastikan timezone konsisten:

```java
// Di BaseEntity
public void softDelete() {
    this.deletedAt = LocalDateTime.now(ZoneId.of(OrderConstants.APP_TIMEZONE));
}
```

Time zone aplikasi didefinisikan di `OrderConstants.APP_TIMEZONE` (satu sumber kebenaran).

### 5. Nullability

- `createdBy` — `NOT NULL` (wajib diisi)
- `updatedBy` — nullable (bisa null pada create)
- `createdAt` — `NOT NULL` (wajib diisi)
- `updatedAt` — nullable (bisa null pada create)

## 🔗 Referensi

- [Spring Data JPA - Auditing](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#auditing)
- [Baeldung - Spring Data JPA Auditing](https://www.baeldung.com/spring-data-jpa-auditing)
- [Spring Security - AuditorAware](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#auditing.auditor-aware)