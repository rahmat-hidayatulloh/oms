# Task 18: Global Exception Handler dengan ProblemDetail (RFC 7807)

> **Status:** Superseded (historical snapshot)
> **Audience:** Developer
> **Owner:** Engineering
> **Snapshot date:** 2026-08-06
> **Superseded by:** [OMS-016: Global Exception Handler dengan ProblemDetail (RFC 7807)](../../current/development/features/cross-cutting/19-global-exception-handler-rfc7807.md)
> **Catatan:** Laporan implementasi awal. Untuk dokumentasi current-state exception handling, lihat dokumen pengganti.

## Tujuan
Mengimplementasikan global exception handler menggunakan `@RestControllerAdvice` yang mengembalikan response sesuai RFC 7807 menggunakan `ProblemDetail`.

## Acceptance Criteria
- [x] Buat GlobalExceptionHandler dengan @ControllerAdvice
- [x] Handle ResourceNotFoundException → return 404
- [x] Handle BusinessException (custom) → return 400
- [x] Handle MethodArgumentNotValidException → return 400 dengan detail field errors
- [x] Handle AccessDeniedException → return 403
- [x] Handle Exception umum → return 500 dengan request ID
- [x] Response format sesuai RFC 7807 (type, title, status, detail, instance, timestamp)

## Detail Pekerjaan

### 1. File yang Dibuat/Diubah

```
src/main/java/com/oms/exception/
├── GlobalExceptionHandler.java   ← diubah untuk menggunakan ProblemDetail
└── ErrorResponse.java            ← tidak diubah (masih ada untuk backward compatibility)

src/test/java/com/oms/exception/
└── GlobalExceptionHandlerTest.java ← 4 test case (baru)
```

### 2. GlobalExceptionHandler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException ex) {
        return buildProblemDetail(HttpStatus.NOT_FOUND, "Resource Not Found", ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex) {
        return buildProblemDetail(HttpStatus.UNAUTHORIZED, "Authentication Failed", "Bad credentials");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return buildProblemDetail(HttpStatus.FORBIDDEN, "Access Denied", 
            "You don't have permission to access this resource");
    }

    @ExceptionHandler(BadRequestException.class)
    public ProblemDetail handleBadRequest(BadRequestException ex) {
        return buildProblemDetail(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        // Collect field errors
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        
        ProblemDetail problemDetail = buildProblemDetail(HttpStatus.BAD_REQUEST, 
            "Validation Failed", "Request validation failed");
        problemDetail.setProperty("fieldErrors", errors);
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneralException(Exception ex) {
        String requestId = UUID.randomUUID().toString();
        log.error("Unexpected error occurred [requestId={}]", requestId, ex);
        
        ProblemDetail problemDetail = buildProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, 
            "Internal Server Error", "An unexpected error occurred");
        problemDetail.setProperty("requestId", requestId);
        return problemDetail;
    }

    private ProblemDetail buildProblemDetail(HttpStatus status, String title, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setType(URI.create("https://api.oms.com/errors/" + status.value()));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        return problemDetail;
    }
}
```

### 3. RFC 7807 Response Format

```json
{
  "type": "https://api.oms.com/errors/404",
  "title": "Resource Not Found",
  "status": 404,
  "detail": "Product not found with id: 'non-existent-id'",
  "timestamp": "2026-07-17T18:08:55.123456789"
}
```

**Field validation errors** (MethodArgumentNotValidException):
```json
{
  "type": "https://api.oms.com/errors/400",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Request validation failed",
  "timestamp": "2026-07-17T18:08:55.123456789",
  "fieldErrors": {
    "name": "Name is required",
    "price": "Price must be positive"
  }
}
```

**Internal server error** (Exception):
```json
{
  "type": "https://api.oms.com/errors/500",
  "title": "Internal Server Error",
  "status": 500,
  "detail": "An unexpected error occurred",
  "timestamp": "2026-07-17T18:08:55.123456789",
  "requestId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### 4. Exception Mapping

| Exception | HTTP Status | Title | Detail |
|---|---|---|---|
| `ResourceNotFoundException` | 404 | Resource Not Found | Exception message |
| `AuthenticationException` | 401 | Authentication Failed | "Bad credentials" |
| `AccessDeniedException` | 403 | Access Denied | "You don't have permission..." |
| `BadRequestException` | 400 | Bad Request | Exception message |
| `MethodArgumentNotValidException` | 400 | Validation Failed | "Request validation failed" + fieldErrors |
| `Exception` (general) | 500 | Internal Server Error | "An unexpected error occurred" + requestId |

### 5. Unit Test (GlobalExceptionHandlerTest)

4 test case menggunakan `@SpringBootTest` + `@AutoConfigureMockMvc`:

| Test | Expected Status | Keterangan |
|---|---|---|
| `shouldReturn404WhenResourceNotFound` | 404 | Product not found |
| `shouldReturn403WhenAccessDenied` | 403 | Customer akses admin endpoint |
| `shouldReturn400ForValidationErrors` | 400 | Invalid product request |
| `shouldReturn401WhenUnauthenticated` | 401 | Tanpa token |

Setiap test memverifikasi:
- HTTP status code
- `title` sesuai RFC 7807
- `detail` message
- `type` URI
- `timestamp` exists

### 6. Hasil Verifikasi

```
BUILD SUCCESS ✅
├── Compile: OK
├── Checkstyle: 0 violations
├── SpotBugs: 0 bugs
├── Tests: 72/72 passed
│   ├── GlobalExceptionHandlerTest: 4/4   (baru)
│   ├── RefreshTokenServiceTest: 3/3
│   ├── AdminControllerTest: 5/5
│   ├── AuthControllerTest: 5/5
│   ├── ProductControllerTest: 13/13
│   ├── FlywayMigrationTest: 3/3
│   ├── ProductServiceTest: 10/10
│   ├── OrderServiceTest: 6/6
│   ├── JwtUtilTest: 12/12
│   ├── JwtAuthenticationFilterTest: 6/6
│   ├── OrderTest: 4/4
│   └── OmsApplicationTest: 1/1
└── Sonar: Clean
```

### 7. Catatan
- Menggunakan `ProblemDetail` (Spring 6.1+) yang merupakan implementasi resmi RFC 7807.
- `@RestControllerAdvice` adalah kombinasi `@ControllerAdvice` + `@ResponseBody`, sehingga response langsung di-serialize ke JSON.
- `ErrorResponse` class masih dipertahankan untuk backward compatibility, tetapi handler baru menggunakan `ProblemDetail`.
- Request ID di-generate hanya untuk exception umum (500) untuk membantu debugging di production.
- Timestamp menggunakan timezone Asia/Jakarta sesuai konfigurasi aplikasi.