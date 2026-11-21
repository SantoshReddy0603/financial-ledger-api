package org.ledgerapi.domain;

import java.util.List;
import java.util.regex.Pattern;

/** Strict identifier validators for lowercase RFC 4122 UUID v4 and idempotency keys (NFR-API-2). */
public final class Identifiers {
  private static final Pattern UUID_V4_LOWER =
      Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
  private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("^[A-Za-z0-9_-]{16,64}$");

  private Identifiers() {}

  /** Validates that {@code value} is a canonical lowercase RFC 4122 UUID v4 string. */
  public static String requireUuidV4(String value, String fieldName) {
    if (value == null || !UUID_V4_LOWER.matcher(value).matches()) {
      throw new LedgerException(
          ErrorCode.MALFORMED_ID,
          "Identifier must be a lowercase RFC 4122 UUID v4.",
          List.of(new FieldError(fieldName, "Invalid UUID v4")));
    }
    return value;
  }

  /** Validates a mandatory {@code Idempotency-Key} header value (FR-2.2). */
  public static String requireIdempotencyKey(String value) {
    if (value == null || value.isEmpty()) {
      throw new LedgerException(
          ErrorCode.MISSING_IDEMPOTENCY_KEY, "Idempotency-Key header is required.");
    }
    if (!IDEMPOTENCY_KEY.matcher(value).matches()) {
      throw new LedgerException(
          ErrorCode.INVALID_IDEMPOTENCY_KEY,
          "Idempotency-Key must be 16 to 64 ASCII alphanumeric, hyphen, or underscore characters.");
    }
    return value;
  }
}
