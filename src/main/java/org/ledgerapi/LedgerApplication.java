package org.ledgerapi;

import io.javalin.Javalin;
import java.util.Objects;
import org.ledgerapi.application.BuildInfo;
import org.ledgerapi.application.HealthService;
import org.ledgerapi.config.AppConfig;
import org.ledgerapi.http.HttpApi;
import org.ledgerapi.infrastructure.sqlite.SchemaMigrator;
import org.ledgerapi.infrastructure.sqlite.SqliteDatabase;
import org.ledgerapi.infrastructure.system.RandomUuidGenerator;
import org.ledgerapi.infrastructure.system.SystemClock;
import org.ledgerapi.provider.ClockProvider;
import org.ledgerapi.provider.IdGenerator;
import org.ledgerapi.provider.StorageProbe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Composition root: wires providers, migrates storage and runs the HTTP server. */
public final class LedgerApplication implements AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(LedgerApplication.class);

  private final SqliteDatabase database;
  private final Javalin http;

  private LedgerApplication(SqliteDatabase database, Javalin http) {
    this.database = database;
    this.http = http;
  }

  /** Starts with real providers. */
  public static LedgerApplication start(AppConfig config) {
    return start(config, new SystemClock(), new RandomUuidGenerator(), null);
  }

  /**
   * Starts with injected providers (DC-4). A non-null {@code probeOverride} replaces the database
   * health probe, which lets tests exercise the degraded health path.
   */
  public static LedgerApplication start(
      AppConfig config, ClockProvider clock, IdGenerator ids, StorageProbe probeOverride) {
    Objects.requireNonNull(config, "config");
    SqliteDatabase database =
        SqliteDatabase.open(config.databasePath(), config.readPoolSize(), config.lockTimeout());
    try {
      new SchemaMigrator(clock, ids).migrate(database, config.currencies());
      StorageProbe probe = probeOverride != null ? probeOverride : database;
      Javalin http = HttpApi.create(new HealthService(probe, BuildInfo.version()));
      http.start(config.listenAddress(), config.port());
      LOG.atInfo()
          .addKeyValue("port", http.port())
          .addKeyValue("version", BuildInfo.version())
          .log("started");
      return new LedgerApplication(database, http);
    } catch (RuntimeException e) {
      database.close();
      throw e;
    }
  }

  public int port() {
    return http.port();
  }

  /** Stops accepting requests, lets in-flight requests finish, then closes storage (FR-6.3). */
  @Override
  public void close() {
    try {
      http.stop();
    } finally {
      database.close();
      LOG.info("stopped");
    }
  }
}
