package org.ledgerapi.domain;

import java.util.List;
import java.util.Objects;

/**
 * A rejected request. Carries a stable {@link ErrorCode}, a client-safe message and optional field
 * details. Messages must never contain SQL, paths, stack traces or credential values.
 */
public class LedgerException extends RuntimeException {
  private final ErrorCode code;
  private final List<FieldError> details;

  public LedgerException(ErrorCode code, String message) {
    this(code, message, List.of());
  }

  public LedgerException(ErrorCode code, String message, List<FieldError> details) {
    super(message);
    this.code = Objects.requireNonNull(code, "code");
    this.details = List.copyOf(details);
  }

  public ErrorCode code() {
    return code;
  }

  public List<FieldError> details() {
    return details;
  }
}
