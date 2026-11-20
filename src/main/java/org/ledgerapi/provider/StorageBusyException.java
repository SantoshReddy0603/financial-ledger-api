package org.ledgerapi.provider;

/** The database write lock was not obtained within the configured timeout (FR-3.5). */
public class StorageBusyException extends RuntimeException {
  public StorageBusyException(String message) {
    super(message);
  }

  public StorageBusyException(String message, Throwable cause) {
    super(message, cause);
  }
}
