---
title: "Stage 2: Architecture and Design"
date: 2026-11-20
tags: [project, stage-2, design, architecture]
status: completed
---

# Stage 2: Architecture and Design

Tracks the Stage 2 deliverables from SRS §11.2. Review: [[projects/stage-2-review|Stage 2 Review]]. Previous stage: [[projects/stage-1-requirements|Stage 1]].

## Deliverables

| Deliverable | Location | Status |
|-------------|----------|--------|
| Architecture document | `docs/design/architecture.md` | Done |
| Data model and ER diagram | `docs/design/data-model.md` | Done |
| UML class and sequence diagrams (DC-8) | `docs/design/uml.md` | Done |
| OpenAPI 3 contract (NFR-DOC-1) | `docs/api/openapi.yaml` | Done |
| ADR-0001 to ADR-0012, each citing notes (SB-3) | `docs/adr/` | Done |
| Development plan and task breakdown | `docs/plan.md` | Done |
| Traceability matrix | `docs/traceability.md` | Done |
| `brain-refine`, `brain-query` skills (SK-5, SK-6) | `skills/` | Done |
| First `local-check` (SK-2) | `skills/local-check`, `scripts/local_check.py` | Done |
| Contribution and release process (PC-7) | `CONTRIBUTING.md`, `CHANGELOG.md` | Done |
| Review note and skill evaluations | [[projects/stage-2-review]], `docs/skill_evaluations.md` | Done |

## Key decisions and the notes behind them

* Java 21, Javalin, Maven wrapper (ADR-0001): [[resources/hexagonal-architecture-provider-ports]].
* Ports and adapters with fake providers (ADR-0002, DC-4): [[resources/hexagonal-architecture-provider-ports]].
* One serialized writer, `BEGIN IMMEDIATE`, read snapshots, `STORAGE_BUSY` on lock timeout (ADR-0003): [[resources/sqlite-wal-locking-mechanics]], [[areas/concurrency-acid]], [[resources/concurrency-race-conditions-wallets]].
* Canonical JSON for fingerprints and hashes (ADR-0004): [[resources/canonical-json-and-hash-chains]].
* Idempotency record committed with the transaction (ADR-0005): [[areas/idempotency-design]], [[resources/ietf-idempotency-http-standard]].
* Integer minor units and string totals (ADR-0006): [[resources/integer-currency-minor-units]].
* Schema constraints and append-only triggers (ADR-0007): [[resources/audit-trail-immutability-patterns]], [[resources/double-entry-accounting-principles]].
* Argon2id passwords and hashed session tokens (ADR-0008): [[resources/argon2id-password-hashing-and-session-tokens]], [[areas/authentication-sessions]].
* Reconciliation on one snapshot (ADR-0010): [[resources/reconciliation-algorithms-accounting]].

## Foundation code

To check that the design works on real code, the Stage 2 work includes a small skeleton, with no feature endpoints: configuration, storage, schema V1, the error catalogue, request ids, the request log and `GET /api/v1/health`. It has 48 tests and passes `./mvnw verify` with the coverage gate. The ledger features are Stage 3 work (`docs/plan.md` T-01 to T-30).

## Requirements touched

DC-1 to DC-8, FR-3.4, FR-3.5, FR-4.1, FR-6.1, FR-6.2, FR-6.4, NFR-API-4, NFR-API-6, NFR-SEC-5, NFR-SEC-6, NFR-MNT-2, NFR-MNT-3, SK-2, SK-5, SK-6, PC-3, PC-6, PC-7. The full mapping is in `docs/traceability.md`.
