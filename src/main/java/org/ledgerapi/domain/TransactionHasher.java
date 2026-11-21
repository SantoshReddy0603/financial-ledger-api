package org.ledgerapi.domain;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Computes tamper-evident SHA-256 transaction hashes chaining each committed transaction to the
 * previous sequence (ADR-0004, FR-4.5, FR-5.3 C6).
 */
public final class TransactionHasher {
  public static final String GENESIS_PREVIOUS_HASH = "0".repeat(64);

  private TransactionHasher() {}

  /** Entry fields included in the canonical transaction hash payload. */
  public record EntryInput(
      long entryNumber,
      String accountId,
      String direction,
      long amount,
      String currency,
      String createdAt) {
    public EntryInput {
      Objects.requireNonNull(accountId, "accountId");
      Objects.requireNonNull(direction, "direction");
      Objects.requireNonNull(currency, "currency");
      Objects.requireNonNull(createdAt, "createdAt");
    }
  }

  /**
   * Complete transaction fields included in the canonical hash payload (excludes derived fields).
   */
  public record TransactionInput(
      String transactionId,
      long sequence,
      String type,
      String sourceAccountId,
      String destinationAccountId,
      long amount,
      String currency,
      String reference,
      String reason,
      String reversalOf,
      String createdAt,
      String previousHash,
      List<EntryInput> entries) {
    public TransactionInput {
      Objects.requireNonNull(transactionId, "transactionId");
      Objects.requireNonNull(type, "type");
      Objects.requireNonNull(sourceAccountId, "sourceAccountId");
      Objects.requireNonNull(destinationAccountId, "destinationAccountId");
      Objects.requireNonNull(currency, "currency");
      Objects.requireNonNull(createdAt, "createdAt");
      Objects.requireNonNull(previousHash, "previousHash");
      Objects.requireNonNull(entries, "entries");
      entries = List.copyOf(entries);
    }
  }

  /**
   * Produces the canonical JSON payload {@code H} sorted by key with entries ordered by
   * entryNumber.
   */
  public static String canonicalPayload(TransactionInput tx) {
    Objects.requireNonNull(tx, "tx");
    List<Map<String, Object>> sortedEntries =
        tx.entries().stream()
            .sorted(Comparator.comparingLong(EntryInput::entryNumber))
            .map(TransactionHasher::entryMap)
            .toList();

    Map<String, Object> payload = new HashMap<>();
    payload.put("amount", tx.amount());
    payload.put("createdAt", tx.createdAt());
    payload.put("currency", tx.currency());
    payload.put("destinationAccountId", tx.destinationAccountId());
    payload.put("entries", sortedEntries);
    payload.put("previousHash", tx.previousHash());
    payload.put("reason", tx.reason());
    payload.put("reference", tx.reference());
    payload.put("reversalOf", tx.reversalOf());
    payload.put("sequence", tx.sequence());
    payload.put("sourceAccountId", tx.sourceAccountId());
    payload.put("transactionId", tx.transactionId());
    payload.put("type", tx.type());
    return CanonicalJson.serialize(payload);
  }

  /** Computes {@code lowercase-hex(SHA-256(UTF-8(canonical(H))))}. */
  public static String hash(TransactionInput tx) {
    return RequestFingerprint.sha256Hex(canonicalPayload(tx));
  }

  private static Map<String, Object> entryMap(EntryInput entry) {
    Map<String, Object> map = new HashMap<>();
    map.put("accountId", entry.accountId());
    map.put("amount", entry.amount());
    map.put("createdAt", entry.createdAt());
    map.put("currency", entry.currency());
    map.put("direction", entry.direction());
    map.put("entryNumber", entry.entryNumber());
    return map;
  }
}
