package org.ledgerapi.application;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Build metadata filtered into the jar by Maven. */
public final class BuildInfo {
  static final String UNKNOWN = "unknown";

  private BuildInfo() {}

  public static String version() {
    return versionFrom(BuildInfo.class.getResourceAsStream("/ledger-version.properties"));
  }

  static String versionFrom(InputStream in) {
    if (in == null) {
      return UNKNOWN;
    }
    try (in) {
      Properties properties = new Properties();
      properties.load(in);
      String value = properties.getProperty("version", "").trim();
      return value.isEmpty() || value.startsWith("${") ? UNKNOWN : value;
    } catch (IOException e) {
      return UNKNOWN;
    }
  }
}
