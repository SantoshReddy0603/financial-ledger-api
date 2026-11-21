package org.ledgerapi.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class IdentifiersTest {
  @Test
  void validatesLowercaseUuidV4() {
    String valid = "550e8400-e29b-41d4-a716-446655440000";
    assertThat(Identifiers.requireUuidV4(valid, "walletId")).isEqualTo(valid);

    // Uppercase, wrong version (v1), wrong variant, null
    for (String bad :
        new String[] {
          null,
          "",
          "550E8400-E29B-41D4-A716-446655440000",
          "550e8400-e29b-11d4-a716-446655440000",
          "550e8400-e29b-41d4-c716-446655440000",
          "not-a-uuid"
        }) {
      assertThatThrownBy(() -> Identifiers.requireUuidV4(bad, "walletId"))
          .isInstanceOf(LedgerException.class)
          .extracting(e -> ((LedgerException) e).code())
          .isEqualTo(ErrorCode.MALFORMED_ID);
    }
  }

  @Test
  void validatesIdempotencyKey() {
    String valid = "transfer_key-0123456789";
    assertThat(Identifiers.requireIdempotencyKey(valid)).isEqualTo(valid);

    for (String missing : new String[] {null, ""}) {
      assertThatThrownBy(() -> Identifiers.requireIdempotencyKey(missing))
          .isInstanceOf(LedgerException.class)
          .extracting(e -> ((LedgerException) e).code())
          .isEqualTo(ErrorCode.MISSING_IDEMPOTENCY_KEY);
    }

    for (String bad :
        new String[] {
          "too-short-15chr", "a".repeat(65), "contains spaces 123456", "bad!char@key#123456"
        }) {
      assertThatThrownBy(() -> Identifiers.requireIdempotencyKey(bad))
          .isInstanceOf(LedgerException.class)
          .extracting(e -> ((LedgerException) e).code())
          .isEqualTo(ErrorCode.INVALID_IDEMPOTENCY_KEY);
    }
  }
}
