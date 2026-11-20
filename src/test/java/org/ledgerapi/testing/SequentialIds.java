package org.ledgerapi.testing;

import java.util.concurrent.atomic.AtomicLong;
import org.ledgerapi.provider.IdGenerator;

/** Deterministic, valid lowercase UUID v4 identifiers for tests (DC-4). */
public final class SequentialIds implements IdGenerator {
  private final AtomicLong next = new AtomicLong(1);

  @Override
  public String newId() {
    return String.format("00000000-0000-4000-8000-%012x", next.getAndIncrement());
  }
}
