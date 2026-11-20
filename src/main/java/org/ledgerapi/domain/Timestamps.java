package org.ledgerapi.domain;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/** UTC RFC 3339 timestamps with exactly three fractional digits and a Z suffix (NFR-API-4). */
public final class Timestamps {
  private static final DateTimeFormatter FORMAT =
      DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

  private Timestamps() {}

  public static String format(Instant instant) {
    return FORMAT.format(instant.truncatedTo(ChronoUnit.MILLIS));
  }

  public static Instant parse(String text) {
    return Instant.from(FORMAT.parse(text));
  }
}
