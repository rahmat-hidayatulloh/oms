# Task 1: Inisialisasi Project Spring Boot

> **Status:** Current
> **Audience:** Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-28 (screening kode aktual `src/`, `pom.xml`)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## Tujuan
Membuat project Spring Boot 3.2.x dengan struktur yang rapi dan dependencies lengkap.

## Acceptance Criteria
- [x] Java 21 terinstall
- [x] Dependencies: Spring Web, Spring Data JPA, PostgreSQL, Spring Security, JJWT, Redis, Lombok, Validation, Actuator
- [x] Struktur package: com.oms
- [x] Main class: OmsApplication.java
- [x] Bisa di-run tanpa error

## Detail Pekerjaan

### 1. Cek Environment
- **Java**: OpenJDK 21.0.11
- **Build Tool**: Maven Wrapper (mvnw) - tanpa perlu install Maven global

### 2. Struktur Project
```
oms/
├── .mvn/wrapper/
│   ├── maven-wrapper.jar
│   └── maven-wrapper.properties
├── src/
│   ├── main/java/com/oms/
│   │   └── OmsApplication.java
│   ├── main/resources/
│   │   ├── application.yml
│   │   └── application-dev.yml
│   └── test/java/com/oms/
├── mvnw (executable)
├── pom.xml
└── README.md
```

### 3. Dependencies Terinstall
| Dependency | Versi | Kegunaan |
|---|---|---|
| Spring Boot Starter Web | 3.2.12 | REST API |
| Spring Data JPA | 3.2.12 | Database ORM |
| PostgreSQL | 42.x | Production database |
| H2 Database | 2.x | Development database (in-memory) |
| Spring Security | 3.2.12 | Authentication & Authorization |
| JJWT | 0.12.5 | JWT Token |
| Redis | 3.2.12 | Caching |
| Lombok | 1.18.x | Boilerplate reduction |
| Validation | 3.2.12 | Request validation |
| Actuator | 3.2.12 | Monitoring & metrics |

### 4. Cara Menjalankan
```bash
# Development (H2 in-memory - tanpa PostgreSQL/Redis)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Production (PostgreSQL + Redis) — profile wajib via env (F-22)
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

### 5. Hasil Test
- ✅ Tomcat started on port **8080**
- ✅ H2 console available at **/h2-console**
- ✅ Actuator endpoints: **/actuator/health**, **/actuator/info**, **/actuator/metrics**
- ✅ Application started in **3.6 seconds**

### 6. Catatan
- Untuk development, gunakan profile `dev` agar menggunakan H2 in-memory database
- Untuk production, pastikan PostgreSQL dan Redis sudah berjalan
- Security password default untuk dev akan digenerate otomatis setiap kali run