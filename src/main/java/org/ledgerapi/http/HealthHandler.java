package org.ledgerapi.http;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import java.util.LinkedHashMap;
import java.util.Map;
import org.ledgerapi.application.HealthService;
import org.ledgerapi.application.HealthService.HealthReport;
import org.ledgerapi.domain.ErrorCode;

/** {@code GET /api/v1/health} (FR-6.1). */
public final class HealthHandler implements Handler {
  private final HealthService health;

  public HealthHandler(HealthService health) {
    this.health = health;
  }

  @Override
  public void handle(Context ctx) {
    HealthReport report = health.check();
    Map<String, Object> body = new LinkedHashMap<>();
    if (report.databaseReachable()) {
      body.put("status", "OK");
      body.put("version", report.version());
      body.put("database", "reachable");
      ctx.status(200).json(body);
      return;
    }
    body.put("status", "DEGRADED");
    body.put("version", report.version());
    body.put("database", "unreachable");
    body.put(
        "error",
        Map.of(
            "code",
            ErrorCode.DATABASE_UNAVAILABLE.name(),
            "message",
            "The database cannot be reached"));
    ctx.attribute(ErrorResponses.HANDLED_ATTRIBUTE, Boolean.TRUE);
    ctx.attribute(ErrorResponses.ERROR_CODE_ATTRIBUTE, ErrorCode.DATABASE_UNAVAILABLE.name());
    ctx.status(ErrorCode.DATABASE_UNAVAILABLE.httpStatus()).json(body);
  }
}
