# ADR-0010: Reconciliation in One Read Snapshot

**Status:** Accepted
**Date:** 2026-11-20
**Requirements:** FR-5.1 to FR-5.6, BR-10, BR-12, NFR-PERF-3, SRS §2.7

## Context

Reconciliation must run checks C1–C7 against one consistent snapshot while transfers continue (FR-5.2), change nothing (FR-5.1), report discrepancies instead of throwing (FR-5.5), and finish within 10 seconds on 100,000 entries (NFR-PERF-3) ([[resources/reconciliation-algorithms-accounting]], [[resources/double-entry-accounting-principles]]).

## Decision

* `ReconciliationService` runs entirely inside `SqliteDatabase.read(...)`: one read-only connection, one deferred transaction, so every query sees the same WAL snapshot and the writer is never blocked.
* The service streams tables in key order once each and computes checks in Java with `BigInteger`/checked `long` arithmetic:

| Check | Method |
|-------|--------|
| C1 structure | group entries by transaction: exactly one DEBIT and one CREDIT, amounts equal the transaction amount within BR-7, currencies match, accounts exist, differ and match source/destination |
| C2 balanced totals | per-currency and overall sum of credits equals sum of debits |
| C3 stored balances | per-account credits − debits equals `account.balance` (system accounts included) |
| C4 bounds | wallet balances in `[0, 2^53−1]`; all balances in signed 64-bit |
| C5 conservation | per currency: Σ wallet balances + system balance = 0; exactly one SYSTEM account per configured currency |
| C6 sequence and hash | sequences 1..n and entry numbers 1..m without gaps or duplicates; recompute each hash (ADR-0004) and the `previousHash` link |
| C7 reversals | each reversal's original exists, is not a reversal, has mirrored entries, and has no second reversal |

* Each discrepancy is `{"check","subjectId","expected","actual"}` with diagnostic strings and no secrets. `status` is `INCONSISTENT` if any exist, else `CONSISTENT`; both return 200. Totals and counts are decimal strings; an empty ledger reports `snapshotSequence` `"0"`.
* Corrupted rows (wrong type, NULL where unexpected) are captured as discrepancies, not exceptions. Only failure to run the queries returns `500 INTERNAL_ERROR`.

## Alternatives Considered

| Option | Why not chosen |
|--------|----------------|
| Checks as SQL aggregates only | Cannot recompute SHA-256 chains; mixing SQL and Java checks in one snapshot is still needed. |
| Taking the write lock | Simplest consistency, but blocks transfers for the duration; WAL snapshots make it unnecessary. |
| Auto-repair of balances | Explicitly forbidden (FR-5.3). |

## Consequences

* One pass over 100,000 entries with SHA-256 per transaction is well under a second on reference hardware; NFR-PERF-3 will be measured three times in Stage 3.
* Tests: empty, clean, concurrently written (reconcile during a transfer storm) and corrupted databases with triggers dropped on disposable copies, one test per check.

## Brain Notes

* [[resources/reconciliation-algorithms-accounting]]
* [[resources/double-entry-accounting-principles]]
* [[resources/sqlite-wal-locking-mechanics]]
