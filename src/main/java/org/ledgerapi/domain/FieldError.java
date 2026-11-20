package org.ledgerapi.domain;

import java.util.Objects;

/** One entry of the error {@code details} array: the offending input and the rule it failed. */
public record FieldError(String field, String rule) {
  public FieldError {
    Objects.requireNonNull(field, "field");
    Objects.requireNonNull(rule, "rule");
  }
}
