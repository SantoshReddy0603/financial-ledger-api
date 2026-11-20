package org.ledgerapi.provider;

import java.time.Instant;

/** Source of the current time. Substituted by a fixed clock in tests (DC-4). */
@FunctionalInterface
public interface ClockProvider {
  Instant now();
}
