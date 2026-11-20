package org.ledgerapi.config;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/** Validated runtime configuration (SRS section 4.6). Holds no raw secrets. */
public record AppConfig(
    String listenAddress,
    int port,
    Path databasePath,
    List<CurrencySpec> currencies,
    Duration sessionLifetime,
    PasswordHashSettings passwordHash,
    List<AdminKey> adminKeys,
    List<String> trustedProxies,
    int readPoolSize,
    Duration lockTimeout) {

  public AppConfig {
    currencies = List.copyOf(currencies);
    adminKeys = List.copyOf(adminKeys);
    trustedProxies = List.copyOf(trustedProxies);
  }

  /** A configured ISO 4217 currency and its minor-unit exponent (BR-2). */
  public record CurrencySpec(String code, int minorUnitExponent) {}

  /** Argon2id work settings (NFR-SEC-4). */
  public record PasswordHashSettings(int memoryKib, int iterations, int parallelism) {}

  /** A stable administrator principal and the SHA-256 hash of its current raw key. */
  public record AdminKey(String principalId, String keySha256Hex) {}
}
