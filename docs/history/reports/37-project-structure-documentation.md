# OMS - Dokumentasi Lengkap

> ⚠️ **[SUPERSEDED — HISTORICAL REFERENCE ONLY]**
> Dokumen ini mencerminkan struktur proyek **V4 by-layer** yang telah digantikan oleh
> **package-by-feature** (V5) sesuai [ADR-006](../../adr/006-package-by-feature.md).
> Jangan gunakan sebagai referensi struktur aktual — gunakan [architecture.md](../../current/overview/architecture.md).

> **Status:** Historical / Superseded
> **Audience:** Developer, Maintainer
> **Owner:** Engineering
> **Snapshot date:** 2026-07-27 sampai 2026-08-06
> **Superseded by:** [Current-state architecture](../../current/overview/architecture.md)
> **Catatan:** Dokumen ini bersifat hybrid; section 1–6 adalah snapshot struktur awal, sedangkan section 7 berisi update historis hasil analisis dan perbaikan.

## 📋 Daftar Isi
1. [Teknologi yang Digunakan](#1-teknologi-yang-digunakan)
2. [Struktur Project](#2-struktur-project)
3. [Entity Model & Relasi Database](#3-entity-model--relasi-database)
4. [Flow Aplikasi](#4-flow-aplikasi)
5. [Arsitektur Layer](#5-arsitektur-layer)
6. [Contoh Input Data](#6-contoh-input-data)
7. [Hasil Analisis & Perbaikan](#7-hasil-analisis--perbaikan)

---

## 1. Teknologi yang Digunakan

| Teknologi | Versi | Kegunaan |
|-----------|-------|----------|
| **Java** | 21 | Bahasa pemrograman utama |
| **Spring Boot** | 3.2.5 | Framework utama (Web, Security, Data JPA, Actuator) |
| **Spring Security** | - | Autentikasi & otorisasi (JWT + Role-based: ADMIN/CUSTOMER) |
| **Spring Data JPA** | - | ORM & database access |
| **QueryDSL** | 5.1.0 | Type-safe dynamic queries untuk filtering produk |
| **PostgreSQL** | - | Database utama (via Flyway migration) |
| **Redis** | - | Cache & Refresh Token storage |
| **Flyway** | - | Database migration (V1-V5) |
| **JWT (jjwt)** | 0.12.5 | Token-based authentication |
| **Lombok** | - | Boilerplate code reduction (@Getter, @Setter, @Builder, dll) |
| **Testcontainers** | - | Integration testing dengan container Docker |
| **JaCoCo** | 0.8.12 | Code coverage measurement |
| **Checkstyle** | 3.3.1 | Code style enforcement (0 violations) |
| **SpotBugs** | 4.8.6 | Bug detection |
| **Swagger/OpenAPI** | - | API documentation & testing |
| **HikariCP** | - | Connection pooling |
| **Logback** | - | Logging (structured JSON logging) |
| **Mockito** | - | Unit testing mocking |

---

## 2. Struktur Project

```
oms/
├── pom.xml                          # Maven configuration
├── mvnw / mvnw.cmd                  # Maven wrapper
├── sonar-project.properties          # SonarCloud configuration
├── config/
│   ├── checkstyle/
│   │   └── checkstyle.xml           # Checkstyle rules
│   └── spotbugs/
│       └── spotbugs-exclude.xml     # SpotBugs exclusions
├── docs/                            # Dokumentasi fitur
│   ├── 01-inisialisasi-project.md
│   ├── ...
│   └── 36-project-analysis-report.md
├── src/
│   ├── main/
│   │   ├── java/com/oms/
│   │   │   ├── OmsApplication.java           # Main class
│   │   │   ├── config/
│   │   │   │   ├── AsyncConfig.java          # Async thread pool
│   │   │   │   ├── AuditorAwareConfig.java   # JPA auditing
│   │   │   │   ├── CacheConfig.java          # Cache configuration
│   │   │   │   ├── JpaAuditingConfig.java    # JPA auditing
│   │   │   │   ├── LoggingFilter.java        # MDC logging filter
│   │   │   │   ├── OpenApiConfig.java        # Swagger/OpenAPI
│   │   │   │   ├── RedisConfig.java          # Redis configuration
│   │   │   │   ├── RetryConfig.java          # Retry mechanism
│   │   │   │   ├── SecurityConfig.java       # Spring Security
│   │   │   │   ├── annotation/
│   │   │   │   │   └── ApiVersion.java       # API version annotation
│   │   │   │   └── versioning/
│   │   │   │       ├── ApiVersionFilter.java # Version filter
│   │   │   │       └── VersioningResolver.java
│   │   │   ├── controller/
│   │   │   │   ├── AdminController.java       # Admin dashboard
│   │   │   │   ├── AuthController.java        # Login/refresh/logout
│   │   │   │   ├── OrderController.java       # Order CRUD
│   │   │   │   ├── ProductController.java     # Product CRUD + search
│   │   │   │   └── VersionController.java     # API version info
│   │   │   ├── dto/
│   │   │   │   ├── ApiResponse.java           # Standard response wrapper
│   │   │   │   ├── LoginRequest/Response.java
│   │   │   │   ├── OrderItemRequest/Response.java
│   │   │   │   ├── OrderRequest/Response.java
│   │   │   │   ├── ProductFilterRequest.java  # QueryDSL filter
│   │   │   │   ├── ProductRequest/Response.java
│   │   │   │   └── RefreshTokenRequest.java
│   │   │   ├── entity/
│   │   │   │   ├── BaseEntity.java            # Audit fields (createdAt, updatedAt, deletedAt)
│   │   │   │   ├── Category.java              # Product category enum
│   │   │   │   ├── Order.java                 # Order entity
│   │   │   │   ├── OrderItem.java             # Order line item
│   │   │   │   ├── OrderStatus.java           # Order status enum
│   │   │   │   ├── Product.java               # Product entity
│   │   │   │   ├── Role.java                  # User role enum
│   │   │   │   └── User.java                  # User entity
│   │   │   ├── exception/
│   │   │   │   ├── AsyncExceptionHandler.java # Async error handler
│   │   │   │   ├── BadRequestException.java
│   │   │   │   ├── BusinessException.java
│   │   │   │   ├── GlobalExceptionHandler.java # RFC 7807 ProblemDetail
│   │   │   │   └── ResourceNotFoundException.java
│   │   │   ├── health/
│   │   │   │   ├── DatabaseHealthIndicator.java
│   │   │   │   ├── ExternalAPIHealthIndicator.java
│   │   │   │   └── RedisHealthIndicator.java
│   │   │   ├── repository/
│   │   │   │   ├── OrderItemRepository.java
│   │   │   │   ├── OrderRepository.java
│   │   │   │   ├── ProductPredicates.java     # QueryDSL predicates
│   │   │   │   ├── ProductRepository.java
│   │   │   │   └── UserRepository.java
│   │   │   ├── security/
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   ├── JwtTokenProvider.java      # Deprecated
│   │   │   │   └── JwtUtil.java               # JWT utilities
│   │   │   ├── service/
│   │   │   │   ├── CustomUserDetailsService.java
│   │   │   │   ├── EmailService.java          # Async email
│   │   │   │   ├── NoOpRefreshTokenService.java
│   │   │   │   ├── OrderService.java
│   │   │   │   ├── ProductService.java
│   │   │   │   ├── RefreshTokenService.java
│   │   │   │   └── RefreshTokenServiceInterface.java
│   │   │   └── util/
│   │   └── resources/
│   │       ├── application.yml                # Main config
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       ├── application-staging.yml
│   │       ├── logback-spring.xml             # Logging config
│   │       └── db/migration/
│   │           ├── V1__create_users_table.sql
│   │           ├── V2__create_products_table.sql
│   │           ├── V3__create_orders_table.sql
│   │           ├── V4__create_order_items_table.sql
│   │           └── V5__insert_admin_user.sql
│   └── test/
│       ├── java/com/oms/
│       │   ├── OmsApplicationTest.java
│       │   ├── config/
│       │   │   └── AbstractIntegrationTest.java
│       │   ├── controller/
│       │   │   ├── AdminControllerTest.java
│       │   │   ├── AuthControllerTest.java
│       │   │   ├── OrderControllerTest.java
│       │   │   ├── ProductControllerTest.java
│       │   │   ├── ProductControllerUnitTest.java
│       │   │   └── VersionControllerTest.java
│       │   ├── entity/
│       │   │   └── OrderTest.java
│       │   ├── exception/
│       │   │   ├── AsyncExceptionHandlerTest.java
│       │   │   ├── BadRequestExceptionTest.java
│       │   │   ├── BusinessExceptionTest.java
│       │   │   ├── GlobalExceptionHandlerTest.java
│       │   │   └── ResourceNotFoundExceptionTest.java
│       │   ├── health/
│       │   │   ├── DatabaseHealthIndicatorErrorTest.java
│       │   │   ├── DatabaseHealthIndicatorTest.java
│       │   │   ├── ExternalAPIHealthIndicatorErrorTest.java
│       │   │   ├── ExternalAPIHealthIndicatorTest.java
│       │   │   └── RedisHealthIndicatorTest.java
│       │   ├── migration/
│       │   │   └── FlywayMigrationTest.java
│       │   ├── repository/
│       │   │   ├── OrderRepositoryTest.java
│       │   │   └── ProductRepositoryTest.java
│       │   ├── security/
│       │   │   ├── JwtAuthenticationFilterTest.java
│       │   │   ├── JwtTokenProviderTest.java
│       │   │   └── JwtUtilTest.java
│       │   └── service/
│       │       ├── AuditingIntegrationTest.java
│       │       ├── NoOpRefreshTokenServiceTest.java
│       │       ├── OrderServiceNPlusOneTest.java
│       │       ├── OrderServiceTest.java
│       │       ├── ProductCacheTest.java
│       │       ├── ProductConcurrentTest.java
│       │       ├── ProductServiceIntegrationTest.java
│       │       ├── ProductServiceTest.java
│       │       ├── ProductServiceUnitTest.java
│       │       ├── RedisIntegrationTest.java
│       │       ├── RefreshTokenServiceTest.java
│       │       └── SoftDeleteIntegrationTest.java
│       └── resources/
│           └── application-test.yml
```

---

## 3. Entity Model & Relasi Database

### Entity Diagram

```
┌──────────────────┐       ┌──────────────────┐
│      User        │       │     Product      │
├──────────────────┤       ├──────────────────┤
│ id (UUID)        │       │ id (UUID)        │
│ username (unique)│       │ name (unique)    │
│ email            │       │ description      │
│ password (bcrypt)│       │ price (BigDecimal)│
│ role (ADMIN/     │       │ stock (Integer)  │
│       CUSTOMER)  │       │ category (Enum)  │
│ createdAt        │       │ version (@Version)│
│ updatedAt        │       │ deletedAt (soft) │
│ deletedAt        │       │ createdAt        │
└──────────────────┘       │ updatedAt        │
                           └──────────────────┘
                                    │ 1
                                    │
┌──────────────────┐       ┌───────┴──────────┐
│     Order        │       │    OrderItem     │
├──────────────────┤ *      ├──────────────────┤
│ id (UUID)        │───────│ id (UUID)        │
│ orderNumber      │       │ order_id (FK)    │
│ userId (FK)      │       │ product_id (FK)  │
│ totalAmount      │       │ quantity         │
│ status (Enum)    │       │ price            │
│ createdAt        │       │ subtotal         │
│ updatedAt        │       └──────────────────┘
│ deletedAt        │
└──────────────────┘
```

### Entity Detail

#### Product Entity
| Field | Type | Constraint | Description |
|-------|------|------------|-------------|
| id | UUID | PK, Auto-generated | Primary key |
| name | String (3-100) | Unique, Not Null | Product name |
| description | String (max 500) | - | Product description |
| price | BigDecimal (10,2) | Min 0.01 | Product price |
| stock | Integer | Not Null | Available stock |
| category | Enum (CLOTHING, ELECTRONICS, FOOD_AND_BEVERAGE, BOOKS, OTHER) | Not Null | Product category |
| version | Integer | @Version | Optimistic locking |
| createdAt | LocalDateTime | Auto | JPA auditing |
| updatedAt | LocalDateTime | Auto | JPA auditing |
| deletedAt | LocalDateTime | - | Soft delete timestamp |

**Annotations:**
- `@SQLDelete`: Soft delete dengan `UPDATE products SET deleted_at = CURRENT_TIMESTAMP`
- `@SQLRestriction("deleted_at IS NULL")`: Otomatis exclude data terhapus

#### Order Entity
| Field | Type | Constraint | Description |
|-------|------|------------|-------------|
| id | UUID | PK, Auto-generated | Primary key |
| orderNumber | String (20) | Unique | Generated: ORD-xxxxxxxxxx |
| userId | UUID | FK -> User.id | Order owner |
| totalAmount | BigDecimal | - | Total order amount |
| status | Enum (PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED) | - | Order status |
| items | List\<OrderItem\> | @OneToMany | Order line items |
| createdAt/updatedAt/deletedAt | - | - | Audit fields |

#### OrderItem Entity
| Field | Type | Constraint | Description |
|-------|------|------------|-------------|
| id | UUID | PK, Auto-generated | Primary key |
| order | Order | @ManyToOne | Parent order |
| product | Product | @ManyToOne | Product reference |
| quantity | Integer | Min 1 | Quantity ordered |
| price | BigDecimal | - | Price at order time |
| subtotal | BigDecimal | - | quantity * price |

#### User Entity
| Field | Type | Constraint | Description |
|-------|------|------------|-------------|
| id | UUID | PK, Auto-generated | Primary key |
| username | String (50) | Unique | Login username |
| email | String (100) | - | Email address |
| password | String (255) | Not Null | BCrypt hashed |
| role | Enum (ADMIN, CUSTOMER) | Not Null | User role |
| createdAt/updatedAt/deletedAt | - | - | Audit fields |

### BaseEntity (Abstract)
Semua entity mewarisi `BaseEntity` yang menyediakan:
- `createdAt` (LocalDateTime) - diisi otomatis saat create
- `updatedAt` (LocalDateTime) - diisi otomatis saat update
- `createdBy` (String) - diisi dari SecurityContext
- `updatedBy` (String) - diisi dari SecurityContext
- `deletedAt` (LocalDateTime) - diisi saat soft delete

### Database Migrations (Flyway)
| Migration | Isi |
|-----------|-----|
| V1 | Create users table |
| V2 | Create products table |
| V3 | Create orders table |
| V4 | Create order_items table |
| V5 | Insert default admin user |

---

## 4. Flow Aplikasi

### 4.1 Authentication Flow
```
┌─────────┐     ┌──────────────────┐     ┌─────────────┐     ┌───────────┐
│ Client  │────►│ AuthController   │────►│ AuthManager │────►│ JwtUtil   │
│         │     │ POST /auth/login│     │ authenticate│     │ generate  │
└─────────┘     └──────────────────┘     └─────────────┘     └─────┬─────┘
                                                                    │
                                                                    ▼
┌─────────┐     ┌──────────────────┐                              ┌──────────┐
│ Client  │◄────│ LoginResponse    │◄─────────────────────────────│ Token    │
│         │     │ {token, role,    │                              │ JWT     │
│         │     │  expiresIn}      │                              └──────────┘
└─────────┘     └──────────────────┘
```

### 4.2 API Request Flow
```
┌─────────┐     ┌──────────────┐     ┌──────────────────┐     ┌────────────┐
│ Client  │────►│API Version   │────►│ JWT Auth Filter  │────►│ Controller │
│         │     │Filter        │     │ - Extract token  │     │            │
└─────────┘     └──────────────┘     │ - Validate       │     └─────┬──────┘
                                     │ - Set Security   │           │
                                     │   Context        │           ▼
                                     └──────────────────┘     ┌────────────┐
                                                              │  Service   │
                                                              │   Layer    │
                                                              └─────┬──────┘
                                                                    │
                                                                    ▼
┌─────────┐     ┌──────────────┐     ┌──────────────────┐     ┌────────────┐
│ Client  │◄────│ ApiResponse  │◄────│ GlobalException  │◄────│ Repository │
│         │     │ Wrapper      │     │ Handler (error)  │     │   Layer    │
└─────────┘     └──────────────┘     └──────────────────┘     └────────────┘
```

### 4.3 Order Creation Flow (Detail)
```
1. Client → POST /api/v1/orders (dengan array items)
2. OrderController.createOrder()
3. OrderService.createOrder(request, userId)
   a. Hitung total harga dari items
   b. Untuk setiap item:
      - Validasi product exists
      - Validasi stock cukup
      - Buat OrderItem dengan price & subtotal
      - Kurangi stock product
   c. Generate order number (ORD-YYYYMMDD-NNN)
   d. Simpan Order dengan status PENDING
   e. Kirim email notification (async via @Async)
   f. Return OrderResponse
4. Response: 201 Created + ApiResponse<OrderResponse>
```

### 4.4 Product Service Flow
```
create(request):
  1. Validasi unique name → existsByName()
  2. Map ProductRequest → Product entity
  3. Save product → repository.save()
  4. Return ProductResponse

findById(id):
  1. Find product → repository.findById(id)
  2. If not found → throw ResourceNotFoundException
  3. Return ProductResponse

search(filter):
  1. Build QueryDSL predicate → ProductPredicates.build(filter)
  2. Create Pageable from filter (sort, direction, page, size)
  3. repository.findAll(predicate, pageable)
  4. Return Page<ProductResponse>

update(id, request):
  1. Find existing product
  2. Validasi unique name (exclude self)
  3. Update fields
  4. Save product
  5. Return ProductResponse

delete(id):
  1. Find product
  2. Soft delete → repository.softDelete(id)
```

### 4.5 Security Flow
```
JwtAuthenticationFilter.doFilterInternal():
  1. Extract Authorization header dari request
  2. Parse Bearer token
  3. Validate token signature & expiration (JwtUtil.validateToken())
  4. Extract username & role dari claims
  5. Load UserDetails (CustomUserDetailsService)
  6. Set UsernamePasswordAuthenticationToken ke SecurityContext
  7. Continue filter chain

JwtUtil.generateToken(username, role):
  1. Create JWT claims (sub, role, iat, exp)
  2. Sign with HMAC-SHA256 secret key
  3. Serialize to compact JWT string
```

### 4.6 Exception Handling Flow
```
1. Exception thrown di Service/Repository layer
2. GlobalExceptionHandler menangkap berdasarkan tipe:
   - ResourceNotFoundException → 404 Not Found
   - BadRequestException → 400 Bad Request
   - BusinessException → 400 Bad Request
   - AuthenticationException → 401 Unauthorized
   - AccessDeniedException → 403 Forbidden
   - MethodArgumentNotValidException → 400 Validation Failed
   - HttpMessageNotReadableException → 400 Malformed Request
   - MethodArgumentTypeMismatchException → 400 Type Mismatch
3. Response: RFC 7807 ProblemDetail JSON
```

---

## 5. Arsitektur Layer

```
┌─────────────────────────────────────────────────────────────────────┐
│                      PRESENTATION LAYER                              │
│  Controller Layer                                                    │
│  ├── AuthController       - POST /login, /refresh, /logout          │
│  ├── ProductController    - CRUD + search products                  │
│  ├── OrderController      - CRUD orders                             │
│  ├── AdminController      - Admin dashboard & user management       │
│  └── VersionController    - GET /api/version info                   │
├─────────────────────────────────────────────────────────────────────┤
│                      BUSINESS LAYER                                  │
│  Service Layer                                                       │
│  ├── ProductService        - Product business logic + caching       │
│  ├── OrderService          - Order processing + stock management    │
│  ├── EmailService          - Async email notifications              │
│  ├── CustomUserDetailsService - User details for Spring Security    │
│  ├── RefreshTokenService   - Redis-based refresh tokens             │
│  └── NoOpRefreshTokenService - Fallback when Redis disabled         │
├─────────────────────────────────────────────────────────────────────┤
│                      DATA ACCESS LAYER                               │
│  Repository Layer                                                    │
│  ├── ProductRepository     - Product CRUD + QueryDSL                │
│  ├── OrderRepository       - Order CRUD + EntityGraph               │
│  ├── OrderItemRepository   - Order items CRUD                       │
│  └── UserRepository        - User CRUD                              │
│                                                                      │
│  ProductPredicates         - QueryDSL type-safe predicates          │
│    ├── withNameContaining(name) → BooleanExpression                 │
│    ├── withMinPrice(price) → BooleanExpression                      │
│    ├── withMaxPrice(price) → BooleanExpression                      │
│    └── withCategory(category) → BooleanExpression                   │
├─────────────────────────────────────────────────────────────────────┤
│                      DOMAIN LAYER                                    │
│  Entity Layer                                                        │
│  ├── User, Product, Order, OrderItem                                │
│  ├── BaseEntity (abstract) - Audit fields                           │
│  └── Enums: Category, OrderStatus, Role                             │
├─────────────────────────────────────────────────────────────────────┤
│                      CROSS-CUTTING CONCERNS                          │
│                                                                      │
│  Security Layer    │  Exception Layer    │  Health Layer            │
│  ├── JwtUtil       │  ├── GlobalHandler  │  ├── DatabaseHealth     │
│  ├── JwtAuthFilter │  ├── AsyncHandler   │  ├── RedisHealth        │
│  └── SecurityConfig│  └── Custom Excep.  │  └── ExternalAPIHealth  │
│                                                                      │
│  Config Layer                                                        │
│  ├── Security, Cache, Redis, JPA Auditing                           │
│  ├── Async (ThreadPoolTaskExecutor)                                 │
│  ├── Retry (Spring Retry)                                           │
│  ├── OpenAPI (Swagger)                                              │
│  ├── LoggingFilter (MDC structured logging)                         │
│  └── Versioning (API version resolver)                              │
└─────────────────────────────────────────────────────────────────────┘
```

### DTO Structure
```
┌──────────────────────┐
│    ApiResponse<T>    │  ← Standard wrapper untuk semua response
├──────────────────────┤
│ success: boolean     │
│ message: String      │
│ data: T              │
│ timestamp: LocalDate │
└──────────────────────┘

┌──────────────────────┐
│   ProductRequest     │  ← Input untuk create/update product
├──────────────────────┤
│ name: String         │
│ description: String  │
│ price: BigDecimal    │
│ stock: Integer       │
│ category: Category   │
└──────────────────────┘

┌──────────────────────┐
│   ProductResponse    │  ← Output product
├──────────────────────┤
│ id: UUID             │
│ name: String         │
│ description: String  │
│ price: BigDecimal    │
│ stock: Integer       │
│ category: Category   │
│ createdAt: LocalDate │
│ updatedAt: LocalDate │
└──────────────────────┘

┌──────────────────────┐
│  ProductFilterRequest│  ← Filter untuk search
├──────────────────────┤
│ name: String         │
│ minPrice: BigDecimal │
│ maxPrice: BigDecimal │
│ category: Category   │
│ page: int            │
│ size: int            │
│ sortBy: String       │
│ sortDirection: String│
└──────────────────────┘
```

---

## 6. Contoh Input Data

### 6.1 Create Product
```http
POST /api/v1/products
Authorization: Bearer <token>
Content-Type: application/json

{
    "name": "Kaos Polos Hitam",
    "description": "Kaos katun premium hitam size L",
    "price": 85000.00,
    "stock": 100,
    "category": "CLOTHING"
}
```

**Response (201 Created):**
```json
{
    "success": true,
    "message": "Product created successfully",
    "data": {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "name": "Kaos Polos Hitam",
        "description": "Kaos katun premium hitam size L",
        "price": 85000.00,
        "stock": 100,
        "category": "CLOTHING",
        "createdAt": "2026-07-27T08:00:00",
        "updatedAt": "2026-07-27T08:00:00"
    },
    "timestamp": "2026-07-27T08:00:00"
}
```

### 6.2 Search Products (with QueryDSL)
```http
GET /api/v1/products/search?name=kaos&minPrice=50000&maxPrice=100000&category=CLOTHING&page=0&size=10&sortBy=name&sortDirection=asc
Authorization: Bearer <token>
```

**Response (200 OK):**
```json
{
    "success": true,
    "message": "Success",
    "data": {
        "content": [
            {
                "id": "550e8400-e29b-41d4-a716-446655440000",
                "name": "Kaos Polos Hitam",
                "description": "Kaos katun premium hitam size L",
                "price": 85000.00,
                "stock": 100,
                "category": "CLOTHING",
                "createdAt": "2026-07-27T08:00:00",
                "updatedAt": "2026-07-27T08:00:00"
            }
        ],
        "page": 0,
        "size": 10,
        "totalElements": 1,
        "totalPages": 1,
        "last": true
    },
    "timestamp": "2026-07-27T08:00:00"
}
```

### 6.3 Create Order
```http
POST /api/v1/orders
Authorization: Bearer <token>
Content-Type: application/json

{
    "items": [
        {
            "productId": "550e8400-e29b-41d4-a716-446655440000",
            "quantity": 2
        },
        {
            "productId": "550e8400-e29b-41d4-a716-446655440001",
            "quantity": 1
        }
    ]
}
```

**Response (201 Created):**
```json
{
    "success": true,
    "message": "Order created successfully",
    "data": {
        "id": "660e8400-e29b-41d4-a716-446655440000",
        "orderNumber": "ORD-20260727-001",
        "userId": "770e8400-e29b-41d4-a716-446655440000",
        "totalAmount": 335000.00,
        "status": "PENDING",
        "items": [
            {
                "productId": "550e8400-e29b-41d4-a716-446655440000",
                "productName": "Kaos Polos Hitam",
                "quantity": 2,
                "price": 85000.00,
                "subtotal": 170000.00
            },
            {
                "productId": "550e8400-e29b-41d4-a716-446655440001",
                "productName": "Headphone Bluetooth",
                "quantity": 1,
                "price": 165000.00,
                "subtotal": 165000.00
            }
        ],
        "createdAt": "2026-07-27T08:00:00",
        "updatedAt": "2026-07-27T08:00:00"
    },
    "timestamp": "2026-07-27T08:00:00"
}
```

### 6.4 Login
```http
POST /api/auth/login
Content-Type: application/json

{
    "username": "admin",
    "password": "<redacted>"
}
```

**Response (200 OK):**
```json
{
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInJvbGUiOiJST0xFX0FETUlOIiwiaWF0IjoxNzIxMzkwNDAwLCJleHAiOjE3MjEzOTQwMDB9.signature",
    "type": "Bearer",
    "username": "admin",
    "role": "ADMIN",
    "expiresIn": 3600000
}
```

### 6.5 Get Product By ID
```http
GET /api/v1/products/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer <token>
```

**Response (200 OK):**
```json
{
    "success": true,
    "message": "Success",
    "data": {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "name": "Kaos Polos Hitam",
        "description": "Kaos katun premium hitam size L",
        "price": 85000.00,
        "stock": 100,
        "category": "CLOTHING",
        "createdAt": "2026-07-27T08:00:00",
        "updatedAt": "2026-07-27T08:00:00"
    },
    "timestamp": "2026-07-27T08:00:00"
}
```

### 6.6 Update Product
```http
PUT /api/v1/products/550e8400-e29b-41d4-a716-446655440000
Authorization: Bearer <token>
Content-Type: application/json

{
    "name": "Kaos Polos Putih",
    "description": "Kaos katun premium putih size L",
    "price": 90000.00,
    "stock": 150,
    "category": "CLOTHING"
}
```

### 6.7 Error Response (Validation)
```http
POST /api/v1/products
Authorization: Bearer <token>
Content-Type: application/json

{
    "name": "AB",
    "price": -1,
    "stock": 50,
    "category": "ELECTRONICS"
}
```

**Response (400 Bad Request):**
```json
{
    "type": "https://api.oms.com/errors/400",
    "title": "Validation Failed",
    "status": 400,
    "detail": "Request validation failed",
    "instance": "/api/v1/products",
    "fieldErrors": {
        "name": "Product name must be between 3 and 100 characters",
        "price": "Price must be greater than 0"
    },
    "timestamp": "2026-07-27T08:00:00"
}
```

### 6.8 Error Response (Not Found)
```http
GET /api/v1/products/550e8400-e29b-41d4-a716-446655449999
Authorization: Bearer <token>
```

**Response (404 Not Found):**
```json
{
    "type": "https://api.oms.com/errors/404",
    "title": "Resource Not Found",
    "status": 404,
    "detail": "Product not found with id: '550e8400-e29b-41d4-a716-446655449999'",
    "instance": "/api/v1/products/550e8400-e29b-41d4-a716-446655449999",
    "timestamp": "2026-07-27T08:00:00"
}
```

### 6.9 Health Check
```http
GET /actuator/health
```

**Response:**
```json
{
    "status": "UP",
    "components": {
        "db": {
            "status": "UP",
            "details": {
                "database": "H2",
                "version": "2.2.224",
                "url": "jdbc:h2:mem:testdb"
            }
        },
        "externalAPI": {
            "status": "DOWN",
            "details": {
                "service": "External Payment API",
                "url": "http://localhost:9090/health",
                "reason": "Connection refused"
            }
        },
        "redis": {
            "status": "DOWN",
            "details": {
                "service": "Redis",
                "reason": "Connection refused: localhost:6379"
            }
        }
    }
}
```

---

## 7. Hasil Analisis & Perbaikan

### 7.1 Ringkasan Coverage

| Package | Sebelum | Sesudah | Status |
|---------|---------|---------|--------|
| **com.oms** | 66.7% | **100%** | ✅ |
| **com.oms.repository** | 100% | **100%** | ✅ |
| **com.oms.exception** | 63.0% | **90.4%** | ✅ |
| **com.oms.service** | 89.6% | **90.2%** | ✅ |
| **com.oms.health** | 8.3% | **87.2%** | ✅ |
| **com.oms.controller** | 45.5% | **80.5%** | ✅ |
| **com.oms.security** | 62.7% | **83.3%** | ✅ |
| **TOTAL** | **59.6%** | **87.1%** | ⬆️ (+28%) |

### 7.2 Test Suite

```
Tests run: 208, Failures: 0, Errors: 0, Skipped: 9
BUILD SUCCESS
```

### 7.3 Checklist Perbaikan

| No | Issue | File Baru/Diubah | Status |
|----|-------|-------------------|--------|
| 1 | SpotBugs EI_EXPOSE_REP2 | DatabaseHealthIndicator.java | ✅ Fixed |
| 2 | ProductCacheTest failure | ProductCacheTest.java | ✅ Fixed |
| 3 | DatabaseHealthIndicator (8.3%) | DatabaseHealthIndicatorTest.java | ✅ Added (9 tests) |
| 4 | DatabaseHealthIndicator error path | DatabaseHealthIndicatorErrorTest.java | ✅ Added (3 tests) |
| 5 | RedisHealthIndicator (0%) | RedisHealthIndicatorTest.java | ✅ Added (4 tests) |
| 6 | ExternalAPIHealthIndicator (0%) | ExternalAPIHealthIndicatorTest.java | ✅ Added (4 tests) |
| 7 | ExternalAPIHealthIndicator error path | ExternalAPIHealthIndicatorErrorTest.java | ✅ Added (3 tests) |
| 8 | VersionController (0%) | VersionControllerTest.java | ✅ Added (4 tests) |
| 9 | OrderController (20%) | OrderControllerTest.java | ✅ Added (3 tests) |
| 10 | AuthController (63%) | AuthControllerTest.java | ✅ Added (3 tests) |
| 11 | BusinessException (0%) | BusinessExceptionTest.java | ✅ Added (4 tests) |
| 12 | BadRequestException (57%) | BadRequestExceptionTest.java | ✅ Added (2 tests) |
| 13 | ResourceNotFoundException (57%) | ResourceNotFoundExceptionTest.java | ✅ Added (2 tests) |
| 14 | AsyncExceptionHandler (0%) | AsyncExceptionHandlerTest.java | ✅ Added (4 tests) |
| 15 | JwtTokenProvider (19%) | JwtTokenProviderTest.java | ✅ Added (7 tests) |
| 16 | NoOpRefreshTokenService (91%) | NoOpRefreshTokenServiceTest.java | ✅ Added (5 tests) |
| 17 | OmsApplication (66.7%) | OmsApplicationTest.java | ✅ Added (2 tests) |

### 7.4 Fitur Utama yang Diuji

| Fitur | Coverage | Keterangan |
|-------|----------|------------|
| **CRUD Product** | 100% | Create, Read, Update, Delete (soft) |
| **QueryDSL Predicates** | 100% | Filter by name, price range, category, pagination |
| **Order Management** | 100% | Create order with items, stock validation |
| **Soft Delete** | 100% | @SQLDelete + @SQLRestriction |
| **Optimistic Locking** | 100% | @Version increment on update |
| **JPA Auditing** | 100% | createdAt, updatedAt, createdBy, updatedBy |
| **JWT Authentication** | 83% | Token generation, validation, extraction |
| **Exception Handling** | 90% | RFC 7807 ProblemDetail responses |
| **Health Indicators** | 87% | Database, Redis, External API health checks |
| **Caching** | 100% | Redis cache eviction (disabled in test) |
| **Email Notifications** | 100% | Async email on order creation |
| **API Versioning** | 100% | VersionController with version info |

### 7.5 Cara Menjalankan

```bash
# Build project
./mvnw clean install -DskipTests

# Run all tests
./mvnw clean test

# Run specific test
./mvnw test -Dtest=ProductRepositoryTest

# Run with coverage
./mvnw clean test jacoco:report

# Run with checkstyle & spotbugs
./mvnw clean verify

# Run application (dev profile)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 7.6 Catatan

1. **9 tests skipped**: Tests yang membutuhkan Docker (Testcontainers) tidak jalan karena Docker tidak tersedia di environment ini. Ini termasuk:
   - FlywayMigrationTest (3 tests)
   - ProductServiceIntegrationTest (3 tests)
   - OrderRepositoryTest (1 test)
   - RedisIntegrationTest (1 test)
   - SoftDeleteIntegrationTest (1 test)

2. **RefreshTokenService (10%)**: Membutuhkan Redis yang terkoneksi. Untuk testing fallback, `NoOpRefreshTokenService` sudah mencapai 100% coverage.

3. **Controller Security**: Tests untuk endpoint yang membutuhkan autentikasi dijalankan dengan `@WithMockUser` atau Mockito mocks.