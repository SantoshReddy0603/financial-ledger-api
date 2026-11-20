package org.ledgerapi.provider;

/** Read-only reachability check of the persistent store, used by the health endpoint (FR-6.1). */
@FunctionalInterface
public interface StorageProbe {
  boolean isReachable();
}
