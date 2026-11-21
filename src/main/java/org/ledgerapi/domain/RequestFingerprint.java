package org.ledgerapi.domain;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;

/**
 * Computes deterministic SHA-256 request fingerprints for money-moving operations (ADR-0004,
 * FR-2.5).
 */
public final class RequestFingerprint {
  private static final HexFormat HEX = HexFormat.of();

  private RequestFingerprint() {}

  /** Canonical JSON representation for a transfer request fingerprint. */
  public static String canonicalTransfer(
      String sourceWalletId,
      String destinationWalletId,
      long amount,
      String currency,
      String reference) {
    Objects.requireNonNull(sourceWalletId, "sourceWalletId");
    Objects.requireNonNull(destinationWalletId, "destinationWalletId");
    Objects.requireNonNull(currency, "currency");
    Map<String, Object> body = new HashMap<>();
    body.put("amount", amount);
    body.put("currency", currency);
    body.put("destinationWalletId", destinationWalletId);
    body.put("reference", reference);
    body.put("sourceWalletId", sourceWalletId);

    Map<String, Object> root = new HashMap<>();
    root.put("op", "TRANSFER");
    root.put("path", Map.of());
    root.put("body", body);
    return CanonicalJson.serialize(root);
  }

  /** SHA-256 lowercase hex fingerprint for a transfer request. */
  public static String forTransfer(
      String sourceWalletId,
      String destinationWalletId,
      long amount,
      String currency,
      String reference) {
    return sha256Hex(
        canonicalTransfer(sourceWalletId, destinationWalletId, amount, currency, reference));
  }

  /** Canonical JSON representation for a funding request fingerprint. */
  public static String canonicalFunding(String walletId, long amount, String currency) {
    Objects.requireNonNull(walletId, "walletId");
    Objects.requireNonNull(currency, "currency");
    Map<String, Object> path = Map.of("walletId", walletId);
    Map<String, Object> body = Map.of("amount", amount, "currency", currency);
    Map<String, Object> root = Map.of("op", "FUNDING", "path", path, "body", body);
    return CanonicalJson.serialize(root);
  }

  /** SHA-256 lowercase hex fingerprint for a funding request. */
  public static String forFunding(String walletId, long amount, String currency) {
    return sha256Hex(canonicalFunding(walletId, amount, currency));
  }

  /** Canonical JSON representation for a reversal request fingerprint. */
  public static String canonicalReversal(String transactionId, String reason) {
    Objects.requireNonNull(transactionId, "transactionId");
    Map<String, Object> body = new HashMap<>();
    body.put("reason", reason);
    Map<String, Object> root = new HashMap<>();
    root.put("op", "REVERSAL");
    root.put("path", Map.of("transactionId", transactionId));
    root.put("body", body);
    return CanonicalJson.serialize(root);
  }

  /** SHA-256 lowercase hex fingerprint for a reversal request. */
  public static String forReversal(String transactionId, String reason) {
    return sha256Hex(canonicalReversal(transactionId, reason));
  }

  static String sha256Hex(String canonicalJson) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(canonicalJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return HEX.formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 unavailable in JDK", e);
    }
  }
}
