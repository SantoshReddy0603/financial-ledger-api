# ADR-0004: Canonical JSON and Transaction Hash Chain

**Status:** Accepted
**Date:** 2026-11-20
**Requirements:** FR-2.5, FR-4.5, FR-5.3 (C6), SRS §4.5, §2.7

## Context

SRS §4.5 requires design documentation to fix an unambiguous canonical serialization, field inclusion, encoding and entry order before implementation. Two hashes depend on it: the idempotency fingerprint (FR-2.5) and the transaction hash chain (FR-4.5). Generic serializers do not guarantee key order or number formatting ([[resources/canonical-json-and-hash-chains]]). The chain is an integrity check within the threat boundary of §2.7, not proof against a privileged rewrite ([[resources/audit-trail-immutability-patterns]]).

## Decision

**Canonical form** (a subset of RFC 8785, implemented in `domain.CanonicalJson`):

* Value types: object, string, signed 64-bit integer, `null`. No floats, booleans or arrays except the `entries` array below.
* Objects: members sorted by key (UTF-16 code-unit order; all keys are ASCII), no whitespace, `,` and `:` separators.
* Strings: UTF-8; escape `"` → `\"`, `\` → `\\`, U+0008/0009/000A/000C/000D → `\b \t \n \f \r`, other code points below U+0020 → `\u00XX` (lowercase hex); everything else literal. No Unicode normalization.
* Integers: `Long.toString` (no leading zeros, `-` only for negatives, no exponent).
* Optional strings: a missing member and explicit `null` both become `null`; `""` stays `""`.

**Idempotency fingerprint:** `lowercase-hex(SHA-256(UTF-8(canonical(F))))` where

| Operation | `F` |
|-----------|-----|
| Transfer | `{"op":"TRANSFER","path":{},"body":{"amount":A,"currency":C,"destinationWalletId":D,"reference":R,"sourceWalletId":S}}` |
| Funding | `{"op":"FUNDING","path":{"walletId":W},"body":{"amount":A,"currency":C}}` |
| Reversal | `{"op":"REVERSAL","path":{"transactionId":T},"body":{"reason":R}}` (omitted body ⇒ `reason:null`) |

Fields come from the parsed, validated request, so member order and whitespace do not matter.

**Transaction hash:** `hash = lowercase-hex(SHA-256(UTF-8(canonical(H))))` where `H` contains:

```
{"amount", "createdAt", "currency", "destinationAccountId",
 "entries":[{"accountId","amount","createdAt","currency","direction","entryNumber"} ...],
 "previousHash", "reason", "reference", "reversalOf", "sequence",
 "sourceAccountId", "transactionId", "type"}
```

* `entries` ordered by ascending `entryNumber` (the DEBIT is allocated first, then the CREDIT).
* `sequence` and `entryNumber` are JSON integers in the hash input (exact `long` text), although the public API serializes them as strings.
* `reversedBy` is derived and excluded, so a later reversal never changes an original's hash.
* `previousHash` is the `hash` of sequence `n-1`; for sequence 1 it is 64 `0` characters.
* Computed inside the `BEGIN IMMEDIATE` transaction after sequence and entry numbers are allocated (FR-4.5).

The scheme is versioned implicitly by schema version 1; changing it would require a new schema version and a migration plan, since FR-2.5 requires stability for the life of the database.

## Alternatives Considered

| Option | Why not chosen |
|--------|----------------|
| Hash the raw request bytes | Whitespace or member order in a retry would cause false `IDEMPOTENCY_KEY_REUSED`. |
| Jackson with `ORDER_MAP_ENTRIES_BY_KEYS` | Ordering is a configuration detail that a later upgrade or mapper change could silently alter. |
| Pipe-delimited concatenation | Ambiguous when values contain the delimiter; JSON escaping is already well defined. |
| Full RFC 8785 library | Its hard part (floating-point formatting) is irrelevant because floats are banned. |

## Consequences

* Unit tests pin known-answer vectors: fixed input → fixed canonical string → fixed hash, including escaping and null normalization (TQ-1).
* Reconciliation check C6 recomputes every hash from stored columns and detects any edited field or broken link.
* A consistent rewrite of the whole table by a database operator is not detected, as §2.7 states.

## Brain Notes

* [[resources/canonical-json-and-hash-chains]]
* [[resources/audit-trail-immutability-patterns]]
* [[resources/ietf-idempotency-http-standard]]
