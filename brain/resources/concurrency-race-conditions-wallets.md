---
title: "Concurrency Race Conditions in Wallet Balances"
date: 2026-11-09
tags: [concurrency, race-conditions, double-spending, locking]
source: "Martin Kleppmann, Designing Data-Intensive Applications; SQLite Concurrency Testing"
status: resource
---

# Concurrency Race Conditions in Wallet Balances

## 1. Summary & Core Concept
The most dangerous defect in digital wallets is the **Time-of-Check to Time-of-Use (TOCTOU)** double-spend race condition. Suppose Alice has a balance of \$100 and sends two simultaneous transfer requests for \$100 to Bob and Charlie:
1. Thread A reads Alice's balance (\$100). Balance $\ge$ 100 $\longrightarrow$ OK.
2. Thread B reads Alice's balance (\$100). Balance $\ge$ 100 $\longrightarrow$ OK.
3. Thread A deducts \$100 and sets balance to \$0.
4. Thread B deducts \$100 and sets balance to -\$100 or overwrites back to \$0, sending \$200 out of a \$100 account.

## 2. Key Architectural Mechanisms
* **Pessimistic Serialization via `BEGIN IMMEDIATE`:**
  * By acquiring a write lock at the start of the transfer transaction, Thread B cannot read or modify the wallet until Thread A completes its commit.
* **Atomic Balance Check Constraints:**
  * In SQLite schema:
    ```sql
    CREATE TABLE wallets (
        id TEXT PRIMARY KEY,
        user_id TEXT NOT NULL,
        currency TEXT NOT NULL,
        balance INTEGER NOT NULL DEFAULT 0 CHECK(balance >= 0)
    );
    ```
  * Even if a race condition managed to execute concurrent updates, the database-level `CHECK(balance >= 0)` constraint causes the second transaction to fail immediately with an integrity error.

## 3. Pitfalls & Failure Modes
* **Read-Modify-Write in Application Memory:** Calculating `new_balance = user.balance - amount` in Python memory and executing `UPDATE wallets SET balance = :new_balance` without serial locks.
* **Lack of Retry Logic:** When `BEGIN IMMEDIATE` encounters another writer, failing immediately with an error rather than sleeping with jitter for 50-100ms.

## 4. Relevance to Financial Ledger API
* Primary focus of **FR-3 (Concurrent Balance Mutability)**.
* Evaluated by **TQ-4 (Simultaneous Concurrent Transfers)** and the `simulate-transfer` skill.

## 5. Wikilinks
* [[index]]
* [[resources/sqlite-wal-locking-mechanics|SQLite WAL and Transaction Locking Mechanics]]
* [[resources/acid-transactions-in-relational-databases|ACID Transactions in Relational Databases]]
