# OMS — API Documentation

> **Status:** Current (overview; contract details require OpenAPI verification)
> **Audience:** API Consumer, Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-28 (screening `last-verified` seluruh dokumentasi + endpoint/dto aktual di `src/`)
> **Source of truth:** Controller mappings, DTO, security configuration, dan OpenAPI configuration
> **Version:** 1.0
> **Base URL (API):** `http://localhost:8080/api/v1`
> **Management/actuator port:** `http://localhost:8081` (endpoint `management.server.port: 8081`)
> **Content-Type:** `application/json`

---

## Authentication

### Register

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "string",
  "email": "user@example.com",
  "password": "Str0ng!Pass"
}
```

**Response:** `201 Created`

```json
{
  "username": "newuser",
  "email": "user@example.com",
  "role": "CUSTOMER",
  "message": "User registered successfully"
}
```

### Login

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "string",
  "password": "string"
}
```

**Response:** `200 OK`

```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "dGhpcyBpcyBh...",
  "tokenType": "Bearer"
}
```

### Refresh Token

```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "refresh-token-value"
}
```

**Response:** `200 OK`

```json
{
  "accessToken": "new.jwt.token.here",
  "refreshToken": "new.refresh.token.here",
  "tokenType": "Bearer"
}
```

### Logout

```http
POST /api/v1/auth/logout
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "refreshToken": "refresh-token-value"
}
```

**Response:** `200 OK`

---

## Order Management

### Create Order

```http
POST /api/v1/orders
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "items": [
    {
      "productId": "550e8400-e29b-41d4-a716-446655440000",
      "quantity": 2
    }
  ]
}
```

**Response:** `201 Created` — Returns `OrderResponse`

### Get Order by ID

```http
GET /api/v1/orders/{id}
Authorization: Bearer <jwt-token>
```

**Response:** `200 OK` — Returns `OrderResponse`

### Get All Orders (Paginated)

```http
GET /api/v1/orders?page=0&size=20
Authorization: Bearer <jwt-token>
```

**Response:** `200 OK` — Returns paginated `OrderResponse`

### Delete Order (Soft Delete)

```http
DELETE /api/v1/orders/{id}
Authorization: Bearer <jwt-token>
```

**Response:** `204 No Content`

---

## Product Management

### Create Product

```http
POST /api/v1/products
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "name": "Product Name",
  "description": "Description",
  "price": 19.99,
  "stock": 100,
  "category": "ELECTRONICS"
}
```

**Response:** `201 Created`

### Get Product by ID

```http
GET /api/v1/products/{id}
Authorization: Bearer <jwt-token>
```

**Auth:** `CUSTOMER` atau `ADMIN`
**Response:** `200 OK` — Returns `ProductResponse` (`id` bertipe UUID)

### Get All Products

```http
GET /api/v1/products
Authorization: Bearer <jwt-token>
```

**Auth:** `CUSTOMER` atau `ADMIN`
**Response:** `200 OK` — Returns `List<ProductResponse>` (daftar produk aktif, non-paginated)

### Search Products (Filtered, Sorted, Paginated)

```http
GET /api/v1/products/search?category=ELECTRONICS&page=0&size=20
GET /api/v1/products/search?name=keyword&minPrice=10000&maxPrice=500000
GET /api/v1/products/search?sortBy=price&sortDirection=asc&page=0&size=20
Authorization: Bearer <jwt-token>
```

**Auth:** `CUSTOMER` atau `ADMIN`
**Query params:** `name`, `minPrice`, `maxPrice`, `category`, `sortBy` (`name`|`price`|`createdAt`, default `createdAt`), `sortDirection` (`asc`|`desc`, default `desc`), `page` (default 0), `size` (default 10)
**Category values:** `CLOTHING`, `ACCESSORIES`, `ELECTRONICS`, `BOOKS`, `FOOD_AND_BEVERAGE`
**Response:** `200 OK` — Returns paginated `Page<ProductResponse>`

### Update Product

```http
PUT /api/v1/products/{id}
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "name": "Updated Name",
  "price": 29.99,
  "stock": 50
}
```

**Response:** `200 OK`

### Delete Product

```http
DELETE /api/v1/products/{id}
Authorization: Bearer <jwt-token>
```

**Response:** `200 OK`

---

## Admin Endpoints

### Get Dashboard Stats

```http
GET /api/v1/admin/dashboard
Authorization: Bearer <jwt-token>
```

**Response:** `200 OK` — Admin dashboard statistics

### Get All Users (Paginated)

```http
GET /api/v1/admin/users?page=0&size=20
Authorization: Bearer <jwt-token>
```

**Response:** `200 OK` — Paginated user list

---

## CSRF Protection

### Get CSRF Token

```http
GET /api/v1/auth/csrf
```

**Response:** `200 OK` — Token juga di-set sebagai cookie `XSRF-TOKEN`.

```json
{
  "csrfToken": "csrf-token-value"
}
```

---

## Health & Monitoring

> Actuator endpoints berjalan pada management port terpisah `8081` (bukan `8080`). Endpoint `metrics` dan `prometheus` memerlukan autentikasi; `health` dan `info` bersifat publik.

### Health Check

```http
GET http://localhost:8081/actuator/health
```

**Response:** `200 OK`

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" },
    "externalAPI": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

### Metrics

```http
GET http://localhost:8081/actuator/metrics
Authorization: Bearer <jwt-token>
```

**Response:** `200 OK` — Micrometer metrics. Endpoint Prometheus tersedia di `GET http://localhost:8081/actuator/prometheus` (juga memerlukan autentikasi).

---

## Error Handling

All errors follow RFC 7807 ProblemDetail format:

```json
{
  "type": "https://api.oms.com/errors/400",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/v1/orders",
  "timestamp": "2026-08-10T12:00:00"
}
```

### Common HTTP Status Codes

| Status | Code | Description |
|--------|------|-------------|
| 200 | OK | Successful request |
| 201 | Created | Resource created successfully |
| 204 | No Content | Resource deleted successfully |
| 400 | Bad Request | Invalid input / validation error |
| 401 | Unauthorized | Missing or invalid authentication |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource not found |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Server error |

---

## Rate Limiting

Rate limiting is applied per endpoint. Headers in response:

| Header | Description |
|--------|-------------|
| `X-RateLimit-Limit` | Maximum requests allowed |
| `X-RateLimit-Remaining` | Remaining requests in window |
| `X-RateLimit-Reset` | Seconds until window resets |

When limit exceeded, a `429 Too Many Requests` response is returned.

> Detail implementasi lengkap (token bucket Bucket4j, bucket store lokal/Redis, per-endpoint limit, resolver client key) ada di [`docs/current/development/features/cross-cutting/28-rate-limit.md`](development/features/cross-cutting/28-rate-limit.md).

---

## API Versioning

API is versioned via URL path prefix: `/api/v1/`

Future versions will use `/api/v2/`, etc. The `Api-Version` request header can also be used for version negotiation.