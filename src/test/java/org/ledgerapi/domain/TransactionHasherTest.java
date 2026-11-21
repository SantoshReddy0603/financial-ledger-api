package org.ledgerapi.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class TransactionHasherTest {
  @Test
  void ordersEntriesByEntryNumberAndProducesStableChainHash() {
    TransactionHasher.EntryInput credit =
        new TransactionHasher.EntryInput(
            2L,
            "22222222-2222-4222-8222-222222222222",
            "CREDIT",
            2500L,
            "INR",
            "2026-11-21T10:00:00.000Z");
    TransactionHasher.EntryInput debit =
        new TransactionHasher.EntryInput(
            1L,
            "11111111-1111-4111-8111-111111111111",
            "DEBIT",
            2500L,
            "INR",
            "2026-11-21T10:00:00.000Z");

    TransactionHasher.TransactionInput txCreditFirst =
        new TransactionHasher.TransactionInput(
            "99999999-9999-4999-8999-999999999999",
            1L,
            "TRANSFER",
            "11111111-1111-4111-8111-111111111111",
            "22222222-2222-4222-8222-222222222222",
            2500L,
            "INR",
            "rent",
            null,
            null,
            "2026-11-21T10:00:00.000Z",
            TransactionHasher.GENESIS_PREVIOUS_HASH,
            List.of(credit, debit));

    TransactionHasher.TransactionInput txDebitFirst =
        new TransactionHasher.TransactionInput(
            "99999999-9999-4999-8999-999999999999",
            1L,
            "TRANSFER",
            "11111111-1111-4111-8111-111111111111",
            "22222222-2222-4222-8222-222222222222",
            2500L,
            "INR",
            "rent",
            null,
            null,
            "2026-11-21T10:00:00.000Z",
            TransactionHasher.GENESIS_PREVIOUS_HASH,
            List.of(debit, credit));

    assertThat(TransactionHasher.canonicalPayload(txCreditFirst))
        .isEqualTo(TransactionHasher.canonicalPayload(txDebitFirst));

    String hash1 = TransactionHasher.hash(txDebitFirst);
    assertThat(hash1).hasSize(64).matches("^[0-9a-f]{64}$");

    // Tampering with amount must change the hash
    TransactionHasher.TransactionInput tampered =
        new TransactionHasher.TransactionInput(
            txDebitFirst.transactionId(),
            txDebitFirst.sequence(),
            txDebitFirst.type(),
            txDebitFirst.sourceAccountId(),
            txDebitFirst.destinationAccountId(),
            2501L,
            txDebitFirst.currency(),
            txDebitFirst.reference(),
            txDebitFirst.reason(),
            txDebitFirst.reversalOf(),
            txDebitFirst.createdAt(),
            txDebitFirst.previousHash(),
            txDebitFirst.entries());

    assertThat(TransactionHasher.hash(tampered)).isNotEqualTo(hash1);
  }
}
