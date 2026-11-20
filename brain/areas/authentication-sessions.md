---
title: "Area: Authentication and Sessions"
date: 2026-11-20
tags: [area, security, authentication, sessions]
status: area
---

# Area: Authentication and Sessions

Responsible for user registration, password storage, bearer sessions, administrator API keys and authentication throttling (FR-7, NFR-SEC-3 to NFR-SEC-5).

## Core Mechanisms
* Argon2id password hashes (19 MiB, t=2, p=1 minimum).
* Opaque 256-bit bearer tokens; only SHA-256 hashes stored; 60-minute default lifetime; single and all-session revocation.
* Administrator `X-API-Key`: SHA-256 hashes in configuration mapped to stable principal IDs, so idempotency history survives key rotation.
* Throttling: 5 logins per login name and 20 per IP per 15 minutes; 10 registrations per IP per 15 minutes.

## Related Resources
* [[resources/argon2id-password-hashing-and-session-tokens|Argon2id Password Hashing and Opaque Session Tokens]]
* [[resources/rest-api-design-financial-services|RESTful API Design for Financial Services]]
* [[areas/security-validation|Area: Security, Decimal Safety and Input Validation]]
