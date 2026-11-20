package org.ledgerapi.http;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/** X-Request-Id handling (NFR-API-6). */
public final class RequestIds {
  public static final String HEADER = "X-Request-Id";
  public static final String ATTRIBUTE = "ledger.requestId";
  private static final Pattern VALID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

  private RequestIds() {}

  /**
   * Returns the single supplied value when it is valid, otherwise a generated UUID. Missing,
   * repeated and malformed headers are all replaced.
   */
  public static String resolve(List<String> suppliedValues) {
    if (suppliedValues != null && suppliedValues.size() == 1) {
      String value = suppliedValues.get(0);
      if (value != null && VALID.matcher(value).matches()) {
        return value;
      }
    }
    return UUID.randomUUID().toString();
  }
}
