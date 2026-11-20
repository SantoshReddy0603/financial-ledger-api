package org.ledgerapi.infrastructure.sqlite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ledgerapi.provider.StorageBusyException;
import org.ledgerapi.provider.StorageException;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

class SqliteDatabaseTest {
  @TempDir Path dir;
  private SqliteDatabase database;

  @BeforeEach
  void open() {
    database = SqliteDatabase.open(dir.resolve("nested/ledger.db"), 2, Duration.ofMillis(200));
    database.write(
        c -> {
          try (Statement s = c.createStatement()) {
            s.execute("CREATE TABLE t (v INTEGER NOT NULL)");
          }
          return null;
        });
  }

  @AfterEach
  void close() {
    database.close();
  }

  @Test
  void everyConnectionUsesTheRequiredPragmas() {
    assertThat(writePragma("journal_mode")).isEqualToIgnoringCase("wal");
    assertThat(writePragma("synchronous")).isEqualTo("2");
    assertThat(writePragma("foreign_keys")).isEqualTo("1");
    assertThat(writePragma("busy_timeout")).isEqualTo("5000");
    assertThat(readPragma("journal_mode")).isEqualToIgnoringCase("wal");
    assertThat(readPragma("synchronous")).isEqualTo("2");
    assertThat(readPragma("foreign_keys")).isEqualTo("1");
    assertThat(readPragma("busy_timeout")).isEqualTo("5000");
  }

  @Test
  void commitsSuccessfulWork() {
    database.write(c -> insert(c, 1));
    assertThat(count()).isEqualTo(1);
  }

  @Test
  void rollsBackEverythingWhenWorkFails() {
    assertThatThrownBy(
            () ->
                database.write(
                    c -> {
                      insert(c, 1);
                      throw new IllegalStateException("fail after first insert");
                    }))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                database.write(
                    c -> {
                      insert(c, 2);
                      try (Statement s = c.createStatement()) {
                        s.execute("INSERT INTO t (v) VALUES (NULL)");
                      }
                      return null;
                    }))
        .isInstanceOf(StorageException.class);
    assertThat(count()).isZero();
  }

  @Test
  void readersCannotWrite() {
    assertThatThrownBy(() -> database.read(c -> insert(c, 1))).isInstanceOf(StorageException.class);
  }

  @Test
  void writeLockTimeoutBecomesStorageBusy() throws Exception {
    CountDownLatch holding = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<?> holder =
          executor.submit(
              () ->
                  database.write(
                      c -> {
                        holding.countDown();
                        await(release);
                        return null;
                      }));
      assertThat(holding.await(5, TimeUnit.SECONDS)).isTrue();
      long started = System.nanoTime();
      assertThatThrownBy(() -> database.write(c -> insert(c, 1)))
          .isInstanceOf(StorageBusyException.class);
      assertThat(Duration.ofNanos(System.nanoTime() - started))
          .isGreaterThan(Duration.ofMillis(150));
      release.countDown();
      holder.get(5, TimeUnit.SECONDS);
    } finally {
      release.countDown();
      executor.shutdownNow();
    }
    assertThat(count()).isZero();
  }

  @Test
  void readersSeeOnlyCommittedData() throws Exception {
    CountDownLatch inserted = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      Future<?> writer =
          executor.submit(
              () ->
                  database.write(
                      c -> {
                        insert(c, 7);
                        inserted.countDown();
                        await(release);
                        return null;
                      }));
      assertThat(inserted.await(5, TimeUnit.SECONDS)).isTrue();
      assertThat(count()).isZero();
      release.countDown();
      writer.get(5, TimeUnit.SECONDS);
    } finally {
      release.countDown();
      executor.shutdownNow();
    }
    assertThat(count()).isEqualTo(1);
  }

  @Test
  void busyAndLockedSqliteErrorsTranslateToStorageBusy() {
    assertThat(SqliteDatabase.translate(new SQLiteException("busy", SQLiteErrorCode.SQLITE_BUSY)))
        .isInstanceOf(StorageBusyException.class);
    assertThat(
            SqliteDatabase.translate(new SQLiteException("locked", SQLiteErrorCode.SQLITE_LOCKED)))
        .isInstanceOf(StorageBusyException.class);
    assertThat(SqliteDatabase.translate(new SQLException("other")))
        .isInstanceOf(StorageException.class);
  }

  @Test
  void probeReflectsLifecycle() {
    assertThat(database.isReachable()).isTrue();
    database.close();
    assertThat(database.isReachable()).isFalse();
    assertThatThrownBy(() -> database.read(c -> 1)).isInstanceOf(StorageException.class);
    assertThatThrownBy(() -> database.write(c -> 1)).isInstanceOf(StorageException.class);
    database.close();
  }

  private String writePragma(String name) {
    SqliteDatabase.Work<String> work = c -> pragma(c, name);
    return database.write(work);
  }

  private String readPragma(String name) {
    SqliteDatabase.Work<String> work = c -> pragma(c, name);
    return database.read(work);
  }

  private int count() {
    return database.read(
        c -> {
          try (Statement s = c.createStatement();
              ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM t")) {
            rs.next();
            return rs.getInt(1);
          }
        });
  }

  private static Void insert(Connection c, int value) throws SQLException {
    try (Statement s = c.createStatement()) {
      s.execute("INSERT INTO t (v) VALUES (" + value + ")");
    }
    return null;
  }

  private static String pragma(Connection c, String name) throws SQLException {
    try (Statement s = c.createStatement();
        ResultSet rs = s.executeQuery("PRAGMA " + name)) {
      rs.next();
      return rs.getString(1);
    }
  }

  private static void await(CountDownLatch latch) {
    try {
      latch.await(10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
