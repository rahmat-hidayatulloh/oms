# Product Caching Implementation

> **Status:** Current
> **Audience:** Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-28 (screening kode aktual `src/`)
> **Source of truth:** Repository source code (`src/`)

## Overview

Dokumen ini menjelaskan implementasi caching berbasis Redis untuk data produk pada OMS application. Hanya modul **Product** yang memakai Spring Cache; modul User, Order, OrderItem, dan Admin **tidak** menggunakan Spring Cache sama sekali (Redis untuk modul lain hanya dipakai refresh token & JWT blacklist).

## Cache Configuration

- Cache manager: `RedisCacheManager` (dari `RedisConfig`; cache name terbentuk otomatis saat pertama kali dipakai)
- TTL: **global** via `spring.data.redis.cache.ttl` (default `3600` detik / 1 jam) melalui `RedisCacheConfiguration.entryTtl(...)` — **tidak ada** override TTL per-cache
- Key serialization: `StringRedisSerializer`
- Value serialization: `GenericJackson2JsonRedisSerializer` + `ObjectMapper` khusus (JavaTimeModule, default-typing whitelist `com.oms.*`)
- Null values: `disableCachingNullValues()` — nilai `null` tidak pernah disimpan
- Prefix pemisah key Redis default Spring: `::` (contoh `products::{id}`, `products_list::all`)
- Nama cache didefinisikan di `CacheConstants`: `CACHE_PRODUCTS = "products"`, `CACHE_PRODUCTS_LIST = "products_list"`

> Detail konfigurasi Redis (host/port/database/password, serialization, `@EnableCaching`, fallback `ConcurrentMapCacheManager`) ada di `docs/current/development/features/cross-cutting/25-redis-cache-config.md`.

## Cached Methods (`ProductService`)

| Method | Annotation | Cache Name | Key SpEL | Redis Key | Behavior |
|--------|------------|------------|----------|-----------|----------|
| `findById(UUID id)` | `@Cacheable` | `products` | `#id` | `products::{id}` | Hit → return value ter-serialize; miss → query DB lalu di-cache |
| `findAll()` | `@Cacheable` | `products_list` | `'all'` | `products_list::all` | Return `List<ProductResponse>` utuh (non-paged) |
| `create(ProductRequest)` | `@CachePut` | `products` | `#result.id()` | `products::{id}` | **Write-through**: hasil create langsung dimasukkan ke cache detail |
| `update(UUID, ProductRequest)` | `@Caching` — 2× `@CacheEvict` | `products` + `products_list` | `#id` + `'all'` | — | **Evict-on-write**: kosongkan detail produk & seluruh list |
| `delete(UUID)` | `@Caching` — 2× `@CacheEvict` | `products` + `products_list` | `#id` + `'all'` | — | Soft delete, lalu evict detail & list |

### Catatan Kunci

- `findAll()` mengembalikan **seluruh** produk aktif sebagai `List<ProductResponse>` dan di-cache sebagai satu entri `products_list::all`.
- Pencarian dinamis `search(ProductFilterRequest filter)` → `Page<ProductResponse>` (QueryDSL predicate + paging/sorting) **tidak di-cache**; selalu dieksekusi ke database.
- `create()` memakai `@CachePut` (write-through) dan **tidak** meng-evict `products_list`, sehingga list bisa basi (stale) hingga di-evict oleh update/delete berikutnya atau TTL habis. Ini perilaku aktual kode — bukan evict-on-write.
- `update()` dan `delete()` memakai `@Caching` dengan dua `@CacheEvict` (evict sebab data lama dihapus dari cache, di-refresh pada pembacaan berikutnya).

## Known Issues (Riwayat)

| Issue | Status |
|-------|--------|
| `LocalDateTime`/`BigDecimal` tidak ter-serialize dengan benar | ✅ **Resolved** — `ObjectMapper` mendaftarkan `JavaTimeModule` dan men-disable `WRITE_DATES_AS_TIMESTAMPS` |
| `ClassCastException` saat retrieve object hasil cache | ✅ **Resolved** — `GenericJackson2JsonRedisSerializer(redisObjectMapper)` + `BasicPolymorphicTypeValidator` whitelist `com.oms.*` |
| TTL berbeda per cache name | ❌ Tidak diimplementasikan — TTL global (1 jam) |

## Next Steps

- Pertimbangkan `withCacheConfiguration(...)` bila ingin TTL berbeda per cache name
- Evaluasi kompresi value (misal GZIP) untuk `products_list` berukuran besar
- Tambahkan observability hit/miss ratio (actuator cache metrics / RedisInsight)

## Referensi

- `src/main/java/com/oms/config/RedisConfig.java`
- `src/main/java/com/oms/constant/CacheConstants.java`
- `src/main/java/com/oms/product/service/ProductService.java`
- `docs/current/development/features/cross-cutting/25-redis-cache-config.md`
