# Architecture Decision Records

Each significant design decision is recorded here (PC-3). Every ADR cites at least one distilled note in `brain/` (SB-3); `python3 scripts/validate_adrs.py` enforces naming, required sections and citations in CI.

| ADR | Title | Status |
|-----|-------|--------|
| [0001](0001-java-javalin-maven-stack.md) | Java 21, Javalin and Maven stack | Accepted |
| [0002](0002-layered-hexagonal-architecture.md) | Layered architecture with provider ports | Accepted |
| [0003](0003-sqlite-write-serialization.md) | SQLite write serialization and read snapshots | Accepted |
| [0004](0004-canonical-json-and-hash-chain.md) | Canonical JSON and transaction hash chain | Accepted |
| [0005](0005-idempotency-records.md) | Idempotency records committed with the transaction | Accepted |
| [0006](0006-money-and-numeric-representation.md) | Money, numbers and exact JSON parsing | Accepted |
| [0007](0007-schema-constraints-and-append-only-triggers.md) | Schema constraints, append-only triggers and migrations | Accepted |
| [0008](0008-authentication-and-sessions.md) | Authentication, sessions and administrator keys | Accepted |
| [0009](0009-request-pipeline-and-error-handling.md) | Request pipeline, validation order and error envelope | Accepted |
| [0010](0010-reconciliation-single-snapshot.md) | Reconciliation in one read snapshot | Accepted |
| [0011](0011-build-quality-gates-and-ci.md) | Build quality gates and CI | Accepted |
| [0012](0012-structured-logging-and-observability.md) | Structured logging and observability | Accepted |

To add a decision: copy `0000-template.md` to the next number, fill every section, cite brain notes, and open a pull request. A changed decision gets a new ADR that supersedes the old one; accepted ADRs are not rewritten.
