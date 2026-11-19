package ua.marketplace.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ua.marketplace.billing.dto.TopUpRequest;
import ua.marketplace.billing.dto.TopUpResponse;
import ua.marketplace.common.error.BusinessRuleException;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.common.error.NotFoundException;
import ua.marketplace.common.money.Money;
import ua.marketplace.support.IntegrationTest;
import ua.marketplace.support.TestFixtures;

@IntegrationTest
class BalanceServiceTest {

    private final BalanceService balanceService;
    private final LedgerAccountRepository accountRepo;
    private final TestFixtures fx;

    BalanceServiceTest(BalanceService balanceService, LedgerAccountRepository accountRepo, TestFixtures fx) {
        this.balanceService = balanceService;
        this.accountRepo = accountRepo;
        this.fx = fx;
    }

    @Test
    void repeatedTopUpWithSameKeyCreditsOnce() {
        long s = fx.registerSeller();
        String key = UUID.randomUUID().toString();

        TopUpResponse first = balanceService.topUp(s, new TopUpRequest(new BigDecimal("100.00"), key));
        TopUpResponse second = balanceService.topUp(s, new TopUpRequest(new BigDecimal("100.00"), key));

        assertThat(first.replayed()).isFalse();
        assertThat(second.replayed()).isTrue();
        assertThat(balanceService.balanceOf(s)).isEqualTo(Money.of("100.00"));
    }

    @Test
    void topUpForUnknownSellerIsNotFound() {
        assertThatThrownBy(() -> balanceService.topUp(Long.MAX_VALUE,
            new TopUpRequest(new BigDecimal("1.00"), UUID.randomUUID().toString())))
            .isInstanceOfSatisfying(NotFoundException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.SELLER_NOT_FOUND));
    }

    @Test
    void nonPositiveAmountIsRejectedByService() {
        long s = fx.registerSeller();
        assertThatThrownBy(() -> balanceService.topUp(s,
            new TopUpRequest(BigDecimal.ZERO, UUID.randomUUID().toString())))
            .isInstanceOfSatisfying(BusinessRuleException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void openSellerAccountsIsIdempotent() {
        long s = fx.registerSeller();
        balanceService.openSellerAccounts(s);
        balanceService.openSellerAccounts(s);

        long count = accountRepo.findAll().stream()
            .filter(a -> a.getSellerId() != null && a.getSellerId() == s)
            .count();
        assertThat(count).isEqualTo(2);
    }

    @Test
    void listsEntriesWithFiltersAndNewestFirst() {
        long s = fx.registerSeller();
        fx.topUp(s, "10.00");
        fx.topUp(s, "20.00");

        assertThat(balanceService.listEntries(s, null, 0, 20).totalElements()).isEqualTo(2);
        assertThat(balanceService.listEntries(s, TransactionType.TOP_UP, 0, 20).totalElements()).isEqualTo(2);
        assertThat(balanceService.listEntries(s, TransactionType.SUBSCRIPTION_CHARGE, 0, 20).totalElements())
            .isZero();
        assertThat(balanceService.listEntries(s, null, 0, 20).content().get(0).amount())
            .isEqualByComparingTo("20.00");
    }
}
