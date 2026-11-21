package org.ledgerapi.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CanonicalJsonTest {
  @Test
  void sortsObjectKeysDeterministicallyRegardlessOfInsertionOrder() {
    Map<String, Object> m1 = new LinkedHashMap<>();
    m1.put("z", 1L);
    m1.put("a", "first");
    m1.put("m", null);

    Map<String, Object> m2 = new LinkedHashMap<>();
    m2.put("m", null);
    m2.put("z", 1);
    m2.put("a", "first");

    assertThat(CanonicalJson.serialize(m1)).isEqualTo("{\"a\":\"first\",\"m\":null,\"z\":1}");
    assertThat(CanonicalJson.serialize(m2)).isEqualTo(CanonicalJson.serialize(m1));
    assertThat(CanonicalJson.serializeUtf8(m1))
        .isEqualTo("{\"a\":\"first\",\"m\":null,\"z\":1}".getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void escapesControlCharactersAndQuotesPerAdr0004() {
    String raw = "quote:\" slash:\\ bs:\b tab:\t nl:\n ff:\f cr:\r nul:\u0000 esc:\u001f ok:₹";
    assertThat(CanonicalJson.serialize(raw))
        .isEqualTo(
            "\"quote:\\\" slash:\\\\ bs:\\b tab:\\t nl:\\n ff:\\f cr:\\r nul:\\u0000 esc:\\u001f ok:₹\"");
  }

  @Test
  void serializesNestedArraysAndNullsAndEmptyStrings() {
    Map<String, Object> obj = new HashMap<>();
    obj.put("empty", "");
    obj.put("missing", null);
    obj.put("items", List.of(Map.of("b", -5L, "a", 0L)));

    assertThat(CanonicalJson.serialize(obj))
        .isEqualTo("{\"empty\":\"\",\"items\":[{\"a\":0,\"b\":-5}],\"missing\":null}");
  }

  @Test
  void rejectsUnsupportedTypesAndNonStringKeys() {
    assertThatThrownBy(() -> CanonicalJson.serialize(Boolean.TRUE))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> CanonicalJson.serialize(Map.of(1, "x")))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
