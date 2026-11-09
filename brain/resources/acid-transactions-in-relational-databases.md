---
title: "ACID Transaction Guarantees in Embedded Relational Databases"
date: 2026-11-09
tags: [acid, transactions, sqlite, atomicity]
source: "Jim Gray, Transaction Processing: Concepts and Techniques; SQLite Transaction Architecture"
status: resource
---

# ACID Transaction Guarantees in Embedded Relational Databases

## 1. Summary & Core Concept
In a financial ledger, partial execution is disastrous. If an application debits Alice's wallet but encounters an exception or crash before crediting Bob's wallet, Alice's funds vanish. ACID guarantees ensure total system safety:
* **Atomicity:** All operations in a transaction succeed, or the entire transaction is rolled back with zero side effects.
* **Consistency:** The database transitions from one valid state to another, enforcing constraints (`CHECK(amount > 0)`, balance $\ge 0$, foreign keys).
* **Isolation:** Uncommitted transactions are invisible to other connections.
* **Durability:** Committed transactions survive process terminations, power outages, and system crashes.

## 2. Key Architectural Mechanisms
* **Explicit Transaction Boundaries:**
  * In Python with SQLite, avoid default autocommit mode. Use explicit context managers:
  ```python
  connection.execute("BEGIN IMMEDIATE")
  try:
      # 1. Verify balances
      # 2. Insert debit entry
      # 3. Insert credit entry
      # 4. Update cached wallet balances
      connection.commit()
  except Exception:
      connection.rollback()
      raise
  ```
* **Foreign Key Enforcement:** SQLite has foreign keys disabled by default. Must run `PRAGMA foreign_keys = ON;` on every new connection.

## 3. Pitfalls & Failure Modes
* **Partial Commits:** Calling `commit()` between writing the debit and writing the credit.
* **Silent Rollback Suppression:** Swallowing exceptions without calling `rollback()`, leaving connection locks dangling.

## 4. Relevance to Financial Ledger API
* Satisfies **FR-1 (Wallet Initialization)** and **FR-3 (Concurrent Balance Mutability)**.
* Evaluated by **TQ-2 (Database Rollback Tests)**.

## 5. Wikilinks
* [[index]]
* [[resources/sqlite-wal-locking-mechanics|SQLite WAL and Transaction Locking Mechanics]]
* [[resources/double-entry-accounting-principles|Double-Entry Accounting Principles]]
