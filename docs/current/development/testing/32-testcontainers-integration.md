# OMS-029: Setup Testcontainers untuk Integration Test

> **Status:** Current
> **Audience:** Developer, Maintainer
> **Owner:** Engineering
> **Last verified:** 2026-08-23 (screening kode aktual `src/`, `pom.xml`)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## Tujuan
Mengimplementasikan integration test menggunakan Testcontainers untuk PostgreSQL dan Redis, memastikan aplikasi berjalan dengan baik di environment yang sesungguhnya.

## Acceptance Criteria
- [x] Dependency: testcontainers, testcontainers-postgresql, testcontainers-junit-jupiter
- [x] AbstractIntegrationTest base class
- [x] Test menggunakan PostgreSQL container
- [x] Test menggunakan Redis container
- [x] Automatic container lifecycle management

## Detail Pekerjaan

### 1. Latar Belakang

Testcontainers adalah library yang memungkinkan kita menjalankan lightweight disposable containers untuk testing. Keuntungan menggunakan Testcontainers:

- **Real Database Testing**: Menggunakan PostgreSQL yang sesungguhnya, bukan H2
- **Isolated Tests**: Setiap test suite bisa menggunakan database yang bersih
- **No External Dependencies**: Containers di-spin up secara otomatis
- **Production Parity**: Test berjalan di environment yang mirip dengan production
- **Fast Execution**: Container reuse untuk mempercepat test execution

### 2. Dependencies

**File:** `pom.xml`

```xml
<!-- Testcontainers -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.8</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.8</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.19.8</version>
    <scope>test</scope>
</dependency>
```

**Catatan:** Redis module tidak digunakan karena belum tersedia di Maven Central untuk versi 1.19.8. Redis di-test via GenericContainer.

### 3. AbstractIntegrationTest Base Class

**File:** `src/test/java/com/oms/config/AbstractIntegrationTest.java`

Base class yang menyediakan:

- PostgreSQL 16 Alpine container
- Redis 7 Alpine container
- Automatic startup/shutdown via JUnit 5 extension
- Shared containers across test methods (faster execution)
- System properties configuration untuk Spring Boot

**Konfigurasi Container:**

```java
@Container
protected static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
    new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

@Container
protected static final GenericContainer<?> REDIS_CONTAINER =
    new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true);
```

**System Properties:**

```java
static {
    POSTGRES_CONTAINER.start();
    REDIS_CONTAINER.start();

    System.setProperty("spring.datasource.url",
        "jdbc:postgresql://localhost:" + POSTGRES_CONTAINER.getMappedPort(5432) + "/testdb");
    System.setProperty("spring.datasource.username", "test");
    System.setProperty("spring.datasource.password", "test");
    System.setProperty("spring.data.redis.host", "localhost");
    System.setProperty("spring.data.redis.port", 
        String.valueOf(REDIS_CONTAINER.getMappedPort(6379)));
    System.setProperty("spring.data.redis.enabled", "true");
}
```

### 4. Test Configuration

**File:** `src/test/resources/application-testcontainers.yml`

Konfigurasi khusus untuk Testcontainers:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/testdb
    username: test
    password: test
    driver-class-name: org.postgresql.Driver
    hikari:
      minimum-idle: 1
      maximum-pool-size: 5
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  
  flyway:
    enabled: true
    locations: classpath:db/migration
  
  data:
    redis:
      host: localhost
      port: 6379
      enabled: true

logging:
  level:
    com.oms: DEBUG
    org.testcontainers: INFO
```

### 5. PostgreSQL Integration Test

**File:** `src/test/java/com/oms/product/service/ProductServiceIntegrationTest.java`

Test yang memverifikasi complete product lifecycle:

**Test Cases:**
1. `createProduct_shouldPersistToDatabase` - Verifikasi product tersimpan ke PostgreSQL
2. `findAll_shouldReturnAllProducts` - Verifikasi query findAll
3. `findById_shouldReturnProduct` - Verifikasi query by ID
4. `updateProduct_shouldUpdateInDatabase` - Verifikasi update operation
5. `deleteProduct_shouldSoftDelete` - Verifikasi soft delete

**Example Test:**
```java
@Test
void createProduct_shouldPersistToDatabase() {
    // Given
    ProductRequest request = new ProductRequest(
            "Test Product",
            "Test Description",
            new BigDecimal("100000"),
            50,
            Category.ELECTRONICS
    );

    // When
    ProductResponse response = productService.create(request);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.id()).isNotNull();
    assertThat(response.name()).isEqualTo("Test Product");
    assertThat(response.price()).isEqualByComparingTo(new BigDecimal("100000"));
}
```

### 6. Redis Integration Test

**File:** `src/test/java/com/oms/config/RedisIntegrationTest.java`

Test yang memverifikasi Redis connectivity dan database operations:

**Test Cases:**
1. `redisConnection_shouldBeEstablished` - Verifikasi Redis connection
2. `redisHealthIndicator_shouldReturnUp` - Verifikasi Redis health
3. `databaseConnection_shouldBeEstablished` - Verifikasi PostgreSQL connection
4. `databaseTransaction_shouldRollback` - Verifikasi transaction rollback

**Example Test:**
```java
@Test
void databaseConnection_shouldBeEstablished() {
    // Given - PostgreSQL container is running
    // When - Save a user to database
    User user = new User();
    user.setUsername("testuser");
    user.setPassword("password");
    user.setRole(Role.CUSTOMER);

    User saved = userRepository.save(user);

    // Then - User should be persisted
    assertThat(saved).isNotNull();
    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getUsername()).isEqualTo("testuser");
}
```

### 7. Container Lifecycle Management

**Automatic Lifecycle:**
- Containers start automatically sebelum test class di-load (static block)
- Containers stop automatically setelah semua tests selesai
- Container reuse (`withReuse(true)`) untuk mempercepat subsequent test runs

**JUnit 5 Integration:**
```java
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractIntegrationTest {
    @Container
    protected static final PostgreSQLContainer<?> POSTGRES_CONTAINER = ...;
    
    static {
        POSTGRES_CONTAINER.start();
        // Configure system properties
    }
}
```

### 8. Cara Menjalankan Integration Tests

#### a. Jalankan semua tests (termasuk integration tests):
```bash
./mvnw clean test
```

#### b. Jalankan hanya integration tests:
```bash
./mvnw test -Dtest=ProductServiceIntegrationTest,RedisIntegrationTest
```

#### c. Jalankan dengan Maven profiles:
```bash
# Test dengan H2 (default)
./mvnw test

# Test dengan Testcontainers (PostgreSQL + Redis)
./mvnw test -Dspring.profiles.active=test
```

### 9. Contoh Output

#### Successful Test Execution:
```
[INFO] ======== ProductServiceIntegrationTest ========
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] ======== RedisIntegrationTest ========
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0

[INFO] BUILD SUCCESS
```

#### Container Startup Logs:
```
[INFO] [testcontainers] Starting to pull image postgres:16-alpine
[INFO] [testcontainers] Pulling image layers...
[INFO] [testcontainers] Container postgres:16-alpine started
[INFO] [testcontainers] Starting to pull image redis:7-alpine
[INFO] [testcontainers] Container redis:7-alpine started
```

### 10. Best Practices

1. **Use @Transactional** - Rollback transactions setelah test untuk keep database clean
2. **Container Reuse** - `withReuse(true)` untuk mempercepat test execution
3. **Helper Methods** - Buat helper methods untuk create test data
4. **AssertJ** - Gunakan AssertJ untuk readable assertions
5. **Test Isolation** - Setiap test method harus independent
6. **Meaningful Test Names** - Gunakan naming convention `methodName_shouldExpectedBehavior`

### 11. Troubleshooting

| Masalah | Penyebab | Solusi |
|---|---|---|
| Container tidak start | Docker tidak running | Start Docker Desktop |
| Port conflict | Port 5432/6379 sudah dipakai | Testcontainers menggunakan random port, pastikan tidak ada conflict |
| Connection timeout | PostgreSQL belum siap | Testcontainers menunggu until healthy, tapi bisa increase timeout |
| Test gagal di CI/CD | Docker tidak available di CI | Gunakan H2 untuk CI, Testcontainers untuk local |
| Slow test execution | Container di-restart setiap test | Enable `withReuse(true)` |

### 12. Files yang Dibuat/Diubah

| File | Tindakan | Deskripsi |
|---|---|---|
| `pom.xml` | **Diubah** | Tambah dependencies: testcontainers, postgresql, junit-jupiter |
| `src/test/java/com/oms/config/AbstractIntegrationTest.java` | **Dibuat** | Base class dengan PostgreSQL + Redis containers |
| `src/test/resources/application-testcontainers.yml` | **Dibuat** | Konfigurasi untuk Testcontainers (PostgreSQL + Redis) |
| `src/test/java/com/oms/product/service/ProductServiceIntegrationTest.java` | **Dibuat** | Integration test untuk ProductService (5 test cases) |
| `src/test/java/com/oms/config/RedisIntegrationTest.java` | **Dibuat** | Integration test untuk Redis + Database (4 test cases) |
| `docs/current/development/testing/32-testcontainers-integration.md` | **Dibuat** | Dokumentasi |

### 13. Keuntungan Testcontainers vs H2

| Aspek | H2 | Testcontainers (PostgreSQL) |
|---|---|---|
| **SQL Compatibility** | Limited | Full PostgreSQL SQL |
| **Data Types** | H2-specific | PostgreSQL-specific |
| **Functions** | Limited | Full PostgreSQL functions |
| **Performance** | Fast | Slower (real DB) |
| **Production Parity** | Low | High |
| **Setup Complexity** | Simple | Requires Docker |
| **Test Isolation** | In-memory | Container-based |

### 14. Future Enhancements

1. **Testcontainers for Kafka** - Add Kafka container untuk event-driven tests
2. **Testcontainers for Elasticsearch** - Add Elasticsearch untuk full-text search tests
3. **Parallel Test Execution** - Configure parallel test execution dengan JUnit 5
4. **Testcontainers Reuse** - Enable container reuse untuk faster local development
5. **CI/CD Integration** - Configure Testcontainers untuk GitHub Actions/GitLab CI