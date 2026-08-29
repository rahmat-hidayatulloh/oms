# Task 5: Entity User untuk Autentikasi dan Authorisasi

> **Status:** Current
> **Audience:** Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-28 (screening kode aktual `src/`, `pom.xml`)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## Tujuan
Membuat entity User yang lengkap dengan fitur autentikasi (Spring Security UserDetails), auditing, dan soft delete.

## Acceptance Criteria
- [x] Entity User dengan fields: id (UUID), username, email, password, role (CUSTOMER/ADMIN)
- [x] Auditing lengkap: createdAt, createdBy, updatedAt, updatedBy, deletedAt (dari BaseEntity)
- [x] Soft delete: @SQLDelete + @SQLRestriction("deleted_at IS NULL")
- [x] Pendaftaran akun baru (role CUSTOMER) via `POST /api/v1/auth/register`
- [x] UserDetails diadaptasi melalui `UserDetailsAdapter` (entity tetap persistence-only)

## Detail Pekerjaan

### 1. File yang Dibuat

```
src/main/java/com/oms/
├── entity/
│   └── BaseEntity.java        ← Abstract @MappedSuperclass: created/updated/deleted fields
├── auth/
│   ├── entity/
│   │   ├── Role.java          ← Enum: CUSTOMER, ADMIN
│   │   └── User.java          ← Entity utama (persistence-only, TIDAK implements UserDetails)
│   ├── repository/
│   │   └── UserRepository.java← JPA Repository
│   └── security/
│       └── UserDetailsAdapter.java ← Membungkus User agar sesuai kontrak UserDetails
├── config/
│   ├── JpaAuditingConfig.java ← @EnableJpaAuditing
│   └── AuditorAwareConfig.java← AuditorAware → username / "system"
```

### 2. Entity User - Fields

| Field | Type | Constraint | Keterangan |
|---|---|---|---|
| `id` | UUID (auto) | PK, not null | Generated via `GenerationType.UUID` |
| `username` | String(50) | unique, not null | Username login |
| `email` | String(100) | unique, not null | Email login |
| `password` | String | not null | BCrypt encoded |
| `role` | Enum(Role) | not null | CUSTOMER / ADMIN |
| `createdAt` | LocalDateTime | auto, not null | @CreatedDate (dari BaseEntity) |
| `createdBy` | String(100) | auto, not null | @CreatedBy → username / "system" (dari BaseEntity) |
| `updatedAt` | LocalDateTime | auto | @LastModifiedDate (dari BaseEntity) |
| `updatedBy` | String(100) | auto | @LastModifiedBy → username / "system" (dari BaseEntity) |
| `deletedAt` | LocalDateTime | nullable | Soft delete marker (dari BaseEntity) |
| `isAccountNonExpired` | boolean | default true | Flag status akun (dipersist di DB) |
| `isAccountNonLocked` | boolean | default true | Flag status akun (dipersist di DB) |
| `isCredentialsNonExpired` | boolean | default true | Flag status akun (dipersist di DB) |
| `isEnabled` | boolean | default true | Flag status akun (dipersist di DB) |

### 3. Role Enum

```java
public enum Role {
    CUSTOMER,
    ADMIN
}
```

### 4. BaseEntity (Auditing + Soft Delete)

```java
@Getter
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

    public boolean isDeleted() { return deletedAt != null; }

    public void softDelete() { this.deletedAt = LocalDateTime.now(ZoneId.of(OrderConstants.APP_TIMEZONE)); }
}
```

### 5. User Entity & UserDetailsAdapter

Entity `User` bersifat **persistence-only** — TIDAK mengimplementasikan `UserDetails`. Kontrak Spring Security dipenuhi oleh `UserDetailsAdapter`:

```java
@Entity
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET deleted_at = CURRENT_TIMESTAMP, version = version + 1 "
        + "WHERE id = ? AND version = ? AND deleted_at IS NULL")
@SQLRestriction("deleted_at IS NULL")
@SequenceGenerator(name = "users_seq", sequenceName = "users_seq", allocationSize = 1)
public class User extends BaseEntity {
    // fields: id, username, email, password, role, isAccount* (bukan UserDetails)
}
```

- **`UserDetailsAdapter implements UserDetails`** — membungkus `User`, return `user.getUsername()`, `user.getPassword()`, dan authorities `ROLE_<role>` dari database.
- `CustomUserDetailsService.loadUserByUsername()` → memuat `User` via repository dan membungkusnya dengan adapter.
- Karena entity tidak terikat interface `UserDetails`, field `isAccountNonExpired`/`isAccountNonLocked`/`isCredentialsNonExpired`/`isEnabled` hanyalah **kolom database** yang tetap dipersist; nilai "aktif" untuk login saat ini di-*hardcode* adapter (semua `true`).

### 6. UserRepository

```java
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
```

### 7. JPA Auditing Config

```java
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
```

### 8. Soft Delete Behavior

- `@SQLDelete` → mengoverride `DELETE` menjadi `UPDATE users SET deleted_at = CURRENT_TIMESTAMP ...`
- `@SQLRestriction("deleted_at IS NULL")` → Semua query otomatis filter data yang tidak terhapus
- `softDelete()` → Set `deletedAt` dengan timestamp sesuai timezone aplikasi (`OrderConstants.APP_TIMEZONE`)

### 9. Endpoint Register — POST /api/v1/auth/register

Selain login (`POST /api/v1/auth/login`), `AuthController` juga menangani pendaftaran akun baru:

| Method | URL | Auth | Request | Success Response | Failed |
|---|---|---|---|---|---|
| POST | `/api/v1/auth/register` | Public (permitAll) | `RegisterRequest {username, email, password}` | `201 Created` + `RegisterResponse {username, email, role, message}` | `400 Bad Request` (username/email sudah dipakai) |

Alur:

1. `AuthController` menerima `@Valid @RequestBody RegisterRequest`.
2. `RegisterService.register()` mengecek `existsByUsername` / `existsByEmail` → `BadRequestException` (400) jika duplikat.
3. Password di-encode dengan `PasswordEncoder` (BCrypt).
4. User baru dibuat dengan role **CUSTOMER** (default) dan disimpan ke tabel `users`.
5. Response: `201 Created` dengan username, email, role (`CUSTOMER`), dan message.

> Catatan: Register hanya menghasilkan role `CUSTOMER`; akun `ADMIN` dibuat via seed/migration, bukan lewat endpoint.

### 10. Soft Delete & Uniqueness

- `@SQLDelete` mengubah `DELETE` menjadi `UPDATE users SET deleted_at = CURRENT_TIMESTAMP ...` -> data tetap ada.
- `@SQLRestriction("deleted_at IS NULL")` membuat semua query otomatis hanya melihat data aktif.
- **Uniqueness:** constraint `unique` pada `username`/`email` tetap berlaku untuk baris yang sudah di-soft delete, sehingga mendaftarkan ulang username/email yang sama akan gagal di level database (unik per tabel, bukan per data aktif).

### 11. Hasil Verifikasi

```
BUILD SUCCESS ✅
├── Compile: OK
├── Checkstyle: 0 violations
├── SpotBugs: 0 bugs (fixed serialVersionUID)
├── Tests: 1/1 passed
└── Context loads with JPA Auditing enabled
```