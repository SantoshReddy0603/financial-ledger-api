package org.ledgerapi.http;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import org.ledgerapi.application.HealthService;
import org.ledgerapi.domain.ErrorCode;
import org.ledgerapi.domain.LedgerException;
import org.ledgerapi.provider.StorageBusyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;

/** HTTP adapter: routing, request identifiers, error mapping and request logging. */
public final class HttpApi {
  public static final String BASE_PATH = "/api/v1";
  public static final String REQUEST_LOGGER = "ledger.request";
  private static final Logger LOG = LoggerFactory.getLogger(HttpApi.class);
  private static final Logger REQUEST_LOG = LoggerFactory.getLogger(REQUEST_LOGGER);

  private HttpApi() {}

  public static Javalin create(HealthService health) {
    RouteTable routes = new RouteTable();
    Javalin app =
        Javalin.create(
            config -> {
              config.showJavalinBanner = false;
              config.useVirtualThreads = false;
              config.http.maxRequestSize = 16_384L;
              config.router.ignoreTrailingSlashes = false;
              config.requestLogger.http(HttpApi::logRequest);
            });

    app.before(
        ctx -> {
          Enumeration<String> supplied = ctx.req().getHeaders(RequestIds.HEADER);
          List<String> values = supplied == null ? List.of() : Collections.list(supplied);
          String requestId = RequestIds.resolve(values);
          ctx.attribute(RequestIds.ATTRIBUTE, requestId);
          ctx.header(RequestIds.HEADER, requestId);
        });

    register(app, routes, HandlerType.GET, BASE_PATH + "/health", new HealthHandler(health));

    app.exception(
        LedgerException.class,
        (e, ctx) -> ErrorResponses.write(ctx, e.code(), e.getMessage(), e.details()));
    app.exception(
        StorageBusyException.class,
        (e, ctx) -> {
          ctx.header("Retry-After", "1");
          ErrorResponses.write(ctx, ErrorCode.STORAGE_BUSY, "Storage is busy; retry later");
        });
    app.exception(
        Exception.class,
        (e, ctx) -> {
          LOG.error("Unhandled failure on {} {}", ctx.method(), LogSanitizer.path(ctx.path()), e);
          ErrorResponses.write(ctx, ErrorCode.INTERNAL_ERROR, "Internal error");
        });
    app.error(404, ctx -> unmatched(ctx, routes));
    return app;
  }

  private static void register(
      Javalin app, RouteTable routes, HandlerType method, String path, Handler handler) {
    routes.add(method.name(), path);
    app.addHttpHandler(method, path, handler);
  }

  private static void unmatched(Context ctx, RouteTable routes) {
    if (Boolean.TRUE.equals(ctx.attribute(ErrorResponses.HANDLED_ATTRIBUTE))) {
      return;
    }
    Set<String> allowed = routes.allowedMethods(ctx.path());
    if (allowed.isEmpty()) {
      ErrorResponses.write(ctx, ErrorCode.NOT_FOUND, "Route not found");
      return;
    }
    ctx.header("Allow", String.join(", ", allowed));
    ErrorResponses.write(ctx, ErrorCode.METHOD_NOT_ALLOWED, "Method not allowed for this route");
  }

  private static void logRequest(Context ctx, Float elapsedMs) {
    String requestId = ctx.attribute(RequestIds.ATTRIBUTE);
    String errorCode = ctx.attribute(ErrorResponses.ERROR_CODE_ATTRIBUTE);
    LoggingEventBuilder record =
        REQUEST_LOG
            .atInfo()
            .addKeyValue("requestId", requestId)
            .addKeyValue("method", ctx.method().name())
            .addKeyValue("path", LogSanitizer.path(ctx.path()))
            .addKeyValue("status", ctx.statusCode())
            .addKeyValue("durationMs", Math.round(elapsedMs));
    if (errorCode != null) {
      record = record.addKeyValue("errorCode", errorCode);
    }
    record.log("request");
  }
}
