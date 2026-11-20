package org.ledgerapi.infrastructure.sqlite;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.ledgerapi.config.AppConfig.CurrencySpec;
import org.ledgerapi.domain.Timestamps;
import org.ledgerapi.provider.ClockProvider;
import org.ledgerapi.provider.IdGenerator;

/**
 * Applies schema migrations and synchronises configured currencies in one write transaction
 * (FR-6.2, FR-6.4). Repeated startup is a no-op apart from adding newly configured currencies.
 */
public final class SchemaMigrator {
  public static final int CURRENT_SCHEMA_VERSION = 1;
  private static final List<String> MIGRATIONS = List.of("/db/V1__initial_schema.sql");

  private final ClockProvider clock;
  private final IdGenerator ids;

  public SchemaMigrator(ClockProvider clock, IdGenerator ids) {
    this.clock = clock;
    this.ids = ids;
  }

  /** Thrown when the database cannot be used with this build or configuration. */
  public static final class IncompatibleDatabaseException extends RuntimeException {
    public IncompatibleDatabaseException(String message) {
      super(message);
    }
  }

  public void migrate(SqliteDatabase database, List<CurrencySpec> currencies) {
    database.write(
        connection -> {
          int version = currentVersion(connection);
          if (version > CURRENT_SCHEMA_VERSION) {
            throw new IncompatibleDatabaseException(
                "Database schema version "
                    + version
                    + " is newer than supported version "
                    + CURRENT_SCHEMA_VERSION);
          }
          for (int next = version + 1; next <= CURRENT_SCHEMA_VERSION; next++) {
            for (String statement : statements(loadResource(MIGRATIONS.get(next - 1)))) {
              try (Statement s = connection.createStatement()) {
                s.execute(statement);
              }
            }
          }
          synchroniseCurrencies(connection, currencies);
          return null;
        });
  }

  private static int currentVersion(Connection connection) throws SQLException {
    try (Statement s = connection.createStatement();
        ResultSet rs =
            s.executeQuery(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'schema_info'")) {
      if (!rs.next()) {
        return 0;
      }
    }
    try (Statement s = connection.createStatement();
        ResultSet rs = s.executeQuery("SELECT schema_version FROM schema_info")) {
      if (!rs.next()) {
        throw new IncompatibleDatabaseException("schema_info has no version record");
      }
      return rs.getInt(1);
    }
  }

  private void synchroniseCurrencies(Connection connection, List<CurrencySpec> configured)
      throws SQLException {
    Map<String, Integer> stored = new HashMap<>();
    try (Statement s = connection.createStatement();
        ResultSet rs = s.executeQuery("SELECT code, minor_unit_exponent FROM currency")) {
      while (rs.next()) {
        stored.put(rs.getString(1), rs.getInt(2));
      }
    }
    Map<String, Integer> wanted = new HashMap<>();
    for (CurrencySpec spec : configured) {
      wanted.put(spec.code(), spec.minorUnitExponent());
    }
    for (Map.Entry<String, Integer> existing : stored.entrySet()) {
      Integer exponent = wanted.get(existing.getKey());
      if (exponent == null) {
        throw new IncompatibleDatabaseException(
            "Configured currencies omit " + existing.getKey() + ", which exists in the database");
      }
      if (!exponent.equals(existing.getValue())) {
        throw new IncompatibleDatabaseException(
            "Configured exponent for " + existing.getKey() + " differs from the database");
      }
    }
    String now = Timestamps.format(clock.now());
    for (CurrencySpec spec : configured) {
      if (stored.containsKey(spec.code())) {
        continue;
      }
      try (PreparedStatement insertCurrency =
              connection.prepareStatement(
                  "INSERT INTO currency (code, minor_unit_exponent) VALUES (?, ?)");
          PreparedStatement insertAccount =
              connection.prepareStatement(
                  "INSERT INTO account (account_id, kind, currency, balance, created_at)"
                      + " VALUES (?, 'SYSTEM', ?, 0, ?)")) {
        insertCurrency.setString(1, spec.code());
        insertCurrency.setInt(2, spec.minorUnitExponent());
        insertCurrency.executeUpdate();
        insertAccount.setString(1, ids.newId());
        insertAccount.setString(2, spec.code());
        insertAccount.setString(3, now);
        insertAccount.executeUpdate();
      }
    }
  }

  private static String loadResource(String name) {
    try (InputStream in = SchemaMigrator.class.getResourceAsStream(name)) {
      if (in == null) {
        throw new IllegalStateException("Missing migration resource " + name);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Splits a migration script into statements. A statement ends with ';' at end of line, except
   * inside a CREATE TRIGGER, which ends at a line reading 'END;'. Lines starting with '--' are
   * comments.
   */
  static List<String> statements(String script) {
    List<String> result = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inTrigger = false;
    for (String rawLine : script.split("\\R")) {
      String line = rawLine.strip();
      if (line.isEmpty() || line.startsWith("--")) {
        continue;
      }
      if (current.isEmpty()) {
        inTrigger = line.toUpperCase(Locale.ROOT).startsWith("CREATE TRIGGER");
      }
      current.append(rawLine).append('\n');
      boolean ends = inTrigger ? line.equalsIgnoreCase("END;") : line.endsWith(";");
      if (ends) {
        result.add(current.toString().strip());
        current.setLength(0);
        inTrigger = false;
      }
    }
    if (!current.isEmpty()) {
      throw new IllegalStateException("Migration script ends with an unterminated statement");
    }
    return result;
  }
}
