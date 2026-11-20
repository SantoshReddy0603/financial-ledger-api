package org.ledgerapi.infrastructure.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ledgerapi.config.AppConfig.CurrencySpec;
import org.ledgerapi.infrastructure.sqlite.SchemaMigrator.IncompatibleDatabaseException;
import org.ledgerapi.provider.StorageException;
import org.ledgerapi.testing.FixedClock;
import org.ledgerapi.testing.SequentialIds;

class SchemaMigratorTest {
  private static final List<CurrencySpec> INR_USD =
      List.of(new CurrencySpec("INR", 2), new CurrencySpec("USD", 2));
  private static final String T0 = "2026-01-01T00:00:00.000Z";
  private static final String ZERO_HASH = "0".repeat(64);
  private static final String USER = "10000000-0000-4000-8000-000000000001";
  private static final String WALLET = "20000000-0000-4000-8000-000000000001";
  private static final String TX = "30000000-0000-4000-8000-000000000001";

  @TempDir Path dir;
  private SqliteDatabase database;
  private SchemaMigrator migrator;

  @BeforeEach
  void setUp() {
    database = open();
    migrator = new SchemaMigrator(new FixedClock(Instant.parse(T0)), new SequentialIds());
    migrator.migrate(database, INR_USD);
  }

  @AfterEach
  void tearDown() {
    database.close();
  }

  private SqliteDatabase open() {
    return SqliteDatabase.open(dir.resolve("ledger.db"), 1, Duration.ofSeconds(1));
  }

  @Test
  void createsSchemaVersionCurrenciesAndOneSystemAccountEach() {
    assertThat(query("SELECT schema_version FROM schema_info")).containsExactly("1");
    assertThat(query("SELECT code || ':' || minor_unit_exponent FROM currency ORDER BY code"))
        .containsExactly("INR:2", "USD:2");
    assertThat(
            query(
                "SELECT currency || ':' || balance FROM account WHERE kind = 'SYSTEM'"
                    + " ORDER BY currency"))
        .containsExactly("INR:0", "USD:0");
  }

  @Test
  void repeatedStartupChangesNothing() {
    List<String> before = query("SELECT account_id FROM account ORDER BY account_id");
    migrator.migrate(database, INR_USD);
    database.close();
    database = open();
    migrator.migrate(database, INR_USD);
    assertThat(query("SELECT account_id FROM account ORDER BY account_id")).isEqualTo(before);
  }

  @Test
  void addsNewlyConfiguredCurrencyWithSystemAccount() {
    migrator.migrate(
        database,
        List.of(
            new CurrencySpec("INR", 2), new CurrencySpec("USD", 2), new CurrencySpec("JPY", 0)));
    assertThat(query("SELECT currency FROM account WHERE kind = 'SYSTEM' ORDER BY currency"))
        .containsExactly("INR", "JPY", "USD");
  }

  @Test
  void rejectsRemovedCurrencyOrChangedExponentAtomically() {
    assertThatThrownBy(() -> migrator.migrate(database, List.of(new CurrencySpec("INR", 2))))
        .isInstanceOf(IncompatibleDatabaseException.class)
        .hasMessageContaining("USD");
    assertThatThrownBy(
            () ->
                migrator.migrate(
                    database,
                    List.of(
                        new CurrencySpec("INR", 3),
                        new CurrencySpec("USD", 2),
                        new CurrencySpec("EUR", 2))))
        .isInstanceOf(IncompatibleDatabaseException.class)
        .hasMessageContaining("INR");
    assertThat(query("SELECT code FROM currency ORDER BY code")).containsExactly("INR", "USD");
  }

  @Test
  void rejectsNewerSchemaVersion() {
    execute("UPDATE schema_info SET schema_version = 2");
    assertThatThrownBy(() -> migrator.migrate(database, INR_USD))
        .isInstanceOf(IncompatibleDatabaseException.class)
        .hasMessageContaining("newer");
  }

  @Test
  void ledgerRecordsAreAppendOnly() {
    seedFunding();
    for (String sql :
        List.of(
            "UPDATE ledger_transaction SET amount = 1",
            "DELETE FROM ledger_transaction",
            "UPDATE ledger_entry SET amount = 1",
            "DELETE FROM ledger_entry",
            "UPDATE idempotency_record SET response_status = 200",
            "DELETE FROM idempotency_record",
            "UPDATE currency SET minor_unit_exponent = 3",
            "DELETE FROM currency WHERE code = 'USD'",
            "UPDATE account SET currency = 'USD' WHERE account_id = '" + WALLET + "'",
            "UPDATE account SET kind = 'SYSTEM' WHERE account_id = '" + WALLET + "'",
            "DELETE FROM account WHERE account_id = '" + WALLET + "'",
            "UPDATE wallet SET currency = 'USD'",
            "DELETE FROM wallet",
            "UPDATE app_user SET display_name = 'x'",
            "DELETE FROM app_user")) {
      assertThatThrownBy(() -> execute(sql)).as(sql).isInstanceOf(StorageException.class);
    }
    assertThat(query("SELECT COUNT(*) FROM ledger_entry")).containsExactly("2");
  }

  @Test
  void cachedBalanceMayChangeWithinLimits() {
    seedFunding();
    execute("UPDATE account SET balance = 600 WHERE account_id = '" + WALLET + "'");
    assertThatThrownBy(
            () -> execute("UPDATE account SET balance = -1 WHERE account_id = '" + WALLET + "'"))
        .isInstanceOf(StorageException.class);
    assertThatThrownBy(
            () ->
                execute(
                    "UPDATE account SET balance = 9007199254740992 WHERE account_id = '"
                        + WALLET
                        + "'"))
        .isInstanceOf(StorageException.class);
    execute("UPDATE account SET balance = -500 WHERE kind = 'SYSTEM' AND currency = 'INR'");
  }

  @Test
  void entriesMustMatchTheirTransaction() {
    seedFunding();
    String tx2 = "30000000-0000-4000-8000-000000000002";
    execute(
        "INSERT INTO ledger_transaction (transaction_id, sequence, type, source_account_id,"
            + " destination_account_id, amount, currency, created_at, previous_hash, hash)"
            + " VALUES ('"
            + tx2
            + "', 2, 'FUNDING', '"
            + systemAccount("INR")
            + "', '"
            + WALLET
            + "', 10, 'INR', '"
            + T0
            + "', '"
            + "a".repeat(64)
            + "', '"
            + "b".repeat(64)
            + "')");
    assertThatThrownBy(() -> entry(3, tx2, WALLET, "CREDIT", 11))
        .isInstanceOf(StorageException.class);
    assertThatThrownBy(() -> entry(3, tx2, WALLET, "DEBIT", 10))
        .isInstanceOf(StorageException.class);
    entry(3, tx2, WALLET, "CREDIT", 10);
    assertThatThrownBy(() -> entry(4, tx2, WALLET, "CREDIT", 10))
        .isInstanceOf(StorageException.class);
  }

  @Test
  void schemaConstraintsRejectInvalidRows() {
    seedFunding();
    String base =
        "INSERT INTO ledger_transaction (transaction_id, sequence, type, source_account_id,"
            + " destination_account_id, amount, currency, created_at, previous_hash, hash) VALUES ";
    String sys = systemAccount("INR");
    assertThatThrownBy(
            () ->
                execute(
                    base + row("30000000-0000-4000-8000-000000000009", 2, sys, WALLET, "0", "c")))
        .isInstanceOf(StorageException.class);
    assertThatThrownBy(
            () ->
                execute(
                    base
                        + row(
                            "30000000-0000-4000-8000-000000000009",
                            2,
                            sys,
                            WALLET,
                            "1000000000001",
                            "c")))
        .isInstanceOf(StorageException.class);
    assertThatThrownBy(
            () ->
                execute(base + row("30000000-0000-4000-8000-000000000009", 2, sys, sys, "5", "c")))
        .isInstanceOf(StorageException.class);
    assertThatThrownBy(
            () ->
                execute(
                    base + row("30000000-0000-4000-8000-000000000009", 1, sys, WALLET, "5", "c")))
        .as("duplicate sequence")
        .isInstanceOf(StorageException.class);
    assertThatThrownBy(
            () ->
                execute(
                    "INSERT INTO account (account_id, kind, currency, balance, created_at) VALUES ('"
                        + "40000000-0000-4000-8000-000000000001', 'SYSTEM', 'INR', 0, '"
                        + T0
                        + "')"))
        .as("second system account")
        .isInstanceOf(StorageException.class);
    assertThatThrownBy(
            () ->
                execute(
                    "INSERT INTO wallet (wallet_id, user_id, currency, created_at) VALUES ('"
                        + sys
                        + "', '"
                        + USER
                        + "', 'INR', '"
                        + T0
                        + "')"))
        .as("wallet on a system account")
        .isInstanceOf(StorageException.class);
  }

  @Test
  void sessionsCanOnlyBeRevokedOnce() {
    seedFunding();
    String session = "50000000-0000-4000-8000-000000000001";
    execute(
        "INSERT INTO user_session (session_id, user_id, token_hash, created_at, expires_at)"
            + " VALUES ('"
            + session
            + "', '"
            + USER
            + "', '"
            + "d".repeat(64)
            + "', '"
            + T0
            + "', '"
            + T0
            + "')");
    assertThatThrownBy(() -> execute("UPDATE user_session SET expires_at = 'later'"))
        .isInstanceOf(StorageException.class);
    execute("UPDATE user_session SET revoked_at = '" + T0 + "'");
    assertThatThrownBy(() -> execute("UPDATE user_session SET revoked_at = 'again'"))
        .isInstanceOf(StorageException.class);
  }

  @Test
  void splitterKeepsTriggerBodiesWhole() {
    List<String> statements =
        SchemaMigrator.statements(
            "-- comment\nCREATE TABLE a (x INTEGER);\n\nCREATE TRIGGER t BEFORE DELETE ON a\n"
                + "BEGIN\n  SELECT RAISE(ABORT, 'no');\nEND;\nINSERT INTO a VALUES (1);\n");
    assertThat(statements).hasSize(3);
    assertThat(statements.get(1)).startsWith("CREATE TRIGGER").endsWith("END;");
    assertThatThrownBy(() -> SchemaMigrator.statements("CREATE TABLE a (x INTEGER)"))
        .isInstanceOf(IllegalStateException.class);
  }

  private void seedFunding() {
    String sys = systemAccount("INR");
    execute(
        "INSERT INTO app_user (user_id, login_name, display_name, password_hash, created_at)"
            + " VALUES ('"
            + USER
            + "', 'alice', 'Alice', 'hash', '"
            + T0
            + "')");
    execute(
        "INSERT INTO account (account_id, kind, currency, balance, created_at) VALUES ('"
            + WALLET
            + "', 'WALLET', 'INR', 500, '"
            + T0
            + "')");
    execute(
        "INSERT INTO wallet (wallet_id, user_id, currency, created_at) VALUES ('"
            + WALLET
            + "', '"
            + USER
            + "', 'INR', '"
            + T0
            + "')");
    execute(
        "INSERT INTO ledger_transaction (transaction_id, sequence, type, source_account_id,"
            + " destination_account_id, amount, currency, created_at, previous_hash, hash) VALUES "
            + row(TX, 1, sys, WALLET, "500", "e"));
    entry(1, TX, sys, "DEBIT", 500);
    entry(2, TX, WALLET, "CREDIT", 500);
    execute(
        "INSERT INTO idempotency_record (principal_id, idempotency_key, fingerprint,"
            + " response_status, response_body, transaction_id, created_at) VALUES ('admin:ops',"
            + " 'key-0123456789abcdef', '"
            + "f".repeat(64)
            + "', 201, '{}', '"
            + TX
            + "', '"
            + T0
            + "')");
  }

  private static String row(
      String id, int sequence, String source, String destination, String amount, String hashChar) {
    return "('"
        + id
        + "', "
        + sequence
        + ", 'FUNDING', '"
        + source
        + "', '"
        + destination
        + "', "
        + amount
        + ", 'INR', '"
        + T0
        + "', '"
        + ZERO_HASH
        + "', '"
        + hashChar.repeat(64)
        + "')";
  }

  private void entry(int number, String tx, String account, String direction, long amount) {
    execute(
        "INSERT INTO ledger_entry (entry_number, transaction_id, account_id, direction, amount,"
            + " currency, created_at) VALUES ("
            + number
            + ", '"
            + tx
            + "', '"
            + account
            + "', '"
            + direction
            + "', "
            + amount
            + ", 'INR', '"
            + T0
            + "')");
  }

  private String systemAccount(String currency) {
    return query(
            "SELECT account_id FROM account WHERE kind = 'SYSTEM' AND currency = '"
                + currency
                + "'")
        .get(0);
  }

  private void execute(String sql) {
    database.write(
        c -> {
          try (Statement s = c.createStatement()) {
            s.execute(sql);
          }
          return null;
        });
  }

  private List<String> query(String sql) {
    return database.read(c -> rows(c, sql));
  }

  private static List<String> rows(Connection c, String sql) throws SQLException {
    List<String> result = new ArrayList<>();
    try (Statement s = c.createStatement();
        ResultSet rs = s.executeQuery(sql)) {
      while (rs.next()) {
        result.add(rs.getString(1));
      }
    }
    return result;
  }
}
