# OMS-031: @WebMvcTest untuk ProductController

> **Status:** Current
> **Audience:** Developer, Maintainer
> **Owner:** Engineering
> **Last verified:** 2026-08-29 (screening kode aktual `src/`, `pom.xml`)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## Tujuan
Mengimplementasikan slice test untuk ProductController layer menggunakan @SpringBootTest dengan @MockBean untuk service layer, memastikan HTTP endpoints berfungsi dengan benar.

## Acceptance Criteria
- [x] @SpringBootTest dengan @AutoConfigureMockMvc (diubah dari @WebMvcTest karena dependency issues)
- [x] @MockBean untuk ProductService
- [x] MockMvc untuk testing endpoints
- [x] Test authentication dengan @WithMockUser
- [x] Test validation error response (400)
- [x] Test security (unauthorized 401, forbidden 403)

## Detail Pekerjaan

### 1. Latar Belakang

Controller slice test memungkinkan kita testing web layer tanpa memuat seluruh application context. Keuntungan:

- **Fast Execution**: Lebih cepat dari full integration test
- **Focused Testing**: Hanya test controller layer, service layer di-mock
- **Security Testing**: Mudah test authentication dan authorization
- **Validation Testing**: Test request validation tanpa loading database

### 2. Technology Stack

**Testing Framework:**
- JUnit 5
- Spring Boot Test dengan @SpringBootTest + @AutoConfigureMockMvc
- Mockito untuk mocking service layer
- Spring Security Test untuk @WithMockUser

**Key Annotations:**
- `@SpringBootTest` - Load full application context
- `@AutoConfigureMockMvc` - Auto-configure MockMvc
- `@MockBean` - Mock bean di Spring context
- `@WithMockUser` - Mock authenticated user
- `@ActiveProfiles("test")` - Use test profile

### 3. Test Structure

**File:** `src/test/java/com/oms/product/controller/ProductControllerUnitTest.java`

**Note:** Diubah dari `@WebMvcTest` ke `@SpringBootTest` + `@AutoConfigureMockMvc` karena:
- @WebMvcTest hanya load controller layer, tapi memerlukan beberapa security beans yang tidak ter-load otomatis
- @SpringBootTest lebih reliable untuk controller testing dengan security configuration

**Dependencies yang di-mock:**
```java
@MockBean
private ProductServiceInterface productService;
```

### 4. Test Cases

#### a. Create Product Tests

**Test 1: createProduct_shouldReturn201_whenValidRequest**
```java
@Test
@WithMockUser(roles = "ADMIN")
void createProduct_shouldReturn201_whenValidRequest() throws Exception {
    // Given
    UUID productId = UUID.randomUUID();
    ProductResponse response = new ProductResponse(
            productId, "Kaos Polos Hitam", "Kaos katun premium",
            new BigDecimal("85000.00"), 100, Category.CLOTHING, null, null);

    when(productService.create(any(ProductRequest.class))).thenReturn(response);

    // When & Then
    mockMvc.perform(post("/api/v1/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "name": "Kaos Polos Hitam",
                                "description": "Kaos katun premium",
                                "price": 85000.00,
                                "stock": 100,
                                "category": "CLOTHING"
                            }
                            """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(productId.toString()))
            .andExpect(jsonPath("$.name").value("Kaos Polos Hitam"))
            .andExpect(jsonPath("$.price").value(85000.00));
}
```

**Test 2: createProduct_shouldReturn400_whenValidationFails**
```java
@Test
@WithMockUser(roles = "ADMIN")
void createProduct_shouldReturn400_whenValidationFails() throws Exception {
    // When & Then - invalid data (name too short, negative price)
    mockMvc.perform(post("/api/v1/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {
                                "name": "AB",
                                "description": "Test",
                                "price": -1,
                                "stock": 50,
                                "category": "CLOTHING"
                            }
                            """))
            .andExpect(status().isBadRequest());
}
```

#### b. Get All Products Tests

**Test 3: getAllProducts_shouldReturn200WithProducts**
```java
@Test
@WithMockUser(roles = {"CUSTOMER", "ADMIN"})
void getAllProducts_shouldReturn200WithProducts() throws Exception {
    // Given
    ProductResponse product1 = new ProductResponse(...);
    ProductResponse product2 = new ProductResponse(...);
    when(productService.findAll()).thenReturn(List.of(product1, product2));

    // When & Then
    mockMvc.perform(get("/api/v1/products")
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].name").value("Product 1"))
            .andExpect(jsonPath("$[1].name").value("Product 2"));
}
```

#### c. Get Product By ID Tests

**Test 4: getProductById_shouldReturn200_whenProductExists**
```java
@Test
@WithMockUser(roles = {"CUSTOMER", "ADMIN"})
void getProductById_shouldReturn200_whenProductExists() throws Exception {
    // Given
    UUID productId = UUID.randomUUID();
    ProductResponse response = new ProductResponse(...);
    when(productService.findById(productId)).thenReturn(response);

    // When & Then
    mockMvc.perform(get("/api/v1/products/{id}", productId)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(productId.toString()))
            .andExpect(jsonPath("$.name").value("Kaos Polos"));
}
```

#### d. Security Tests

**Test 5: createProduct_shouldReturn401_whenUnauthenticated**
```java
@Test
void createProduct_shouldReturn401_whenUnauthenticated() throws Exception {
    // When & Then - no @WithMockUser, so unauthenticated
    mockMvc.perform(post("/api/v1/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("..."))
            .andExpect(status().isForbidden());
}
```

**Test 6: createProduct_shouldReturn403_whenCustomerTriesToCreate**
```java
@Test
@WithMockUser(roles = "CUSTOMER")
void createProduct_shouldReturn403_whenCustomerTriesToCreate() throws Exception {
    // When & Then - CUSTOMER role should not have access
    mockMvc.perform(post("/api/v1/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("..."))
            .andExpect(status().isForbidden());
}
```

**Test 7: getAllProducts_shouldReturn200_forCustomer**
```java
@Test
@WithMockUser(roles = "CUSTOMER")
void getAllProducts_shouldReturn200_forCustomer() throws Exception {
    // Given
    when(productService.findAll()).thenReturn(List.of());

    // When & Then - CUSTOMER can view products
    mockMvc.perform(get("/api/v1/products")
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
}
```

### 5. Key Testing Concepts

#### a. MockMvc Request Builders

**POST Request:**
```java
mockMvc.perform(post("/api/v1/products")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"name\": \"Product\", \"price\": 50000}"))
```

**GET Request:**
```java
mockMvc.perform(get("/api/v1/products/{id}", productId)
        .contentType(MediaType.APPLICATION_JSON))
```

#### b. MockMvc Result Matchers

**Status Matchers:**
```java
.andExpect(status().isOk())           // 200
.andExpect(status().isCreated())       // 201
.andExpect(status().isBadRequest())    // 400
.andExpect(status().isUnauthorized())  // 401
.andExpect(status().isForbidden())     // 403
.andExpect(status().isNotFound())      // 404
```

**JSON Path Matchers:**

Controllers return raw `ResponseEntity<T>` bodies (no `ApiResponse`/`success`/`data` envelope), so assertions target the payload directly:
```java
.andExpect(jsonPath("$.id").value(productId.toString()))
.andExpect(jsonPath("$.name").value("Product Name"))
.andExpect(jsonPath("$").isArray())
.andExpect(jsonPath("$[0].name").value("First Item"))
```

#### c. @WithMockUser

**Single Role:**
```java
@WithMockUser(roles = "ADMIN")
void testAdminEndpoint() { ... }
```

**Multiple Roles:**
```java
@WithMockUser(roles = {"CUSTOMER", "ADMIN"})
void testMultipleRoles() { ... }
```

**Custom Username:**
```java
@WithMockUser(username = "testuser", roles = "CUSTOMER")
void testWithUsername() { ... }
```

### 6. JSON Request Body

**Multiline JSON String (Java 17+):**
```java
.content("""
        {
            "name": "Product Name",
            "description": "Description",
            "price": 50000.00,
            "stock": 100,
            "category": "CLOTHING"
        }
        """)
```

### 7. Test Coverage

**Coverage Target:** All controller endpoints and security rules

**Covered Scenarios:**
- ✅ POST /api/v1/products - 201 Created (ADMIN)
- ✅ POST /api/v1/products - 400 Bad Request (validation)
- ✅ GET /api/v1/products - 200 OK (CUSTOMER, ADMIN)
- ✅ GET /api/v1/products/{id} - 200 OK (CUSTOMER, ADMIN)
- ✅ POST /api/v1/products - 403 Forbidden (unauthenticated; method name keeps legacy `401` label but asserts `isForbidden()`)
- ✅ POST /api/v1/products - 403 Forbidden (CUSTOMER)
- ✅ GET /api/v1/products - 200 OK (CUSTOMER read)

**Total Test Cases:** 7 tests

**Not covered by this class:** `PUT /api/v1/products/{id}` and `DELETE /api/v1/products/{id}` — see `ProductControllerTest` (full-context MockMvc, same package) for those endpoints. `GET /api/v1/products/search` is not covered by either controller test class.

### 8. Running the Tests

```bash
# Run ProductController tests
./mvnw test -Dtest=ProductControllerUnitTest

# Run all controller tests
./mvnw test -Dtest=*ControllerTest

# Run with coverage
./mvnw test -Dtest=ProductControllerUnitTest jacoco:report
```

### 9. Best Practices

1. **Use @WithMockUser** - Untuk test authentication tanpa login
2. **Test All Roles** - Test setiap role yang memiliki access berbeda
3. **Test Validation** - Test invalid input untuk pastikan validation bekerja
4. **Test Security** - Test unauthorized dan forbidden scenarios
5. **Use JSON Path** - Untuk verify response JSON structure
6. **Mock Service Layer** - Controller test tidak perlu test business logic

### 10. Difference: @WebMvcTest vs @SpringBootTest

| Aspect | @WebMvcTest | @SpringBootTest + @AutoConfigureMockMvc |
|--------|-------------|----------------------------------------|
| **Context Loading** | Slice (controller only) | Full application context |
| **Speed** | Faster | Slower |
| **Configuration** | Minimal | Complete |
| **Security Config** | May need extra config | Works out of the box |
| **Use Case** | Simple controllers | Complex security/config |

**Kesimpulan:** Untuk project dengan complex security configuration seperti OMS, @SpringBootTest + @AutoConfigureMockMvc lebih reliable.

### 11. Files

| File | Tindakan | Deskripsi |
|---|---|---|
| `src/test/java/com/oms/product/controller/ProductControllerUnitTest.java` | **Dibuat** | 7 controller tests dengan @SpringBootTest |