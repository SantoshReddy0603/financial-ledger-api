package org.ledgerapi.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class TimestampsTest {
  @Test
  void formatsUtcWithExactlyThreeFractionDigits() {
    assertThat(Timestamps.format(Instant.parse("2026-01-02T03:04:05Z")))
        .isEqualTo("2026-01-02T03:04:05.000Z");
    assertThat(Timestamps.format(Instant.parse("2026-01-02T03:04:05.123456789Z")))
        .isEqualTo("2026-01-02T03:04:05.123Z");
  }

  @Test
  void parseIsTheInverseOfFormat() {
    Instant instant = Instant.parse("2026-11-20T10:15:30.042Z");
    assertThat(Timestamps.parse(Timestamps.format(instant))).isEqualTo(instant);
  }
}
