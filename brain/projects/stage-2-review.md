---
title: "Stage 2 Review: Architecture and Design"
date: 2026-11-20
tags: [review, stage-2, design, sdlc]
status: completed
---

# Stage 2 Review: Architecture and Design

* **Date:** 2026-11-20
* **Stage Status:** **COMPLETE**, pending PR review
* **Tracker:** [[projects/stage-2-design]] · **Previous review:** [[projects/stage-1-review]]

---

## 1. What Worked

* **Following the submitted SRS.** Once `docs/srs.pdf` was confirmed as the authoritative specification, every design artefact used one set of requirement IDs (DC, BR, FR, NFR, SK, PC, TQ). The traceability matrix has one row per ID.
* **Building a skeleton early.** Writing the storage layer, schema and health endpoint during design caught problems that diagrams alone would have missed:
  * Request paths stay percent-encoded in the request log, so the log test now checks the encoded form.
  * JaCoCo instrumenting every library class made the forked test JVM time out on the Windows filesystem. It is now limited to project classes.
* **Constraints in the database.** `CHECK` constraints and `BEFORE UPDATE/DELETE` triggers enforce BR-3, BR-4, BR-7 and BR-8 even if the application has a bug. `SchemaMigratorTest` tests them directly.
* **Validators in CI.** `validate_adrs.py` enforces SB-3 (every ADR cites a brain note). `validate_brain.py` now also checks frontmatter and status per PARA folder.
* **A machine-checked contract.** `openapi.yaml` passes `openapi-spec-validator`. A script checks that its error-code enum matches `ErrorCode.java` (26 codes on each side).

## 2. What Did Not Work

* **Specification drift.** The repository's `docs/srs.md` (Python/FastAPI) had drifted from the SRS we submitted (Java 21/Maven). We lost time before noticing. `srs.md` is now marked superseded and points to `srs.pdf`.
* **Missing folders in git.** `brain/inbox/` and `brain/archive/` were empty and so not tracked, which made `validate_brain.py` fail on a fresh clone. Both now hold a `.gitkeep`.
* **Slow local builds on WSL.** A full `./mvnw verify` on `/mnt/c` takes about 95 seconds. CI runs on native runners.

## 3. What to Change in Stage 3

* Open one GitHub issue per `T-xx` task in `docs/plan.md` before starting it. Fill in the Issue and PR columns of the traceability matrix in the same PR.
* Extract the `LedgerStore` port (T-05) before writing any service, so the services never depend on `SqliteDatabase` directly.
* Start the concurrency and fault-injection tests (T-23 to T-25) as soon as transfers exist; do not leave them to the end of the stage.
* Write `simulate-transfer` (T-28) as soon as login and reconciliation exist, and evaluate it before the Stage 3 sign-off.
* Run a performance smoke test after T-10 to check that the single-writer design can reach NFR-PERF-2.

## 4. Traceability

* Decisions: ADR-0001 to ADR-0012 in `docs/adr/`.
* Evidence: `docs/traceability.md`, `docs/skill_evaluations.md` (Stage 2 section).
* Notes used: [[resources/hexagonal-architecture-provider-ports]], [[resources/canonical-json-and-hash-chains]], [[resources/argon2id-password-hashing-and-session-tokens]], [[areas/authentication-sessions]], [[resources/sqlite-wal-locking-mechanics]].
