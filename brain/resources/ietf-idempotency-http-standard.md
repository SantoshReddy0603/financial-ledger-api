---
title: "IETF HTTP Idempotency-Key Specification and Fingerprinting"
date: 2026-11-09
tags: [http, idempotency, api-design, distributed-systems]
source: "IETF Draft: draft-ietf-httpapi-idempotency-key-header-04; Stripe Engineering Blog"
status: resource
---

# IETF HTTP Idempotency-Key Specification and Fingerprinting

## 1. Summary & Core Concept
In distributed networks, requests can time out while being processed by the server. If the client retries a `POST /transfers` request, the server might execute the transfer a second time, resulting in double-charging. An idempotency key is an opaque string provided in an HTTP header (e.g. `Idempotency-Key: <UUID>`) that uniquely identifies a client's operation.

## 2. Key Architectural Mechanisms
* **Request Fingerprinting:** Storing just the idempotency key is vulnerable to payload substitution attacks. The system must compute a cryptographic hash of the request parameters:
  $$\text{Fingerprint} = \text{SHA-256}(\text{Method} + \text{Path} + \text{Client-ID} + \text{Body-Payload})$$
* **Storage Schema:**
  * `key`: Unique client key string (Primary Key or indexed).
  * `request_hash`: SHA-256 digest of request attributes.
  * `status`: `PROCESSING`, `COMPLETED`, or `FAILED`.
  * `response_code`: Cached HTTP status code (e.g., 201).
  * `response_body`: Cached JSON payload.
  * `created_at`: Expiration timestamp (e.g., 24 hours).
* **Execution Flow:**
  1. Check if `Idempotency-Key` exists.
  2. If missing: Reject request (`400 Bad Request`).
  3. If exists and matches hash with `COMPLETED`: Return cached response immediately.
  4. If exists and matches hash with `PROCESSING`: Return `409 Conflict` (concurrent duplicate).
  5. If exists and hash does NOT match: Return `422 Unprocessable Entity` (payload mismatch).
  6. If new: Mark `PROCESSING`, execute transaction, record `COMPLETED` and cache response.

## 3. Pitfalls & Failure Modes
* **Silent Corruption via Payload Mutation:** Accepting the same key with different amounts or receiver IDs.
* **Race on the Idempotency Record:** Two simultaneous requests arriving with the same new key. A unique database constraint on `key` must serialize insertion.

## 4. Relevance to Financial Ledger API
* Implements **FR-2 (Idempotent Transfers)**.
* Evaluated by **TQ-1 (Hashing Tests)** and **TQ-4 (Invalid Payload Negative Tests)**.

## 5. Wikilinks
* [[index]]
* [[resources/rest-api-design-financial-services|REST API Design for Financial Services]]
* [[resources/concurrency-race-conditions-wallets|Concurrency Race Conditions in Wallet Balances]]
