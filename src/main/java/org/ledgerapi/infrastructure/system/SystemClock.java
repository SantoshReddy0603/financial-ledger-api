package org.ledgerapi.infrastructure.system;

import java.time.Instant;
import org.ledgerapi.provider.ClockProvider;

public final class SystemClock implements ClockProvider {
  @Override
  public Instant now() {
    return Instant.now();
  }
}
