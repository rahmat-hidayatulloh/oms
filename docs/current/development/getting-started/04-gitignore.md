# Task 4: .gitignore Configuration

> **Status:** Current
> **Audience:** Developer
> **Owner:** Engineering
> **Last verified:** 2026-08-23 (screening kode aktual `src/`, `pom.xml`)
> **Source of truth:** Repository source code (`src/`), `pom.xml`, dan konfigurasi runtime

## Tujuan
Membuat file .gitignore yang komprehensif untuk project Spring Boot / Java Maven.

## Acceptance Criteria
- [x] Ignore build artifacts (target/, *.jar, *.war)
- [x] Ignore IDE files (IntelliJ, Eclipse, VS Code, NetBeans)
- [x] Ignore OS files (.DS_Store, Thumbs.db)
- [x] Ignore log files (logs/, *.log)
- [x] Ignore environment/sensitive config (.env, application-local.yml)
- [x] Ignore test reports dan temporary files

## Detail Pekerjaan

### 1. Struktur .gitignore

```
.gitignore
├── Java / Maven
│   ├── target/              ← Build output
│   ├── *.class              ← Compiled classes
│   └── *.jar, *.war         ← Package files
│
├── Maven Wrapper
│   └── maven-wrapper.jar    ← Binary (tapi tetap track properties)
│
├── IDE (Multi-Platform)
│   ├── IntelliJ: .idea/, *.iml
│   ├── Eclipse: .project, .classpath, .settings/
│   └── VS Code: .vscode/
│
├── OS Files
│   ├── .DS_Store (macOS)
│   ├── Thumbs.db (Windows)
│   └── *.swp, *~ (Linux/Unix)
│
├── Logs
│   ├── logs/                ← Directory log
│   └── *.log, *.log.gz      ← File log
│
├── Environment / Sensitive
│   ├── .env, .env.*.local   ← Environment files
│   └── application-local.yml ← Local config (tidak boleh ter-commit)
│
├── Test Reports
│   ├── test-output/
│   ├── surefire-reports/
│   └── jacoco.exec
│
├── Spring Boot
│   ├── spring-pid-*.pid
│   └── *.h2.db, *.mv.db (H2 database files)
│
├── Heap Dumps
│   ├── heapdump*.hprof
│   └── *.hprof
│
└── Temp Files
    ├── tmp/, temp/
    └── *.tmp, *.bak
```

### 2. Best Practices yang Diterapkan

| Praktik | Implementasi |
|---|---|
| **Jangan commit binary** | `maven-wrapper.jar` di-ignore, tapi `maven-wrapper.properties` tetap di-track |
| **Jangan commit credentials** | `.env`, `application-local.yml`, `application-dev-local.yml` di-ignore |
| **Jangan commit build output** | `target/`, `build/`, `*.jar` di-ignore |
| **Jangan commit file IDE** | Semua folder IDE di-ignore, developer bebas pakai IDE apapun |
| **Jangan commit log files** | `logs/` directory dan `*.log` di-ignore |
| **Jangan commit system files** | `.DS_Store`, `Thumbs.db` di-ignore |

### 3. File yang Tetap di Git Track (dengan !)

```gitignore
# Maven Wrapper JAR di-ignore
.mvn/wrapper/maven-wrapper.jar

# Tapi tetap track properties
!.mvn/wrapper/maven-wrapper.properties
```

### 4. Pengecualian Penting
File-file berikut **WAJIB** di-commit:
- `pom.xml` - Build configuration
- `src/` - Source code
- `mvnw` - Maven Wrapper script
- `.mvn/wrapper/maven-wrapper.properties` - Wrapper config
- `config/checkstyle/checkstyle.xml` - Code quality rules
- `config/spotbugs/spotbugs-exclude.xml` - Bug exclusion rules
- `sonar-project.properties` - Sonar configuration
- `docs/` - Dokumentasi

File-file berikut **JANGAN** di-commit:
- `.env` - Environment variables
- `application-local.yml` - Local configuration
- `target/` - Build artifacts
- `logs/` - Application logs
- `*.hprof` - Heap dumps