package org.ledgerapi.testing;

import java.time.Instant;
import org.ledgerapi.provider.ClockProvider;

/** Deterministic clock for tests (DC-4). */
public final class FixedClock implements ClockProvider {
  private Instant now;

  public FixedClock(Instant now) {
    this.now = now;
  }

  @Override
  public synchronized Instant now() {
    return now;
  }

  public synchronized void set(Instant instant) {
    now = instant;
  }
}
