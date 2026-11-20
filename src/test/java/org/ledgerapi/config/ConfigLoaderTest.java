package org.ledgerapi.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.ledgerapi.config.AppConfig.AdminKey;
import org.ledgerapi.config.AppConfig.CurrencySpec;

class ConfigLoaderTest {
  private static final String HASH = "a".repeat(64);

  @Test
  void defaultsAreSafeAndDocumented() {
    AppConfig config = ConfigLoader.load(Map.of());

    assertThat(config.listenAddress()).isEqualTo("127.0.0.1");
    assertThat(config.port()).isEqualTo(8080);
    assertThat(config.databasePath()).isEqualTo(Path.of("data/ledger.db"));
    assertThat(config.currencies())
        .containsExactly(
            new CurrencySpec("INR", 2), new CurrencySpec("USD", 2), new CurrencySpec("EUR", 2));
    assertThat(config.sessionLifetime()).isEqualTo(Duration.ofMinutes(60));
    assertThat(config.passwordHash().memoryKib()).isEqualTo(19 * 1024);
    assertThat(config.passwordHash().iterations()).isEqualTo(2);
    assertThat(config.passwordHash().parallelism()).isEqualTo(1);
    assertThat(config.adminKeys()).isEmpty();
    assertThat(config.trustedProxies()).isEmpty();
    assertThat(config.readPoolSize()).isEqualTo(8);
    assertThat(config.lockTimeout()).isEqualTo(Duration.ofMillis(5_000));
  }

  @Test
  void readsEveryOverride() {
    AppConfig config =
        ConfigLoader.load(
            Map.ofEntries(
                Map.entry(ConfigLoader.LISTEN_ADDRESS, "0.0.0.0"),
                Map.entry(ConfigLoader.PORT, "0"),
                Map.entry(ConfigLoader.DB_PATH, "build/test.db"),
                Map.entry(ConfigLoader.CURRENCIES, " JPY:0 , USD:2 "),
                Map.entry(ConfigLoader.SESSION_LIFETIME_MINUTES, "15"),
                Map.entry(ConfigLoader.ARGON2_MEMORY_KIB, "65536"),
                Map.entry(ConfigLoader.ARGON2_ITERATIONS, "3"),
                Map.entry(ConfigLoader.ARGON2_PARALLELISM, "2"),
                Map.entry(ConfigLoader.ADMIN_KEYS, "ops-1=" + HASH + ",ops-2=" + "b".repeat(64)),
                Map.entry(ConfigLoader.TRUSTED_PROXIES, "10.0.0.1, 10.0.0.2"),
                Map.entry(ConfigLoader.READ_POOL_SIZE, "2"),
                Map.entry(ConfigLoader.LOCK_TIMEOUT_MS, "250")));

    assertThat(config.listenAddress()).isEqualTo("0.0.0.0");
    assertThat(config.port()).isZero();
    assertThat(config.currencies())
        .containsExactly(new CurrencySpec("JPY", 0), new CurrencySpec("USD", 2));
    assertThat(config.sessionLifetime()).isEqualTo(Duration.ofMinutes(15));
    assertThat(config.passwordHash().memoryKib()).isEqualTo(65536);
    assertThat(config.adminKeys())
        .containsExactly(new AdminKey("ops-1", HASH), new AdminKey("ops-2", "b".repeat(64)));
    assertThat(config.trustedProxies()).containsExactly("10.0.0.1", "10.0.0.2");
    assertThat(config.readPoolSize()).isEqualTo(2);
    assertThat(config.lockTimeout()).isEqualTo(Duration.ofMillis(250));
  }

  @Test
  void rejectsWeakPasswordHashSettings() {
    assertThatThrownBy(() -> ConfigLoader.load(Map.of(ConfigLoader.ARGON2_MEMORY_KIB, "1024")))
        .isInstanceOf(ConfigException.class)
        .hasMessageContaining(ConfigLoader.ARGON2_MEMORY_KIB);
    assertThatThrownBy(() -> ConfigLoader.load(Map.of(ConfigLoader.ARGON2_ITERATIONS, "1")))
        .isInstanceOf(ConfigException.class);
  }

  @Test
  void rejectsMalformedNumbersAndRanges() {
    assertThatThrownBy(() -> ConfigLoader.load(Map.of(ConfigLoader.PORT, "eighty")))
        .isInstanceOf(ConfigException.class)
        .hasMessageContaining(ConfigLoader.PORT);
    assertThatThrownBy(() -> ConfigLoader.load(Map.of(ConfigLoader.PORT, "70000")))
        .isInstanceOf(ConfigException.class);
    assertThatThrownBy(() -> ConfigLoader.load(Map.of(ConfigLoader.LISTEN_ADDRESS, "  ")))
        .isInstanceOf(ConfigException.class);
  }

  @Test
  void rejectsInvalidCurrencyLists() {
    for (String bad : new String[] {"usd:2", "USD", "USD:x", "USD:19", "USD:2,USD:2", " , "}) {
      assertThatThrownBy(() -> ConfigLoader.load(Map.of(ConfigLoader.CURRENCIES, bad)))
          .as(bad)
          .isInstanceOf(ConfigException.class)
          .hasMessageContaining(ConfigLoader.CURRENCIES);
    }
  }

  @Test
  void rejectsInvalidAdminKeysWithoutEchoingThem() {
    String secretLooking = "ops=" + "Z".repeat(64);
    assertThatThrownBy(() -> ConfigLoader.load(Map.of(ConfigLoader.ADMIN_KEYS, secretLooking)))
        .isInstanceOf(ConfigException.class)
        .hasMessageNotContaining("ZZZZ");
    assertThatThrownBy(() -> ConfigLoader.load(Map.of(ConfigLoader.ADMIN_KEYS, "=" + HASH)))
        .isInstanceOf(ConfigException.class);
    assertThatThrownBy(() -> ConfigLoader.load(Map.of(ConfigLoader.ADMIN_KEYS, "a b=" + HASH)))
        .isInstanceOf(ConfigException.class);
    assertThatThrownBy(
            () -> ConfigLoader.load(Map.of(ConfigLoader.ADMIN_KEYS, "a=" + HASH + ",b=" + HASH)))
        .isInstanceOf(ConfigException.class);
  }
}
