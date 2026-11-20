---
title: "Argon2id Password Hashing and Opaque Session Tokens"
date: 2026-11-20
tags: [security, authentication, argon2, sessions, research]
source: "RFC 9106 (Argon2); OWASP Password Storage Cheat Sheet; OWASP Session Management Cheat Sheet"
status: resource
---

# Argon2id Password Hashing and Opaque Session Tokens

## 1. Summary & Core Concept
Passwords must be stored with a slow, memory-hard, salted hash so a stolen database cannot be brute-forced cheaply. Argon2id (RFC 9106) is the current recommendation: it mixes data-independent and data-dependent memory access, resisting both GPU cracking and side-channel attacks. OWASP's minimum profile is **19 MiB memory, 2 iterations, parallelism 1**.

Session credentials should be **opaque random tokens** (at least 256 bits from a CSPRNG) rather than self-describing tokens, because a server-side session row can be revoked instantly. Only a **SHA-256 hash of the token** is stored; a database leak then does not hand out live sessions. A fast hash is enough here because the token already has 256 bits of entropy.

## 2. Key Architectural Mechanisms
* Encoded hash format: `$argon2id$v=19$m=19456,t=2,p=1$<salt-b64>$<hash-b64>`. The parameters are stored with the hash, so the work factor can be raised later without breaking old hashes (rehash on next login).
* Salt: 16 random bytes per password. Output: 32 bytes.
* Verification compares hashes in constant time (`MessageDigest.isEqual`).
* Login with an unknown login name still runs one Argon2id verification against a dummy hash, so response time does not reveal whether the account exists.
* Token issue: `SecureRandom` 32 bytes, base64url without padding (43 chars), returned once; store `sha256(token)` with `expiresAt` (default 60 minutes) and nullable `revokedAt`.
* Lookup: hash the presented bearer token, select by `token_hash` (unique index), reject if revoked or `expiresAt <= now`.
* Logout revokes one row; logout-all revokes every active row for the user in one transaction.
* Throttling: counters per login name and per client IP over a sliding 15-minute window bound online guessing; exceeding returns 429 with `Retry-After`.
* Java: BouncyCastle `Argon2BytesGenerator` (pure Java, no native library) keeps the build portable across Windows/Linux.

## 3. Pitfalls & Failure Modes
* Using SHA-256 or bcrypt-with-low-cost for passwords: cheap to crack offline.
* Storing raw tokens: a read-only DB leak becomes account takeover.
* Different error messages for "no such user" and "wrong password": account enumeration.
* Logging the `Authorization` header or token in request logs.
* Argon2 memory per hash × concurrent logins can exhaust heap; bound concurrent verifications or rely on throttling.
* Trusting `X-Forwarded-For` from any client defeats per-IP throttling; only honour it from configured trusted proxies.

## 4. Relevance to Financial Ledger API
* **FR-7.1–FR-7.6** (registration, login, logout, logout-all, expiry, throttling).
* **NFR-SEC-3, NFR-SEC-4, NFR-SEC-5** (credential storage, Argon2id settings, token entropy).
* **DC-4**: hashing and randomness sit behind providers so tests can run fast with fixed values.
* Decision recorded in `docs/adr/0008-authentication-and-sessions.md`.

## 5. Wikilinks
* [[index]]
* [[areas/authentication-sessions|Area: Authentication and Sessions]]
* [[areas/security-validation|Area: Security, Decimal Safety and Input Validation]]
* [[resources/rest-api-design-financial-services|RESTful API Design for Financial Services]]
