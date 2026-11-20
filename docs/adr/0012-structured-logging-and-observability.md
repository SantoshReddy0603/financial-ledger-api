# ADR-0012: Structured Logging and Observability

**Status:** Accepted
**Date:** 2026-11-20
**Requirements:** NFR-OBS-1, NFR-OBS-2, NFR-SEC-5, NFR-API-6

## Context

NFR-OBS-1 requires one structured record per request (request ID, method, sanitized path, status, duration) and one per committed transaction (ID and sequence). NFR-OBS-2 requires error codes in request records and forbids logging a replay as a new commit. NFR-SEC-5 forbids logging keys and bodies, query strings and unsafe characters ([[resources/audit-trail-immutability-patterns]], [[areas/security-validation]]).

## Decision

* SLF4J with Logback's `JsonEncoder` writes one JSON object per line to stdout.
* Logger `ledger.request`: `requestId`, `method`, `path` (query string removed, non-printable or non-ASCII characters replaced by `?`, capped at 256 characters), `status`, `durationMs`, and `errorCode` when the response is an error.
* Logger `ledger.transaction`: `transactionId`, `sequence`, `type` after commit only. A replay logs `idempotencyReplay=true` on the request record and no transaction record.
* Never logged: request or response bodies, `Authorization`, `X-API-Key`, `Idempotency-Key` values, rejected `X-Request-Id` values.
* Jetty and Javalin internals log at WARN.

## Alternatives Considered

| Option | Why not chosen |
|--------|----------------|
| Plain-text logs | Not structured; harder to assert in tests and to search. |
| Logging bodies at DEBUG | One misconfiguration away from leaking secrets; forbidden outright. |
| Using logs as an audit source | Logs are operational (NFR-OBS-1); the ledger and hash chain are authoritative. |

## Consequences

* System tests capture the log appender and assert fields, redaction and log-injection handling (`\n` in a path).
* Operators can pipe stdout to any JSON log collector without parsing rules.

## Brain Notes

* [[resources/audit-trail-immutability-patterns]]
* [[areas/security-validation]]
