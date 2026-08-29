# AppConstants Externalization Analysis

> **Tanggal:** 3 Agustus 2026
> **Tujuan:** Analisis konstanta di AppConstants.java yang harus dipindah ke application.yml

---

## 📋 Daftar Konstanta yang HARUS Dipindah ke application.yml

### 1. URL/Endpoint (1 konstanta)

| Konstanta | Nilai | Alasan |
|-----------|-------|--------|
| `PAYMENT_API_HEALTH_URL` | `http://localhost:9090/health` | URL eksternal yang berbeda per environment (dev/staging/prod) |

### 2. Timeout/Duration (5 konstanta)

| Konstanta | Nilai | Alasan |
|-----------|-------|--------|
| `HEALTH_CHECK_TIMEOUT` | `5 seconds` | Timeout bisa di-tuning per environment |
| `BLACKLIST_DURATION` | `1 hour` | Bisa diubah tanpa recompile |
| `CACHE_TTL_DEFAULT` | `30 minutes` | Cache TTL bisa di-tuning |
| `REFRESH_TOKEN_EXPIRATION_DAYS` | `7` | Security policy bisa berubah |
| `BLACKLIST_DURATION` | `1 hour` | Bisa diubah sesuai kebutuhan |

### 3. Thread Pool (3 konstanta)

| Konstanta | Nilai | Alasan |
|-----------|-------|--------|
| `EMAIL_THREAD_POOL_CORE_SIZE` | `5` | Thread pool bisa di-tuning per environment |
| `EMAIL_THREAD_POOL_MAX_SIZE` | `10` | Thread pool bisa di-tuning |
| `EMAIL_QUEUE_CAPACITY` | `20` | Queue capacity bisa di-tuning |

### 4. Retry (2 konstanta)

| Konstanta | Nilai | Alasan |
|-----------|-------|--------|
| `RETRY_BACKOFF_DELAY_MS` | `100` | Retry config bisa di-tuning |
| `RETRY_MAX_ATTEMPTS` | `3` | Retry policy bisa berubah |

### 5. Rate Limiting (2 konstanta)

| Konstanta | Nilai | Alasan |
|-----------|-------|--------|
| `RATE_LIMIT_MAX_REQUESTS_PER_MINUTE` | `10` | Rate limit bisa di-tuning per environment |
| `RATE_LIMIT_WINDOW_MS` | `60000` | Window size bisa diubah |

### 6. Default Values yang Bisa Berubah (7 konstanta)

| Konstanta | Nilai | Alasan |
|-----------|-------|--------|
| `DEFAULT_PAGE_SIZE` | `20` | Pagination bisa diubah |
| `DASHBOARD_DEFAULT_TOTAL_USERS` | `150` | Default value untuk dashboard |
| `DASHBOARD_DEFAULT_TOTAL_PRODUCTS` | `45` | Default value untuk dashboard |
| `DASHBOARD_DEFAULT_TOTAL_ORDERS` | `320` | Default value untuk dashboard |
| `DASHBOARD_DEFAULT_REVENUE` | `15000000` | Default value untuk dashboard |
| `DEFAULT_FALLBACK_EMAIL` | `unknown@example.com` | Fallback email bisa diubah |
| `MIN_SECRET_LENGTH` | `32` | Security policy bisa berubah |

**Total: 20 konstanta yang harus dipindah**

---

## 📊 Konstanta yang TIDAK Perlu Dipindah

### Security/JWT (tetap di AppConstants)
- `AUTHORIZATION_HEADER`, `BEARER_PREFIX`, `CLAIM_ROLE`, `CLAIM_TOKEN_TYPE`
- `TOKEN_TYPE_ACCESS`, `TOKEN_TYPE_REFRESH`, `ROLE_PREFIX`, `ROLE_ADMIN`
- `ERROR_SECRET_EMPTY`, `ERROR_SECRET_LENGTH`

**Alasan:** String constants yang tetap sama di semua environment.

### Error Messages (tetap di AppConstants)
- `MSG_BAD_CREDENTIALS`, `MSG_ACCESS_DENIED`, `MSG_VALIDATION_FAILED`, dll.

**Alasan:** Pesan error yang tetap sama di semua environment.

### Entity Names (tetap di AppConstants)
- `ENTITY_PRODUCT`, `ENTITY_ORDER`, `ENTITY_USER`

**Alasan:** Nama entity yang tetap sama.

### Logging/MDC (tetap di AppConstants)
- `MDC_TRACE_ID`, `MDC_USER_ID`, `MDC_METHOD`, `MDC_PATH`

**Alasan:** MDC key names yang tetap sama.

### OpenAPI/Swagger (tetap di AppConstants)
- `SCHEME_NAME_BEARER_AUTH`, `SCHEME_BEARER`, `FORMAT_JWT`

**Alasan:** OpenAPI configuration yang tetap sama.

### API Versioning (tetap di AppConstants)
- `DEFAULT_API_VERSION`, `HEADER_DEPRECATED`, `HEADER_REPLACEMENT`, `HEADER_SUNSET`, `SUNSET_DATE`

**Alasan:** API versioning configuration yang tetap sama.

### Order (tetap di AppConstants)
- `ORDER_NUMBER_PREFIX`, `ORDER_NUMBER_DATE_FORMAT`, `ORDER_NUMBER_RANDOM_LENGTH`

**Alasan:** Business logic yang tetap sama.

### Cache Names (tetap di AppConstants)
- `CACHE_PRODUCTS`, `CACHE_PRODUCTS_LIST`, `CACHE_KEY_ALL`

**Alasan:** Cache key names yang tetap sama.

### Sort Fields (tetap di AppConstants)
- `SORT_NAME`, `SORT_PRICE`, `SORT_CREATED_AT`, `ALLOWED_SORT_FIELDS`

**Alasan:** Sort field names yang tetap sama.

### Version Info (tetap di AppConstants)
- `VERSION_CURRENT`, `VERSION_AVAILABLE`, `VERSION_DEPRECATED`, `VERSION_LATEST`, `VERSION_API_DOCS`, `VERSION_SERVER_TIME`, `SWAGGER_UI_PATH`

**Alasan:** Version info yang tetap sama.

### Dashboard Keys (tetap di AppConstants)
- `DASHBOARD_TOTAL_USERS`, `DASHBOARD_TOTAL_PRODUCTS`, `DASHBOARD_TOTAL_ORDERS`, `DASHBOARD_REVENUE`

**Alasan:** JSON key names yang tetap sama.

### Validation Messages (tetap di AppConstants)
- `MSG_PRODUCT_NAME_EXISTS`, `MSG_QUANTITY_POSITIVE`, `MSG_INSUFFICIENT_STOCK`, `MSG_USER_NOT_FOUND`

**Alasan:** Validation message templates yang tetap sama.

### Timezone (tetap di AppConstants)
- `APP_TIMEZONE = "Asia/Jakarta"`

**Alasan:** Application timezone yang tetap sama.

---

## 🎯 Kesimpulan

**20 konstanta harus dipindah ke application.yml** karena:
1. Bisa berubah per environment (dev/staging/prod)
2. Bisa di-tuning tanpa recompile
3. Best practice untuk configuration management

**40 konstanta tetap di AppConstants** karena:
1. Tetap sama di semua environment
2. String constants untuk key names, headers, messages
3. Business logic yang tidak berubah

---

*Dokumen ini dibuat untuk analisis sebelum implementasi externalization configuration*