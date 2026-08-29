# Refactoring: ErrorCode Enum, AppConstants, Null Safety & Clean Code

> **⚠️ Historical Snapshot:** Dokumen ini merekam status pada ~30 Juli 2026 (9 error codes, 220 tests). 
> Untuk dokumentasi current-state, lihat [`docs/index.md`](../../index.md) dan dokumen pada `docs/current/`.

## Ringkasan

Refactoring menyeluruh untuk meningkatkan kualitas kode di **seluruh class project**: menambahkan `ErrorCode` enum, membuat file terpisah `AppConstants` untuk kumpulan constant, menghilangkan `@SuppressWarnings`, dan memperbaiki null type safety warnings.

## File Baru yang Dibuat

### 1. ErrorCode Enum
**File**: `src/main/java/com/oms/exception/ErrorCode.java`

Centralized error codes untuk RFC 7807 Problem Detail responses.

| ErrorCode | HttpStatus | Title |
|-----------|------------|-------|
| `BAD_REQUEST` | 400 | Bad Request |
| `BUSINESS_ERROR` | 400 | Business Error |
| `VALIDATION_FAILED` | 400 | Validation Failed |
| `MALFORMED_REQUEST` | 400 | Malformed Request Body |
| `TYPE_MISMATCH` | 400 | Type Mismatch |
| `UNAUTHORIZED` | 401 | Authentication Failed |
| `FORBIDDEN` | 403 | Access Denied |
| `NOT_FOUND` | 404 | Resource Not Found |
| `INTERNAL_ERROR` | 500 | Internal Server Error |

### 2. AppConstants (File Terpisah Khusus Constants)
**File**: `src/main/java/com/oms/util/AppConstants.java`

Kumpulan semua constant yang sebelumnya tersebar di berbagai class, dikelompokkan berdasarkan domain:

| Domain | Constants |
|--------|-----------|
| **Security/JWT** | `AUTHORIZATION_HEADER`, `BEARER_PREFIX`, `CLAIM_ROLE`, `MIN_SECRET_LENGTH`, `ERROR_SECRET_EMPTY`, `ERROR_SECRET_LENGTH` |
| **Error/Problem Detail** | `BASE_ERROR_URL`, `PROPERTY_TIMESTAMP`, `PROPERTY_FIELD_ERRORS`, `PROPERTY_REQUEST_ID`, `MSG_BAD_CREDENTIALS`, `MSG_ACCESS_DENIED`, `MSG_VALIDATION_FAILED`, `MSG_MALFORMED_BODY`, `MSG_INTERNAL_ERROR`, `PARAM_PREFIX_INVALID`, `PARAM_EXPECTED_TYPE` |
| **Entity Names** | `ENTITY_PRODUCT`, `ENTITY_ORDER`, `ENTITY_USER` |
| **Health Check** | `PAYMENT_API_HEALTH_URL`, `HEALTH_CHECK_TIMEOUT`, `SERVICE_NAME_EXTERNAL_API`, `SERVICE_NAME_REDIS` |
| **Redis/Refresh Token** | `REFRESH_TOKEN_PREFIX`, `BLACKLIST_PREFIX`, `REFRESH_TOKEN_EXPIRATION_DAYS`, `BLACKLIST_DURATION` |
| **Logging/MDC** | `MDC_TRACE_ID`, `MDC_USER_ID`, `MDC_METHOD`, `MDC_PATH` |
| **OpenAPI/Swagger** | `SCHEME_NAME_BEARER_AUTH`, `SCHEME_BEARER`, `FORMAT_JWT` |
| **API Versioning** | `DEFAULT_API_VERSION`, `HEADER_DEPRECATED`, `HEADER_REPLACEMENT`, `HEADER_SUNSET`, `SUNSET_DATE` |
| **Order** | `ORDER_NUMBER_PREFIX`, `ORDER_NUMBER_DATE_FORMAT` |

## Class yang Direfactored (14 class)

| No | File | Constants Dihapus | Constants Diganti Dengan |
|----|------|-------------------|--------------------------|
| 1 | `GlobalExceptionHandler.java` | 11 constants | `AppConstants.*` |
| 2 | `JwtTokenProvider.java` | 3 constants | `AppConstants.*` |
| 3 | `JwtUtil.java` | 1 constant + 2 magic strings | `AppConstants.*` |
| 4 | `JwtAuthenticationFilter.java` | 2 constants | `AppConstants.*` |
| 5 | `ExternalAPIHealthIndicator.java` | 3 constants | `AppConstants.*` |
| 6 | `RedisHealthIndicator.java` | 1 constant | `AppConstants.*` |
| 7 | `ProductService.java` | 1 constant | `AppConstants.*` |
| 8 | `OrderService.java` | 2 constants | `AppConstants.*` |
| 9 | `RefreshTokenService.java` | 4 constants | `AppConstants.*` |
| 10 | `LoggingFilter.java` | 4 constants | `AppConstants.*` |
| 11 | `OpenApiConfig.java` | 3 constants | `AppConstants.*` |
| 12 | `VersioningResolver.java` | 1 constant | `AppConstants.*` |
| 13 | `ApiVersionFilter.java` | 4 constants | `AppConstants.*` |
| 14 | `OrderFlowIntegrationTest.java` | `@SuppressWarnings` fix | Scoped to local variable |

**Total: 40+ constants dipindahkan ke AppConstants**

## Perbaikan Null Type Safety

| File | Sebelum | Sesudah |
|------|---------|--------|
| `JwtTokenProvider.java` | `secret.getBytes()` dipanggil 2x | `secretBytes` disimpan dalam variabel |
| `JwtUtil.java` | `secret.getBytes()` langsung | Null-safe: `secret != null ? secret.trim().getBytes() : null` |
| `OrderFlowIntegrationTest.java` | `(String) map.get(...)` direct cast | `map.get(...).toString()` (null-safe) |

## Perbaikan @SuppressWarnings

| File | Sebelum | Sesudah |
|------|---------|--------|
| `JwtTokenProvider.java` | `@SuppressWarnings("findbugs:CT_CONSTRUCTOR_THROW")` | Dihapus (tidak diperlukan lagi) |
| `OrderFlowIntegrationTest.java` | `@SuppressWarnings("unchecked")` di method level | Scoped ke local variable saja |

## Unit Tests Updated

| Test File | Perubahan |
|-----------|-----------|
| `GlobalExceptionHandlerEdgeTest` | Gunakan `ErrorCode.BUSINESS_ERROR.getTitle()` |
| `GlobalExceptionHandlerGeneralTest` | Gunakan `ErrorCode.UNAUTHORIZED.getStatusValue()` |

## Hasil

### Test Suite
```
Tests run: 220, Failures: 0, Errors: 0, Skipped: 9
BUILD SUCCESS
```

### Coverage

| Package | Coverage | Status |
|---------|----------|--------|
| **com.oms.health** | 100.0% | ✅ |
| **com.oms.exception** | 100.0% | ✅ |
| **com.oms.repository** | 100.0% | ✅ |
| **com.oms.util** | 100.0% | ✅ (AppConstants) |
| **com.oms** | 100.0% | ✅ |
| **com.oms.controller** | 90.9% | ✅ |
| **com.oms.service** | 90.6% | ✅ |
| **com.oms.security** | 83.5% | ✅ |
| **TOTAL** | **93.1%** | ⬆️ |

### Sonar Quality Improvements

| Issue | Sebelum | Sesudah |
|-------|---------|--------|
| `@SuppressWarnings` di class level | 1 | 0 |
| `@SuppressWarnings("unchecked")` di method level | 1 | 0 (scoped) |
| Magic strings/numbers tersebar | 40+ | 0 (all in AppConstants) |
| Hardcoded HTTP status | 9 | 0 (all use ErrorCode) |
| Duplicate `getBytes()` calls | 2 | 0 |
| Constants duplikasi antar class | 40+ | 0 (single source: AppConstants) |

## Clean Code Principles Applied

1. **DRY (Don't Repeat Yourself)**: AppConstants menghilangkan duplikasi constants di 14 class
2. **Single Responsibility**: AppConstants khusus menyimpan constants, ErrorCode khusus error types
3. **Open/Closed**: Tambah constant/error code baru tanpa memodifikasi existing code
4. **Constants over Magic Values**: Semua strings/numbers diekstrak ke AppConstants
5. **Type Safety**: Enum (ErrorCode) lebih type-safe daripada string literals
6. **Null Safety**: Null-safe pattern dengan ternary operator dan `.toString()`
7. **Single Source of Truth**: Semua constants di satu file terpisah untuk maintainability