# Requirements Traceability

**Specification:** [`docs/srs.pdf`](srs.pdf) §10.2 · **Plan:** [`plan.md`](plan.md) · **Updated:** 2026-11-20 (Stage 2)

One row per requirement identifier. The chain is Requirement → Issue → PR → Test/inspection → Skill → Brain note (AGENTS.md §4).

* **Issue / PR:** `_pending_` means the GitHub issue or pull request has not been created yet. Issues are opened from the `T-xx` tasks in [`plan.md`](plan.md); fill in the link here in the PR that closes the issue. No link is invented.
* **Test / inspection:** backticked names are existing JUnit tests (`Class.method`) or scripts; `T-xx (planned)` names the task that will add the evidence.
* **Status:** *Implemented*: evidence exists and passes. *Partial*: some evidence exists. *Designed*: design is complete; no code yet. *Pending* / *Not yet due*: scheduled for a later stage.
* Design references: ADRs are in [`adr/`](adr/README.md); `architecture`, `data-model` and `uml` are in [`design/`](design/architecture.md); `openapi` is [`api/openapi.yaml`](api/openapi.yaml).

## Functional requirements (FR)

| ID | Summary | Design | Issue | PR | Test / inspection | Skill | Brain note | Status |
|----|---------|--------|-------|----|-------------------|-------|------------|--------|
| FR-1.1 | Register user | ADR-0008, openapi | _pending_ | _pending_ | T-08 system/negative (planned) | `simulate-transfer` (Stage 3) | [authentication-sessions](../brain/areas/authentication-sessions.md), [argon2id-password-hashing-and-session-tokens](../brain/resources/argon2id-password-hashing-and-session-tokens.md) | Designed |
| FR-1.2 | Create wallet | data-model, openapi | _pending_ | _pending_ | T-08 (planned) | `simulate-transfer` (Stage 3) | [ledger-architecture](../brain/areas/ledger-architecture.md) | Designed |
| FR-1.3 | One wallet per currency, immutable currency | ADR-0007 | _pending_ | _pending_ | `SchemaMigratorTest.schemaConstraintsRejectInvalidRows`; T-08 | `simulate-transfer` (Stage 3) | [ledger-architecture](../brain/areas/ledger-architecture.md) | Partial: DB constraint tested |
| FR-1.4 | Atomic wallet + account creation | ADR-0003 | _pending_ | _pending_ | T-08 integration (planned) | n/a: no skill exercises this | [acid-transactions-in-relational-databases](../brain/resources/acid-transactions-in-relational-databases.md) | Designed |
| FR-1.5 | Get wallet, one snapshot | ADR-0003 | _pending_ | _pending_ | `SqliteDatabaseTest.readersSeeOnlyCommittedData`; T-08 | `simulate-transfer` (Stage 3) | [concurrency-acid](../brain/areas/concurrency-acid.md) | Partial: snapshot primitive tested |
| FR-1.6 | Get user with wallets | openapi | _pending_ | _pending_ | T-08 (planned) | n/a: no skill exercises this | [rest-api-design-financial-services](../brain/resources/rest-api-design-financial-services.md) | Designed |
| FR-1.7 | Admin funding | ADR-0005, uml §2.5 | _pending_ | _pending_ | T-09 (planned) | `simulate-transfer` (Stage 3) | [double-entry-accounting-principles](../brain/resources/double-entry-accounting-principles.md), [idempotency-design](../brain/areas/idempotency-design.md) | Designed |
| FR-1.8 | Balances backed by entries | ADR-0007, architecture §5 | _pending_ | _pending_ | T-06, T-20 (planned) | `simulate-transfer` (Stage 3) | [double-entry-accounting-principles](../brain/resources/double-entry-accounting-principles.md) | Designed |
| FR-2.1 | Transfer request | openapi | _pending_ | _pending_ | T-10 (planned) | `simulate-transfer` (Stage 3) | [rest-api-design-financial-services](../brain/resources/rest-api-design-financial-services.md) | Designed |
| FR-2.2 | Idempotency-Key header rules | ADR-0005 | _pending_ | _pending_ | T-04, T-10 (planned) | `simulate-transfer` (Stage 3) | [ietf-idempotency-http-standard](../brain/resources/ietf-idempotency-http-standard.md) | Designed |
| FR-2.3 | Transfer check order | ADR-0009, uml §2.1 | _pending_ | _pending_ | T-10 negative (planned) | n/a: no skill exercises this | [concurrency-race-conditions-wallets](../brain/resources/concurrency-race-conditions-wallets.md) | Designed |
| FR-2.4 | 201 transaction representation | openapi | _pending_ | _pending_ | T-10 (planned) | `simulate-transfer` (Stage 3) | [rest-api-design-financial-services](../brain/resources/rest-api-design-financial-services.md) | Designed |
| FR-2.5 | Canonical fingerprint | ADR-0004 | _pending_ | _pending_ | T-02, T-03 vectors (planned) | n/a: no skill exercises this | [canonical-json-and-hash-chains](../brain/resources/canonical-json-and-hash-chains.md) | Designed |
| FR-2.6 | Replay returns stored response | ADR-0005, uml §2.2 | _pending_ | _pending_ | T-07, T-10 (planned) | `simulate-transfer` (Stage 3) | [idempotency-design](../brain/areas/idempotency-design.md) | Designed |
| FR-2.7 | Key reuse → 422 | ADR-0005 | _pending_ | _pending_ | T-07 (planned) | n/a: no skill exercises this | [idempotency-design](../brain/areas/idempotency-design.md) | Designed |
| FR-2.8 | Record committed atomically | ADR-0005, data-model | _pending_ | _pending_ | T-07, T-23 (planned) | n/a: no skill exercises this | [ietf-idempotency-http-standard](../brain/resources/ietf-idempotency-http-standard.md), [acid-transactions-in-relational-databases](../brain/resources/acid-transactions-in-relational-databases.md) | Designed |
| FR-2.9 | Records retained across restart | ADR-0007 (append-only trigger) | _pending_ | _pending_ | `SchemaMigratorTest.ledgerRecordsAreAppendOnly`; T-24 | n/a: no skill exercises this | [audit-trail-immutability-patterns](../brain/resources/audit-trail-immutability-patterns.md) | Partial: trigger tested |
| FR-2.10 | Concurrent same-key requests | ADR-0003, ADR-0005 | _pending_ | _pending_ | T-25 (planned) | `simulate-transfer` (Stage 3) | [concurrency-race-conditions-wallets](../brain/resources/concurrency-race-conditions-wallets.md) | Designed |
| FR-2.11 | Get transaction, derived reversedBy | ADR-0004 | _pending_ | _pending_ | T-11 (planned) | n/a: no skill exercises this | [canonical-json-and-hash-chains](../brain/resources/canonical-json-and-hash-chains.md) | Designed |
| FR-3.1 | One BEGIN IMMEDIATE per money move | ADR-0003 | _pending_ | _pending_ | `SqliteDatabaseTest.commitsSuccessfulWork`, `rollsBackEverythingWhenWorkFails`; T-06 | `simulate-transfer` (Stage 3) | [sqlite-wal-locking-mechanics](../brain/resources/sqlite-wal-locking-mechanics.md), [concurrency-acid](../brain/areas/concurrency-acid.md) | Partial: primitive tested |
| FR-3.2 | Serializable outcome | ADR-0003, uml §2.3 | _pending_ | _pending_ | T-25 (planned) | `simulate-transfer` (Stage 3) | [concurrency-race-conditions-wallets](../brain/resources/concurrency-race-conditions-wallets.md) | Designed |
| FR-3.3 | No overdraft under concurrency | uml §2.3 | _pending_ | _pending_ | T-25 (planned) | `simulate-transfer` (Stage 3) | [concurrency-race-conditions-wallets](../brain/resources/concurrency-race-conditions-wallets.md) | Designed |
| FR-3.4 | WAL, FULL, FKs, busy timeout | ADR-0003 | _pending_ | _pending_ | `SqliteDatabaseTest.everyConnectionUsesTheRequiredPragmas` | `local-check` | [sqlite-wal-locking-mechanics](../brain/resources/sqlite-wal-locking-mechanics.md) | Implemented |
| FR-3.5 | Lock timeout → 503 STORAGE_BUSY | ADR-0003, uml §2.4 | _pending_ | _pending_ | `SqliteDatabaseTest.writeLockTimeoutBecomesStorageBusy`, `busyAndLockedSqliteErrorsTranslateToStorageBusy`; HTTP mapping in T-10 | n/a: no skill exercises this | [sqlite-wal-locking-mechanics](../brain/resources/sqlite-wal-locking-mechanics.md) | Partial: storage layer tested |
| FR-3.6 | Gap-free numbering under concurrency | ADR-0007 | _pending_ | _pending_ | T-25 (planned) | n/a: no skill exercises this | [acid-transactions-in-relational-databases](../brain/resources/acid-transactions-in-relational-databases.md) | Designed |
| FR-3.7 | No half-visible transaction | ADR-0003 | _pending_ | _pending_ | `SqliteDatabaseTest.readersSeeOnlyCommittedData` | n/a: no skill exercises this | [concurrency-acid](../brain/areas/concurrency-acid.md) | Partial: primitive tested |
| FR-3.8 | Rollback on injected failure, crash recovery | ADR-0003 | _pending_ | _pending_ | `SqliteDatabaseTest.rollsBackEverythingWhenWorkFails`; T-23, T-24 | n/a: no skill exercises this | [acid-transactions-in-relational-databases](../brain/resources/acid-transactions-in-relational-databases.md) | Partial |
| FR-4.1 | Append-only triggers | ADR-0007 | _pending_ | _pending_ | `SchemaMigratorTest.ledgerRecordsAreAppendOnly` | n/a: no skill exercises this | [audit-trail-immutability-patterns](../brain/resources/audit-trail-immutability-patterns.md) | Implemented (DB level) |
| FR-4.2 | Admin reversal | ADR-0005, uml §2.6 | _pending_ | _pending_ | T-12 (planned) | n/a: no skill exercises this | [audit-trail-immutability-patterns](../brain/resources/audit-trail-immutability-patterns.md) | Designed |
| FR-4.3 | Reversal mirrors original | uml §2.6 | _pending_ | _pending_ | T-12 (planned) | n/a: no skill exercises this | [double-entry-accounting-principles](../brain/resources/double-entry-accounting-principles.md) | Designed |
| FR-4.4 | Reversal check order | uml §2.6 | _pending_ | _pending_ | T-12 negative (planned) | n/a: no skill exercises this | [audit-trail-immutability-patterns](../brain/resources/audit-trail-immutability-patterns.md) | Designed |
| FR-4.5 | Hash chain | ADR-0004 | _pending_ | _pending_ | T-03, T-06 (planned) | n/a: no skill exercises this | [canonical-json-and-hash-chains](../brain/resources/canonical-json-and-hash-chains.md) | Designed |
| FR-4.6 | Wallet entries pagination | data-model §4, openapi | _pending_ | _pending_ | T-13 (planned) | n/a: no skill exercises this | [rest-api-design-financial-services](../brain/resources/rest-api-design-financial-services.md) | Designed |
| FR-4.7 | History readable for life of DB | ADR-0007 | _pending_ | _pending_ | `SchemaMigratorTest.ledgerRecordsAreAppendOnly`; T-13 | n/a: no skill exercises this | [audit-trail-immutability-patterns](../brain/resources/audit-trail-immutability-patterns.md) | Partial |
| FR-5.1 | Admin-only, read-only reconciliation | ADR-0010 | _pending_ | _pending_ | T-20 (planned) | `simulate-transfer` (Stage 3) | [reconciliation-algorithms-accounting](../brain/resources/reconciliation-algorithms-accounting.md) | Designed |
| FR-5.2 | Single snapshot | ADR-0010, uml §2.8 | _pending_ | _pending_ | T-20 (planned) | `simulate-transfer` (Stage 3) | [reconciliation-algorithms-accounting](../brain/resources/reconciliation-algorithms-accounting.md), [concurrency-acid](../brain/areas/concurrency-acid.md) | Designed |
| FR-5.3 | Checks C1–C7 | ADR-0010 | _pending_ | _pending_ | T-20 (planned) | `simulate-transfer` (Stage 3) | [reconciliation-algorithms-accounting](../brain/resources/reconciliation-algorithms-accounting.md) | Designed |
| FR-5.4 | Report schema | openapi | _pending_ | _pending_ | T-20 (planned) | `simulate-transfer` (Stage 3) | [reconciliation-algorithms-accounting](../brain/resources/reconciliation-algorithms-accounting.md) | Designed |
| FR-5.5 | 200 for either status, 500 on failure | ADR-0010 | _pending_ | _pending_ | T-20 (planned) | n/a: no skill exercises this | [reconciliation-algorithms-accounting](../brain/resources/reconciliation-algorithms-accounting.md) | Designed |
| FR-5.6 | Corruption tests | ADR-0010 | _pending_ | _pending_ | T-21 (planned) | n/a: no skill exercises this | [reconciliation-algorithms-accounting](../brain/resources/reconciliation-algorithms-accounting.md) | Designed |
| FR-6.1 | GET /health | architecture §4 | _pending_ | _pending_ | `HealthSystemTest.healthReportsReachableDatabase`, `healthReportsDegradedWhenProbeFails`; `HealthServiceTest` | `local-check` | [rest-api-design-financial-services](../brain/resources/rest-api-design-financial-services.md) | Implemented |
| FR-6.2 | Atomic migrations, reject newer schema | data-model §6 | _pending_ | _pending_ | `SchemaMigratorTest.createsSchemaVersionCurrenciesAndOneSystemAccountEach`, `repeatedStartupChangesNothing`, `rejectsNewerSchemaVersion` | `local-check` | [acid-transactions-in-relational-databases](../brain/resources/acid-transactions-in-relational-databases.md) | Implemented |
| FR-6.3 | Graceful shutdown | uml §2.9 | _pending_ | _pending_ | `local-check` stop step; T-22 under load | `local-check` | [acid-transactions-in-relational-databases](../brain/resources/acid-transactions-in-relational-databases.md) | Partial |
| FR-6.4 | Persisted currency metadata | data-model §6 | _pending_ | _pending_ | `SchemaMigratorTest.addsNewlyConfiguredCurrencyWithSystemAccount`, `rejectsRemovedCurrencyOrChangedExponentAtomically` | n/a: no skill exercises this | [integer-currency-minor-units](../brain/resources/integer-currency-minor-units.md) | Implemented |
| FR-7.1 | Login | ADR-0008, uml §2.7 | _pending_ | _pending_ | T-17 (planned) | `simulate-transfer` (Stage 3) | [authentication-sessions](../brain/areas/authentication-sessions.md), [argon2id-password-hashing-and-session-tokens](../brain/resources/argon2id-password-hashing-and-session-tokens.md) | Designed |
| FR-7.2 | Session lifetime, multi-device | ADR-0008 | _pending_ | _pending_ | `ConfigLoaderTest.defaultsAreSafeAndDocumented`; T-15 | n/a: no skill exercises this | [authentication-sessions](../brain/areas/authentication-sessions.md) | Partial: config tested |
| FR-7.3 | Logout and logout-all | ADR-0008 | _pending_ | _pending_ | `SchemaMigratorTest.sessionsCanOnlyBeRevokedOnce`; T-17 | n/a: no skill exercises this | [authentication-sessions](../brain/areas/authentication-sessions.md) | Partial: DB rule tested |
| FR-7.4 | Admin provisioning, server-side ownership | ADR-0008 | _pending_ | _pending_ | `ConfigLoaderTest.rejectsInvalidAdminKeysWithoutEchoingThem`; T-16, T-18 | `simulate-transfer` (Stage 3) | [authentication-sessions](../brain/areas/authentication-sessions.md), [security-validation](../brain/areas/security-validation.md) | Partial |
| FR-8.x | Optional interest | n/a | n/a: optional, not selected | n/a: optional, not selected | n/a: extension not selected | n/a | [integer-currency-minor-units](../brain/resources/integer-currency-minor-units.md) | Not selected (optional, SRS §5.8) |
| FR-9.x | Optional currency exchange | n/a | n/a: optional, not selected | n/a: optional, not selected | n/a: extension not selected | n/a | [integer-currency-minor-units](../brain/resources/integer-currency-minor-units.md) | Not selected (optional, SRS §5.8) |

## Non-functional requirements (NFR)

| ID | Summary | Design | Issue | PR | Test / inspection | Skill | Brain note | Status |
|----|---------|--------|-------|----|-------------------|-------|------------|--------|
| NFR-API-1 | UTF-8 JSON, 415 | ADR-0009 | _pending_ | _pending_ | T-04 (planned) | n/a: no skill exercises this | [rest-api-design-financial-services](../brain/resources/rest-api-design-financial-services.md) | Designed |
| NFR-API-2 | Lowercase UUID v4 | ADR-0006 | _pending_ | _pending_ | T-01 (planned) | n/a: no skill exercises this | [rest-api-design-financial-services](../brain/resources/rest-api-design-financial-services.md) | Designed |
| NFR-API-3 | Integer amount tokens | ADR-0006 | _pending_ | _pending_ | T-04, T-26 (planned) | n/a: no skill exercises this | [integer-currency-minor-units](../brain/resources/integer-currency-minor-units.md) | Designed |
| NFR-API-4 | RFC 3339 ms timestamps | architecture §7 | _pending_ | _pending_ | `TimestampsTest` | n/a: no skill exercises this | [rest-api-design-financial-services](../brain/resources/rest-api-design-financial-services.md) | Implemented |
| NFR-API-5 | Public/user/admin routes | ADR-0008 | _pending_ | _pending_ | T-16 (planned) | n/a: no skill exercises this | [authentication-sessions](../brain/areas/authentication-sessions.md) | Designed |
| NFR-API-6 | X-Request-Id | ADR-0009 | _pending_ | _pending_ | `HttpSupportTest.requestIdEchoesOneValidValue`, `requestIdReplacesMissingDuplicateOrInvalidValues`; `HealthSystemTest.validRequestIdIsEchoedAndInvalidOneReplaced` | `local-check` | [rest-api-design-financial-services](../brain/resources/rest-api-design-financial-services.md) | Implemented |
| NFR-API-7 | 413 and VALIDATION_ERROR rules | ADR-0009 | _pending_ | _pending_ | T-04 (planned) | n/a: no skill exercises this | [security-validation](../brain/areas/security-validation.md) | Designed |
| NFR-API-8 | /api/v1 base path | openapi | _pending_ | _pending_ | `HealthSystemTest` | `local-check` | [rest-api-design-financial-services](../brain/resources/rest-api-design-financial-services.md) | Implemented |
| NFR-PERF-1 | p95 latency | architecture §9 | _pending_ | _pending_ | T-31 (planned) | n/a: no skill exercises this | [concurrency-acid](../brain/areas/concurrency-acid.md) | Pending (Stage 4) |
| NFR-PERF-2 | 100 transfers/s | architecture §9 | _pending_ | _pending_ | T-31 (planned) | n/a: no skill exercises this | [concurrency-acid](../brain/areas/concurrency-acid.md) | Pending (Stage 4) |
| NFR-PERF-3 | Reconciliation 100k entries < 10 s | ADR-0010 | _pending_ | _pending_ | T-31 (planned) | n/a: no skill exercises this | [reconciliation-algorithms-accounting](../brain/resources/reconciliation-algorithms-accounting.md) | Pending (Stage 4) |
| NFR-PERF-4 | Capacity dataset | data-model §4 | _pending_ | _pending_ | T-31 (planned) | n/a: no skill exercises this | [concurrency-acid](../brain/areas/concurrency-acid.md) | Pending (Stage 4) |
| NFR-PERF-5 | Startup < 5 s | architecture §4.1 | _pending_ | _pending_ | T-31 (planned) | n/a: no skill exercises this | [concurrency-acid](../brain/areas/concurrency-acid.md) | Pending (Stage 4) |
| NFR-REL-1 | Durability | ADR-0003 (synchronous=FULL) | _pending_ | _pending_ | `SqliteDatabaseTest.everyConnectionUsesTheRequiredPragmas`; T-24 | n/a: no skill exercises this | [sqlite-wal-locking-mechanics](../brain/resources/sqlite-wal-locking-mechanics.md), [acid-transactions-in-relational-databases](../brain/resources/acid-transactions-in-relational-databases.md) | Partial: configuration inspected |
| NFR-REL-2 | All-or-nothing after recovery | ADR-0003 | _pending_ | _pending_ | T-23, T-24 (planned) | n/a: no skill exercises this | [acid-transactions-in-relational-databases](../brain/resources/acid-transactions-in-relational-databases.md) | Designed |
| NFR-REL-3 | Overflow detection | ADR-0006 | _pending_ | _pending_ | T-01, T-26 (planned) | n/a: no skill exercises this | [integer-currency-minor-units](../brain/resources/integer-currency-minor-units.md) | Designed |
| NFR-REL-4 | At most one commit per key | ADR-0005 | _pending_ | _pending_ | T-25 (planned) | `simulate-transfer` (Stage 3) | [idempotency-design](../brain/areas/idempotency-design.md) | Designed |
| NFR-SEC-1 | Validate all inputs | ADR-0009 | _pending_ | _pending_ | T-04 (planned) | n/a: no skill exercises this | [security-validation](../brain/areas/security-validation.md) | Designed |
| NFR-SEC-2 | Invalid amounts → INVALID_AMOUNT | ADR-0006 | _pending_ | _pending_ | T-04 (planned) | n/a: no skill exercises this | [integer-currency-minor-units](../brain/resources/integer-currency-minor-units.md), [security-validation](../brain/areas/security-validation.md) | Designed |
| NFR-SEC-3 | Bound SQL parameters | ADR-0007 | _pending_ | _pending_ | Inspection of `SchemaMigrator`; T-05 review | n/a: no skill exercises this | [security-validation](../brain/areas/security-validation.md) | Partial |
| NFR-SEC-4 | Argon2id, hashed tokens/keys, secret scan | ADR-0008, ADR-0011 | _pending_ | _pending_ | `ConfigLoaderTest.rejectsWeakPasswordHashSettings`; T-15, T-29 | n/a: no skill exercises this | [argon2id-password-hashing-and-session-tokens](../brain/resources/argon2id-password-hashing-and-session-tokens.md) | Partial |
| NFR-SEC-5 | Log redaction and injection safety | ADR-0012 | _pending_ | _pending_ | `HealthSystemTest.requestLogRecordsFieldsErrorCodeAndSanitizedPath`, `HttpSupportTest.logSanitizerNeutralisesControlCharactersAndLength` | n/a: no skill exercises this | [security-validation](../brain/areas/security-validation.md) | Implemented |
| NFR-SEC-6 | Default 127.0.0.1 | architecture §6 | _pending_ | _pending_ | `ConfigLoaderTest.defaultsAreSafeAndDocumented` | `local-check` | [security-validation](../brain/areas/security-validation.md) | Implemented |
| NFR-SEC-7 | Ownership rules | ADR-0008 | _pending_ | _pending_ | T-18 (planned) | n/a: no skill exercises this | [authentication-sessions](../brain/areas/authentication-sessions.md) | Designed |
| NFR-SEC-8 | TLS for non-local access | architecture §2 | _pending_ | _pending_ | Deployment documentation (T-32) | n/a: no skill exercises this | [security-validation](../brain/areas/security-validation.md) | Pending (documentation) |
| NFR-SEC-9 | Login/registration throttling | ADR-0008 | _pending_ | _pending_ | T-19 (planned) | n/a: no skill exercises this | [authentication-sessions](../brain/areas/authentication-sessions.md) | Designed |
| NFR-DOC-1 | OpenAPI in docs/api | docs/api/openapi.yaml | _pending_ | _pending_ | Validated with `openapi-spec-validator`; T-33 conformance | n/a: no skill exercises this | [rest-api-design-financial-services](../brain/resources/rest-api-design-financial-services.md) | Partial: contract written |
| NFR-DOC-2 | User and developer docs | architecture, data-model | _pending_ | _pending_ | T-32 (planned) | n/a: no skill exercises this | [hexagonal-architecture-provider-ports](../brain/resources/hexagonal-architecture-provider-ports.md) | Partial |
| NFR-DOC-3 | Field/rule error details | ADR-0009 | _pending_ | _pending_ | `ErrorCodeTest.ledgerExceptionCarriesCodeAndImmutableDetails`, `HttpSupportTest.errorEnvelopeHasCodeMessageAndOptionalDetails` | n/a: no skill exercises this | [rest-api-design-financial-services](../brain/resources/rest-api-design-financial-services.md) | Partial |
| NFR-MNT-1 | Domain independent of HTTP/JDBC | ADR-0002 | _pending_ | _pending_ | T-27 architecture test (planned) | n/a: no skill exercises this | [hexagonal-architecture-provider-ports](../brain/resources/hexagonal-architecture-provider-ports.md) | Designed |
| NFR-MNT-2 | ≥ 85% coverage, documented scope | ADR-0011 | _pending_ | _pending_ | JaCoCo `check` in `./mvnw verify` | `local-check` | [hexagonal-architecture-provider-ports](../brain/resources/hexagonal-architecture-provider-ports.md) | Implemented |
| NFR-MNT-3 | Formatting and static checks | ADR-0011 | _pending_ | _pending_ | Spotless check, `-Xlint:all -Werror` in `./mvnw verify` | `local-check` | [hexagonal-architecture-provider-ports](../brain/resources/hexagonal-architecture-provider-ports.md) | Implemented |
| NFR-POR-1 | Linux + Windows/macOS | ADR-0011 | _pending_ | _pending_ | CI matrix ubuntu + windows | n/a: no skill exercises this | [hexagonal-architecture-provider-ports](../brain/resources/hexagonal-architecture-provider-ports.md) | Partial: CI defined, awaiting first run |
| NFR-OBS-1 | Request and transaction log records | ADR-0012 | _pending_ | _pending_ | `HealthSystemTest.requestLogRecordsFieldsErrorCodeAndSanitizedPath`; T-14 | n/a: no skill exercises this | [audit-trail-immutability-patterns](../brain/resources/audit-trail-immutability-patterns.md) | Partial: request log implemented |
| NFR-OBS-2 | Error code in log; replays not logged as commits | ADR-0012 | _pending_ | _pending_ | `HealthSystemTest.requestLogRecordsFieldsErrorCodeAndSanitizedPath`; T-14 | n/a: no skill exercises this | [audit-trail-immutability-patterns](../brain/resources/audit-trail-immutability-patterns.md) | Partial |

## Skill requirements (SK)

| ID | Summary | Design | Issue | PR | Test / inspection | Skill | Brain note | Status |
|----|---------|--------|-------|----|-------------------|-------|------------|--------|
| SK-1 | Agent Skills format, brain citation, CI | skills/* | _pending_ | _pending_ | `scripts/validate_skills.py` in CI | all | [agentskills-specification-standards](../brain/resources/agentskills-specification-standards.md) | Implemented |
| SK-2 | local-check | skills/local-check | _pending_ | _pending_ | `docs/skill_evaluations.md` Stage 2 | `local-check` | [agentskills-specification-standards](../brain/resources/agentskills-specification-standards.md) | Implemented (v1) |
| SK-3 | simulate-transfer | plan T-28 | _pending_ | _pending_ | Stage 3 evaluation | `simulate-transfer` (Stage 3) | [concurrency-race-conditions-wallets](../brain/resources/concurrency-race-conditions-wallets.md), [reconciliation-algorithms-accounting](../brain/resources/reconciliation-algorithms-accounting.md) | Not yet due (Stage 3) |
| SK-4 | brain-ingest | skills/brain-ingest | _pending_ | _pending_ | `docs/skill_evaluations.md` | `brain-ingest` | [agentskills-specification-standards](../brain/resources/agentskills-specification-standards.md) | Implemented |
| SK-5 | brain-refine | skills/brain-refine | _pending_ | _pending_ | `docs/skill_evaluations.md` Stage 2 | `brain-refine` | [agentskills-specification-standards](../brain/resources/agentskills-specification-standards.md) | Implemented |
| SK-6 | brain-query | skills/brain-query | _pending_ | _pending_ | `docs/skill_evaluations.md` Stage 2 | `brain-query` | [agentskills-specification-standards](../brain/resources/agentskills-specification-standards.md) | Implemented |
| SK-7 | Evaluations before each stage | docs/skill_evaluations.md | _pending_ | _pending_ | Review of evaluation record | all | [agentskills-specification-standards](../brain/resources/agentskills-specification-standards.md) | Implemented for Stage 2 |
| SK-8 | No credentials in skills | skills/* | _pending_ | _pending_ | Secret patterns in `scripts/validate_skills.py` | all | [agentskills-specification-standards](../brain/resources/agentskills-specification-standards.md), [security-validation](../brain/areas/security-validation.md) | Implemented |

## Design constraints (DC)

| ID | Summary | Design | Issue | PR | Test / inspection | Skill | Brain note | Status |
|----|---------|--------|-------|----|-------------------|-------|------------|--------|
| DC-1 | Java 21, Maven wrapper, pinned versions | ADR-0001 | _pending_ | _pending_ | Enforcer rules in `./mvnw verify` | `local-check` | [hexagonal-architecture-provider-ports](../brain/resources/hexagonal-architecture-provider-ports.md) | Implemented |
| DC-2 | SQLite only; file-backed WAL tests | ADR-0003 | _pending_ | _pending_ | `SqliteDatabaseTest` uses temp files | n/a: no skill exercises this | [sqlite-wal-locking-mechanics](../brain/resources/sqlite-wal-locking-mechanics.md) | Implemented |
| DC-3 | REST/JSON over HTTP/1.1 | ADR-0009, openapi | _pending_ | _pending_ | `HealthSystemTest` | `local-check` | [rest-api-design-financial-services](../brain/resources/rest-api-design-financial-services.md) | Implemented |
| DC-4 | Provider interfaces | ADR-0002 | _pending_ | _pending_ | `HealthServiceTest` (fake probe), `FixedClock`, `SequentialIds` | n/a: no skill exercises this | [hexagonal-architecture-provider-ports](../brain/resources/hexagonal-architecture-provider-ports.md) | Partial |
| DC-5 | long / INTEGER money, checked arithmetic | ADR-0006 | _pending_ | _pending_ | Schema `typeof` checks (`SchemaMigratorTest.schemaConstraintsRejectInvalidRows`); T-01, T-26 | n/a: no skill exercises this | [integer-currency-minor-units](../brain/resources/integer-currency-minor-units.md) | Partial |
| DC-6 | SQLite locking only | ADR-0003 | _pending_ | _pending_ | `SqliteDatabaseTest.writeLockTimeoutBecomesStorageBusy` | n/a: no skill exercises this | [sqlite-wal-locking-mechanics](../brain/resources/sqlite-wal-locking-mechanics.md) | Implemented |
| DC-7 | One GitHub repository | CONTRIBUTING | _pending_ | _pending_ | Repository inspection | n/a: no skill exercises this | [stage-1-requirements](../brain/projects/stage-1-requirements.md) | Implemented |
| DC-8 | UML class/sequence diagrams | docs/design/uml.md | _pending_ | _pending_ | Review of this PR | n/a: no skill exercises this | [hexagonal-architecture-provider-ports](../brain/resources/hexagonal-architecture-provider-ports.md) | Implemented |

## Business rules (BR)

| ID | Summary | Design | Issue | PR | Test / inspection | Skill | Brain note | Status |
|----|---------|--------|-------|----|-------------------|-------|------------|--------|
| BR-1 | Integer minor units | ADR-0006 | _pending_ | _pending_ | `SchemaMigratorTest.schemaConstraintsRejectInvalidRows`; T-01 | n/a: no skill exercises this | [integer-currency-minor-units](../brain/resources/integer-currency-minor-units.md) | Partial |
| BR-2 | ISO 4217 codes, configured exponents | data-model | _pending_ | _pending_ | `ConfigLoaderTest.rejectsInvalidCurrencyLists`, `SchemaMigratorTest` | n/a: no skill exercises this | [integer-currency-minor-units](../brain/resources/integer-currency-minor-units.md) | Implemented |
| BR-3 | WALLET/SYSTEM, one SYSTEM per currency | ADR-0007 | _pending_ | _pending_ | `SchemaMigratorTest.createsSchemaVersionCurrenciesAndOneSystemAccountEach`, `schemaConstraintsRejectInvalidRows` | n/a: no skill exercises this | [double-entry-accounting-principles](../brain/resources/double-entry-accounting-principles.md) | Implemented (DB level) |
| BR-4 | Exactly two balanced entries | ADR-0007 | _pending_ | _pending_ | `SchemaMigratorTest.entriesMustMatchTheirTransaction`; T-06, T-20 | `simulate-transfer` (Stage 3) | [double-entry-accounting-principles](../brain/resources/double-entry-accounting-principles.md) | Partial |
| BR-5 | Credits increase, debits decrease | architecture §5 | _pending_ | _pending_ | T-06, T-20 (planned) | n/a: no skill exercises this | [double-entry-accounting-principles](../brain/resources/double-entry-accounting-principles.md) | Designed |
| BR-6 | Wallet ≥ 0, SYSTEM may go negative | ADR-0007 | _pending_ | _pending_ | `SchemaMigratorTest.cachedBalanceMayChangeWithinLimits` | `simulate-transfer` (Stage 3) | [double-entry-accounting-principles](../brain/resources/double-entry-accounting-principles.md) | Partial |
| BR-7 | Amount and balance limits | ADR-0006 | _pending_ | _pending_ | `SchemaMigratorTest.schemaConstraintsRejectInvalidRows`, `cachedBalanceMayChangeWithinLimits`; T-01 | n/a: no skill exercises this | [integer-currency-minor-units](../brain/resources/integer-currency-minor-units.md) | Partial |
| BR-8 | Append-only records | ADR-0007 | _pending_ | _pending_ | `SchemaMigratorTest.ledgerRecordsAreAppendOnly` | n/a: no skill exercises this | [audit-trail-immutability-patterns](../brain/resources/audit-trail-immutability-patterns.md) | Implemented (DB level) |
| BR-9 | Reversal mirrors original | uml §2.6 | _pending_ | _pending_ | T-12 (planned) | n/a: no skill exercises this | [audit-trail-immutability-patterns](../brain/resources/audit-trail-immutability-patterns.md) | Designed |
| BR-10 | Per-currency conservation | ADR-0010 | _pending_ | _pending_ | T-20 (planned) | `simulate-transfer` (Stage 3) | [reconciliation-algorithms-accounting](../brain/resources/reconciliation-algorithms-accounting.md) | Designed |
| BR-11 | Gap-free sequences and entry numbers | ADR-0007 | _pending_ | _pending_ | T-06, T-25 (planned) | n/a: no skill exercises this | [acid-transactions-in-relational-databases](../brain/resources/acid-transactions-in-relational-databases.md) | Designed |
| BR-12 | Exact arithmetic, string totals | ADR-0006, openapi | _pending_ | _pending_ | T-20, T-26 (planned) | n/a: no skill exercises this | [integer-currency-minor-units](../brain/resources/integer-currency-minor-units.md) | Designed |

## Process requirements (PC)

| ID | Summary | Design | Issue | PR | Test / inspection | Skill | Brain note | Status |
|----|---------|--------|-------|----|-------------------|-------|------------|--------|
| PC-1 | PARA vault, note schema | brain/index.md | _pending_ | _pending_ | `scripts/validate_brain.py` in CI | `brain-refine` | [stage-1-requirements](../brain/projects/stage-1-requirements.md) | Implemented |
| PC-2 | Stage review notes | brain/projects/stage-2-review.md | _pending_ | _pending_ | Review of note | n/a: no skill exercises this | [stage-2-review](../brain/projects/stage-2-review.md) | Implemented for Stage 2 |
| PC-3 | ADRs cite brain notes | docs/adr | _pending_ | _pending_ | `scripts/validate_adrs.py` | `brain-query` | [hexagonal-architecture-provider-ports](../brain/resources/hexagonal-architecture-provider-ports.md) | Implemented |
| PC-4 | AGENTS.md at root | AGENTS.md | _pending_ | _pending_ | Repository inspection | n/a: no skill exercises this | [stage-1-requirements](../brain/projects/stage-1-requirements.md) | Implemented |
| PC-5 | Brain changes via PRs, visible authorship | CONTRIBUTING | _pending_ | _pending_ | PR history | n/a: no skill exercises this | [stage-1-requirements](../brain/projects/stage-1-requirements.md) | Pending: per-PR evidence |
| PC-6 | SRS, design, plan, traceability | docs/ | _pending_ | _pending_ | This document set | n/a: no skill exercises this | [stage-2-design](../brain/projects/stage-2-design.md) | Implemented for Stage 2 |
| PC-7 | Issues, PR review, branching, tags, changelog | CONTRIBUTING, plan | _pending_ | _pending_ | GitHub issue/PR history | n/a: no skill exercises this | [stage-2-design](../brain/projects/stage-2-design.md) | Pending: issues to be opened from plan |
| PC-8 | CI on push/PR, isolated DB, all checks | ADR-0011, ci.yml | _pending_ | _pending_ | CI run history | `local-check` | [hexagonal-architecture-provider-ports](../brain/resources/hexagonal-architecture-provider-ports.md) | Partial: workflow defined |
| PC-9 | Human review of AI changes, session notes | CONTRIBUTING | _pending_ | _pending_ | PR reviews | n/a: no skill exercises this | [stage-2-design](../brain/projects/stage-2-design.md) | Pending: per-PR evidence |
| PC-10 | Regression test per defect | plan T-30 | _pending_ | _pending_ | Defect log (Stage 3) | n/a: no skill exercises this | [stage-2-design](../brain/projects/stage-2-design.md) | Not yet due (Stage 3) |

## Assumptions

| ID | Assumption | Established by |
|----|------------|----------------|
| A-1 | Untrusted devices; server-side ownership | ADR-0008; NFR-SEC-7 tests (T-18) |
| A-2 | One process per database file | ADR-0003 (in-process lock is sufficient only under A-2) |
| A-3 | Host honours fsync | ADR-0003 (`synchronous=FULL`); NFR-REL-1 reports kill tests separately |
| A-4 | Currencies fixed per process | FR-6.4 migrator checks (`SchemaMigratorTest`) |
| A-5 | Sequence defines order, not time | ADR-0004, data-model §5 |

## Summary

| Status | Rows |
|--------|------|
| Designed | 49 |
| Implemented | 30 |
| Partial | 28 |
| Pending | 9 |
| Implemented for Stage 2 | 3 |
| Not selected | 2 |
| Not yet due | 2 |
