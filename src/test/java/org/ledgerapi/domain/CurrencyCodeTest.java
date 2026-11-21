package org.ledgerapi.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CurrencyCodeTest {
  @Test
  void acceptsThreeLetterUppercaseIsoCodes() {
    CurrencyCode inr = CurrencyCode.of("INR");
    assertThat(inr.code()).isEqualTo("INR");
    assertThat(inr.toString()).isEqualTo("INR");
    assertThat(CurrencyCode.of("USD")).isEqualTo(new CurrencyCode("USD"));
  }

  @Test
  void rejectsInvalidCurrencyCodes() {
    for (String bad : new String[] {null, "", "us", "usd", "US1", "USDD", "U$D"}) {
      assertThatThrownBy(() -> CurrencyCode.of(bad))
          .isInstanceOf(LedgerException.class)
          .extracting(e -> ((LedgerException) e).code())
          .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }
  }
}
