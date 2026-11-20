# ADR-0005: Idempotency Records Committed with the Transaction

**Status:** Accepted
**Date:** 2026-11-20
**Requirements:** FR-2.2, FR-2.5 to FR-2.10, FR-1.7, FR-4.2, NFR-REL-4, SRS §4.4, §4.6

## Context

Clients retry after timeouts. FR-2.8 requires the key record to be committed atomically with the ledger entries, and only for successful operations. FR-2.6 requires a recorded key with an equal fingerprint to replay the stored status and body with `Idempotency-Replayed: true`, even if balances have changed since. FR-2.10 requires concurrent same-key requests to commit at most once. Principals must be stable across devices, logins and admin key rotation, and user and admin namespaces must never collide (§4.6) ([[resources/ietf-idempotency-http-standard]], [[areas/idempotency-design]]).

## Decision

* **Header:** exactly one `Idempotency-Key` with 16–64 characters from `[A-Za-z0-9_-]`. Absent → `400 MISSING_IDEMPOTENCY_KEY`; duplicated or malformed → `400 INVALID_IDEMPOTENCY_KEY`.
* **Principal identifier:** `user:<userId>` for USER sessions; `admin:<principalId>` for administrator keys, where `principalId` comes from configuration (`LEDGER_ADMIN_KEYS=principalId=sha256hex,...`). Rotating a key keeps the principal, so history survives. The prefixes keep the namespaces distinct.
* **Table:** `idempotency_record(principal_id, idempotency_key, fingerprint, response_status, response_body, transaction_id, created_at)` with primary key `(principal_id, idempotency_key)`, append-only triggers and a unique reference to the committed transaction.
* **Flow inside `BEGIN IMMEDIATE`** (after structural validation and, for transfers, source existence and ownership):
  1. Compute the fingerprint (ADR-0004).
  2. `SELECT` the record by principal and key.
  3. Same fingerprint → return stored status and body, add `Idempotency-Replayed: true`, write nothing, log as replay (NFR-OBS-2).
  4. Different fingerprint → `422 IDEMPOTENCY_KEY_REUSED`, write nothing.
  5. Not found → business checks, ledger writes, then `INSERT` the record with the exact response body that will be returned, then commit.
* Any rejection or rollback leaves no record, so the key stays reusable (FR-2.8).
* Records are never deleted (FR-2.9).
* Because the whole flow holds the write lock, a second concurrent request with the same key waits, then sees the committed record and replays (FR-2.10).
* `X-Request-Id` is per attempt and not stored.

## Alternatives Considered

| Option | Why not chosen |
|--------|----------------|
| Separate "in-progress" record before doing the work | Needs cleanup of abandoned records and a 409 "in progress" state the SRS does not define; the write lock already serializes. |
| Caching responses in memory | Lost on restart (FR-2.9). |
| Fingerprint over raw body | See ADR-0004. |
| Raw admin key as principal | Breaks history on rotation and would persist a secret. |

## Consequences

* A replay after a balance change still returns the original 201 (acceptance criterion 4).
* `response_body` stores the canonical response at creation, where `reversedBy` is `null`; a replay therefore differs from a later `GET` by design (§4.5).
* Tests: fingerprint vectors; replay after restart; replay after balance change; reuse with changed payload; concurrent same-key requests (exactly one transaction); rollback leaves the key unrecorded.

## Brain Notes

* [[resources/ietf-idempotency-http-standard]]
* [[areas/idempotency-design]]
* [[resources/canonical-json-and-hash-chains]]
