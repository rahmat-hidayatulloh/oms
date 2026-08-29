# OMS-022: Setup Redis Cache Configuration

> **Status:** Current
> **Audience:** Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-28 (screening kode aktual `src/`, `pom.xml`)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## 📋 Deskripsi

Mengkonfigurasi **Redis sebagai cache provider** untuk aplikasi Spring Boot. Redis digunakan untuk:
1. **Application Cache** — Cache data yang sering diakses (products, orders, etc.)
2. **Refresh Token Storage** — Menyimpan JWT refresh tokens (sudah ada)
3. **Token Blacklist** — Menyimpan revoked JWT tokens (sudah ada)

## 🎯 Acceptance Criteria

| No | Kriteria | Status |
|----|----------|--------|
| 1 | Dependency: `spring-boot-starter-data-redis` | ✅ |
| 2 | `@EnableCaching` | ✅ |
| 3 | `RedisCacheConfiguration` dengan TTL dari `spring.data.redis.cache.ttl` (default 3600 detik) | ✅ |
| 4 | Serialization menggunakan `GenericJackson2JsonRedisSerializer` (+ `ObjectMapper` dengan JavaTimeModule) | ✅ |
| 5 | Connection via Lettuce + `RedisStandaloneConfiguration` (host, port, database, password) | ✅ |

## 🔍 Mengapa Redis Cache?

### Tanpa Cache

```
Request 1: GET /api/products/1
  → Query database (100ms)
  → Return product

Request 2: GET /api/products/1
  → Query database (100ms)
  → Return product

Request 3: GET /api/products/1
  → Query database (100ms)
  → Return product
```

**Masalah:**
- Repeated database queries untuk data yang sama
- High latency untuk data yang jarang berubah
- Database overload

### Dengan Redis Cache

```
Request 1: GET /api/products/1
  → Cache miss
  → Query database (100ms)
  → Store in Redis (TTL: dari spring.data.redis.cache.ttl, default 3600s/1 jam)
  → Return product

Request 2: GET /api/products/1 (dalam rentang TTL)
  → Cache hit (5ms)
  → Return product from Redis

Request 3: GET /api/products/1 (dalam rentang TTL)
  → Cache hit (5ms)
  → Return product from Redis
```

**Keuntungan:**
- **Faster response** — Redis in-memory: ~5ms vs Database: ~100ms
- **Reduced database load** — 95%+ cache hit rate = 95% less DB queries
- **Scalability** — Redis bisa handle thousands of requests per second

## 🏗️ Struktur File yang Diubah/Dibuat

```
src/main/java/com/oms/
└── config/
    ├── CacheConfig.java                  ← [BARU] @EnableCaching
    └── RedisConfig.java                  ← [DIUBAH] RedisCacheConfiguration, RedisCacheManager, RedisTemplate

src/main/resources/
└── application.yml                      ← [SUDAH ADA] spring.data.redis config

docs/
└── 25-redis-cache-config.md            ← [BARU] Dokumentasi ini
```

## 🔧 Implementasi

### 1. Dependency (pom.xml)

```xml
<!-- Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

**Sudah ada di project** — tidak perlu ditambahkan.

### 2. CacheConfig — @EnableCaching

```java
@Configuration
@EnableCaching
public class CacheConfig {
}
```

**Cara kerja:**
- `@EnableCaching` mengaktifkan Spring's caching abstraction
- Mencari cache manager bean di context (`RedisCacheManager` dari `RedisConfig`)
- Memungkinkan penggunaan `@Cacheable`, `@CacheEvict`, `@CachePut`
- `CacheConfig` aktif di **semua profil**; `RedisConfig` aktif hanya jika `spring.data.redis.enabled=true` (`@ConditionalOnProperty`, `matchIfMissing = false`)
- Tanpa config Redis aktif, Spring Boot fallback ke `ConcurrentMapCacheManager` (caching in-memory) sehingga aplikasi tetap berjalan.

### 3. RedisCacheConfiguration — TTL dari `spring.data.redis.cache.ttl`

TTL default diambil dari property `spring.data.redis.cache.ttl` (**default `3600` detik / 60 menit**) dan diterapkan ke **seluruh** cache entry melalui `entryTtl(...)`:

```java
@Configuration
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true",
        matchIfMissing = false)
public class RedisConfig {

    private final Duration cacheTtl; // @Value("${spring.data.redis.cache.ttl:3600}")

    // V4-02: constructor injection (bukan field-level @Value)
    public RedisConfig(@Value("${spring.data.redis.cache.ttl:3600}") Duration cacheTtl) {
        this.cacheTtl = cacheTtl;
    }

    @Bean
    public RedisCacheConfiguration cacheConfiguration(ObjectMapper redisObjectMapper) {
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper);
        StringRedisSerializer stringSerializer = new StringRedisSerializer();

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(cacheTtl)                     // ← spring.data.redis.cache.ttl
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext
                        .SerializationPair.fromSerializer(stringSerializer))
                .serializeValuesWith(RedisSerializationContext
                        .SerializationPair.fromSerializer(jsonSerializer));
    }
}
```

**Parameter:**
- `entryTtl(cacheTtl)` — TTL configurable, tidak di-hardcode. Override per environment via env var `SPRING_DATA_REDIS_CACHE_TTL` (misal `30m`).
- `disableCachingNullValues()` — Tidak cache null values (mencegah cache penetration)
- Key serializer `StringRedisSerializer` + value serializer `GenericJackson2JsonRedisSerializer` (dengan `ObjectMapper` custom — lihat bagian 5)
- Saat ini **tidak ada** override TTL per cache name (`withCacheConfiguration` belum dipakai)

### 4. RedisCacheManager — Cache Manager

```java
@Bean
public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory,
                                      RedisCacheConfiguration cacheConfiguration) {
    return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(cacheConfiguration)
            .build();
}
```

**Cara kerja:**
- `RedisCacheManager` adalah entry point untuk Spring Cache abstraction
- Menggunakan `RedisConnectionFactory` untuk koneksi ke Redis
- Menggunakan `RedisCacheConfiguration` untuk default cache settings

### 5. Serialization — GenericJackson2JsonRedisSerializer

```java
@Bean
public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory,
                                                   ObjectMapper redisObjectMapper) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    // Use String serializer for keys
    StringRedisSerializer stringSerializer = new StringRedisSerializer();
    template.setKeySerializer(stringSerializer);
    template.setHashKeySerializer(stringSerializer);

    // Use JSON serializer for values (dengan ObjectMapper custom)
    GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper);
    template.setValueSerializer(jsonSerializer);
    template.setHashValueSerializer(jsonSerializer);

    template.afterPropertiesSet();
    return template;
}
```

**ObjectMapper khusus (`redisObjectMapper`):**
- `registerModule(new JavaTimeModule())` — agar `LocalDateTime` dapat diserialisasi (bukan error `InvalidDefinitionException`)
- `disable(WRITE_DATES_AS_TIMESTAMPS)` — tanggal diserialisasi sebagai ISO-8601 string, bukan epoch timestamp
- `activateDefaultTyping(BasicPolymorphicTypeValidator, DefaultTyping.NON_FINAL)` — menambahkan metadata tipe (`@class`) untuk deserialisasi type-safe, dengan whitelist subtype `com.oms.*`, `java.util.*`, `java.lang.*` (menangkal deserialization arbitrary class / F-11)

**Serialization Strategy:**
- **Keys:** `StringRedisSerializer` — Human-readable, compact
- **Values:** `GenericJackson2JsonRedisSerializer` — JSON dengan metadata tipe

**Keuntungan JSON Serialization:**
- **Human-readable** — Bisa dibaca di Redis CLI
- **Type-safe** — Metadata tipe (`@class`) membuat deserialisasi mengembalikan tipe yang benar
- **Interoperable** — Format JSON tetap bisa di-read oleh tool lain, selama field yang di-serialize merupakan POJO plain

### 6. Connection — RedisStandaloneConfiguration (Lettuce)

```java
@Configuration
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true",
        matchIfMissing = false)
public class RedisConfig {

    private final String redisHost;
    private final int redisPort;
    private final int redisDatabase;
    private final String redisPassword;
    private final Duration cacheTtl;

    // V4-02: constructor injection (bukan field-level @Value)
    public RedisConfig(
            @Value("${spring.data.redis.host:localhost}") String redisHost,
            @Value("${spring.data.redis.port:6379}") int redisPort,
            @Value("${spring.data.redis.database:0}") int redisDatabase,
            @Value("${spring.data.redis.password:}") String redisPassword,
            @Value("${spring.data.redis.cache.ttl:3600}") Duration cacheTtl) {
        this.redisHost = redisHost;
        this.redisPort = redisPort;
        this.redisDatabase = redisDatabase;
        this.redisPassword = redisPassword;
        this.cacheTtl = cacheTtl;
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        config.setDatabase(redisDatabase);
        config.setPassword(redisPassword); // "" → tanpa AUTH (ditangani oleh Lettuce)
        return new LettuceConnectionFactory(config);
    }
}
```

**Connection Details:**
- **Host:** `localhost` (default, bisa di-override via `application.yml`)
- **Port:** `6379` (default Redis port)
- **Database:** `0` (index redis DB; memisahkan data cache vs session/token bisa dilakukan via index berbeda)
- **Password:** default kosong (`${REDIS_PASSWORD:}`); di-set langsung via `setPassword(...)` tanpa kondisional — string kosong berarti koneksi tanpa AUTH
- **Client:** Lettuce (async, high-performance Redis client)
- **Aktivasi:** seluruh bean `RedisConfig` hanya dibuat saat `spring.data.redis.enabled=true`

**application.yml:** (parent tidak mendefinisikan `enabled` — di-set per profile: `application-dev.yml` → `false`, `application-staging.yml` & `application-prod.yml` → `true`)
```yaml
spring:
  data:
    redis:
      enabled: true         # contoh staging/prod; RedisConfig aktif hanya jika true (matchIfMissing=false)
      host: localhost
      port: 6379
      database: 0
      password: ${REDIS_PASSWORD:}
      cache:
        ttl: 3600         # detik; default 1 jam
```

## 🧪 Penggunaan Cache

### 1. @Cacheable — Cache Read Operations

```java
@Service
public class ProductService {

    @Cacheable(value = CacheConstants.CACHE_PRODUCTS, key = "#id")
    public ProductResponse findById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(PRODUCT, "id", id));
        return toResponse(product);
    }

    @Cacheable(value = CacheConstants.CACHE_PRODUCTS_LIST, key = "'all'")
    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }
}
```

**Cara kerja:**
1. Method dipanggil dengan `id = "123"`
2. Spring check Redis cache: `products::{id}` (detail) dan `products_list::all` (daftar)
3. **Cache Hit:** Return cached value (skip method execution)
4. **Cache Miss:** Execute method, store result in cache, return value

> Nama cache didefinisikan di `CacheConstants`: `CACHE_PRODUCTS` = `"products"`, `CACHE_PRODUCTS_LIST` = `"products_list"` — cache daftar merupakan **cache name terpisah**, bukan key `all` di cache `products`. `findAll()` mengembalikan `List<ProductResponse>` tanpa paging; method pencarian ber-paging (`search(ProductFilterRequest)` → `Page<ProductResponse>`) **tidak di-cache**. Pemisah key Redis default Spring adalah `::` (jadi `products::{id}`, `products_list::all`). Mapping method → cache selengkapnya di doc `26-product-caching.md`.

### 2. @CachePut — Update Cache Saat Create

Create mem-publish hasil langsung ke cache (write-through):

```java
@Override
@CachePut(value = CacheConstants.CACHE_PRODUCTS, key = "#result.id()")
@Transactional
public ProductResponse create(ProductRequest request) {
    // ... validation & save
    return toResponse(saved);
}
```

**Cara kerja:**
1. Method dieksekusi normal
2. Hasilnya disimpan ke `products:{id}` — `getDetailProduct` berikutnya langsung cache hit
3. Cache `products_list` TIDAK diperbarui otomatis (stale list) — ditangani oleh evict pada update/delete di bawah

### 3. @Caching — Evict saat Update

```java
@Override
@Caching(evict = {
        @CacheEvict(value = CacheConstants.CACHE_PRODUCTS, key = "#id"),
        @CacheEvict(value = CacheConstants.CACHE_PRODUCTS_LIST, key = "'all'")
})
@Transactional
public ProductResponse update(UUID id, ProductRequest request) {
    return retryTemplate.execute(context -> {
        // ... update logic
    });
}
```

**Cara kerja:**
1. `products:{id}` di-evict → detail berikutnya di-query ulang dari database
2. `products_list:all` di-evict → list berikutnya di-query ulang
3. Menghindari stale data setelah modifikasi

### 4. @Caching — Evict saat Delete

```java
@Override
@Caching(evict = {
        @CacheEvict(value = CacheConstants.CACHE_PRODUCTS, key = "#id"),
        @CacheEvict(value = CacheConstants.CACHE_PRODUCTS_LIST, key = "'all'")
})
@Transactional
public void delete(UUID id) {
    findProductOrThrow(id);
    productRepository.deleteById(id); // memicu @SQLDelete (soft delete)
}
```

> Catatan: karena dua key berada di **dua cache name berbeda** (`products` dan `products_list`), operate gabungan harus dibungkus `@Caching`.

## 📊 Cache Key Naming Convention

| Cache Name | Constant (CacheConstants) | Key Pattern (Redis) | Example |
|------------|---------------------------|---------------------|---------|
| `products` | `CACHE_PRODUCTS` | `products::{id}` | `products::550e8400-e29b-41d4-a716-446655440000` |
| `products_list` | `CACHE_PRODUCTS_LIST` | `products_list::all` | `products_list::all` |

> Hanya cache Product yang aktif dipakai saat ini. `User`/`Order`/`OrderItem` **tidak** memakai Spring Cache annotation.

**Best Practice:**
- Gunakan `:` sebagai separator (Redis convention)
- Gunakan string literal untuk static keys: `'all'`
- Gunakan parameter untuk dynamic keys: `#id`

## 🚀 Running Tests

### Test Cache Configuration

```java
@SpringBootTest
class RedisCacheTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void shouldConnectToRedis() {
        // Test connection
        redisTemplate.opsForValue().set("test", "value");
        String value = (String) redisTemplate.opsForValue().get("test");
        
        assertThat(value).isEqualTo("value");
    }
}
```

### Test Cache with @Cacheable

```java
@SpringBootTest
class ProductServiceCacheTest {

    @Autowired
    private ProductService productService;

    @Test
    void shouldCacheProduct() {
        // First call: cache miss
        ProductResponse product1 = productService.findById(productId);
        
        // Second call: cache hit
        ProductResponse product2 = productService.findById(productId);
        
        assertThat(product1).isEqualTo(product2);
    }
}
```

## 📊 Cache TTL Configuration

### Global TTL (`spring.data.redis.cache.ttl`)

TTL berlaku **global** untuk semua cache entry, diterapkan lewat `entryTtl(cacheTtl)` di `RedisCacheConfiguration`:

| Property | Default | Keterangan |
|----------|---------|------------|
| `spring.data.redis.cache.ttl` | `3600` detik (60 menit) | Berlaku untuk setiap cache entry (`products`, `products_list`, dst.) |

Override per environment (contoh staging memakai 30 menit):

```yaml
spring:
  data:
    redis:
      cache:
        ttl: 30m
```

> Opsional Spring: TTL berbeda per cache bisa diatur via `RedisCacheManager.builder(...).withCacheConfiguration(name, config)` — namun **saat ini belum dipakai** di OMS; seluruh cache mengikuti TTL global dari property (default 3600s/1 jam).

## ⚠️ Catatan Penting

### 1. Cache Invalidation Strategy

**Write-Through vs Cache-Aside:**

**Cache-Aside / evict-on-write (dipakai untuk update & delete di OMS):**
```java
@Cacheable(value = CacheConstants.CACHE_PRODUCTS, key = "#id")
public ProductResponse findById(UUID id) { ... }

@Caching(evict = {
        @CacheEvict(value = CacheConstants.CACHE_PRODUCTS, key = "#id"),
        @CacheEvict(value = CacheConstants.CACHE_PRODUCTS_LIST, key = "'all'")
})
public ProductResponse update(UUID id, ProductRequest request) { ... }
```

**Write-Through (dipakai untuk create di OMS):**
```java
@CachePut(value = CacheConstants.CACHE_PRODUCTS, key = "#result.id()")
public ProductResponse create(ProductRequest request) { ... }
```

> Ringkasannya: detail product **di-put saat create** lalu **di-evict saat update/delete**; list `products_list` selalu di-evict pada update/delete karena tidak ada `@CachePut` untuk list.

### 2. Cache Penetration

**Masalah:** Repeated queries untuk non-existent keys.

**Solusi:** `disableCachingNullValues()` — nilai null (misal product tidak ditemukan) tidak pernah masuk cache:
```java
RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(cacheTtl) // spring.data.redis.cache.ttl
        .disableCachingNullValues(); // ✅ Prevent cache penetration
```

### 3. Cache Breakdown (Thundering Herd)

**Masalah:** Banyak requests hit database simultaneously saat cache expired.

**Solusi:** Use mutex lock atau extend TTL on cache miss.

### 4. Redis Connection Pool

Lettuce menggunakan Netty untuk connection management. Untuk high-concurrency:

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
          max-wait: -1ms
```

### 5. Redis Monitoring

Monitor Redis dengan:
- **Redis CLI:** `redis-cli INFO`
- **Spring Boot Actuator:** `GET /actuator/metrics/redis.*`
- **RedisInsight:** GUI tool untuk Redis

## 🔗 Referensi

- [Spring Data Redis Documentation](https://docs.spring.io/spring-data/redis/docs/current/reference/html/)
- [Spring Cache Abstraction](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache)
- [Baeldung - Spring Cache with Redis](https://www.baeldung.com/spring-cache-redis)
- [Redis Best Practices](https://redis.io/docs/management/optimization/)