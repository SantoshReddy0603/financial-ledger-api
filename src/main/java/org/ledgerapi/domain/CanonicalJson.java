package org.ledgerapi.domain;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Deterministic canonical JSON serializer (RFC 8785 subset) for idempotency fingerprints and the
 * transaction hash chain (ADR-0004, FR-2.5, FR-4.5).
 *
 * <p>Permitted value types are {@code null}, {@link String}, {@link Long}, {@link Integer}, {@link
 * Map} (string keys), and {@link List}. Floating-point numbers and booleans are rejected.
 */
public final class CanonicalJson {
  private static final char[] HEX = "0123456789abcdef".toCharArray();

  private CanonicalJson() {}

  /** Serializes {@code value} into its canonical JSON string representation. */
  public static String serialize(Object value) {
    StringBuilder out = new StringBuilder();
    writeValue(value, out);
    return out.toString();
  }

  /** Serializes {@code value} into canonical UTF-8 JSON bytes. */
  public static byte[] serializeUtf8(Object value) {
    return serialize(value).getBytes(StandardCharsets.UTF_8);
  }

  private static void writeValue(Object value, StringBuilder out) {
    if (value == null) {
      out.append("null");
    } else if (value instanceof String s) {
      writeString(s, out);
    } else if (value instanceof Long l) {
      out.append(Long.toString(l));
    } else if (value instanceof Integer i) {
      out.append(Long.toString(i.longValue()));
    } else if (value instanceof Map<?, ?> map) {
      writeObject(map, out);
    } else if (value instanceof List<?> list) {
      writeArray(list, out);
    } else {
      throw new IllegalArgumentException(
          "Unsupported canonical JSON value type: " + value.getClass().getName());
    }
  }

  private static void writeObject(Map<?, ?> map, StringBuilder out) {
    List<Map.Entry<String, Object>> entries = new ArrayList<>(map.size());
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      if (!(entry.getKey() instanceof String key)) {
        throw new IllegalArgumentException("Canonical JSON object keys must be strings");
      }
      entries.add(
          Map.entry(key, entry.getValue() == null ? NullSentinel.INSTANCE : entry.getValue()));
    }
    // Sort by UTF-16 code-unit order (String::compareTo) per ADR-0004.
    entries.sort(Comparator.comparing(Map.Entry::getKey));

    out.append('{');
    for (int i = 0; i < entries.size(); i++) {
      if (i > 0) {
        out.append(',');
      }
      Map.Entry<String, Object> entry = entries.get(i);
      writeString(entry.getKey(), out);
      out.append(':');
      Object val = entry.getValue() == NullSentinel.INSTANCE ? null : entry.getValue();
      writeValue(val, out);
    }
    out.append('}');
  }

  private static void writeArray(List<?> list, StringBuilder out) {
    out.append('[');
    for (int i = 0; i < list.size(); i++) {
      if (i > 0) {
        out.append(',');
      }
      writeValue(list.get(i), out);
    }
    out.append(']');
  }

  private static void writeString(String s, StringBuilder out) {
    out.append('"');
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '"' -> out.append("\\\"");
        case '\\' -> out.append("\\\\");
        case '\b' -> out.append("\\b");
        case '\t' -> out.append("\\t");
        case '\n' -> out.append("\\n");
        case '\f' -> out.append("\\f");
        case '\r' -> out.append("\\r");
        default -> {
          if (c < 0x20) {
            out.append("\\u00");
            out.append(HEX[(c >>> 4) & 0x0F]);
            out.append(HEX[c & 0x0F]);
          } else {
            out.append(c);
          }
        }
      }
    }
    out.append('"');
  }

  private enum NullSentinel {
    INSTANCE
  }
}
