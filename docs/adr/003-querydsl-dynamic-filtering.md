# ADR-003: QueryDSL for Dynamic Filtering and Sorting

- **Status:** Accepted
- **Date:** 2026-08-11 (V2 Phase 0)
- **Deciders:** Development Team

## Context

The Product API supports dynamic filtering (by name, category, price range, stock) and sorting with pagination. Standard JPA Specifications become verbose and hard to maintain for complex multi-field queries.

## Decision

Adopt **QueryDSL** (`querydsl-jpa` v5.1.0) for type-safe dynamic query building.

- Q-classes are auto-generated at compile time via `querydsl-apt` (JPA annotation processor).
- `ProductPredicates` encapsulates filter logic using `BooleanBuilder` and `BooleanExpression`.
- Repositories extend `QuerydslPredicateExecutor<T>` for seamless integration with Spring Data.

## Consequences

- Type-safe queries eliminate runtime string-based JPQL errors.
- `BooleanBuilder` composes conditions dynamically — adding a new filter is additive (OCP-friendly).
- Trade-off: Compile-time Q-class generation adds ~2s to build; Q-classes are committed to source control.
- Trade-off: Learning curve for developers unfamiliar with QueryDSL.

## Alternatives Considered

- **JPA Specifications:** Rejected — verbose, string-based, error-prone for complex filters.
- **Spring Data JPA derived queries:** Rejected — cannot handle dynamic multi-field combinations.
- **jOOQ:** Considered — more powerful but heavier; QueryDSL sufficient for current needs.