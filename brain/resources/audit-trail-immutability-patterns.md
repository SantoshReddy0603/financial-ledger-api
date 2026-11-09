---
title: "Immutable Audit Trail Design Patterns"
date: 2026-11-09
tags: [audit-trail, immutability, ledger, compliance]
source: "Greg Young, CQRS and Event Sourcing; NIST SP 800-92 Guide to Computer Security Log Management"
status: resource
---

# Immutable Audit Trail Design Patterns

## 1. Summary & Core Concept
Financial regulations (SOX, PCI-DSS) and sound accounting principles dictate that financial ledgers must be tamper-evident and append-only. Once an entry is written to the ledger, it cannot be modified (`UPDATE`) or removed (`DELETE`).

If an error occurs (such as an incorrect charge or payment cancellation), the original transaction remains intact in the ledger, and an explicit **compensating transaction** (reversal) is appended.

## 2. Key Architectural Mechanisms
* **Append-Only Schema:**
  ```sql
  CREATE TABLE ledger_entries (
      id TEXT PRIMARY KEY,
      transaction_id TEXT NOT NULL,
      wallet_id TEXT NOT NULL,
      entry_type TEXT CHECK(entry_type IN ('DEBIT', 'CREDIT')),
      amount INTEGER NOT NULL CHECK(amount > 0),
      balance_after INTEGER NOT NULL,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY(transaction_id) REFERENCES transactions(id),
      FOREIGN KEY(wallet_id) REFERENCES wallets(id)
  );
  ```
* **Database Triggers for Hard Immutability:**
  * SQLite triggers that abort `UPDATE` and `DELETE` queries on `ledger_entries` and `transactions`:
  ```sql
  CREATE TRIGGER abort_ledger_update BEFORE UPDATE ON ledger_entries
  BEGIN
      SELECT RAISE(FAIL, 'Ledger entries are strictly immutable');
  END;
  ```
* **Reversal Modeling:**
  * A reversal is a new distinct transaction referencing `original_transaction_id` with inverted debits and credits.

## 3. Pitfalls & Failure Modes
* **In-Place Corrections:** Running `UPDATE wallets SET balance = balance + 100` during customer support workflows, erasing historical lineage.
* **Cascading Deletes:** Configuring `ON DELETE CASCADE` on foreign keys that inadvertently wipe ledger history when a test user or wallet is deleted.

## 4. Relevance to Financial Ledger API
* Satisfies **FR-4 (Immutable Audit Trail)**.
* Validates **TQ-2 (Database Rollback Behavior)** and **TQ-3 (E2E Tests)**.

## 5. Wikilinks
* [[index]]
* [[resources/double-entry-accounting-principles|Double-Entry Accounting Principles]]
* [[resources/reconciliation-algorithms-accounting|Reconciliation Algorithms in Accounting Engines]]
