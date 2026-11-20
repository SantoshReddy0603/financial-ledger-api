---
title: "Hexagonal Architecture and Provider Ports for Testable Services"
date: 2026-11-20
tags: [architecture, hexagonal, ports-and-adapters, testing, research]
source: "Alistair Cockburn, 'Hexagonal Architecture' (2005); Robert C. Martin, 'Clean Architecture' (2017), ch. 22"
status: resource
---

# Hexagonal Architecture and Provider Ports for Testable Services

## 1. Summary & Core Concept
Hexagonal architecture (ports and adapters) keeps business rules in the centre and pushes every external concern (HTTP, database, clock, randomness, hashing) to the edge. The core defines **ports** (interfaces it needs); **adapters** implement them. Dependencies point inward only: HTTP and SQLite know about the domain, never the reverse.

The payoff for a ledger is deterministic testing. With a fake clock, sequential ID generator and in-memory or temp-file storage, a test can assert an exact transaction hash, an exact `createdAt`, or inject a failure right before commit.

## 2. Key Architectural Mechanisms
* **Layers:** `domain` (pure values and rules, no I/O) → `application` (use cases, transaction boundaries) → `provider` (port interfaces) ← `infrastructure` (SQLite, system clock, UUID, SHA-256 adapters) and `http` (Javalin adapter).
* **Composition root:** one place (`LedgerApplication`) wires real adapters; tests wire fakes through the same constructor.
* **Ports in this project:** `ClockProvider`, `IdGenerator`, `HashProvider`, `StorageProbe`, plus repository ports per aggregate and a `TransactionRunner` that executes a unit of work atomically.
* **Fault injection:** a test adapter that throws at a chosen persistence boundary proves rollback leaves no partial state (TQ-2).
* **Enforcement:** package-level rules (domain imports nothing from `http`/`infrastructure`) can be checked by a test that scans imports.

## 3. Pitfalls & Failure Modes
* Leaking JDBC types (`Connection`, `ResultSet`) into use cases makes them untestable without SQLite.
* Over-abstraction: a port per SQL statement. Keep ports at aggregate/use-case granularity.
* Calling `Instant.now()` or `UUID.randomUUID()` directly inside the domain breaks reproducible hash-chain tests.
* Putting transaction boundaries in HTTP handlers: business invariants then depend on the web framework.

## 4. Relevance to Financial Ledger API
* **DC-4** (providers for storage, clock, IDs, hashing, HTTP transport).
* **NFR-MNT-1/2** (85% coverage on domain and application; achievable because those layers are I/O-free).
* **TQ-1, TQ-2** (unit tests with fakes; fault injection before each persistence boundary).
* Decision recorded in `docs/adr/0002-layered-hexagonal-architecture.md`.

## 5. Wikilinks
* [[index]]
* [[areas/ledger-architecture|Area: Double-Entry Ledger Architecture]]
* [[resources/acid-transactions-in-relational-databases|ACID Transactions in Relational Databases]]
