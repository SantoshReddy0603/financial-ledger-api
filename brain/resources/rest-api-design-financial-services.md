---
title: "RESTful API Design for Financial Services"
date: 2026-11-09
tags: [rest, api-design, http-status-codes, financial-systems]
source: "Zalando RESTful API Guidelines; Stripe API Reference"
status: resource
---

# RESTful API Design for Financial Services

## 1. Summary & Core Concept
Financial APIs require deterministic error semantics, strict input validation, and clear resource representation. Ambiguous HTTP responses (such as returning HTTP 200 with `{"error": "insufficient funds"}`) break automated clients and lead to unhandled double-charges during retries.

## 2. Key Architectural Mechanisms
* **Resource Hierarchy:**
  * `POST /wallets`: Create a new user wallet with currency.
  * `GET /wallets/{wallet_id}`: Retrieve wallet details and current balance.
  * `POST /transfers`: Execute a double-entry funds transfer (requires `Idempotency-Key` header).
  * `GET /wallets/{wallet_id}/entries`: Retrieve historical ledger lines for audit.
  * `POST /reconcile`: Administrative trigger to execute ledger verification.
* **HTTP Status Code Precision:**
  * `201 Created`: Transfer or wallet successfully created.
  * `200 OK`: Successful read or idempotent cached return.
  * `400 Bad Request`: Malformed UUID, missing headers, negative amount.
  * `404 Not Found`: Source or destination wallet ID does not exist.
  * `409 Conflict`: Insufficient funds, or concurrent in-flight transfer with identical idempotency key.
  * `422 Unprocessable Entity`: Idempotency key payload mismatch, or currency mismatch between wallets.
  * `500 Internal Server Error`: Unrecoverable database failure.

## 3. Pitfalls & Failure Modes
* **Inconsistent Error Formats:** Returning plain text errors or unformatted tracebacks. APIs must return standard RFC 7807 problem details or uniform JSON `{ "code": "INSUFFICIENT_FUNDS", "message": "..." }`.
* **Exposing Raw Float Representation:** Returning fractional numbers like `10.5` in JSON responses instead of integer minor units `1050` with currency metadata.

## 4. Relevance to Financial Ledger API
* Fulfills **SE-7 (Developer API Documentation / OpenAPI)**.
* Dictates API contracts for **FR-1 through FR-5**.

## 5. Wikilinks
* [[index]]
* [[resources/ietf-idempotency-http-standard|IETF HTTP Idempotency Standard]]
* [[resources/integer-currency-minor-units|Financial Minor Units and Floating Point Traps]]
