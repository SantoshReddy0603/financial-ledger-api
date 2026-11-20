package org.ledgerapi.config;

/** Invalid configuration. Messages name the setting, never its value. */
public class ConfigException extends RuntimeException {
  public ConfigException(String message) {
    super(message);
  }
}
