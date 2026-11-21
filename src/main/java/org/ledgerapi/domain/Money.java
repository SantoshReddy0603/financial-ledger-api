package org.ledgerapi.domain;

import java.util.Objects;

/**
 * Exact signed 64-bit integer monetary value in minor units (BR-1, BR-5, BR-6, BR-7, BR-12, DC-5,
 * ADR-0006).
 *
 * <p>All arithmetic is checked via {@link Math#addExact} and {@link Math#subtractExact}, converting
 * overflow into {@link ErrorCode#NUMERIC_LIMIT_EXCEEDED}.
 */
public record Money(long minorUnits, CurrencyCode currency) {
  public static final long MIN_TRANSACTION_AMOUNT = 1L;
  public static final long MAX_TRANSACTION_AMOUNT = 1_000_000_000_000L;
  public static final long MAX_WALLET_BALANCE = (1L << 53) - 1L; // 9,007,199,254,740,991

  public Money {
    Objects.requireNonNull(currency, "currency");
  }

  /** Creates a validated positive transaction amount in minor units (BR-7). */
  public static Money transactionAmount(long minorUnits, CurrencyCode currency) {
    if (minorUnits < MIN_TRANSACTION_AMOUNT || minorUnits > MAX_TRANSACTION_AMOUNT) {
      throw new LedgerException(
          ErrorCode.INVALID_AMOUNT,
          "Amount must be between "
              + MIN_TRANSACTION_AMOUNT
              + " and "
              + MAX_TRANSACTION_AMOUNT
              + " minor units.");
    }
    return new Money(minorUnits, currency);
  }

  /** Creates a validated non-negative wallet balance within [0, 2^53 - 1] (BR-6, BR-7). */
  public static Money walletBalance(long minorUnits, CurrencyCode currency) {
    if (minorUnits < 0L) {
      throw new LedgerException(ErrorCode.INSUFFICIENT_FUNDS, "Wallet balance cannot be negative.");
    }
    if (minorUnits > MAX_WALLET_BALANCE) {
      throw new LedgerException(
          ErrorCode.BALANCE_LIMIT_EXCEEDED,
          "Wallet balance cannot exceed " + MAX_WALLET_BALANCE + " minor units.");
    }
    return new Money(minorUnits, currency);
  }

  /** Creates a system funding account balance (may be negative per BR-6). */
  public static Money systemBalance(long minorUnits, CurrencyCode currency) {
    return new Money(minorUnits, currency);
  }

  /** Credits a wallet balance with overflow and upper-bound checks (ADR-0006). */
  public Money creditWallet(Money amount) {
    requireSameCurrency(amount);
    long next = addExactOrThrow(this.minorUnits, amount.minorUnits);
    if (next > MAX_WALLET_BALANCE) {
      throw new LedgerException(
          ErrorCode.BALANCE_LIMIT_EXCEEDED,
          "Resulting wallet balance exceeds maximum allowed limit.");
    }
    return new Money(next, currency);
  }

  /** Debits a wallet balance, rejecting overdrafts with {@link ErrorCode#INSUFFICIENT_FUNDS}. */
  public Money debitWallet(Money amount) {
    requireSameCurrency(amount);
    long next = subtractExactOrThrow(this.minorUnits, amount.minorUnits);
    if (next < 0L) {
      throw new LedgerException(
          ErrorCode.INSUFFICIENT_FUNDS, "Insufficient funds in source wallet.");
    }
    return new Money(next, currency);
  }

  /** Adds a signed delta to a system account balance with 64-bit overflow protection. */
  public Money addSystem(Money amount) {
    requireSameCurrency(amount);
    return new Money(addExactOrThrow(this.minorUnits, amount.minorUnits), currency);
  }

  /** Subtracts a signed delta from a system account balance with 64-bit overflow protection. */
  public Money subtractSystem(Money amount) {
    requireSameCurrency(amount);
    return new Money(subtractExactOrThrow(this.minorUnits, amount.minorUnits), currency);
  }

  private void requireSameCurrency(Money other) {
    Objects.requireNonNull(other, "other");
    if (!this.currency.equals(other.currency)) {
      throw new LedgerException(
          ErrorCode.CURRENCY_MISMATCH, "Cannot combine monetary values of different currencies.");
    }
  }

  private static long addExactOrThrow(long a, long b) {
    try {
      return Math.addExact(a, b);
    } catch (ArithmeticException e) {
      throw new LedgerException(
          ErrorCode.NUMERIC_LIMIT_EXCEEDED, "Signed 64-bit integer addition overflow.");
    }
  }

  private static long subtractExactOrThrow(long a, long b) {
    try {
      return Math.subtractExact(a, b);
    } catch (ArithmeticException e) {
      throw new LedgerException(
          ErrorCode.NUMERIC_LIMIT_EXCEEDED, "Signed 64-bit integer subtraction overflow.");
    }
  }
}
