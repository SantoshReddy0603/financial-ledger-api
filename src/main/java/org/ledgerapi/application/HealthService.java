package org.ledgerapi.application;

import java.util.Objects;
import org.ledgerapi.provider.StorageProbe;

/** Read-only service health probe (FR-6.1). */
public final class HealthService {
  private final StorageProbe storage;
  private final String version;

  public HealthService(StorageProbe storage, String version) {
    this.storage = Objects.requireNonNull(storage, "storage");
    this.version = Objects.requireNonNull(version, "version");
  }

  /** Result of one probe. Contains no configuration values. */
  public record HealthReport(String version, boolean databaseReachable) {}

  public HealthReport check() {
    boolean reachable;
    try {
      reachable = storage.isReachable();
    } catch (RuntimeException e) {
      reachable = false;
    }
    return new HealthReport(version, reachable);
  }
}
