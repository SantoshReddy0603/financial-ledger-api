# ADR-0001: Java 21, Javalin and Maven Stack

**Status:** Accepted
**Date:** 2026-11-20
**Requirements:** DC-1, DC-2, DC-3, DC-5, NFR-POR-1, NFR-PERF-1, NFR-PERF-5

## Context

DC-1 fixes Java 21 (or later LTS) with Maven and a committed Maven Wrapper, and requires the JDK, HTTP framework, SQLite driver and all dependency versions to be documented and pinned. The SRS leaves the HTTP framework open. The forces:

* Concurrency correctness depends on controlling exactly when a SQLite transaction begins and commits ([[resources/sqlite-wal-locking-mechanics]]). Frameworks that manage transactions or connection pools implicitly make `BEGIN IMMEDIATE` harder to guarantee.
* NFR-PERF-5 requires startup on a 1,000,000-entry database within 5 seconds; NFR-PERF-1 requires transfer p95 ≤ 100 ms.
* NFR-MNT-1 forbids domain dependence on the HTTP framework, so the framework should be thin and replaceable ([[resources/hexagonal-architecture-provider-ports]]).
* Exact JSON token validation (NFR-API-3, NFR-API-7: duplicate members, integer tokens only) needs low-level parser access.

## Decision

| Concern | Choice | Pinned version |
|---------|--------|----------------|
| JDK | Eclipse Temurin 21 (any vendor's Java 21+ works) | enforced `[21,)` |
| Build | Maven via Maven Wrapper | Wrapper 3.3.4, Maven 3.9.12 |
| HTTP | Javalin (Jetty 12 underneath) | 6.7.0 |
| SQLite driver | Xerial sqlite-jdbc (bundles native SQLite) | 3.53.2.1 |
| JSON | Jackson databind and streaming parser | 2.22.4 |
| Argon2id | BouncyCastle `bcprov-jdk18on` (pure Java) | 1.87 |
| Logging | SLF4J with Logback and its JSON encoder | 2.0.20 / 1.5.38 |
| Tests | JUnit Jupiter, AssertJ, JDK `HttpClient` for system tests | 5.14.4 / 3.27.7 |

All versions are properties in `pom.xml`; `maven-enforcer-plugin` requires dependency convergence, Java ≥ 21 and Maven ≥ 3.9. The build produces one runnable shaded jar, `target/financial-ledger-api.jar`. No dependency injection container and no ORM are used: the composition root (`LedgerApplication`) wires objects by constructor.

## Alternatives Considered

| Option | Why not chosen |
|--------|----------------|
| Spring Boot | Implicit transaction management and connection pooling obscure `BEGIN IMMEDIATE`; multi-second startup and a large dependency graph; auto-configuration hides behaviour graders need to see. |
| Quarkus / Micronaut | Fast, but build-time DI and extensions add concepts the team must learn for little gain at this size. |
| JDK `com.sun.net.httpserver` | No routing, limited request-size control, more hand-written plumbing. |
| JDBI / jOOQ / Hibernate | Abstractions over a handful of hand-checked SQL statements; Hibernate would fight the append-only design. |
| Native Argon2 (argon2-jvm) | Needs a native library per OS; complicates the Windows CI leg (NFR-POR-1). |

## Consequences

* Startup is under one second on an empty database (measured by `HealthSystemTest` and `local-check`), leaving headroom for NFR-PERF-5.
* Javalin types appear only in `org.ledgerapi.http`; swapping the framework would touch that package and `LedgerApplication` only.
* Without an ORM, every SQL statement is explicit and uses bound parameters (NFR-SEC-3), reviewed in pull requests.
* Dependency upgrades are single-line property changes; the enforcer fails the build on version conflicts.

## Brain Notes

* [[resources/sqlite-wal-locking-mechanics]]
* [[resources/hexagonal-architecture-provider-ports]]
* [[resources/argon2id-password-hashing-and-session-tokens]]
* [[resources/rest-api-design-financial-services]]
