package org.ledgerapi.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class HealthServiceTest {
  @Test
  void reportsReachableDatabase() {
    HealthService.HealthReport report = new HealthService(() -> true, "1.2.3").check();
    assertThat(report.databaseReachable()).isTrue();
    assertThat(report.version()).isEqualTo("1.2.3");
  }

  @Test
  void reportsUnreachableDatabase() {
    assertThat(new HealthService(() -> false, "v").check().databaseReachable()).isFalse();
  }

  @Test
  void treatsProbeFailureAsUnreachable() {
    HealthService service =
        new HealthService(
            () -> {
              throw new IllegalStateException("boom");
            },
            "v");
    assertThat(service.check().databaseReachable()).isFalse();
  }

  @Test
  void buildVersionIsFilteredIntoTheJar() {
    assertThat(BuildInfo.version()).isNotBlank().isNotEqualTo(BuildInfo.UNKNOWN);
  }

  @Test
  void buildVersionFallsBackWhenMissingOrUnfiltered() {
    assertThat(BuildInfo.versionFrom(null)).isEqualTo(BuildInfo.UNKNOWN);
    assertThat(BuildInfo.versionFrom(stream("version=${project.version}")))
        .isEqualTo(BuildInfo.UNKNOWN);
    assertThat(BuildInfo.versionFrom(stream("other=1"))).isEqualTo(BuildInfo.UNKNOWN);
    assertThat(BuildInfo.versionFrom(stream("version=0.9.0"))).isEqualTo("0.9.0");
    InputStream failing =
        new InputStream() {
          @Override
          public int read() throws IOException {
            throw new IOException("unreadable");
          }
        };
    assertThat(BuildInfo.versionFrom(failing)).isEqualTo(BuildInfo.UNKNOWN);
  }

  private static InputStream stream(String text) {
    return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
  }
}
