package org.ledgerapi.infrastructure.system;

import java.util.UUID;
import org.ledgerapi.provider.IdGenerator;

/** {@link UUID#randomUUID()} is a version 4 UUID from a cryptographically strong generator. */
public final class RandomUuidGenerator implements IdGenerator {
  @Override
  public String newId() {
    return UUID.randomUUID().toString();
  }
}
