package org.ledgerapi;

import org.ledgerapi.config.AppConfig;
import org.ledgerapi.config.ConfigException;
import org.ledgerapi.config.ConfigLoader;
import org.ledgerapi.infrastructure.sqlite.SchemaMigrator.IncompatibleDatabaseException;

/** Process entry point. Exit code 2 means invalid configuration or an incompatible database. */
public final class Main {
  static final int EXIT_CONFIGURATION = 2;

  private Main() {}

  public static void main(String[] args) {
    AppConfig config;
    try {
      config = ConfigLoader.fromEnvironment();
    } catch (ConfigException e) {
      System.err.println("Configuration error: " + e.getMessage());
      System.exit(EXIT_CONFIGURATION);
      return;
    }
    LedgerApplication application;
    try {
      application = LedgerApplication.start(config);
    } catch (IncompatibleDatabaseException e) {
      System.err.println("Startup refused: " + e.getMessage());
      System.exit(EXIT_CONFIGURATION);
      return;
    }
    Runtime.getRuntime().addShutdownHook(new Thread(application::close, "ledger-shutdown"));
  }
}
