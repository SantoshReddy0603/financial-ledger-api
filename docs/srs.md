# Software Requirements Specification (SRS)
## Financial Ledger & Digital Wallet API

> **Superseded.** The authoritative specification is the submitted SRS, [`srs.pdf`](srs.pdf) (Java 21, Maven, Javalin, SQLite). Design, plan and traceability documents reference that PDF. This Markdown file is kept as the Stage 1 baseline history only; where it disagrees with `srs.pdf`, the PDF wins.

* **Document Version:** 1.2
* **Date:** 2026-11-09
* **Status:** Revised Stage 1 Baseline (Adapting v1.1 Draft)
* **Traceability Reference:** `brain/projects/stage-1-requirements.md`

---

## 1. Introduction

### 1.1 Purpose
This document specifies the software requirements for a backend financial service that manages user digital wallets and records all movements of funds in a strict double-entry ledger. It serves as the definitive reference for design, implementation, testing, verification, and acceptance under the AI-Assisted Software Development Life Cycle (SDLC).

### 1.2 Scope
The product exposes a RESTful HTTP API that creates users and single-currency wallets, funds wallets via system accounts, transfers money under high concurrency and retries, records immutable audit trails with explicit reversals, and reconciles ledger balances. Real payment gateways, end-user authentication UI, fees, and currency conversion are explicitly out of scope.

### 1.3 Definitions & Invariants
* **Double-Entry Invariant:** Every financial transaction consists of exactly two entries (one debit and one credit) of identical magnitude: $\sum \text{debits} = \sum \text{credits} = 0$.
* **Minor Unit:** The smallest unit of a currency (e.g., cents, paise). All amounts are strictly 64-bit signed integers.
* **Idempotency Key:** A client-supplied header (`Idempotency-Key`) uniquely identifying a money-moving operation within a credential namespace.
* **Request Fingerprint:** A SHA-256 digest computed across the canonical HTTP method, route, credential ID, and JSON payload.
* **Reversal:** An append-only compensating transaction that mirrors and inverts an earlier transaction without modifying the original record.
* **Reconciliation:** An audit operation performing mathematical consistency checks across all ledger lines, cached balances, and hash chains.
* **Brain:** The Obsidian vault located in `brain/`, structured using PARA.

---

## 2. Design and Implementation Constraints (DC)

| ID | Requirement Description | Verification |
| :--- | :--- | :--- |
| **DC-1** | The service shall use Python 3.11+ with FastAPI, Uvicorn, and standard library SQLite (replacing C++17/Qt 6 from v1.1 to optimize CI velocity and testability). | R |
| **DC-2** | SQLite 3 shall be the exclusive persistent data store. In-memory databases are permitted for unit tests; file-backed databases with WAL mode are required for concurrency/integration tests. | R, I |
| **DC-3** | The API shall use REST over HTTP/1.1 with JSON payloads. Base path: `/api/v1`. | S |
| **DC-4** | External dependencies (storage, clock, ID generation, hashing) shall be isolated behind provider interfaces (SE-6). | R |
| **DC-5** | All monetary amounts and balances shall be signed 64-bit integers in minor units. Floating-point numbers are strictly forbidden. | U, R |
| **DC-6** | Concurrency control shall use SQLite transactions with `BEGIN IMMEDIATE` and WAL mode; no external distributed lock manager. | R, I |
| **DC-7** | Source, issues, pull requests, CI workflows, and documentation shall be maintained in a single Git repository. | R |
| **DC-8** | Architectural designs shall include UML sequence and ER diagrams matching the implementation. | R |

---

## 3. Business Rules (BR)

* **BR-1:** All money is represented strictly as integer minor units.
* **BR-2:** Currencies are uppercase ISO 4217 codes (`USD`, `INR`, `EUR`, default exponent 2). A wallet's currency is immutable.
* **BR-3:** An account is either a user `WALLET` or a `SYSTEM` funding account. Exactly one system funding account exists per currency.
* **BR-4:** Every committed transaction has exactly two entries on different accounts of the same currency: one DEBIT and one CREDIT.
* **BR-5:** Credits increase balances; debits decrease balances. Balance equals $\sum \text{Credits} - \sum \text{Debits}$.
* **BR-6:** Wallet balances must remain non-negative ($\ge 0$). System funding accounts may be negative to reflect net funding outstanding.
* **BR-7:** Transaction amounts must be between $1$ and $1,000,000,000,000$ minor units. Wallet balances must not exceed $2^{53} - 1$ ($9,007,199,254,740,991$).
* **BR-8:** Committed transactions, ledger entries, and idempotency records are strictly append-only and immutable.
* **BR-9:** Reversals generate two entries mirroring the original transaction's accounts, directions, and amount.
* **BR-10:** For each currency, the sum of all wallet balances plus the system funding account balance must equal zero ($\sum \text{Wallets} + \text{SystemAccount} = 0$).
* **BR-11:** Transaction sequence numbers are positive, gap-free, monotonically increasing integers ($1, 2, 3 \dots$). Rolled-back writes consume no sequence numbers.
* **BR-12:** All arithmetic must be exact and checked for overflow. Public aggregate totals (`totalCredits`, `totalDebits`) are serialized as base-10 integer strings.

---

## 4. External Interface & API Specification

### 4.1 Endpoints

| Method | Path | Role | Idempotency Key Required |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/health` | None | No |
| `POST` | `/api/v1/users` | CLIENT / ADMIN | No |
| `GET` | `/api/v1/users/{userId}` | CLIENT / ADMIN | No |
| `POST` | `/api/v1/users/{userId}/wallets` | CLIENT / ADMIN | No |
| `GET` | `/api/v1/wallets/{walletId}` | CLIENT / ADMIN | No |
| `GET` | `/api/v1/wallets/{walletId}/entries` | CLIENT / ADMIN | No |
| `POST` | `/api/v1/transfers` | CLIENT / ADMIN | **Yes** |
| `GET` | `/api/v1/transactions/{transactionId}` | CLIENT / ADMIN | No |
| `POST` | `/api/v1/admin/wallets/{walletId}/funding` | ADMIN | **Yes** |
| `POST` | `/api/v1/admin/transactions/{transactionId}/reversal` | ADMIN | **Yes** |
| `GET` | `/api/v1/admin/reconciliation` | ADMIN | No |

### 4.2 Error Catalogue
Errors return standard JSON: `{"error": {"code": "...", "message": "...", "details": [...]}}`.
* `400 VALIDATION_ERROR`: Malformed JSON, missing fields, or unrecognized parameters.
* `400 INVALID_AMOUNT`: Amount is not an integer token in the permitted range.
* `400 MALFORMED_ID`: Identifier is not a valid lowercase RFC 4122 UUID v4.
* `400 MISSING_IDEMPOTENCY_KEY`: Mandatory header is absent on money-moving POSTs.
* `400 INVALID_IDEMPOTENCY_KEY`: Key does not match 16–64 ASCII alphanumeric characters.
* `401 UNAUTHENTICATED`: Missing or invalid `X-API-Key`.
* `403 FORBIDDEN`: CLIENT credential attempted access to `/admin` endpoints.
* `404 NOT_FOUND`: Resource does not exist.
* `409 WALLET_ALREADY_EXISTS`: User already owns a wallet in the requested currency.
* `409 ALREADY_REVERSED`: Transaction has already been reversed.
* `422 INSUFFICIENT_FUNDS`: Debit would result in negative wallet balance.
* `422 CURRENCY_MISMATCH`: Currencies between source and destination wallets differ.
* `422 SAME_WALLET_TRANSFER`: Source and destination wallet are identical.
* `422 IDEMPOTENCY_KEY_REUSED`: Same idempotency key used with a different request payload.
* `422 REVERSAL_NOT_ALLOWED`: Attempt to reverse a transaction that is itself a reversal.
* `503 STORAGE_BUSY`: Database write lock not acquired within timeout (`Retry-After: 1`).

---

## 5. Functional Requirements (FR)

### FR-1: Wallet Initialization and Funding
* **FR-1.1:** `POST /users` creates a user with `displayName` (1–100 chars) and returns 201 with `userId`, `displayName`, and `createdAt`.
* **FR-1.2:** `POST /users/{userId}/wallets` creates a zero-balance wallet for a specified currency.
* **FR-1.3:** A user may own multiple wallets, but only one per currency.
* **FR-1.4:** Creation of wallet and ledger account is atomic. Zero-balance creation requires no ledger entry.
* **FR-1.5:** `GET /wallets/{walletId}` returns wallet ID, user ID, currency, balance, and creation timestamp.
* **FR-1.6:** `GET /users/{userId}` returns user details and an array of all owned wallets.
* **FR-1.7:** `POST /admin/wallets/{walletId}/funding` credits the wallet by debiting the system funding account.
* **FR-1.8:** All balance changes are backed by committed double-entry transactions.

### FR-2: Idempotent Transfers
* **FR-2.1:** `POST /transfers` accepts `sourceWalletId`, `destinationWalletId`, `amount`, `currency`, and optional `reference`.
* **FR-2.2:** Transfers require an `Idempotency-Key` header (16–64 ASCII characters).
* **FR-2.3:** Request fingerprint is computed via SHA-256 over operation type, path, and canonical JSON body.
* **FR-2.4:** Matching fingerprint replay returns cached 201 response with `Idempotency-Replayed: true` header.
* **FR-2.5:** Duplicate key with differing fingerprint returns `422 IDEMPOTENCY_KEY_REUSED`.
* **FR-2.6:** Idempotency records are committed atomically with the transaction.

### FR-3: Concurrent Balance Mutability
* **FR-3.1:** All mutating requests execute inside a single transaction using `BEGIN IMMEDIATE`.
* **FR-3.2:** Concurrent executions serialize according to acquired write locks without data loss.
* **FR-3.3:** Debits never overdraw a wallet; concurrent race conditions return `422 INSUFFICIENT_FUNDS` for the losing request.
* **FR-3.4:** SQLite connections configure WAL mode, `PRAGMA synchronous = FULL`, `foreign_keys = ON`, and `busy_timeout = 5000`.
* **FR-3.5:** Lock timeout returns `503 STORAGE_BUSY` with `Retry-After: 1`.
* **FR-3.6:** Transaction sequence numbers remain strictly sequential and gap-free.

### FR-4: Immutable Audit Trail and Reversals
* **FR-4.1:** Ledger entries and transactions are append-only. Database triggers abort `UPDATE` and `DELETE`.
* **FR-4.2:** `POST /admin/transactions/{transactionId}/reversal` creates a compensating reversal transaction.
* **FR-4.3:** Reversals mirror debits and credits, leaving original records unmodified.
* **FR-4.4:** Reversals of reversals return `422 REVERSAL_NOT_ALLOWED`.
* **FR-4.5:** Each transaction links to the previous via cryptographic hash (`SHA-256(canonical_fields + previousHash)`).
* **FR-4.6:** `GET /wallets/{walletId}/entries` provides paginated entry history ordered newest first (`before` cursor).

### FR-5: Ledger Reconciliation
* **FR-5.1:** `GET /admin/reconciliation` performs read-only mathematical consistency checks across the ledger.
* **FR-5.2:** Reconciliation uses a single consistent snapshot.
* **FR-5.3:** Validates 7 core checks (C1–C7):
  * **C1:** Balanced transaction structure (1 debit, 1 credit, matching amounts/currencies).
  * **C2:** Balanced global totals ($\sum \text{Credits} = \sum \text{Debits}$).
  * **C3:** Wallet cached balance matches sum of ledger entries.
  * **C4:** Balance bounds (wallets $\ge 0$).
  * **C5:** Currency conservation ($\sum \text{Wallets} + \text{SystemAccount} = 0$).
  * **C6:** Gap-free sequence and hash chain continuity.
  * **C7:** Reversal validity (no double reversals, original exists).
* **FR-5.4:** Returns `CONSISTENT` or `INCONSISTENT` with detailed diagnostics.

---

## 6. Agent Skills Requirements (SK)

* **SK-1:** All skills adhere to `agentskills.io` standard with YAML frontmatter.
* **SK-2 (`local-check`):** Builds project, runs test suite, starts server, pings health endpoint, and reports pass/fail.
* **SK-3 (`simulate-transfer`):** End-to-end workflow executing wallet funding, transfers, idempotency replay, concurrent racing debits, and reconciliation verification.
* **SK-4 (`brain-ingest`):** Ingests structured research notes with citations into `brain/inbox/`.
* **SK-5 (`brain-refine`):** Organizes notes into PARA, merges duplicates, and validates `[[wikilinks]]`.
* **SK-6 (`brain-query`):** Queries vault for grounded decision making and spec authoring.
* **SK-7:** Skills are evaluated and results logged prior to each stage submission.

---

## 7. Process & Quality Requirements (PC & TQ)

* **PC-1 to PC-10:** PARA Second Brain maintenance, ADR records citing notes, Git branching with PR reviews, automated CI gates (tests, linting, skill validation, brain link checks), and human AI review policy.
* **TQ-1 to TQ-6:** Comprehensive test suite covering unit math, database transaction rollbacks, E2E HTTP flows, concurrency/race stress testing, CI test isolation, and integer precision enforcement.
