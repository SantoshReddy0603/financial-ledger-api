package org.ledgerapi.testing;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.ledgerapi.config.AppConfig;
import org.ledgerapi.config.ConfigLoader;

/** Builds configurations bound to a disposable database and an ephemeral port. */
public final class TestConfigs {
  private TestConfigs() {}

  public static AppConfig forDatabase(Path database, Map<String, String> overrides) {
    Map<String, String> env = new HashMap<>();
    env.put(ConfigLoader.PORT, "0");
    env.put(ConfigLoader.DB_PATH, database.toString());
    env.put(ConfigLoader.READ_POOL_SIZE, "2");
    env.putAll(overrides);
    return ConfigLoader.load(env);
  }
}
