---
title: "Canonical JSON Serialization and Tamper-Evident Hash Chains"
date: 2026-11-20
tags: [hashing, canonicalization, rfc8785, audit, idempotency, research]
source: "RFC 8785 (JSON Canonicalization Scheme); RFC 6234 (SHA-2); Crosby & Wallach, 'Efficient Data Structures for Tamper-Evident Logging' (USENIX Security 2009)"
status: resource
---

# Canonical JSON Serialization and Tamper-Evident Hash Chains

## 1. Summary & Core Concept
Hashing JSON is only meaningful if the same logical value always produces the same bytes. JSON allows any key order, optional whitespace and several spellings of one number, so a hash over "whatever the serializer emitted" is unstable across libraries and versions. **RFC 8785 (JCS)** fixes this: UTF-8, object members sorted by key code points, no insignificant whitespace, minimal string escaping, and a single number form.

A **hash chain** links each record to its predecessor: `hash_n = SHA-256(canonical(record_n) || hash_{n-1})`. Changing, removing or reordering any historical record changes every later hash, so verification by recomputation detects tampering by anyone who cannot also rewrite the whole tail and the published head.

## 2. Key Architectural Mechanisms
* **Subset we need:** values are only objects, strings, integers (`long`) and null. No floating point, so the hard part of JCS (number formatting) reduces to `Long.toString`.
* **Key order:** sort by UTF-16 code units (JCS rule); all our keys are ASCII so this equals byte order.
* **Strings:** escape `"` and `\`, control characters as `\uXXXX` (with `\b \f \n \r \t` short forms); everything else literal UTF-8.
* **Absent vs null:** normalize missing optional fields to explicit `null` so `{}` and `{"reference":null}` fingerprint identically.
* **Idempotency fingerprint:** `SHA-256(canonical({"op","path","body"}))`, where `body` is the parsed, validated request, not the raw bytes. Whitespace or key-order differences in a retry are the same request.
* **Transaction hash input:** every immutable field (id, sequence, type, accounts, amount, currency, reference, reason, reversalOf, createdAt) plus entries ordered by entry number plus `previousHash`. Mutable or derived fields (`reversedBy`) are excluded.
* **Genesis:** first `previousHash` is 64 zeros.
* **Encoding:** lowercase hex, 64 chars.

## 3. Pitfalls & Failure Modes
* Hashing `Map.toString()` or Jackson default output: order depends on implementation.
* Including `reversedBy` in the hash: reversing a transaction would change the original's hash and break the chain.
* Integers above 2^53 serialized as JSON numbers lose precision in JavaScript clients; serialize sequence numbers and aggregate totals as decimal strings in API responses (hash input still uses the exact integer text).
* Unicode normalization: JCS deliberately does **not** normalize (NFC); two visually equal strings may hash differently. Acceptable because references are opaque client strings.
* A chain alone does not stop an attacker who can rewrite the entire database; it bounds the threat to "detect modification of committed history" (see C5/C6 in reconciliation).

## 4. Relevance to Financial Ledger API
* **FR-2.2** (request fingerprint), **FR-4.3** (hash chain), **FR-5** reconciliation checks C5–C6.
* **SRS §4.5** requires canonical serialization to be fixed in design before implementation.
* Decision recorded in `docs/adr/0004-canonical-json-and-hash-chain.md`.

## 5. Wikilinks
* [[index]]
* [[resources/audit-trail-immutability-patterns|Immutable Audit Trail Design Patterns]]
* [[resources/ietf-idempotency-http-standard|IETF HTTP Idempotency Standard]]
* [[areas/idempotency-design|Area: API Idempotency and Fault Tolerance]]
