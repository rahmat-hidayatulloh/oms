# OMS-030: Unit Test untuk ProductService

> **Status:** Current
> **Audience:** Developer, Maintainer
> **Owner:** Engineering
> **Last verified:** 2026-08-29 (screening kode aktual `src/`, `pom.xml`)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## Tujuan
Mengimplementasikan unit test untuk ProductService menggunakan Mockito, memastikan business logic berjalan dengan benar tanpa memuat Spring context.

## Acceptance Criteria
- [x] @ExtendWith(MockitoExtension.class)
- [x] Mock ProductRepository
- [x] Test cases: create success, create duplicate name, findById found, findById not found, update success, delete soft
- [x] verify() untuk memastikan repository method dipanggil
- [x] Coverage target: 90% untuk service class

## Detail Pekerjaan

### 1. Latar Belakang

Unit test adalah test yang fokus pada testing single unit of code (biasanya method) dalam isolasi. Keuntungan unit test:

- **Fast Execution**: Tidak memuat Spring context, eksekusi sangat cepat
- **Isolation**: Test hanya fokus pada business logic, tidak terganggu oleh external dependencies
- **Easy Debugging**: Ketika test gagal, mudah untuk identify masalahnya
- **Documentation**: Unit test berfungsi sebagai dokumentasi untuk behavior yang diharapkan

### 2. Technology Stack

**Testing Framework:**
- JUnit 5 ( Jupiter )
- Mockito untuk mocking dependencies
- AssertJ untuk assertions yang readable

**Key Annotations:**
- `@ExtendWith(MockitoExtension.class)` - Integrasi Mockito dengan JUnit 5
- `@Mock` - Membuat mock object
- `@InjectMocks` - Inject mock objects ke class yang di-test
- `@BeforeEach` - Setup sebelum setiap test

### 3. Test Structure

**File:** `src/test/java/com/oms/product/service/ProductServiceUnitTest.java`

**Dependencies yang di-mock:**
```java
@Mock
private ProductRepository productRepository;

@InjectMocks
private ProductService productService;
```

### 4. Test Cases

#### a. Create Product Tests

**Test 1: create_shouldSaveProduct_whenNameIsUnique**
```java
@Test
void create_shouldSaveProduct_whenNameIsUnique() {
    // Given
    when(productRepository.existsByName("Kaos Polos Hitam")).thenReturn(false);
    when(productRepository.save(any(Product.class))).thenReturn(product);

    // When
    ProductResponse response = productService.create(productRequest);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.name()).isEqualTo("Kaos Polos Hitam");

    verify(productRepository).existsByName("Kaos Polos Hitam");
    verify(productRepository).save(any(Product.class));
}
```

**Test 2: create_shouldThrowException_whenNameAlreadyExists**
```java
@Test
void create_shouldThrowException_whenNameAlreadyExists() {
    // Given
    when(productRepository.existsByName("Kaos Polos Hitam")).thenReturn(true);

    // When & Then
    assertThatThrownBy(() -> productService.create(productRequest))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("already exists");

    verify(productRepository, never()).save(any(Product.class));
}
```

#### b. FindById Tests

**Test 3: findById_shouldReturnProduct_whenExists**
```java
@Test
void findById_shouldReturnProduct_whenExists() {
    // Given
    UUID productId = product.getId();
    when(productRepository.findById(productId)).thenReturn(Optional.of(product));

    // When
    ProductResponse response = productService.findById(productId);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.id()).isEqualTo(productId);

    verify(productRepository).findById(productId);
}
```

**Test 4: findById_shouldThrowException_whenNotFound**
```java
@Test
void findById_shouldThrowException_whenNotFound() {
    // Given
    UUID randomId = UUID.randomUUID();
    when(productRepository.findById(randomId)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> productService.findById(randomId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("not found");

    verify(productRepository).findById(randomId);
}
```

#### c. FindAll Tests

**Test 5: findAll_shouldReturnAllProducts**
```java
@Test
void findAll_shouldReturnAllProducts() {
    // Given
    when(productRepository.findAll()).thenReturn(List.of(product, product2));

    // When
    List<ProductResponse> responses = productService.findAll();

    // Then
    assertThat(responses).hasSize(2);
    assertThat(responses).extracting(ProductResponse::name)
            .containsExactlyInAnyOrder("Kaos Polos Hitam", "Headphone Bluetooth");

    verify(productRepository).findAll();
}
```

#### d. Update Tests

**Test 6: update_shouldUpdateProduct_whenNameIsUnique**
```java
@Test
void update_shouldUpdateProduct_whenNameIsUnique() {
    // Given
    UUID productId = product.getId();
    when(productRepository.findById(productId)).thenReturn(Optional.of(product));
    when(productRepository.existsByName("Kaos Polos Putih")).thenReturn(false);
    when(productRepository.save(any(Product.class))).thenReturn(product);

    ProductRequest updateRequest = new ProductRequest(
            "Kaos Polos Putih", "Desc", new BigDecimal("90000.00"), 80, Category.CLOTHING);

    // When
    ProductResponse response = productService.update(productId, updateRequest);

    // Then
    assertThat(response).isNotNull();
    verify(productRepository).findById(productId);
    verify(productRepository).save(any(Product.class));
}
```

**Test 7: update_shouldThrowException_whenNewNameAlreadyExists**
```java
@Test
void update_shouldThrowException_whenNewNameAlreadyExists() {
    // Given
    when(productRepository.findById(productId)).thenReturn(Optional.of(product));
    when(productRepository.existsByName("Kaos Polos Putih")).thenReturn(true);

    // When & Then
    assertThatThrownBy(() -> productService.update(productId, updateRequest))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("already exists");

    verify(productRepository, never()).save(any(Product.class));
}
```

#### e. Delete Tests

**Test 8: delete_shouldCallRepositoryDeleteById**
```java
@Test
void delete_shouldCallRepositoryDeleteById() {
    // Given
    UUID productId = product.getId();

    // When
    productService.delete(productId);

    // Then
    verify(productRepository).deleteById(productId);
}
```

#### f. Search Tests

**Test 9: search_shouldReturnPageOfProducts**
```java
@Test
void search_shouldReturnPageOfProducts() {
    // Given
    Page<Product> productPage = new PageImpl<>(List.of(product));
    when(productRepository.findAll(any(Pageable.class))).thenReturn(productPage);

    // When
    Page<ProductResponse> result = productService.search(filter);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getContent()).hasSize(1);

    verify(productRepository).findAll(any(Pageable.class));
}
```

**Test 10: search_shouldReturnEmptyPage_whenNoProductsMatch**
```java
@Test
void search_shouldReturnEmptyPage_whenNoProductsMatch() {
    // Given
    Page<Product> emptyPage = new PageImpl<>(List.of());
    when(productRepository.findAll(any(com.querydsl.core.types.Predicate.class), any(Pageable.class)))
            .thenReturn(emptyPage);

    // When
    Page<ProductResponse> result = productService.search(filter);

    // Then
    assertThat(result).isEmpty();
    assertThat(result.getTotalElements()).isZero();
}
```

#### g. Parameterized Test

**Test 11: create_shouldWorkWithAllCategories**
```java
@ParameterizedTest
@EnumSource(Category.class)
void create_shouldWorkWithAllCategories(Category category) {
    // Given
    ProductRequest request = new ProductRequest("Test Product", "Description",
            new BigDecimal("50000.00"), 25, category);

    Product savedProduct = Product.builder()
            .id(UUID.randomUUID())
            .name("Test Product")
            .category(category)
            .build();

    when(productRepository.existsByName("Test Product")).thenReturn(false);
    when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

    // When
    ProductResponse response = productService.create(request);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.category()).isEqualTo(category);
}
```

### 5. Key Mockito Methods

**Stubbing:**
- `when(mock.method(args)).thenReturn(value)` - Stubbing method dengan return value
- `when(mock.method(args)).thenThrow(exception)` - Stubbing method untuk throw exception
- `any(Class.class)` - Match any argument of specified type
- `anyBoolean()`, `anyString()` - Match any primitive/wrapper type

**Verification:**
- `verify(mock).method(args)` - Verify method dipanggil exactly once
- `verify(mock, times(n)).method(args)` - Verify method dipanggil n times
- `verify(mock, never()).method(args)` - Verify method tidak pernah dipanggil
- `verify(mock, atLeastOnce()).method(args)` - Verify method dipanggil minimal sekali

### 6. AssertJ Assertions

**Basic Assertions:**
```java
assertThat(object).isNotNull();
assertThat(string).isEqualTo("expected");
assertThat(number).isEqualTo(10);
assertThat(list).hasSize(5);
```

**Exception Assertions:**
```java
assertThatThrownBy(() -> method())
    .isInstanceOf(BadRequestException.class)
    .hasMessageContaining("error message");
```

**Collection Assertions:**
```java
assertThat(list).containsExactlyInAnyOrder("item1", "item2");
assertThat(list).extracting(ProductResponse::name).contains("Product 1");
```

### 7. Test Coverage

**Coverage Target:** 90% untuk ProductService class

**Covered Methods:**
- ✅ create() - 2 test cases (success, duplicate)
- ✅ findById() - 2 test cases (found, not found)
- ✅ findAll() - 1 test case
- ✅ update() - 2 test cases (success, duplicate name)
- ✅ delete() - 1 test case
- ✅ search() - 2 test cases (with results, empty)
- ✅ Parameterized test for all 5 categories

**Total Test Cases:** 15 test executions (10 `@Test` + 1 `@ParameterizedTest` × 5 `Category`)

### 8. Running the Tests

```bash
# Run all ProductService tests
./mvnw test -Dtest=ProductServiceUnitTest

# Run with coverage report
./mvnw test -Dtest=ProductServiceUnitTest jacoco:report

# Run specific test
./mvnw test -Dtest=ProductServiceUnitTest#create_shouldSaveProduct_whenNameIsUnique
```

### 9. Best Practices

1. **Test Method Naming** - Gunakan pattern `methodName_shouldExpectedBehavior_WhenCondition`
2. **Arrange-Act-Assert (AAA)** - Setiap test harus memiliki 3 bagian: Given, When, Then
3. **One Assertion Per Test** - Setiap test sebaiknya hanya test satu behavior
4. **Mock Only External Dependencies** - Jangan mock object yang sedang di-test
5. **Use Parameterized Tests** - Untuk test yang sama dengan different inputs
6. **Verify Interactions** - Gunakan verify() untuk memastikan method dipanggil dengan benar

### 10. Files

| File | Tindakan | Deskripsi |
|---|---|---|
| `src/test/java/com/oms/product/service/ProductServiceUnitTest.java` | **Dibuat** | 15 unit test executions untuk ProductService |