package org.ledgerapi.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MoneyTest {
  private static final CurrencyCode INR = CurrencyCode.of("INR");
  private static final CurrencyCode USD = CurrencyCode.of("USD");

  @Test
  void validatesTransactionAmountBounds() {
    assertThat(Money.transactionAmount(1L, INR).minorUnits()).isEqualTo(1L);
    assertThat(Money.transactionAmount(Money.MAX_TRANSACTION_AMOUNT, INR).minorUnits())
        .isEqualTo(Money.MAX_TRANSACTION_AMOUNT);

    for (long bad : new long[] {0L, -1L, Money.MAX_TRANSACTION_AMOUNT + 1L}) {
      assertThatThrownBy(() -> Money.transactionAmount(bad, INR))
          .isInstanceOf(LedgerException.class)
          .extracting(e -> ((LedgerException) e).code())
          .isEqualTo(ErrorCode.INVALID_AMOUNT);
    }
  }

  @Test
  void validatesWalletBalanceBounds() {
    assertThat(Money.walletBalance(0L, INR).minorUnits()).isZero();
    assertThat(Money.walletBalance(Money.MAX_WALLET_BALANCE, INR).minorUnits())
        .isEqualTo(Money.MAX_WALLET_BALANCE);

    assertThatThrownBy(() -> Money.walletBalance(-1L, INR))
        .isInstanceOf(LedgerException.class)
        .extracting(e -> ((LedgerException) e).code())
        .isEqualTo(ErrorCode.INSUFFICIENT_FUNDS);

    assertThatThrownBy(() -> Money.walletBalance(Money.MAX_WALLET_BALANCE + 1L, INR))
        .isInstanceOf(LedgerException.class)
        .extracting(e -> ((LedgerException) e).code())
        .isEqualTo(ErrorCode.BALANCE_LIMIT_EXCEEDED);
  }

  @Test
  void creditsAndDebitsWalletsWithBoundaryChecks() {
    Money start = Money.walletBalance(500L, INR);
    Money delta = Money.transactionAmount(200L, INR);

    assertThat(start.creditWallet(delta).minorUnits()).isEqualTo(700L);
    assertThat(start.debitWallet(delta).minorUnits()).isEqualTo(300L);

    assertThatThrownBy(() -> start.debitWallet(Money.transactionAmount(501L, INR)))
        .isInstanceOf(LedgerException.class)
        .extracting(e -> ((LedgerException) e).code())
        .isEqualTo(ErrorCode.INSUFFICIENT_FUNDS);

    Money atMax = Money.walletBalance(Money.MAX_WALLET_BALANCE, INR);
    assertThatThrownBy(() -> atMax.creditWallet(Money.transactionAmount(1L, INR)))
        .isInstanceOf(LedgerException.class)
        .extracting(e -> ((LedgerException) e).code())
        .isEqualTo(ErrorCode.BALANCE_LIMIT_EXCEEDED);
  }

  @Test
  void systemBalanceAllowsNegativesAndDetects64BitOverflow() {
    Money sys = Money.systemBalance(-1_000L, USD);
    assertThat(sys.subtractSystem(Money.transactionAmount(500L, USD)).minorUnits())
        .isEqualTo(-1_500L);
    assertThat(sys.addSystem(Money.transactionAmount(500L, USD)).minorUnits()).isEqualTo(-500L);

    Money minSys = Money.systemBalance(Long.MIN_VALUE, USD);
    assertThatThrownBy(() -> minSys.subtractSystem(Money.transactionAmount(1L, USD)))
        .isInstanceOf(LedgerException.class)
        .extracting(e -> ((LedgerException) e).code())
        .isEqualTo(ErrorCode.NUMERIC_LIMIT_EXCEEDED);

    Money maxSys = Money.systemBalance(Long.MAX_VALUE, USD);
    assertThatThrownBy(() -> maxSys.addSystem(Money.transactionAmount(1L, USD)))
        .isInstanceOf(LedgerException.class)
        .extracting(e -> ((LedgerException) e).code())
        .isEqualTo(ErrorCode.NUMERIC_LIMIT_EXCEEDED);
  }

  @Test
  void rejectsCrossCurrencyArithmetic() {
    Money inr = Money.walletBalance(100L, INR);
    Money usd = Money.transactionAmount(50L, USD);

    assertThatThrownBy(() -> inr.creditWallet(usd))
        .isInstanceOf(LedgerException.class)
        .extracting(e -> ((LedgerException) e).code())
        .isEqualTo(ErrorCode.CURRENCY_MISMATCH);
  }
}
