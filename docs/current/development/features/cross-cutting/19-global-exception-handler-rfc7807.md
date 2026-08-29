# OMS-016: Global Exception Handler dengan ProblemDetail (RFC 7807)

> **Status:** Current
> **Audience:** Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-28 (screening kode aktual `src/`, `pom.xml`)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## 📋 Deskripsi

Mengimplementasikan global exception handling menggunakan `@RestControllerAdvice` yang mengembalikan response sesuai standar **RFC 7807 Problem Details for HTTP APIs**.

> **Penting:** Handler tunggal `GlobalExceptionHandler` sudah **`@Deprecated(forRemoval = true)`**
> dan digantikan oleh handler spesifik domain yang dipecah per kategori
> (Security, Validation, Business, Infrastructure). Kelas lama hanya tersisa
> sebagai placeholder kosong.

RFC 7807 mendefinisikan struktur response error yang konsisten dengan field:
- `type` — URI yang mengidentifikasi tipe masalah (base URL dari `app.error.base-url`)
- `title` — Judul singkat yang human-readable
- `status` — HTTP status code
- `detail` — Penjelasan spesifik tentang masalah
- `instance` — URI yang merujuk ke endpoint yang menghasilkan error
- `timestamp` — Waktu terjadinya error (custom property)

## 🏗️ Struktur File

```
src/main/java/com/oms/exception/
├── ErrorCode.java                  ← Enum: kode error + HTTP status + title
├── BaseExceptionHandler.java       ← Abstract: build RFC 7807 ProblemDetail
├── SecurityExceptionHandler.java   ← @RestControllerAdvice: auth/authorization/JWT
├── ValidationExceptionHandler.java ← @RestControllerAdvice: validasi request
├── BusinessExceptionHandler.java   ← @RestControllerAdvice: business/routing
├── InfrastructureExceptionHandler.java ← @RestControllerAdvice: Redis/DB/async
├── AsyncExceptionHandler.java      ← AsyncUncaughtExceptionHandler untuk @Async
├── BusinessException.java          ← Custom exception → HTTP 400
├── BadRequestException.java        ← Bad request → HTTP 400
├── ResourceNotFoundException.java  ← Resource tidak ditemukan → HTTP 404
└── GlobalExceptionHandler.java     ← @Deprecated(forRemoval) placeholder
```

> **Catatan:** `ErrorResponse.java` telah dihapus karena fungsinya digantikan sepenuhnya oleh `ProblemDetail` dari Spring Framework. Base URL untuk field `type` kini di-externalize via `ErrorProperties` (`app.error.base-url`, default `https://api.oms.com/errors`).

## 🎯 Acceptance Criteria

| No | Kriteria | Status |
|----|----------|--------|
| 1 | Handler `@RestControllerAdvice` spesifik domain (Security, Validation, Business, Infrastructure) | ✅ |
| 2 | `ResourceNotFoundException` → HTTP 404 | ✅ |
| 3 | `BusinessException` (custom) → HTTP 400 | ✅ |
| 4 | `MethodArgumentNotValidException` → HTTP 400 + field errors | ✅ |
| 5 | `AccessDeniedException` → HTTP 403 | ✅ |
| 6 | Error infrastruktur (Redis/DB/async/optimistic lock) → 503/500/409 eksplisit | ✅ |
| 7 | Response format sesuai RFC 7807 dengan `ErrorCode` terpusat | ✅ |

## 🔧 Implementasi

### BaseExceptionHandler (shared RFC 7807 builder)

Semua handler spesifik domain mewarisi `BaseExceptionHandler` yang membangun
`ProblemDetail` dari enum `ErrorCode` dan base URL yang di-externalize:

```java
public abstract class BaseExceptionHandler {

    private final ErrorProperties errorProperties;   // app.error.base-url
    private final HttpServletRequest request;

    protected ProblemDetail build(ErrorCode errorCode, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                errorCode.getHttpStatus(), detail);
        pd.setTitle(errorCode.getTitle());
        pd.setType(URI.create(errorProperties.getBaseUrl() + "/" + errorCode.getStatusValue()));
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("timestamp", LocalDateTime.now());
        return pd;
    }
}
```

`ErrorCode` adalah enum terpusat yang memetakan setiap error ke HTTP status
dan title (mis. `NOT_FOUND` → 404 "Resource Not Found", `BUSINESS_ERROR` →
400 "Business Error", `CONFLICT` → 409, `SERVICE_UNAVAILABLE` → 503).

### Handler Spesifik Domain

Setiap handler ber-`@RestControllerAdvice(basePackages = "com.oms")` dan
menangani kategori exception tertentu:

| Handler | Exception yang ditangani |
|---|---|
| `SecurityExceptionHandler` | `AuthenticationException` (401), `AccessDeniedException` (403), JWT errors (`ExpiredJwtException`, `MalformedJwtException`, `SecurityException` → 401) |
| `ValidationExceptionHandler` | `MethodArgumentNotValidException`, `BindException`, `ConstraintViolationException` (400 + field errors), `BadRequestException`, `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException`, `HttpRequestMethodNotSupportedException`, `MissingServletRequestParameterException`, `HttpMediaTypeNotSupportedException` |
| `BusinessExceptionHandler` | `ResourceNotFoundException` (404), `BusinessException` (400), `NoHandlerFoundException` (404), `MissingPathVariableException` (400) |
| `InfrastructureExceptionHandler` | `RedisConnectionFailureException` (503), `AsyncRequestTimeoutException` (503), `DataIntegrityViolationException` (500), `OptimisticLockException` (409) |

#### Contoh: ResourceNotFoundException → HTTP 404 (BusinessExceptionHandler)

```java
@ExceptionHandler(ResourceNotFoundException.class)
public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
    log.warn("Resource not found: {}", ex.getMessage());
    return build(ErrorCode.NOT_FOUND, ex.getMessage());
}
```

**Response Example:**
```json
{
    "type": "https://api.oms.com/errors/404",
    "title": "Resource Not Found",
    "status": 404,
    "detail": "Product not found with id: '12345678-1234-1234-1234-123456789012'",
    "instance": "/api/v1/products/12345678-1234-1234-1234-123456789012",
    "timestamp": "2026-07-23T08:30:00"
}
```

#### 2. BusinessException → HTTP 400 (BusinessExceptionHandler)

```java
@ExceptionHandler(BusinessException.class)
public ProblemDetail handleBusiness(BusinessException ex) {
    log.warn("Business error: {}", ex.getMessage());
    return build(ErrorCode.BUSINESS_ERROR, ex.getMessage());
}
```

**Response Example:**
```json
{
    "type": "https://api.oms.com/errors/400",
    "title": "Business Error",
    "status": 400,
    "detail": "Product name 'Kaos Polos Hitam' already exists",
    "instance": "/api/v1/products",
    "timestamp": "2026-07-23T08:30:00"
}
```

#### 3. MethodArgumentNotValidException → HTTP 400 + Field Errors (ValidationExceptionHandler)

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
    return withFieldErrors(build(ErrorCode.VALIDATION_FAILED, "Request validation failed"),
            ex.getBindingResult());
}

private ProblemDetail withFieldErrors(ProblemDetail pd, BindingResult br) {
    Map<String, String> errors = new HashMap<>();
    for (FieldError fe : br.getFieldErrors()) {
        errors.put(fe.getField(), fe.getDefaultMessage());
    }
    pd.setProperty("fieldErrors", errors);
    return pd;
}
```

**Response Example:**
```json
{
    "type": "https://api.oms.com/errors/400",
    "title": "Validation Failed",
    "status": 400,
    "detail": "Request validation failed",
    "instance": "/api/v1/products",
    "fieldErrors": {
        "name": "Product name must be between 3 and 100 characters",
        "price": "Price must be greater than 0",
        "category": "Category is required"
    },
    "timestamp": "2026-07-23T08:30:00"
}
```

#### 4. AccessDeniedException → HTTP 403 (SecurityExceptionHandler)

```java
@ExceptionHandler(AccessDeniedException.class)
public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
    log.warn("Access denied: {}", ex.getMessage());
    return build(ErrorCode.FORBIDDEN,
            "You don't have permission to access this resource");
}
```

**Response Example:**
```json
{
    "type": "https://api.oms.com/errors/403",
    "title": "Access Denied",
    "status": 403,
    "detail": "You don't have permission to access this resource",
    "instance": "/api/admin/dashboard",
    "timestamp": "2026-07-23T08:30:00"
}
```

#### 5. Error Infrastruktur → HTTP 500/503/409 (InfrastructureExceptionHandler)

Tidak ada lagi catch-all `Exception.class`. Error infrastruktur ditangani
eksplisit; error tak terduga yang tidak tertangani handler mana pun akan
menghasilkan 500 default dari Spring Boot.

```java
@ExceptionHandler(DataIntegrityViolationException.class)
public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
    log.error("Data integrity violation: {}", ex.getMessage());
    return build(ErrorCode.INTERNAL_ERROR,
            "An unexpected error occurred. Please try again later.");
}

@ExceptionHandler(OptimisticLockException.class)
public ProblemDetail handleOptimisticLock(OptimisticLockException ex) {
    log.warn("Optimistic lock conflict: {}", ex.getMessage());
    return build(ErrorCode.CONFLICT,
            "Resource was modified by another request. Please retry.");
}

@ExceptionHandler(RedisConnectionFailureException.class)
public ProblemDetail handleRedisConnectionFailure(RedisConnectionFailureException ex) {
    log.error("Redis connection failed: {}", ex.getMessage());
    return build(ErrorCode.SERVICE_UNAVAILABLE,
            "Cache service is currently unavailable");
}
```

**Response Example (409 Optimistic Lock):**
```json
{
    "type": "https://api.oms.com/errors/409",
    "title": "Resource Conflict",
    "status": 409,
    "detail": "Resource was modified by another request. Please retry.",
    "instance": "/api/v1/products",
    "timestamp": "2026-07-23T08:30:00"
}
```

## 📊 Exception Handling Matrix

| Exception Class | Handler | HTTP Status | ErrorCode / Title |
|----------------|---------|-------------|-------------------|
| `ResourceNotFoundException` | `BusinessExceptionHandler` | 404 | `NOT_FOUND` — Resource Not Found |
| `BusinessException` | `BusinessExceptionHandler` | 400 | `BUSINESS_ERROR` — Business Error |
| `NoHandlerFoundException` | `BusinessExceptionHandler` | 404 | `NOT_FOUND` — Resource Not Found |
| `MissingPathVariableException` | `BusinessExceptionHandler` | 400 | `BAD_REQUEST` — Bad Request |
| `AuthenticationException` | `SecurityExceptionHandler` | 401 | `UNAUTHORIZED` — Authentication Failed |
| `AccessDeniedException` | `SecurityExceptionHandler` | 403 | `FORBIDDEN` — Access Denied |
| `ExpiredJwtException` / `MalformedJwtException` / `SecurityException` | `SecurityExceptionHandler` | 401 | `UNAUTHORIZED` — Authentication Failed |
| `MethodArgumentNotValidException` / `BindException` / `ConstraintViolationException` | `ValidationExceptionHandler` | 400 | `VALIDATION_FAILED` — Validation Failed (+ `fieldErrors`) |
| `BadRequestException` | `ValidationExceptionHandler` | 400 | `BAD_REQUEST` — Bad Request |
| `HttpMessageNotReadableException` | `ValidationExceptionHandler` | 400 | `MALFORMED_REQUEST` — Malformed Request Body |
| `MethodArgumentTypeMismatchException` | `ValidationExceptionHandler` | 400 | `TYPE_MISMATCH` — Type Mismatch |
| `HttpRequestMethodNotSupportedException` | `ValidationExceptionHandler` | 405 | `METHOD_NOT_ALLOWED` — Method Not Allowed |
| `MissingServletRequestParameterException` | `ValidationExceptionHandler` | 400 | `BAD_REQUEST` — Bad Request |
| `HttpMediaTypeNotSupportedException` | `ValidationExceptionHandler` | 415 | `UNSUPPORTED_MEDIA_TYPE` — Unsupported Media Type |
| `DataIntegrityViolationException` | `InfrastructureExceptionHandler` | 500 | `INTERNAL_ERROR` — Internal Server Error |
| `OptimisticLockException` | `InfrastructureExceptionHandler` | 409 | `CONFLICT` — Resource Conflict |
| `RedisConnectionFailureException` | `InfrastructureExceptionHandler` | 503 | `SERVICE_UNAVAILABLE` — Service Unavailable |
| `AsyncRequestTimeoutException` | `InfrastructureExceptionHandler` | 503 | `SERVICE_UNAVAILABLE` — Service Unavailable |

## 🧪 Testing

### Test Classes

| Test Class | Tipe | Fokus |
|---|---|---|
| `GlobalExceptionHandlerTest` | `@SpringBootTest` + `@AutoConfigureMockMvc` | End-to-end RFC 7807 responses via MockMvc (404 resource, 400 business, 400 validation + fieldErrors, 403 access denied, 403 unauthenticated, 400 malformed body, 400 type mismatch) |
| `GlobalExceptionHandlerGeneralTest` | Unit (Mockito) | `SecurityExceptionHandler`: `AuthenticationException` → 401, `AccessDeniedException` → 403 |
| `GlobalExceptionHandlerEdgeTest` | Unit (Mockito) | `BusinessExceptionHandler` / `ValidationExceptionHandler`: business, bad request, not found, default error base URL |
| `AsyncExceptionHandlerTest` | Unit (Mockito) | Async exception handling |
| `BusinessExceptionTest` / `BadRequestExceptionTest` / `ResourceNotFoundExceptionTest` | Unit | Exception class behavior |

> **Catatan:** Untuk request *unauthenticated*, Spring Security menolak request
> di filter chain sebelum controller dipanggil, sehingga respons default 403
> (bukan ProblemDetail) yang dikembalikan. Handler ProblemDetail hanya aktif
> untuk request terautentikasi yang mencapai controller. `AccessDeniedException`
> hanya terjadi ketika user sudah terautentikasi tetapi tidak memiliki role
> yang dibutuhkan.

### Running Tests

```bash
# Run all exception handler tests
./mvnw test -Dtest='GlobalExceptionHandler*Test'

# Run a single test class
./mvnw test -Dtest=GlobalExceptionHandlerTest

# Run all tests
./mvnw test
```

## 🔗 Referensi

- [RFC 7807 - Problem Details for HTTP APIs](https://tools.ietf.org/html/rfc7807)
- [Spring Framework - ProblemDetail](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/http/ProblemDetail.html)
- [Spring Boot - Error Handling](https://docs.spring.io/spring-boot/docs/current/reference/html/web.html#web.servlet.spring-mvc.error-handling)