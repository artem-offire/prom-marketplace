package ua.marketplace.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ua.marketplace.billing.AccountCodes.PLATFORM_CLEARING;
import static ua.marketplace.billing.AccountCodes.REVENUE_ADS;
import static ua.marketplace.billing.AccountCodes.REVENUE_COMMISSION;
import static ua.marketplace.billing.AccountCodes.REVENUE_SUBSCRIPTION;
import static ua.marketplace.billing.AccountCodes.sellerBalance;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import ua.marketplace.common.error.BusinessRuleException;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.common.money.Money;
import ua.marketplace.support.IntegrationTest;
import ua.marketplace.support.TestFixtures;

@IntegrationTest
class LedgerServiceTest {

    private final LedgerService ledger;
    private final BalanceService balanceService;
    private final LedgerTransactionRepository txRepo;
    private final LedgerEntryRepository entryRepo;
    private final TestFixtures fx;

    LedgerServiceTest(LedgerService ledger, BalanceService balanceService, LedgerTransactionRepository txRepo,
                      LedgerEntryRepository entryRepo, TestFixtures fx) {
        this.ledger = ledger;
        this.balanceService = balanceService;
        this.txRepo = txRepo;
        this.entryRepo = entryRepo;
        this.fx = fx;
    }

    @Test
    void topUpCreditsSellerBalanceAndDebitsClearing() {
        long s = fx.registerSeller();
        Money clearingBefore = fx.ledger(PLATFORM_CLEARING);

        fx.topUp(s, "1000.00");

        assertThat(balanceService.balanceOf(s)).isEqualTo(Money.of("1000.00"));
        assertThat(balanceService.payableOf(s)).isEqualTo(Money.ZERO);
        assertThat(fx.ledger(PLATFORM_CLEARING).minus(clearingBefore)).isEqualTo(Money.of("1000.00"));
    }

    @Test
    void subscriptionChargeReducesBalance() {
        long s = fx.registerSeller();
        fx.topUp(s, "1000.00");
        Money revenueBefore = fx.ledger(REVENUE_SUBSCRIPTION);

        ledger.post(new Posting(TransactionType.SUBSCRIPTION_CHARGE, "TEST-SUB-" + UUID.randomUUID(), "TEST",
            "1", "test", false, List.of(
                PostingLine.debit(sellerBalance(s), Money.of("990.00")),
                PostingLine.credit(REVENUE_SUBSCRIPTION, Money.of("990.00")))));

        assertThat(balanceService.balanceOf(s)).isEqualTo(Money.of("10.00"));
        assertThat(fx.ledger(REVENUE_SUBSCRIPTION).minus(revenueBefore)).isEqualTo(Money.of("990.00"));
    }

    @Test
    void overdraftIsRejectedAndNothingChanges() {
        long s = fx.registerSeller();
        fx.topUp(s, "10.00");
        String key = "TEST-OVERDRAFT-" + UUID.randomUUID();

        assertThatThrownBy(() -> ledger.post(new Posting(TransactionType.AD_CLICK_CHARGE, key, "TEST", "1", "test",
            false, List.of(
                PostingLine.debit(sellerBalance(s), Money.of("10.01")),
                PostingLine.credit(REVENUE_ADS, Money.of("10.01"))))))
            .isInstanceOfSatisfying(BusinessRuleException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.INSUFFICIENT_FUNDS));

        assertThat(balanceService.balanceOf(s)).isEqualTo(Money.of("10.00"));
        assertThat(txRepo.findByIdempotencyKey(key)).isEmpty();
    }

    @Test
    void allowNegativePermitsOverdraftForCommission() {
        long s = fx.registerSeller();
        fx.topUp(s, "10.00");

        ledger.post(new Posting(TransactionType.ORDER_COMMISSION, "TEST-NEG-" + UUID.randomUUID(), "ORDER", "1",
            "test", true, List.of(
                PostingLine.debit(sellerBalance(s), Money.of("25.00")),
                PostingLine.credit(REVENUE_COMMISSION, Money.of("25.00")))));

        assertThat(balanceService.balanceOf(s)).isEqualTo(Money.of("-15.00"));
    }

    @Test
    void idempotencyReplaysSameAndRejectsDifferentPostings() {
        long s = fx.registerSeller();
        String key = "TEST-IDEM-" + UUID.randomUUID();
        Posting p = topUpPosting(s, key, "5.00");

        PostedTransaction first = ledger.post(p);
        PostedTransaction second = ledger.post(p);

        assertThat(first.replayed()).isFalse();
        assertThat(second.replayed()).isTrue();
        assertThat(second.transactionId()).isEqualTo(first.transactionId());
        assertThat(balanceService.balanceOf(s)).isEqualTo(Money.of("5.00"));

        assertThatThrownBy(() -> ledger.post(topUpPosting(s, key, "6.00")))
            .isInstanceOfSatisfying(BusinessRuleException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.IDEMPOTENCY_CONFLICT));
    }

    @Test
    void invalidPostingsAreProgrammingErrors() {
        long s = fx.registerSeller();
        String base = "TEST-INV-" + UUID.randomUUID();

        assertThatThrownBy(() -> ledger.post(new Posting(TransactionType.ADJUSTMENT, base + "-1", "T", "1", "t",
            false, List.of(PostingLine.debit(sellerBalance(s), Money.of("10.00")),
                PostingLine.credit(REVENUE_ADS, Money.of("9.00")))))).isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> ledger.post(new Posting(TransactionType.ADJUSTMENT, base + "-2", "T", "1", "t",
            false, List.of(PostingLine.debit(sellerBalance(s), Money.of("10.00")))))).isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> ledger.post(new Posting(TransactionType.ADJUSTMENT, base + "-3", "T", "1", "t",
            false, List.of(PostingLine.debit(sellerBalance(s), Money.ZERO),
                PostingLine.credit(REVENUE_ADS, Money.ZERO))))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void commissionRefundRestoresBalance() {
        long s = fx.registerSeller();
        fx.topUp(s, "10.00");
        Money commissionBefore = fx.ledger(REVENUE_COMMISSION);

        ledger.post(new Posting(TransactionType.ORDER_COMMISSION, "TEST-COM-" + UUID.randomUUID(), "ORDER", "1",
            "test", true, List.of(
                PostingLine.debit(sellerBalance(s), Money.of("25.00")),
                PostingLine.credit(REVENUE_COMMISSION, Money.of("25.00")))));
        ledger.post(new Posting(TransactionType.ORDER_COMMISSION_REFUND, "TEST-REF-" + UUID.randomUUID(), "ORDER",
            "1", "test", false, List.of(
                PostingLine.debit(REVENUE_COMMISSION, Money.of("25.00")),
                PostingLine.credit(sellerBalance(s), Money.of("25.00")))));

        assertThat(balanceService.balanceOf(s)).isEqualTo(Money.of("10.00"));
        assertThat(fx.ledger(REVENUE_COMMISSION).minus(commissionBefore)).isEqualTo(Money.ZERO);
    }

    @Test
    void sellerBalanceIsLiabilityAndRevenueGrowsOnCredit() {
        long s = fx.registerSeller();
        Money clearingBefore = fx.ledger(PLATFORM_CLEARING);

        fx.topUp(s, "1000.00");

        assertThat(fx.ledger(sellerBalance(s))).isEqualTo(Money.of("1000.00"));
        assertThat(fx.ledger(PLATFORM_CLEARING).minus(clearingBefore)).isEqualTo(Money.of("1000.00"));
    }

    @Test
    void concurrentChargesNeverOverdraw() throws Exception {
        long s = fx.registerSeller();
        fx.topUp(s, "500.00");

        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final String key = "TEST-CONC-" + UUID.randomUUID();
            Callable<Boolean> task = () -> {
                start.await();
                // H2 може повернути тайм-аут блокування або конфлікт версії під навантаженням CI.
                // Такі технічні збої повторюються: інваріант перевіряється на бізнес-результаті.
                RuntimeException last = null;
                for (int attempt = 0; attempt < 30; attempt++) {
                    try {
                        ledger.post(new Posting(TransactionType.AD_CLICK_CHARGE, key, "TEST", "1", "test", false,
                            List.of(PostingLine.debit(sellerBalance(s), Money.of("100.00")),
                                PostingLine.credit(REVENUE_ADS, Money.of("100.00")))));
                        return true;
                    } catch (BusinessRuleException e) {
                        assertThat(e.code()).isEqualTo(ErrorCode.INSUFFICIENT_FUNDS);
                        return false;
                    } catch (RuntimeException e) {
                        last = e;
                        Thread.sleep(20L * (attempt + 1));
                    }
                }
                throw last;
            };
            futures.add(pool.submit(task));
        }
        start.countDown();

        int ok = 0;
        int failed = 0;
        for (Future<Boolean> f : futures) {
            if (f.get(60, TimeUnit.SECONDS)) {
                ok++;
            } else {
                failed++;
            }
        }
        pool.shutdown();

        assertThat(ok).isEqualTo(5);
        assertThat(failed).isEqualTo(5);
        assertThat(balanceService.balanceOf(s)).isEqualTo(Money.ZERO);
    }

    @Test
    void everyTransactionIsBalanced() {
        long s = fx.registerSeller();
        String key = "TEST-BAL-" + UUID.randomUUID();
        PostedTransaction pt = ledger.post(topUpPosting(s, key, "42.42"));

        List<LedgerEntry> entries = entryRepo.findFetchedByTransactionId(pt.transactionId());
        Money debit = Money.ZERO;
        Money credit = Money.ZERO;
        for (LedgerEntry e : entries) {
            if (e.getDirection() == EntryDirection.DEBIT) {
                debit = debit.plus(e.getAmount());
            } else {
                credit = credit.plus(e.getAmount());
            }
        }
        assertThat(debit).isEqualTo(credit);
    }

    private Posting topUpPosting(long sellerId, String key, String amount) {
        return new Posting(TransactionType.TOP_UP, key, "SELLER", String.valueOf(sellerId), "test", false,
            List.of(PostingLine.debit(PLATFORM_CLEARING, Money.of(amount)),
                PostingLine.credit(sellerBalance(sellerId), Money.of(amount))));
    }
}
