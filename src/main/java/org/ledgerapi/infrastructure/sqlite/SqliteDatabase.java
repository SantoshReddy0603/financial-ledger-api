package org.ledgerapi.infrastructure.sqlite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.ledgerapi.provider.StorageBusyException;
import org.ledgerapi.provider.StorageException;
import org.ledgerapi.provider.StorageProbe;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

/**
 * File-backed SQLite storage (ADR-0003).
 *
 * <p>One writer connection, guarded by a fair in-process lock, runs every write inside {@code BEGIN
 * IMMEDIATE}. A small pool of read-only connections serves consistent WAL snapshots without the
 * write lock (FR-3.4, FR-3.7). Every connection uses WAL, {@code synchronous=FULL}, foreign keys
 * and a 5000 ms busy timeout.
 */
public final class SqliteDatabase implements StorageProbe, AutoCloseable {
  public static final int BUSY_TIMEOUT_MS = 5_000;

  /** A unit of work executed on a connection inside one SQLite transaction. */
  @FunctionalInterface
  public interface Work<T> {
    T execute(Connection connection) throws SQLException;
  }

  private final Connection writer;
  private final BlockingQueue<Connection> readers;
  private final List<Connection> allReaders;
  private final ReentrantLock writeLock = new ReentrantLock(true);
  private final long lockTimeoutMillis;
  private volatile boolean closed;

  private SqliteDatabase(Connection writer, List<Connection> readers, Duration lockTimeout) {
    this.writer = writer;
    this.allReaders = List.copyOf(readers);
    this.readers = new ArrayBlockingQueue<>(readers.size(), true, readers);
    this.lockTimeoutMillis = lockTimeout.toMillis();
  }

  public static SqliteDatabase open(Path file, int readPoolSize, Duration lockTimeout) {
    try {
      Path parent = file.toAbsolutePath().getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
    } catch (IOException e) {
      throw new StorageException("Cannot create the database directory", e);
    }
    String url = "jdbc:sqlite:" + file.toAbsolutePath();
    Connection writer = null;
    List<Connection> readers = new ArrayList<>();
    try {
      writer = writerConfig().createConnection(url);
      for (int i = 0; i < readPoolSize; i++) {
        readers.add(readerConfig().createConnection(url));
      }
      return new SqliteDatabase(writer, readers, lockTimeout);
    } catch (SQLException e) {
      closeQuietly(writer);
      readers.forEach(SqliteDatabase::closeQuietly);
      throw new StorageException("Cannot open the database", e);
    }
  }

  private static SQLiteConfig baseConfig() {
    SQLiteConfig config = new SQLiteConfig();
    config.setSynchronous(SQLiteConfig.SynchronousMode.FULL);
    config.enforceForeignKeys(true);
    config.setBusyTimeout(BUSY_TIMEOUT_MS);
    return config;
  }

  private static SQLiteConfig writerConfig() {
    SQLiteConfig config = baseConfig();
    config.setJournalMode(SQLiteConfig.JournalMode.WAL);
    config.setTransactionMode(SQLiteConfig.TransactionMode.IMMEDIATE);
    return config;
  }

  private static SQLiteConfig readerConfig() {
    SQLiteConfig config = baseConfig();
    config.setReadOnly(true);
    config.setTransactionMode(SQLiteConfig.TransactionMode.DEFERRED);
    return config;
  }

  /**
   * Runs {@code work} in one {@code BEGIN IMMEDIATE} transaction and commits before returning. Any
   * exception rolls back every change. Throws {@link StorageBusyException} when the write lock is
   * not obtained within the lock timeout.
   */
  public <T> T write(Work<T> work) {
    ensureOpen();
    acquireWriteLock();
    try {
      try {
        writer.setAutoCommit(false);
      } catch (SQLException e) {
        throw translate(e);
      }
      try {
        T result = work.execute(writer);
        writer.commit();
        return result;
      } catch (SQLException e) {
        rollbackQuietly(writer);
        throw translate(e);
      } catch (RuntimeException | Error e) {
        rollbackQuietly(writer);
        throw e;
      }
    } finally {
      resetAutoCommit(writer);
      writeLock.unlock();
    }
  }

  /** Runs {@code work} against one consistent read snapshot without taking the write lock. */
  public <T> T read(Work<T> work) {
    ensureOpen();
    Connection connection = borrowReader();
    try {
      connection.setAutoCommit(false);
      try {
        T result = work.execute(connection);
        connection.commit();
        return result;
      } catch (SQLException | RuntimeException | Error e) {
        rollbackQuietly(connection);
        throw e;
      }
    } catch (SQLException e) {
      throw translate(e);
    } finally {
      resetAutoCommit(connection);
      readers.add(connection);
    }
  }

  @Override
  public boolean isReachable() {
    if (closed) {
      return false;
    }
    try {
      return read(
          connection -> {
            try (Statement statement = connection.createStatement()) {
              return statement.executeQuery("SELECT 1").next();
            }
          });
    } catch (RuntimeException e) {
      return false;
    }
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }
    closed = true;
    writeLock.lock();
    try {
      allReaders.forEach(SqliteDatabase::closeQuietly);
      closeQuietly(writer);
    } finally {
      writeLock.unlock();
    }
  }

  private void acquireWriteLock() {
    boolean acquired;
    try {
      acquired = writeLock.tryLock(lockTimeoutMillis, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new StorageBusyException("Interrupted while waiting for the write lock", e);
    }
    if (!acquired) {
      throw new StorageBusyException("Write lock not acquired within the timeout");
    }
  }

  private Connection borrowReader() {
    try {
      Connection connection = readers.poll(BUSY_TIMEOUT_MS, TimeUnit.MILLISECONDS);
      if (connection == null) {
        throw new StorageBusyException("No read connection available within the timeout");
      }
      return connection;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new StorageBusyException("Interrupted while waiting for a read connection", e);
    }
  }

  private void ensureOpen() {
    if (closed) {
      throw new StorageException("Database is closed", null);
    }
  }

  static RuntimeException translate(SQLException e) {
    if (e instanceof SQLiteException sqlite) {
      SQLiteErrorCode code = sqlite.getResultCode();
      if (code == SQLiteErrorCode.SQLITE_BUSY
          || code == SQLiteErrorCode.SQLITE_BUSY_SNAPSHOT
          || code == SQLiteErrorCode.SQLITE_BUSY_RECOVERY
          || code == SQLiteErrorCode.SQLITE_LOCKED) {
        return new StorageBusyException("Database is locked", e);
      }
    }
    return new StorageException("Database operation failed", e);
  }

  private static void rollbackQuietly(Connection connection) {
    try {
      connection.rollback();
    } catch (SQLException ignored) {
      // The original failure is reported; SQLite discards an unfinished transaction on close.
    }
  }

  private static void resetAutoCommit(Connection connection) {
    try {
      if (!connection.isClosed() && !connection.getAutoCommit()) {
        connection.setAutoCommit(true);
      }
    } catch (SQLException ignored) {
      // Leaves the connection in its current mode; the next use reports any persistent fault.
    }
  }

  private static void closeQuietly(Connection connection) {
    if (connection == null) {
      return;
    }
    try {
      connection.close();
    } catch (SQLException ignored) {
      // Nothing useful can be done while shutting down.
    }
  }
}
