# ADR-0006: Money, Numbers and Exact JSON Parsing

**Status:** Accepted
**Date:** 2026-11-20
**Requirements:** BR-1, BR-5, BR-6, BR-7, BR-12, DC-5, NFR-API-3, NFR-API-7, NFR-SEC-2, NFR-REL-3, TQ-6

## Context

Floating-point money loses cents and makes equality unreliable ([[resources/integer-currency-minor-units]]). DC-5 requires Java `long` and SQLite `INTEGER` minor units, parsing that rejects non-integer tokens without passing through floating point, checked arithmetic, and `BigInteger` for large aggregates. NFR-API-3 requires amounts to be JSON integer tokens, rejecting `10.0`, `1e3` and `"10"`. JSON clients in JavaScript cannot represent integers above 2^53−1 exactly, which is why wallet balances are capped there (BR-7) and aggregates are strings (BR-12).

## Decision

* **Storage:** every monetary column is `INTEGER` with `typeof(...) = 'integer'` checks; amounts `BETWEEN 1 AND 1000000000000`; wallet balances `BETWEEN 0 AND 9007199254740991`; system balances any signed 64-bit value.
* **Domain:** `Money` wraps `long minorUnits` plus `CurrencyCode`; constructors validate range. Arithmetic uses `Math.addExact` / `subtractExact`; an `ArithmeticException` becomes `NUMERIC_LIMIT_EXCEEDED`.
* **Business checks before persistence** (NFR-REL-3): a wallet debit below zero → `422 INSUFFICIENT_FUNDS`; a wallet credit above 2^53−1 → `422 BALANCE_LIMIT_EXCEEDED`; a system balance leaving signed 64-bit range → `422 NUMERIC_LIMIT_EXCEEDED`.
* **Parsing:** request bodies are read with Jackson's streaming `JsonParser` with `STRICT_DUPLICATE_DETECTION` enabled. For `amount`, only `VALUE_NUMBER_INT` tokens are accepted, read via `getNumberType()`; `BIG_INTEGER` or out-of-range → `INVALID_AMOUNT`. `VALUE_NUMBER_FLOAT` (including `1e3`, `10.0`), strings, booleans and `null` → `INVALID_AMOUNT`. `double` and `float` are never used on this path.
* **Responses:** `amount` and wallet `balance` are JSON integers. `sequence`, `entryNumber`, counts, `snapshotSequence`, `nextBefore` and aggregate `totalCredits`/`totalDebits` are decimal strings.
* **Aggregates:** reconciliation sums with `BigInteger` (or `Math.addExact` on `long` with a `BigInteger` fallback) so totals beyond 2^63 stay exact.
* **Sign convention:** balance = credits − debits for both account kinds (BR-5).

## Alternatives Considered

| Option | Why not chosen |
|--------|----------------|
| `BigDecimal` for amounts | Exact, but heavier and invites fractional minor units; DC-5 mandates `long`. |
| Amounts as JSON strings | Contradicts NFR-API-3. |
| Jackson tree model (`readTree`) | Loses token-level distinction in some configurations and is weaker for duplicate detection; the streaming parser gives exact control. |

## Consequences

* Precision tests (TQ-6): reject `0`, `-1`, `1.0`, `1e3`, `"1"`, `1000000000001`; accept `1` and `1000000000000`; overflow tests at both balance limits; aggregate totals above 2^53−1 serialized exactly.
* A build-time grep in CI for `double`/`float` in `domain` and `application` supports the TQ-6 inspection.

## Brain Notes

* [[resources/integer-currency-minor-units]]
* [[resources/double-entry-accounting-principles]]
* [[areas/security-validation]]
