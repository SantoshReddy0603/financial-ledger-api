package org.ledgerapi.provider;

/** An unexpected persistence failure. Never exposed to clients beyond INTERNAL_ERROR. */
public class StorageException extends RuntimeException {
  public StorageException(String message, Throwable cause) {
    super(message, cause);
  }
}
