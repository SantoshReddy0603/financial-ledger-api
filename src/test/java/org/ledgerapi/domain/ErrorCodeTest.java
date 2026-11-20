package org.ledgerapi.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class ErrorCodeTest {
  @Test
  void statusesMatchTheCatalogue() {
    assertThat(ErrorCode.VALIDATION_ERROR.httpStatus()).isEqualTo(400);
    assertThat(ErrorCode.UNAUTHENTICATED.httpStatus()).isEqualTo(401);
    assertThat(ErrorCode.FORBIDDEN.httpStatus()).isEqualTo(403);
    assertThat(ErrorCode.NOT_FOUND.httpStatus()).isEqualTo(404);
    assertThat(ErrorCode.METHOD_NOT_ALLOWED.httpStatus()).isEqualTo(405);
    assertThat(ErrorCode.ALREADY_REVERSED.httpStatus()).isEqualTo(409);
    assertThat(ErrorCode.PAYLOAD_TOO_LARGE.httpStatus()).isEqualTo(413);
    assertThat(ErrorCode.UNSUPPORTED_MEDIA_TYPE.httpStatus()).isEqualTo(415);
    assertThat(ErrorCode.IDEMPOTENCY_KEY_REUSED.httpStatus()).isEqualTo(422);
    assertThat(ErrorCode.TOO_MANY_REQUESTS.httpStatus()).isEqualTo(429);
    assertThat(ErrorCode.INTERNAL_ERROR.httpStatus()).isEqualTo(500);
    assertThat(ErrorCode.STORAGE_BUSY.httpStatus()).isEqualTo(503);
    assertThat(ErrorCode.DATABASE_UNAVAILABLE.httpStatus()).isEqualTo(503);
  }

  @Test
  void catalogueHasEveryCode() {
    assertThat(ErrorCode.values()).hasSize(26);
  }

  @Test
  void ledgerExceptionCarriesCodeAndImmutableDetails() {
    LedgerException e =
        new LedgerException(
            ErrorCode.VALIDATION_ERROR,
            "Invalid request",
            List.of(new FieldError("x", "required")));
    assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_ERROR);
    assertThat(e.getMessage()).isEqualTo("Invalid request");
    assertThat(e.details()).containsExactly(new FieldError("x", "required"));
    assertThatThrownBy(() -> e.details().clear()).isInstanceOf(UnsupportedOperationException.class);
    assertThat(new LedgerException(ErrorCode.NOT_FOUND, "missing").details()).isEmpty();
  }

  @Test
  void fieldErrorRejectsNulls() {
    assertThatThrownBy(() -> new FieldError(null, "rule")).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new FieldError("f", null)).isInstanceOf(NullPointerException.class);
  }
}
