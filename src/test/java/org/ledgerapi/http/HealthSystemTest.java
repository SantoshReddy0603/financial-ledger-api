package org.ledgerapi.http;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ledgerapi.LedgerApplication;
import org.ledgerapi.provider.StorageProbe;
import org.ledgerapi.testing.FixedClock;
import org.ledgerapi.testing.SequentialIds;
import org.ledgerapi.testing.TestConfigs;
import org.slf4j.LoggerFactory;
import org.slf4j.event.KeyValuePair;

/** End-to-end HTTP tests against a real server and a disposable file-backed database. */
class HealthSystemTest {
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final String UUID_PATTERN =
      "[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";

  @TempDir Path dir;
  private LedgerApplication app;
  private final HttpClient client = HttpClient.newHttpClient();

  @AfterEach
  void stop() {
    if (app != null) {
      app.close();
    }
  }

  private void start(StorageProbe probe) {
    app =
        LedgerApplication.start(
            TestConfigs.forDatabase(dir.resolve("ledger.db"), Map.of()),
            new FixedClock(Instant.parse("2026-01-01T00:00:00Z")),
            new SequentialIds(),
            probe);
  }

  private HttpResponse<String> send(String method, String path, String... headers)
      throws Exception {
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + app.port() + path))
            .method(method, HttpRequest.BodyPublishers.noBody());
    if (headers.length > 0) {
      builder.headers(headers);
    }
    return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void healthReportsReachableDatabase() throws Exception {
    start(null);
    HttpResponse<String> response = send("GET", "/api/v1/health");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("Content-Type"))
        .hasValueSatisfying(v -> assertThat(v).startsWith("application/json"));
    JsonNode body = JSON.readTree(response.body());
    assertThat(body.get("status").asText()).isEqualTo("OK");
    assertThat(body.get("database").asText()).isEqualTo("reachable");
    assertThat(body.get("version").asText()).isNotBlank().doesNotContain("${");
    assertThat(body.size()).isEqualTo(3);
    assertThat(response.headers().firstValue(RequestIds.HEADER))
        .hasValueSatisfying(v -> assertThat(v).matches(UUID_PATTERN));
  }

  @Test
  void healthReportsDegradedWhenProbeFails() throws Exception {
    start(() -> false);
    HttpResponse<String> response = send("GET", "/api/v1/health");

    assertThat(response.statusCode()).isEqualTo(503);
    JsonNode body = JSON.readTree(response.body());
    assertThat(body.get("status").asText()).isEqualTo("DEGRADED");
    assertThat(body.get("database").asText()).isEqualTo("unreachable");
    assertThat(body.at("/error/code").asText()).isEqualTo("DATABASE_UNAVAILABLE");
    assertThat(response.body()).doesNotContain(dir.toString());
  }

  @Test
  void validRequestIdIsEchoedAndInvalidOneReplaced() throws Exception {
    start(null);
    assertThat(
            send("GET", "/api/v1/health", RequestIds.HEADER, "trace-123")
                .headers()
                .firstValue(RequestIds.HEADER))
        .hasValue("trace-123");
    assertThat(
            send("GET", "/api/v1/health", RequestIds.HEADER, "bad id!")
                .headers()
                .firstValue(RequestIds.HEADER))
        .hasValueSatisfying(v -> assertThat(v).matches(UUID_PATTERN));
    assertThat(
            send("GET", "/api/v1/health", RequestIds.HEADER, "one", RequestIds.HEADER, "two")
                .headers()
                .firstValue(RequestIds.HEADER))
        .hasValueSatisfying(v -> assertThat(v).matches(UUID_PATTERN));
  }

  @Test
  void unknownRouteReturnsNotFoundEnvelope() throws Exception {
    start(null);
    HttpResponse<String> response = send("GET", "/api/v1/nope", RequestIds.HEADER, "r1");

    assertThat(response.statusCode()).isEqualTo(404);
    assertThat(JSON.readTree(response.body()).at("/error/code").asText()).isEqualTo("NOT_FOUND");
    assertThat(response.headers().firstValue(RequestIds.HEADER)).hasValue("r1");
  }

  @Test
  void wrongMethodReturnsMethodNotAllowedWithAllow() throws Exception {
    start(null);
    HttpResponse<String> response = send("DELETE", "/api/v1/health");

    assertThat(response.statusCode()).isEqualTo(405);
    assertThat(response.headers().firstValue("Allow")).hasValue("GET, HEAD");
    assertThat(JSON.readTree(response.body()).at("/error/code").asText())
        .isEqualTo("METHOD_NOT_ALLOWED");
  }

  @Test
  void requestLogRecordsFieldsErrorCodeAndSanitizedPath() throws Exception {
    Logger logger = (Logger) LoggerFactory.getLogger(HttpApi.REQUEST_LOGGER);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    try {
      start(null);
      send("GET", "/api/v1/health", RequestIds.HEADER, "trace-1");
      send("GET", "/api/v1/missing%0Aforged?token=secret", RequestIds.HEADER, "trace-2");
      waitForRecords(appender, 2);
    } finally {
      logger.detachAppender(appender);
    }

    Map<String, Object> ok = fields(appender.list.get(0));
    assertThat(ok)
        .containsEntry("requestId", "trace-1")
        .containsEntry("method", "GET")
        .containsEntry("path", "/api/v1/health")
        .containsEntry("status", 200)
        .containsKey("durationMs")
        .doesNotContainKey("errorCode");

    Map<String, Object> failed = fields(appender.list.get(1));
    assertThat(failed)
        .containsEntry("requestId", "trace-2")
        .containsEntry("status", 404)
        .containsEntry("errorCode", "NOT_FOUND");
    assertThat((String) failed.get("path"))
        .isEqualTo("/api/v1/missing%0Aforged")
        .doesNotContain("\n")
        .doesNotContain("token")
        .doesNotContain("secret");
  }

  private static void waitForRecords(ListAppender<ILoggingEvent> appender, int count)
      throws InterruptedException {
    long deadline = System.nanoTime() + 5_000_000_000L;
    while (appender.list.size() < count && System.nanoTime() < deadline) {
      Thread.sleep(10);
    }
    assertThat(appender.list).hasSizeGreaterThanOrEqualTo(count);
  }

  private static Map<String, Object> fields(ILoggingEvent event) {
    Map<String, Object> result = new HashMap<>();
    List<KeyValuePair> pairs = event.getKeyValuePairs();
    if (pairs != null) {
      for (KeyValuePair pair : pairs) {
        result.put(pair.key, pair.value);
      }
    }
    return result;
  }

  @Test
  void restartOnSameDatabaseSucceeds() throws Exception {
    start(null);
    app.close();
    app = null;
    start(null);
    assertThat(send("GET", "/api/v1/health").statusCode()).isEqualTo(200);
  }
}
