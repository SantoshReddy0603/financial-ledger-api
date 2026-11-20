package org.ledgerapi.http;

import io.javalin.http.Context;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.ledgerapi.domain.ErrorCode;
import org.ledgerapi.domain.FieldError;

/** Writes the standard error envelope (SRS section 4.3). */
public final class ErrorResponses {
  public static final String HANDLED_ATTRIBUTE = "ledger.errorHandled";

  /** Stable error code of the response, added to the request log record (NFR-OBS-2). */
  public static final String ERROR_CODE_ATTRIBUTE = "ledger.errorCode";

  private ErrorResponses() {}

  public static Map<String, Object> envelope(
      ErrorCode code, String message, List<FieldError> details) {
    Map<String, Object> error = new LinkedHashMap<>();
    error.put("code", code.name());
    error.put("message", message);
    if (!details.isEmpty()) {
      List<Map<String, String>> items = new ArrayList<>();
      for (FieldError detail : details) {
        items.add(Map.of("field", detail.field(), "rule", detail.rule()));
      }
      error.put("details", items);
    }
    return Map.of("error", error);
  }

  public static void write(Context ctx, ErrorCode code, String message) {
    write(ctx, code, message, List.of());
  }

  public static void write(Context ctx, ErrorCode code, String message, List<FieldError> details) {
    ctx.attribute(HANDLED_ATTRIBUTE, Boolean.TRUE);
    ctx.attribute(ERROR_CODE_ATTRIBUTE, code.name());
    ctx.status(code.httpStatus()).json(envelope(code, message, details));
  }
}
