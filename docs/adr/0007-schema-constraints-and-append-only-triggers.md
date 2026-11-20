# ADR-0007: Schema Constraints, Append-Only Triggers and Migrations

**Status:** Accepted
**Date:** 2026-11-20
**Requirements:** BR-2, BR-3, BR-4, BR-8, BR-11, FR-4.1, FR-6.2, FR-6.4, SRS §9, §2.7

## Context

SRS §9 requires the database itself to enforce uniqueness, references, positive amounts, ranges and append-only behaviour in addition to service checks. FR-4.1 requires triggers that abort `UPDATE` and `DELETE` on transactions, entries and idempotency records. FR-6.2 requires atomic migrations, idempotent startup and rejection of newer schemas; FR-6.4 requires persisted currency metadata that cannot be removed or re-exponented ([[resources/audit-trail-immutability-patterns]], [[resources/double-entry-accounting-principles]]).

## Decision

The physical model is `src/main/resources/db/V1__initial_schema.sql`, documented in `docs/design/data-model.md`. Key choices:

* **Composite foreign keys** `(account_id, currency) → account(account_id, currency)` on wallets, transactions and entries make a currency mismatch impossible at the storage level.
* **One SYSTEM account per currency** via the partial unique index `ux_account_one_system_per_currency`.
* **A wallet is its account:** `wallet.wallet_id` references `account.account_id`; a trigger requires that account's kind to be `WALLET`.
* **Entries:** `UNIQUE(transaction_id, direction)` allows at most one DEBIT and one CREDIT per transaction; a `BEFORE INSERT` trigger requires the entry's amount, currency and account (source for DEBIT, destination for CREDIT) to match its transaction. The "exactly two" rule is a cross-row invariant SQLite cannot defer, so the service inserts both entries in the same transaction and reconciliation C1 verifies it.
* **Transactions:** `sequence UNIQUE > 0`, `reversal_of UNIQUE` (at most one reversal per original), `CHECK` constraints tying `type` to `reversal_of`, `reference` and `reason`.
* **Append-only triggers** (`RAISE(ABORT, ...)`) on `ledger_transaction`, `ledger_entry`, `idempotency_record`; immutability triggers on `currency`, `app_user`, `wallet` and the identity columns of `account`. Only `account.balance` and `user_session.revoked_at` (once) may change.
* **Migrations:** `schema_info` holds one row with `schema_version`. `SchemaMigrator` runs every pending script and the currency synchronization in one write transaction. A newer version, a removed currency or a changed exponent raises `IncompatibleDatabaseException`; `Main` exits with code 2 without touching the file. New configured currencies are inserted with a zero-balance SYSTEM account.

## Alternatives Considered

| Option | Why not chosen |
|--------|----------------|
| Service-only enforcement | A bug or manual SQL could corrupt history silently; §9 forbids relying on it. |
| Flyway / Liquibase | Adds a dependency and its own metadata table for one migration; our migrator is ~150 lines and tested. |
| Storing balances only as ledger sums (no cached balance) | Every transfer would aggregate history; the cached balance is verified by C3 instead. |

## Consequences

* Ordinary SQL cannot rewrite history; a privileged operator can still drop triggers (§2.7), which reconciliation is designed to expose.
* Tests that corrupt data (FR-5.6) drop triggers on a disposable copy only; no production bypass exists.
* `SchemaMigratorTest` covers fresh creation, idempotent restart, added currency, rejected removal/exponent change, newer schema, every trigger, balance limits and the entry-match rule.

## Brain Notes

* [[resources/audit-trail-immutability-patterns]]
* [[resources/double-entry-accounting-principles]]
* [[resources/acid-transactions-in-relational-databases]]
* [[areas/ledger-architecture]]
