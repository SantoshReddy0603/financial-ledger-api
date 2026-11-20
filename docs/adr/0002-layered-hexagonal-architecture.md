# ADR-0002: Layered Architecture with Provider Ports

**Status:** Accepted
**Date:** 2026-11-20
**Requirements:** DC-4, NFR-MNT-1, NFR-MNT-2, TQ-1, TQ-2, FR-3.8

## Context

DC-4 requires storage, clock, identifier generation, hashing and HTTP transport behind provider interfaces so tests can substitute them. NFR-MNT-1 forbids domain logic from depending on the HTTP framework, JDBC or SQLite types, and NFR-MNT-2 requires 85% line coverage of domain and service layers. TQ-2 requires fault injection before each persistence boundary. Hash-chain and idempotency tests need deterministic timestamps and identifiers ([[resources/hexagonal-architecture-provider-ports]], [[resources/canonical-json-and-hash-chains]]).

## Decision

Five packages under `org.ledgerapi`, with dependencies pointing inward only:

| Package | Contents | May depend on |
|---------|----------|---------------|
| `domain` | Value types (`Money`, `Currency`, IDs, `Timestamps`), rules (`TransferRules`, `Balances`), `ErrorCode`, `LedgerException` | JDK only |
| `provider` | Port interfaces: `ClockProvider`, `IdGenerator`, `HashProvider`, `PasswordHasher`, `TokenGenerator`, `StorageProbe`, `LedgerStore` (unit-of-work), repository ports | `domain` |
| `application` | Use cases: `UserService`, `WalletService`, `TransferService`, `FundingService`, `ReversalService`, `ReconciliationService`, `AuthService`, `HealthService` | `domain`, `provider` |
| `infrastructure` | Adapters: `sqlite` (database, migrator, repositories), `system` (clock, UUID, SHA-256, Argon2id, `SecureRandom`) | all inner packages, JDBC |
| `http` | Javalin handlers, JSON parsing, authentication filter, error mapping | `application`, `domain` |

`LedgerApplication` is the composition root and the only class that names concrete adapters. Tests construct it with `FixedClock`, `SequentialIds` and a temporary database.

Transaction boundaries belong to application services: a service asks `LedgerStore.write(work)` to run a unit of work atomically; repositories receive the active unit-of-work handle, never a raw `Connection` exposed to the domain.

## Alternatives Considered

| Option | Why not chosen |
|--------|----------------|
| Handlers calling SQL directly | Fast to write, but business rules become untestable without HTTP and SQLite, failing NFR-MNT-1/2. |
| Full DDD with aggregates and domain events | Heavier than five use cases need. |
| A port per SQL statement | Too fine-grained; ports are kept at aggregate/use-case level. |

## Consequences

* Domain and application tests run without I/O; JaCoCo measures `org/ledgerapi/domain/**` and `org/ledgerapi/application/**` against the 85% gate.
* A failing `LedgerStore` adapter in tests can throw at each boundary (after sequence allocation, after entry insert, after balance update, before commit) to prove rollback (FR-3.8).
* An architecture test (Stage 3) will scan imports to fail the build if `domain` or `application` imports `io.javalin`, `java.sql` or `org.sqlite`.
* Slightly more interfaces than a script-style service; accepted for testability.

## Brain Notes

* [[resources/hexagonal-architecture-provider-ports]]
* [[areas/ledger-architecture]]
* [[resources/acid-transactions-in-relational-databases]]
