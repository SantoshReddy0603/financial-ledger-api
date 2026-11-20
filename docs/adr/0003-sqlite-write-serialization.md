# ADR-0003: SQLite Write Serialization and Read Snapshots

**Status:** Accepted
**Date:** 2026-11-20
**Requirements:** FR-3.1 to FR-3.8, DC-2, DC-6, A-2, NFR-REL-1, NFR-REL-2, NFR-PERF-1, NFR-PERF-2

## Context

SQLite allows one writer at a time. A deferred transaction that reads a balance and later upgrades to a write lock can fail with `SQLITE_BUSY` after doing work, or, in a naive read-modify-write, lose an update ([[resources/sqlite-wal-locking-mechanics]], [[resources/concurrency-race-conditions-wallets]]). FR-3.1 requires every money-moving operation to run key lookup, balance reads, checks, sequence allocation, inserts, balance updates, hash extension and key recording inside one `BEGIN IMMEDIATE` transaction. FR-3.4 requires WAL, `synchronous=FULL`, foreign keys and a 5000 ms busy timeout on every connection, and reads that do not take the application write lock. FR-3.5 requires a lock timeout to become `503 STORAGE_BUSY` with `Retry-After: 1` and no partial effects. A-2 guarantees one process per database file.

## Decision

`SqliteDatabase` owns all connections:

1. **One writer connection**, opened with `journal_mode=WAL`, `synchronous=FULL`, `foreign_keys=ON`, `busy_timeout=5000` and sqlite-jdbc `TransactionMode.IMMEDIATE`, so `setAutoCommit(false)` begins with `BEGIN IMMEDIATE`.
2. **A fair `ReentrantLock`** guards the writer. `write(work)` calls `tryLock(lockTimeout)` (default 5000 ms, `LEDGER_LOCK_TIMEOUT_MS`). Fairness gives FIFO ordering, so committed results follow lock-acquisition order (FR-3.2).
3. **Inside the lock**, the work runs in one transaction; success commits, any exception rolls back. Sequence and entry numbers are read as `MAX()+1` inside that transaction, so a rollback consumes nothing and numbers stay gap-free (BR-11, FR-3.6).
4. **Lock timeout or `SQLITE_BUSY*`/`SQLITE_LOCKED`** becomes `StorageBusyException`, mapped by the HTTP layer to `503 STORAGE_BUSY` with `Retry-After: 1`. Nothing was written, so the client retries with the same idempotency key.
5. **A pool of read-only connections** (default 8, `LEDGER_READ_POOL_SIZE`), each running `read(work)` inside one deferred transaction. In WAL mode the first read fixes a snapshot, so multi-row responses (user with wallets, reconciliation) never see half a transaction (FR-3.7, FR-5.2). Readers never touch the write lock.
6. **Shutdown** sets a closed flag, waits for the write lock (letting an in-progress write commit or roll back) and closes every connection (FR-6.3).

## Alternatives Considered

| Option | Why not chosen |
|--------|----------------|
| Rely on `busy_timeout` alone with many writer connections | Writers retry inside SQLite with back-off; ordering is unfair and `SQLITE_BUSY_SNAPSHOT` can still surface mid-transaction. |
| `BEGIN DEFERRED` plus optimistic retry | Retrying the whole use case is complex and weakens the "lock order equals commit order" argument. |
| `SELECT ... FOR UPDATE` style row locks | SQLite has no row locks. |
| Per-wallet in-memory locks | Transfers touch two wallets plus the global sequence and hash head, so the whole ledger is one serialization point anyway; extra locks add deadlock risk. |
| `synchronous=NORMAL` | Faster, but in WAL mode the latest commits can be lost on power failure after 201 was returned (NFR-REL-1). |

## Consequences

* Write throughput is bounded by one writer. A transfer is a handful of indexed statements plus an fsync; 100 transfers/second (NFR-PERF-2) is within reach on an SSD and will be measured in Stage 3.
* The in-process lock is valid only because A-2 forbids a second process; a second process would still be safe (SQLite's own lock and the busy timeout apply) but could see 503s.
* Verified by `SqliteDatabaseTest` (pragmas on writer and readers, commit, rollback, read-only readers, lock timeout, snapshot isolation) and, in Stage 3, contended-transfer and fault-injection tests.

## Brain Notes

* [[resources/sqlite-wal-locking-mechanics]]
* [[resources/concurrency-race-conditions-wallets]]
* [[resources/acid-transactions-in-relational-databases]]
* [[areas/concurrency-acid]]
