package org.ledgerapi.provider;

/** SHA-256 hashing used for fingerprints, the transaction hash chain and credential hashes. */
@FunctionalInterface
public interface HashProvider {
  /** Returns the lowercase 64-character hexadecimal SHA-256 digest of the given bytes. */
  String sha256Hex(byte[] input);
}
