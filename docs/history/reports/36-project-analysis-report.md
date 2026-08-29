# Project Analysis Report - OMS

> **Status:** Historical / Superseded
> **Audience:** Maintainer
> **Owner:** Engineering
> **Snapshot date:** 2026-07-27
> **Superseded by:** [Current architecture overview](../../current/overview/architecture.md) dan [historical V0 analysis](../optimalisasi-v0/ANALISIS-PROYEK-OMS-V0.md)
> **Catatan:** Dokumen ini merekam status historis (154 tests, 59.6% coverage).

## Executive Summary
Comprehensive analysis of the OMS project covering code quality, test coverage, and standards compliance.

**Analysis Date**: 2026-07-27  
**Project**: oms  
**Branch**: main  
**Commit**: 7276a3cb6f6d0b63eeb908b9799fb4f9114e4dcd

---

## 1. Test Coverage Analysis (JaCoCo)

### Overall Coverage Metrics
| Metric | Covered | Missed | Total | Coverage % | Target (90%) | Status |
|--------|---------|--------|-------|------------|--------------|--------|
| **LINE** | 336 | 228 | 564 | **59.6%** | 90% | ❌ CRITICAL |
| **INSTRUCTION** | 1,401 | 813 | 2,214 | **63.3%** | 90% | ❌ CRITICAL |
| **BRANCH** | 47 | 39 | 86 | **54.7%** | 90% | ❌ CRITICAL |
| **METHOD** | 96 | 39 | 135 | **71.1%** | 90% | ❌ CRITICAL |
| **CLASS** | 21 | 3 | 24 | **87.5%** | 90% | ⚠️ BELOW TARGET |

### Coverage by Package

#### com.oms.health (Health Indicators)
- **LINE**: 9/109 = **8.3%** ❌
- **CLASS**: 2/3 = **66.7%** ❌
- **Issue**: Health indicators not tested (DatabaseHealthIndicator, RedisHealthIndicator, ExternalAPIHealthIndicator)

#### com.oms.controller (REST Controllers)
- **LINE**: 35/77 = **45.5%** ❌
- **INSTRUCTION**: 177/365 = **48.5%** ❌
- **METHOD**: 16/26 = **61.5%** ❌
- **Issue**: Controllers have low test coverage
  - VersionController: 0% coverage
  - AuthController: 63% line coverage
  - OrderController: 20% line coverage
  - ProductController: 87% line coverage ✅
  - AdminController: 100% line coverage ✅

#### com.oms.repository (Data Access)
- **LINE**: 15/15 = **100%** ✅
- **CLASS**: 1/1 = **100%** ✅
- **Note**: Only ProductPredicates tested, repositories not directly tested

#### com.oms.exception (Exception Handlers)
- **LINE**: 46/73 = **63.0%** ❌
- **CLASS**: 3/5 = **60.0%** ❌
- **Issue**: 
  - BusinessException: 0% coverage
  - AsyncExceptionHandler: 0% coverage

#### com.oms.security (JWT & Authentication)
- **LINE**: 64/102 = **62.7%** ❌
- **CLASS**: 3/3 = **100%** ✅
- **Issue**: 
  - JwtTokenProvider: 19% line coverage (deprecated class)
  - JwtUtil: 90% line coverage ✅
  - JwtAuthenticationFilter: 84% line coverage ✅

#### com.oms.service (Business Logic)
- **LINE**: 163/182 = **89.6%** ⚠️
- **CLASS**: 6/6 = **100%** ✅
- **METHOD**: 40/47 = **85.1%** ⚠️
- **Issue**: 
  - RefreshTokenService: 10% line coverage (requires Redis)
  - NoOpRefreshTokenService: 91% line coverage ✅
  - ProductService: 100% line coverage ✅
  - OrderService: 100% line coverage ✅
  - EmailService: 100% line coverage ✅
  - CustomUserDetailsService: 100% line coverage ✅

#### com.oms (Main Application)
- **LINE**: 4/6 = **66.7%** ⚠️
- **Issue**: OmsApplication.main() not tested

### Critical Coverage Gaps (< 50%)
1. **Health Indicators**: 8.3% (DatabaseHealthIndicator, RedisHealthIndicator, ExternalAPIHealthIndicator)
2. **VersionController**: 0% (all methods untested)
3. **OrderController**: 20% (only constructor tested)
4. **JwtTokenProvider**: 19% (deprecated but still in codebase)
5. **RefreshTokenService**: 10% (requires Redis infrastructure)
6. **BusinessException**: 0% (no tests for constructors)
7. **AsyncExceptionHandler**: 0% (no tests for error handling)

---

## 2. Code Quality Issues

### 2.1 Checkstyle Analysis
**Status**: ✅ PASSED (0 violations)

No checkstyle violations detected. Code follows formatting standards.

### 2.2 SpotBugs Analysis
**Status**: ❌ FAILED (1 Medium issue)

#### Issue #1: EI_EXPOSE_REP2
- **Severity**: Medium
- **File**: `src/main/java/com/oms/health/DatabaseHealthIndicator.java`
- **Line**: 19
- **Description**: `new com.oms.health.DatabaseHealthIndicator(DataSource)` may expose internal representation by storing an externally mutable object into DatabaseHealthIndicator.dataSource
- **Impact**: Potential security vulnerability - DataSource object could be modified externally
- **Recommendation**: 
  ```java
  // Option 1: Defensive copy (if DataSource is cloneable)
  this.dataSource = (DataSource) dataSource.clone();
  
  // Option 2: Use immutable wrapper
  this.dataSource = new ImmutableDataSourceWrapper(dataSource);
  
  // Option 3: Document that DataSource should not be modified
  ```

### 2.3 Clean Code Violations

#### Unused Code
1. **JwtTokenProvider.java** - Deprecated class, 19% coverage, should be removed
2. **VersionController.java** - 0% coverage, API versioning feature not tested
3. **AsyncExceptionHandler.java** - 0% coverage, async error handling not tested

#### Code Smells
1. **RefreshTokenService** - 78 instructions missed (requires Redis, not available in test)
2. **Health Indicators** - 313 instructions missed (no tests for health checks)
3. **OrderController** - 32 instructions missed (only happy path tested)

---

## 3. Test Quality Issues

### 3.1 Test Failures
**Status**: ❌ 1 FAILURE out of 154 tests

#### Failure #1: ProductCacheTest.shouldCacheProductOnFirstCall
- **Error**: `Expecting actual not to be null`
- **Line**: ProductCacheTest.java:70
- **Root Cause**: Redis cache not available in test environment
- **Impact**: Cache functionality cannot be tested without Redis
- **Recommendation**: 
  - Add `@Testcontainers` with Redis container
  - Or mark as `@Disabled("Requires Redis")`
  - Or use embedded Redis for testing

### 3.2 Skipped Tests
**Total**: 9 tests skipped (5.8% of total)

**Reason**: Testcontainers not available (Docker not running)

### 3.3 Test Coverage by Layer

| Layer | Tests | Coverage | Status |
|-------|-------|----------|--------|
| **Controller** | 15 | 45-100% | ⚠️ Variable |
| **Service** | 18 | 0-100% | ⚠️ Variable |
| **Repository** | 2 | 100% | ✅ Good |
| **Security** | 3 | 63-100% | ⚠️ Variable |
| **Entity** | 1 | N/A | ✅ N/A |
| **Exception** | 1 | 63% | ⚠️ Low |
| **Health** | 0 | 8% | ❌ Critical |
| **Migration** | 1 | N/A | ✅ N/A |

---

## 4. SonarQube Standards Compliance

### 4.1 Code Quality Gates
| Metric | Current | Target | Status |
|--------|---------|--------|--------|
| **Coverage** | 59.6% | ≥ 90% | ❌ CRITICAL |
| **Duplicated Lines** | Unknown | < 3% | ⚠️ Not measured |
| **Maintainability** | Unknown | A | ⚠️ Not measured |
| **Reliability** | 1 failure | 0 | ❌ FAILED |
| **Security** | 1 Medium | 0 | ❌ FAILED |

### 4.2 Technical Debt
**Estimated Debt**: 
- **Test Coverage Gap**: 30.4% (90% - 59.6%) = ~170 lines untested
- **SpotBugs Issue**: 1 Medium (DatabaseHealthIndicator)
- **Test Failure**: 1 (ProductCacheTest)
- **Dead Code**: JwtTokenProvider (deprecated)

### 4.3 Code Smells (Sonar Metrics)
1. **Insufficient Test Coverage**: 59.6% vs 90% target
2. **Unused Deprecated Code**: JwtTokenProvider still in codebase
3. **Potential Security Issue**: EI_EXPOSE_REP2 in DatabaseHealthIndicator
4. **Incomplete Testing**: Health indicators, controllers, exception handlers

---

## 5. Recommendations

### 5.1 Critical (Must Fix)
1. **Increase Test Coverage to 90%**
   - Add tests for Health Indicators (DatabaseHealthIndicator, RedisHealthIndicator, ExternalAPIHealthIndicator)
   - Add tests for VersionController
   - Add tests for OrderController (search, update, delete endpoints)
   - Add tests for BusinessException constructors
   - Add tests for AsyncExceptionHandler

2. **Fix SpotBugs Issue**
   - Address EI_EXPOSE_REP2 in DatabaseHealthIndicator
   - Add defensive copy or immutable wrapper for DataSource

3. **Fix ProductCacheTest**
   - Add Redis Testcontainer or mark as @Disabled
   - Ensure all tests pass in CI/CD pipeline

### 5.2 High Priority (Should Fix)
1. **Remove Deprecated Code**
   - Remove JwtTokenProvider (deprecated, 19% coverage)
   - Update any references to use JwtUtil instead

2. **Improve Controller Tests**
   - OrderController: Add tests for search, update, delete
   - AuthController: Add tests for refresh, logout
   - VersionController: Add basic tests

3. **Test Exception Handlers**
   - Add tests for AsyncExceptionHandler
   - Add tests for BusinessException constructors

### 5.3 Medium Priority (Nice to Have)
1. **Increase Service Test Coverage**
   - RefreshTokenService: Add integration tests with Testcontainers Redis
   - NoOpRefreshTokenService: Already at 91%, good

2. **Add Integration Tests**
   - Health indicator integration tests
   - End-to-end API tests with Testcontainers

3. **Code Quality Metrics**
   - Add SonarQube scanner to CI/CD
   - Track code coverage over time
   - Set up quality gates

### 5.4 Low Priority (Optional)
1. **Performance Testing**
   - Load tests for QueryDSL predicates
   - Cache performance benchmarks

2. **Documentation**
   - Add README for running tests
   - Document test strategy
   - Add testing guidelines

---

## 6. Positive Findings

### ✅ Strengths
1. **Checkstyle**: 0 violations - code follows formatting standards
2. **Repository Layer**: 100% coverage for ProductPredicates
3. **Core Services**: ProductService, OrderService, EmailService have 100% coverage
4. **Security**: JwtUtil and JwtAuthenticationFilter have >80% coverage
5. **ProductController**: 87% line coverage
6. **AdminController**: 100% line coverage
7. **CustomUserDetailsService**: 100% coverage
8. **NoOpRefreshTokenService**: 91% coverage

### ✅ Best Practices Observed
1. **@DataJpaTest** for repository testing (fast, focused)
2. **@SpringBootTest** for integration testing
3. **TestEntityManager** for test data setup
4. **QueryDSL predicates** tested comprehensively
5. **Soft delete** tested with @SQLRestriction
6. **Optimistic locking** tested with @Version
7. **JPA auditing** enabled and tested
8. **Clear test naming** (shouldXWhenY pattern)
9. **AAA pattern** (Arrange-Act-Assert) followed
10. **Comprehensive documentation** for each test

---

## 7. Action Plan

### Phase 1: Critical Fixes (Week 1)
- [ ] Fix SpotBugs EI_EXPOSE_REP2 issue
- [ ] Fix ProductCacheTest (Redis dependency)
- [ ] Add tests for Health Indicators (target: 80% coverage)
- [ ] Add tests for VersionController (target: 80% coverage)

### Phase 2: Coverage Improvement (Week 2-3)
- [ ] Add tests for OrderController endpoints (target: 80% coverage)
- [ ] Add tests for AuthController refresh/logout (target: 80% coverage)
- [ ] Add tests for BusinessException (target: 100% coverage)
- [ ] Add tests for AsyncExceptionHandler (target: 80% coverage)

### Phase 3: Code Quality (Week 4)
- [ ] Remove deprecated JwtTokenProvider
- [ ] Add SonarQube integration to CI/CD
- [ ] Set up quality gates (coverage ≥ 90%, no new bugs)
- [ ] Add performance tests for QueryDSL

### Phase 4: Documentation (Week 4)
- [ ] Create testing guidelines document
- [ ] Document test strategy
- [ ] Add README for running tests
- [ ] Create coverage dashboard

---

## 8. Conclusion

### Summary
The project has **good code quality** (0 checkstyle violations) but **critical test coverage gaps** (59.6% vs 90% target). The main issues are:

1. **Test Coverage**: 59.6% (30.4% below target)
2. **SpotBugs**: 1 Medium security issue
3. **Test Failures**: 1 failure (ProductCacheTest)
4. **Dead Code**: JwtTokenProvider (deprecated)

### Risk Assessment
- **High Risk**: Insufficient test coverage for critical components (health indicators, controllers)
- **Medium Risk**: SpotBugs security issue in DatabaseHealthIndicator
- **Low Risk**: 1 test failure (cache test, Redis dependency)

### Recommendation
**Prioritize increasing test coverage to 90%** by adding tests for:
1. Health indicators (8.3% → 80%)
2. Controllers (45-100% → 80%+)
3. Exception handlers (63% → 90%)
4. Remove deprecated code (JwtTokenProvider)

**Estimated Effort**: 2-3 weeks for full compliance with 90% coverage target.

---

## Appendix A: Test Execution Summary

```
Tests run: 154
Failures: 1 (ProductCacheTest)
Errors: 0
Skipped: 9 (Testcontainers not available)
Success Rate: 99.3% (excluding skipped)
```

## Appendix B: Coverage Calculation

**Line Coverage Formula**:
```
Coverage % = (Covered Lines / Total Lines) × 100
           = (336 / (336 + 228)) × 100
           = (336 / 564) × 100
           = 59.6%
```

**Target Calculation**:
```
Target: 90%
Current: 59.6%
Gap: 30.4%
Lines to cover: 564 × 0.304 = 171 lines
```

## Appendix C: Related Documentation

- [ProductRepositoryTest source](../../../src/test/java/com/oms/repository/ProductRepositoryTest.java)
- [Clean Code Standards](../../current/development/standards/02-clean-code-quality-gates.md)
- [Testing Strategy](../../current/development/testing/32-testcontainers-integration.md)
- [SonarQube Configuration](../../../sonar-project.properties)