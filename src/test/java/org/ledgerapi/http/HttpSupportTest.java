package org.ledgerapi.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.ledgerapi.domain.ErrorCode;
import org.ledgerapi.domain.FieldError;

class HttpSupportTest {
  @Test
  void requestIdEchoesOneValidValue() {
    assertThat(RequestIds.resolve(List.of("abc_DEF-123"))).isEqualTo("abc_DEF-123");
    assertThat(RequestIds.resolve(List.of("x".repeat(64)))).isEqualTo("x".repeat(64));
  }

  @Test
  void requestIdReplacesMissingDuplicateOrInvalidValues() {
    for (List<String> supplied :
        List.of(
            List.<String>of(),
            List.of("a", "b"),
            List.of(""),
            List.of("x".repeat(65)),
            List.of("has space"),
            List.of("new\nline"))) {
      assertThat(RequestIds.resolve(supplied))
          .as(supplied.toString())
          .matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
    }
    assertThat(RequestIds.resolve(null)).hasSize(36);
    assertThat(RequestIds.resolve(Arrays.asList((String) null))).hasSize(36);
  }

  @Test
  void routeTableDistinguishesUnknownRoutesFromWrongMethods() {
    RouteTable routes = new RouteTable();
    routes.add("GET", "/api/v1/wallets/{walletId}");
    routes.add("POST", "/api/v1/users/{userId}/wallets");

    assertThat(routes.allowedMethods("/api/v1/wallets/123")).containsExactly("GET", "HEAD");
    assertThat(routes.allowedMethods("/api/v1/users/u/wallets")).containsExactly("POST");
    assertThat(routes.allowedMethods("/api/v1/wallets")).isEmpty();
    assertThat(routes.allowedMethods("/api/v1/wallets//")).isEmpty();
    assertThat(routes.allowedMethods("/nothing")).isEmpty();
    assertThat(routes.allowedMethods("/")).isEmpty();
  }

  @Test
  void errorEnvelopeHasCodeMessageAndOptionalDetails() {
    assertThat(ErrorResponses.envelope(ErrorCode.NOT_FOUND, "Route not found", List.of()))
        .isEqualTo(Map.of("error", Map.of("code", "NOT_FOUND", "message", "Route not found")));
    Map<String, Object> withDetails =
        ErrorResponses.envelope(
            ErrorCode.VALIDATION_ERROR,
            "Invalid request",
            List.of(new FieldError("currency", "required")));
    assertThat(withDetails.toString()).contains("details=[{", "field=currency", "rule=required");
  }

  @Test
  void logSanitizerNeutralisesControlCharactersAndLength() {
    assertThat(LogSanitizer.path("/api/v1/health")).isEqualTo("/api/v1/health");
    assertThat(LogSanitizer.path("/a\r\nforged")).isEqualTo("/a??forged");
    assertThat(LogSanitizer.path("/é")).isEqualTo("/?");
    assertThat(LogSanitizer.path("/" + "x".repeat(1000))).hasSize(LogSanitizer.MAX_PATH_LENGTH);
    assertThat(LogSanitizer.path(null)).isEmpty();
  }
}
