---
title: "SQLite WAL Mode and Transaction Locking Mechanics"
date: 2026-11-09
tags: [sqlite, wal, concurrency, database-locking]
source: "SQLite Documentation: https://www.sqlite.org/wal.html, https://www.sqlite.org/lockingv3.html"
status: resource
---

# SQLite WAL Mode and Transaction Locking Mechanics

## 1. Summary & Core Concept
SQLite is an embedded, serverless ACID relational database. In its default rollback-journal mode, reading and writing mutually block each other. Enabling Write-Ahead Logging (`PRAGMA journal_mode=WAL;`) decouples readers from writers:
* Readers do not block writers.
* Writers do not block readers.
* However, **SQLite supports strictly one concurrent writer at any given instant**.

## 2. Key Architectural Mechanisms
* **Transaction Types:**
  * `BEGIN DEFERRED`: Lock is acquired only when the first write operation occurs. Highly susceptible to `SQLITE_BUSY` when two concurrent transactions read then attempt to write.
  * `BEGIN IMMEDIATE`: Acquires the reserved write lock immediately at the start of the transaction. If another writer holds the lock, it immediately halts or waits up to `PRAGMA busy_timeout`. This prevents deadlocks between concurrent transfers.
  * `BEGIN EXCLUSIVE`: Blocks all readers and writers immediately.
* **Busy Timeout Configuration:**
  ```sql
  PRAGMA journal_mode=WAL;
  PRAGMA synchronous=NORMAL;
  PRAGMA busy_timeout=5000; -- Wait up to 5 seconds before returning SQLITE_BUSY
  ```

## 3. Pitfalls & Failure Modes
* **`SQLITE_BUSY` Crashes:** Under concurrent load testing (e.g., 20 parallel transfers), unhandled busy exceptions cause HTTP 500 errors unless connection timeouts and retry backoffs are implemented.
* **Shared In-Memory Connection Pitfall:** `sqlite3.connect(":memory:")` creates a distinct, private database per connection. For concurrent testing, use a shared in-memory URI (`file:memdb1?mode=memory&cache=shared`) or a temp disk file.

## 4. Relevance to Financial Ledger API
* Satisfies **FR-3 (Concurrent Balance Mutability)**.
* Validates **TQ-2 (Integration Tests & Rollbacks)** and **TQ-4 (Concurrency Safety)**.

## 5. Wikilinks
* [[index]]
* [[resources/concurrency-race-conditions-wallets|Concurrency Race Conditions in Wallet Balances]]
* [[resources/acid-transactions-in-relational-databases|ACID Transactions in Relational Databases]]
