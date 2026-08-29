# OMS-017: Implementasi QueryDSL untuk Dynamic Filtering Product

> **Status:** Current
> **Audience:** Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-29 (screening kode aktual `src/`, `pom.xml`)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## 📋 Deskripsi

Mengimplementasikan **QueryDSL** untuk melakukan filtering, sorting, dan pagination produk secara dinamis tanpa perlu menulis query manual.

## 🎯 Acceptance Criteria

| No | Kriteria | Status |
|----|----------|--------|
| 1 | Dependency: querydsl-apt, querydsl-jpa | ✅ |
| 2 | Generate Q classes dengan maven plugin | ✅ |
| 3 | Buat ProductPredicates class dengan method build Predicate | ✅ |
| 4 | Support filter: name (contain), price (min-max), category (equal) | ✅ |
| 5 | Support sorting: name, price, createdAt | ✅ |
| 6 | Support pagination | ✅ |

## 🏗️ Struktur File Baru/Diubah

```
src/main/java/com/oms/product/
├── dto/
│   └── ProductFilterRequest.java       ← [BARU] DTO untuk parameter filter
├── repository/
│   ├── ProductPredicates.java          ← [BARU] Builder QueryDSL predicates
│   └── ProductRepository.java          ← [DIUBAH] extends QuerydslPredicateExecutor
├── service/
│   └── ProductService.java             ← [DIUBAH] method search() dengan QueryDSL
└── controller/
    └── ProductController.java          ← [DIUBAH] endpoint GET /search

src/test/java/com/oms/product/
└── repository/
    └── ProductPredicatesTest.java      ← [BARU] unit test predicate

docs/
└── 20-querydsl-product-filter.md      ← [BARU] Dokumentasi ini
```

## 🔧 Konfigurasi

### 1. Dependency (pom.xml)

```xml
<!-- QueryDSL -->
<dependency>
    <groupId>com.querydsl</groupId>
    <artifactId>querydsl-jpa</artifactId>
    <version>5.1.0</version>
    <classifier>jakarta</classifier>
</dependency>
<dependency>
    <groupId>com.querydsl</groupId>
    <artifactId>querydsl-apt</artifactId>
    <version>5.1.0</version>
    <classifier>jakarta</classifier>
    <scope>provided</scope>
</dependency>
```

### 2. APT Maven Plugin (pom.xml)

```xml
<plugin>
    <groupId>com.mysema.maven</groupId>
    <artifactId>apt-maven-plugin</artifactId>
    <version>1.1.3</version>
    <executions>
        <execution>
            <goals>
                <goal>process</goal>
            </goals>
            <configuration>
                <outputDirectory>target/generated-sources/annotations</outputDirectory>
                <processor>com.querydsl.apt.jpa.JPAAnnotationProcessor</processor>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## 📝 Implementasi

### 1. ProductFilterRequest (DTO)

```java
public record ProductFilterRequest(
    String name,          // Filter by name (case-insensitive contain)
    BigDecimal minPrice,  // Minimum price
    BigDecimal maxPrice,  // Maximum price
    Category category,    // Exact category match
    String sortBy,        // Sort field: name, price, createdAt
    String sortDirection, // asc or desc
    int page,             // Page number (0-based)
    int size              // Page size
) {
    public ProductFilterRequest {
        // Default values
        if (sortBy == null || sortBy.isBlank()) sortBy = "createdAt";
        if (sortDirection == null || sortDirection.isBlank()) sortDirection = "desc";
        if (page < 0) page = 0;
        if (size <= 0) size = 10;
    }
}
```

### 2. ProductPredicates

```java
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProductPredicates {

    private static final QProduct Q_PRODUCT = QProduct.product;

    public static Predicate build(ProductFilterRequest filter) {
        BooleanBuilder builder = new BooleanBuilder();

        if (filter.name() != null && !filter.name().isBlank()) {
            builder.and(withNameContaining(filter.name().trim()));
        }
        if (filter.minPrice() != null) {
            builder.and(withMinPrice(filter.minPrice()));
        }
        if (filter.maxPrice() != null) {
            builder.and(withMaxPrice(filter.maxPrice()));
        }
        if (filter.category() != null) {
            builder.and(withCategory(filter.category()));
        }

        return builder.getValue(); // null jika tidak ada filter
    }

    public static BooleanExpression withNameContaining(String name) {
        return Q_PRODUCT.name.containsIgnoreCase(name);
    }

    public static BooleanExpression withMinPrice(BigDecimal minPrice) {
        return Q_PRODUCT.price.goe(minPrice);
    }

    public static BooleanExpression withMaxPrice(BigDecimal maxPrice) {
        return Q_PRODUCT.price.loe(maxPrice);
    }

    public static BooleanExpression withCategory(Category category) {
        return Q_PRODUCT.category.eq(category);
    }
}
```

### 3. ProductRepository

```java
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>,
        QuerydslPredicateExecutor<Product> {
    boolean existsByName(String name);
}
```

### 4. ProductService — Method search()

```java
@Transactional(readOnly = true)
public Page<ProductResponse> search(ProductFilterRequest filter) {
    Predicate predicate = ProductPredicates.build(filter);
    Pageable pageable = createPageable(filter);

    Page<Product> productPage;

    if (predicate != null) {
        productPage = productRepository.findAll(predicate, pageable);
    } else {
        productPage = productRepository.findAll(pageable);
    }

    return productPage.map(this::toResponse);
}

private Pageable createPageable(ProductFilterRequest filter) {
    // Validasi sort field
    String sortField = filter.sortBy();
    if (!ALLOWED_SORT_FIELDS.contains(sortField)) {
        log.warn("Invalid sort field '{}', falling back to 'createdAt'", sortField);
        sortField = "createdAt";
    }

    Sort.Direction direction = "asc".equalsIgnoreCase(filter.sortDirection())
            ? Sort.Direction.ASC : Sort.Direction.DESC;

    Sort sort = Sort.by(direction, sortField);
    return PageRequest.of(filter.page(), filter.size(), sort);
}
```

### 5. ProductController — Endpoint Search

```java
@GetMapping("/search")
@PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
public ResponseEntity<Page<ProductResponse>> searchProducts(
        @Valid ProductFilterRequest filter) {
    return ResponseEntity.ok(productService.search(filter));
}
```

## 🌐 API Usage

### Search Products (Filter + Sort + Pagination)

**Request:**
```http
GET /api/v1/products/search?name=kaos&minPrice=50000&maxPrice=200000&category=CLOTHING&sortBy=price&sortDirection=asc&page=0&size=10
Authorization: Bearer <accessToken>
```

**Response:**
```json
{
    "content": [
        {
            "id": "uuid-1",
            "name": "Kaos Polos Hitam",
            "description": "Kaos katun premium hitam",
            "price": 85000.00,
            "stock": 100,
            "category": "CLOTHING",
            "createdAt": "2026-07-23T09:15:00",
            "updatedAt": "2026-07-23T09:15:00"
        }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1
}
```

### Contoh Berbagai Filter

| Parameter | Value | Deskripsi |
|-----------|-------|-----------|
| `name` | `kaos` | Cari produk dengan nama mengandung "kaos" (case-insensitive) |
| `minPrice` | `10000` | Harga minimum |
| `maxPrice` | `500000` | Harga maksimum |
| `category` | `ELECTRONICS` | Filter kategori exact match |
| `sortBy` | `name`, `price`, `createdAt` | Field sorting |
| `sortDirection` | `asc` / `desc` | Arah sorting |
| `page` | `0` | Halaman (0-based) |
| `size` | `20` | Jumlah per halaman |

## 📊 Test Coverage

### ProductServiceTest (13 tests)

| Test Case | Deskripsi |
|-----------|-----------|
| `shouldCreateProduct` | Create product sukses |
| `shouldThrowExceptionWhenCreatingDuplicateProduct` | Duplicate name error |
| `shouldFindProductById` | Find by ID sukses |
| `shouldThrowExceptionWhenProductNotFound` | ID tidak ditemukan |
| `shouldFindAllProducts` | List all products |
| `shouldUpdateProduct` | Update product sukses |
| `shouldSoftDeleteProduct` | Soft delete product |
| `shouldSearchProductsWithDefaultPagination` | Search dengan default values |
| `shouldFilterProductsByName` | Filter by name contain |
| `shouldFilterProductsByPriceRange` | Filter by min-max price |
| `shouldFilterProductsByCategory` | Filter by category |
| `shouldSortProductsByName` | Sort by name ascending |
| `shouldHandleInvalidSortField` | Invalid sort field fallback |

## 🚀 Generate Q Classes

```bash
# Generate Q classes dari entity JPA
./mvnw compile
```

Output: `target/generated-sources/annotations/com/oms/product/entity/QProduct.java` (dan entity lainnya)

## 🔗 Referensi

- [QueryDSL Documentation](http://querydsl.com/)
- [Spring Data QueryDSL Integration](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#core.extensions.querydsl)