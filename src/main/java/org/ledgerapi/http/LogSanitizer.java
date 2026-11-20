package org.ledgerapi.http;

/** Keeps request-controlled text out of logs in a form that could forge or flood log lines. */
public final class LogSanitizer {
  static final int MAX_PATH_LENGTH = 256;

  private LogSanitizer() {}

  public static String path(String raw) {
    if (raw == null) {
      return "";
    }
    StringBuilder out = new StringBuilder(Math.min(raw.length(), MAX_PATH_LENGTH));
    for (int i = 0; i < raw.length() && out.length() < MAX_PATH_LENGTH; i++) {
      char c = raw.charAt(i);
      out.append(c >= 0x20 && c < 0x7f ? c : '?');
    }
    return out.toString();
  }
}
