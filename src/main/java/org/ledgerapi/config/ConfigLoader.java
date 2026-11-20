package org.ledgerapi.config;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.ledgerapi.config.AppConfig.AdminKey;
import org.ledgerapi.config.AppConfig.CurrencySpec;
import org.ledgerapi.config.AppConfig.PasswordHashSettings;

/**
 * Builds {@link AppConfig} from environment variables. Every setting has a documented default
 * except administrator keys. Invalid values stop startup; error messages never echo values.
 */
public final class ConfigLoader {
  public static final String LISTEN_ADDRESS = "LEDGER_LISTEN_ADDRESS";
  public static final String PORT = "LEDGER_PORT";
  public static final String DB_PATH = "LEDGER_DB_PATH";
  public static final String CURRENCIES = "LEDGER_CURRENCIES";
  public static final String SESSION_LIFETIME_MINUTES = "LEDGER_SESSION_LIFETIME_MINUTES";
  public static final String ARGON2_MEMORY_KIB = "LEDGER_ARGON2_MEMORY_KIB";
  public static final String ARGON2_ITERATIONS = "LEDGER_ARGON2_ITERATIONS";
  public static final String ARGON2_PARALLELISM = "LEDGER_ARGON2_PARALLELISM";
  public static final String ADMIN_KEYS = "LEDGER_ADMIN_KEYS";
  public static final String TRUSTED_PROXIES = "LEDGER_TRUSTED_PROXIES";
  public static final String READ_POOL_SIZE = "LEDGER_READ_POOL_SIZE";
  public static final String LOCK_TIMEOUT_MS = "LEDGER_LOCK_TIMEOUT_MS";

  static final String DEFAULT_CURRENCIES = "INR:2,USD:2,EUR:2";
  static final int MIN_ARGON2_MEMORY_KIB = 19 * 1024;
  static final int MIN_ARGON2_ITERATIONS = 2;
  static final int MIN_ARGON2_PARALLELISM = 1;

  private static final Pattern CURRENCY_CODE = Pattern.compile("[A-Z]{3}");
  private static final Pattern PRINCIPAL_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");
  private static final Pattern SHA256_HEX = Pattern.compile("[0-9a-f]{64}");

  private ConfigLoader() {}

  public static AppConfig fromEnvironment() {
    return load(System.getenv());
  }

  public static AppConfig load(Map<String, String> env) {
    String listenAddress = text(env, LISTEN_ADDRESS, "127.0.0.1");
    int port = integer(env, PORT, 8080, 0, 65_535);
    Path databasePath = path(env, DB_PATH, "data/ledger.db");
    List<CurrencySpec> currencies = currencies(text(env, CURRENCIES, DEFAULT_CURRENCIES));
    Duration sessionLifetime =
        Duration.ofMinutes(integer(env, SESSION_LIFETIME_MINUTES, 60, 1, 24 * 60));
    PasswordHashSettings passwordHash =
        new PasswordHashSettings(
            integer(
                env, ARGON2_MEMORY_KIB, MIN_ARGON2_MEMORY_KIB, MIN_ARGON2_MEMORY_KIB, 4_194_304),
            integer(env, ARGON2_ITERATIONS, MIN_ARGON2_ITERATIONS, MIN_ARGON2_ITERATIONS, 64),
            integer(env, ARGON2_PARALLELISM, MIN_ARGON2_PARALLELISM, MIN_ARGON2_PARALLELISM, 64));
    List<AdminKey> adminKeys = adminKeys(env.getOrDefault(ADMIN_KEYS, ""));
    List<String> trustedProxies = list(env.getOrDefault(TRUSTED_PROXIES, ""));
    int readPoolSize = integer(env, READ_POOL_SIZE, 8, 1, 64);
    Duration lockTimeout = Duration.ofMillis(integer(env, LOCK_TIMEOUT_MS, 5_000, 1, 60_000));

    return new AppConfig(
        listenAddress,
        port,
        databasePath,
        currencies,
        sessionLifetime,
        passwordHash,
        adminKeys,
        trustedProxies,
        readPoolSize,
        lockTimeout);
  }

  private static String text(Map<String, String> env, String name, String defaultValue) {
    String value = env.get(name);
    if (value == null) {
      return defaultValue;
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new ConfigException(name + " must not be blank");
    }
    return trimmed;
  }

  private static int integer(
      Map<String, String> env, String name, int defaultValue, int min, int max) {
    String value = env.get(name);
    if (value == null) {
      return defaultValue;
    }
    int parsed;
    try {
      parsed = Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      throw new ConfigException(name + " must be an integer");
    }
    if (parsed < min || parsed > max) {
      throw new ConfigException(name + " must be between " + min + " and " + max);
    }
    return parsed;
  }

  private static Path path(Map<String, String> env, String name, String defaultValue) {
    try {
      return Path.of(text(env, name, defaultValue));
    } catch (InvalidPathException e) {
      throw new ConfigException(name + " is not a valid path");
    }
  }

  private static List<CurrencySpec> currencies(String value) {
    List<CurrencySpec> result = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    for (String item : list(value)) {
      String[] parts = item.split(":", -1);
      if (parts.length != 2 || !CURRENCY_CODE.matcher(parts[0]).matches()) {
        throw new ConfigException(CURRENCIES + " entries must look like CODE:EXPONENT");
      }
      int exponent;
      try {
        exponent = Integer.parseInt(parts[1]);
      } catch (NumberFormatException e) {
        throw new ConfigException(CURRENCIES + " exponents must be integers");
      }
      if (exponent < 0 || exponent > 18) {
        throw new ConfigException(CURRENCIES + " exponents must be between 0 and 18");
      }
      if (!seen.add(parts[0])) {
        throw new ConfigException(CURRENCIES + " must not repeat a currency code");
      }
      result.add(new CurrencySpec(parts[0], exponent));
    }
    if (result.isEmpty()) {
      throw new ConfigException(CURRENCIES + " must configure at least one currency");
    }
    return result;
  }

  private static List<AdminKey> adminKeys(String value) {
    List<AdminKey> result = new ArrayList<>();
    Set<String> hashes = new HashSet<>();
    for (String item : list(value)) {
      int separator = item.indexOf('=');
      if (separator <= 0) {
        throw new ConfigException(ADMIN_KEYS + " entries must look like principalId=sha256hex");
      }
      String principal = item.substring(0, separator);
      String hash = item.substring(separator + 1);
      if (!PRINCIPAL_ID.matcher(principal).matches()) {
        throw new ConfigException(ADMIN_KEYS + " contains an invalid principal identifier");
      }
      if (!SHA256_HEX.matcher(hash).matches()) {
        throw new ConfigException(
            ADMIN_KEYS + " key hashes must be 64 lowercase hex characters (SHA-256)");
      }
      if (!hashes.add(hash)) {
        throw new ConfigException(ADMIN_KEYS + " must not repeat a key hash");
      }
      result.add(new AdminKey(principal, hash));
    }
    return result;
  }

  private static List<String> list(String value) {
    List<String> items = new ArrayList<>();
    for (String raw : value.split(",")) {
      String item = raw.trim();
      if (!item.isEmpty()) {
        items.add(item);
      }
    }
    return items;
  }
}
