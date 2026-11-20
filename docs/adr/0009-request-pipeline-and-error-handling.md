# ADR-0009: Request Pipeline, Validation Order and Error Envelope

**Status:** Accepted
**Date:** 2026-11-20
**Requirements:** SRS §4.3, §4.4, NFR-API-1, NFR-API-2, NFR-API-6, NFR-API-7, NFR-SEC-1, NFR-DOC-3, FR-2.3, FR-4.4

## Context

SRS §4.4 fixes the processing order: media type, body size, authentication, authorization, structural validation (identifiers, idempotency header, fields), then for money-moving requests the write lock, fingerprint, ownership, replay check and business rules. §4.3 fixes the error envelope and catalogue. Errors must never expose SQL, stack traces, paths or secrets ([[resources/rest-api-design-financial-services]], [[areas/security-validation]]).

## Decision

Javalin `before` handlers and per-route handlers implement the order:

1. **Request ID:** echo a single valid `X-Request-Id` (`[A-Za-z0-9_-]{1,64}`), otherwise generate a UUID; set on every response.
2. **Route resolution:** unknown path → `404 NOT_FOUND`; known path, wrong method → `405 METHOD_NOT_ALLOWED` with `Allow` (GET routes also allow HEAD).
3. **Media type, then size:** non-empty POST body without `application/json` (charset only UTF-8) → `415`; body over 16,384 bytes → `413` (also enforced by Jetty's `maxRequestSize`).
4. **Authentication, then role:** bearer session or `X-API-Key` per route → `401`/`403`.
5. **Structural validation** in schema order: JSON syntax, duplicate members, unknown members, required members, types; then path identifiers (`MALFORMED_ID`), idempotency header, remaining fields and query parameters. Missing `amount` → `VALIDATION_ERROR`; present but invalid → `INVALID_AMOUNT`. Each detail is `{"field","rule"}` (NFR-DOC-3). Structural validation never reads the database.
6. **Money-moving operations** enter `LedgerStore.write`: fingerprint → (transfer) source exists (404) and is owned (403) → replay or `IDEMPOTENCY_KEY_REUSED` → business rules in the FR-2.3 / FR-4.4 order → writes → commit.
7. **Errors:** domain code throws `LedgerException(ErrorCode, message, details)`; one exception handler renders `{"error":{"code","message","details"}}` with the status from `ErrorCode`, omitting empty `details`. `StorageBusyException` → `503 STORAGE_BUSY` + `Retry-After: 1`. Anything else → log with request ID, return `500 INTERNAL_ERROR` with a generic message.

`ErrorCode` is an enum of the 26 catalogue codes with their HTTP statuses; `ErrorCodeTest` pins the list so a code cannot drift.

## Alternatives Considered

| Option | Why not chosen |
|--------|----------------|
| Bean Validation annotations | Cannot express token-level amount rules, duplicate members or the SRS ordering. |
| Framework default error pages | Leak framework details and do not follow the envelope. |
| Validating business rules before the lock | Reads would be stale by the time of the write (TOCTOU). |

## Consequences

* The same request always fails with the same code, regardless of which framework layer noticed it.
* Negative system tests (TQ-4) exercise each step of the order with requests that violate two rules at once to prove precedence.

## Brain Notes

* [[resources/rest-api-design-financial-services]]
* [[areas/security-validation]]
* [[resources/integer-currency-minor-units]]
