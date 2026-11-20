package org.ledgerapi.provider;

/** Generates lowercase hyphenated UUID version 4 identifiers (NFR-API-2). */
@FunctionalInterface
public interface IdGenerator {
  String newId();
}
