---
title: "Reconciliation Algorithms in Accounting Engines"
date: 2026-11-09
tags: [reconciliation, audit, verification, invariants]
source: "AICPA Audit and Accounting Guides; Banking Core System Integrity Checks"
status: resource
---

# Reconciliation Algorithms in Accounting Engines

## 1. Summary & Core Concept
Reconciliation is the administrative audit process of independently verifying that internal ledger books balance and that cached balances match the historical sum of ledger lines. In financial engines, reconciliation acts as the ultimate automated sanity test to catch silent bugs, hardware faults, or undetected race conditions.

## 2. Key Architectural Mechanisms
A comprehensive ledger reconciliation executes two distinct mathematical verifications:
1. **Global Zero-Sum Invariant:**
   Across all transactions in the entire database, the sum of all debits must equal the sum of all credits:
   ```sql
   SELECT 
       COALESCE(SUM(CASE WHEN entry_type = 'DEBIT' THEN amount ELSE 0 END), 0) AS total_debits,
       COALESCE(SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE 0 END), 0) AS total_credits
   FROM ledger_entries;
   ```
   *Pass Condition:* `total_debits == total_credits`.

2. **Per-Wallet Balance Parity:**
   For every individual wallet, the current cached balance in `wallets.balance` must exactly equal the net ledger entries (`credits - debits` for liability/wallet accounts):
   ```sql
   SELECT 
       w.id,
       w.balance AS cached_balance,
       COALESCE(SUM(CASE 
           WHEN le.entry_type = 'CREDIT' THEN le.amount 
           WHEN le.entry_type = 'DEBIT' THEN -le.amount 
           ELSE 0 END), 0) AS calculated_balance
   FROM wallets w
   LEFT JOIN ledger_entries le ON w.id = le.wallet_id
   GROUP BY w.id;
   ```
   *Pass Condition:* `cached_balance == calculated_balance` for every wallet.

## 3. Pitfalls & Failure Modes
* **Ignoring Genesis / Inflow Accounts:** If funds are minted into a wallet from an external bank transfer without a balancing System/Clearing account, the global zero-sum will fail. A designated System/Clearing wallet (`SYSTEM_RESERVE`) must be debited to credit the user's wallet.
* **Lock Contention during Reconciliation:** Running long table scans on `ledger_entries` in write mode blocking transactions. Read-only queries must run in WAL reader mode without holding table locks.

## 4. Relevance to Financial Ledger API
* Directly fulfills **FR-5 (Ledger Reconciliation)**.
* Invoked in Step 5 of the **`simulate-transfer`** agent skill.

## 5. Wikilinks
* [[index]]
* [[resources/double-entry-accounting-principles|Double-Entry Accounting Principles]]
* [[resources/audit-trail-immutability-patterns|Immutable Audit Trail Design Patterns]]
