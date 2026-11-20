package org.ledgerapi.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * The registered method/path pairs. Used to distinguish an unknown route (404) from a known route
 * called with an unsupported method (405 with {@code Allow}).
 */
public final class RouteTable {
  private record Route(String method, String[] segments) {}

  private final List<Route> routes = new ArrayList<>();

  public void add(String method, String pathPattern) {
    routes.add(new Route(method, split(pathPattern)));
  }

  /** Methods registered for a path; empty when no route matches the path at all. */
  public Set<String> allowedMethods(String path) {
    String[] actual = split(path);
    Set<String> methods = new TreeSet<>();
    for (Route route : routes) {
      if (matches(route.segments(), actual)) {
        methods.add(route.method());
        if (route.method().equals("GET")) {
          methods.add("HEAD");
        }
      }
    }
    return methods;
  }

  private static boolean matches(String[] pattern, String[] actual) {
    if (pattern.length != actual.length) {
      return false;
    }
    for (int i = 0; i < pattern.length; i++) {
      boolean parameter = pattern[i].startsWith("{") && pattern[i].endsWith("}");
      if (parameter ? actual[i].isEmpty() : !pattern[i].equals(actual[i])) {
        return false;
      }
    }
    return true;
  }

  private static String[] split(String path) {
    String trimmed = path.startsWith("/") ? path.substring(1) : path;
    if (trimmed.endsWith("/")) {
      trimmed = trimmed.substring(0, trimmed.length() - 1);
    }
    return trimmed.isEmpty() ? new String[0] : trimmed.split("/", -1);
  }
}
