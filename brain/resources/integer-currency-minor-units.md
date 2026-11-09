---
title: "Financial Minor Units and Floating Point Traps"
date: 2026-11-09
tags: [currency, precision, floating-point, integer-arithmetic]
source: "IEEE 754 Standard; Martin Fowler, Patterns of Enterprise Application Architecture (Money Pattern)"
status: resource
---

# Financial Minor Units and Floating Point Traps

## 1. Summary & Core Concept
Standard programming language floating-point types (`float`, `double`) adhere to IEEE 754 binary representation. Because binary cannot precisely represent base-10 fractional decimals like `0.1` or `0.01`, arithmetic calculations accumulate rounding errors:
$$0.1 + 0.2 = 0.30000000000000004$$

In banking and accounting engines, fractional loss or unexpected rounding creates ledger imbalances where debits and credits differ by tiny fractions, breaking the fundamental zero-sum invariant.

## 2. Key Architectural Mechanisms
* **Integer Minor Units:** All monetary values are represented as integers representing the smallest currency denomination (minor units):
  * USD: Cents ($10.50 \longrightarrow 1050$).
  * INR: Paise (₹100.25 $\longrightarrow 10025$).
  * JPY: Yen ($¥500 \longrightarrow 500$, zero-decimal currency).
* **Strict Schema Type Enforcement:**
  * Pydantic / JSON schema validator rejects floats: `amount: int = Field(gt=0, description="Amount in minor units")`.
  * Database column type: `INTEGER` (64-bit signed integer in SQLite).
* **Display Conversion:**
  * Division by $10^{\text{decimals}}$ occurs ONLY at the presentation layer / client UI, never in core ledger calculation or storage.

## 3. Pitfalls & Failure Modes
* **Accidental Coercion:** Using division `/` instead of integer division `//` in Python converts integers to IEEE 754 floats.
* **Negative Value Injection:** Accepting negative numbers (e.g. `amount = -500`) allowing a sender to drain the receiver or artificially inflate their own balance.

## 4. Relevance to Financial Ledger API
* Mandated by **TQ-6 (Floating-Point Rounding Prevention)**.
* Supports **FR-1** through **FR-5**.

## 5. Wikilinks
* [[index]]
* [[resources/double-entry-accounting-principles|Double-Entry Accounting Principles]]
* [[resources/reconciliation-algorithms-accounting|Reconciliation Algorithms in Accounting Engines]]
