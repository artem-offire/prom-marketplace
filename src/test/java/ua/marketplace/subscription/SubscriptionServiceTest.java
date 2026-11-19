package ua.marketplace.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ua.marketplace.billing.AccountCodes.REVENUE_SUBSCRIPTION;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import ua.marketplace.common.error.BusinessRuleException;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.common.error.NotFoundException;
import ua.marketplace.common.money.Money;
import ua.marketplace.subscription.dto.PlanDto;
import ua.marketplace.subscription.dto.SubscriptionDto;
import ua.marketplace.support.IntegrationTest;
import ua.marketplace.support.TestFixtures;

@IntegrationTest
class SubscriptionServiceTest {

    private final SubscriptionService subscriptionService;
    private final SubscriptionRepository subscriptionRepo;
    private final PlanLimits planLimits;
    private final Clock clock;
    private final TestFixtures fx;

    SubscriptionServiceTest(SubscriptionService subscriptionService, SubscriptionRepository subscriptionRepo,
                            PlanLimits planLimits, Clock clock, TestFixtures fx) {
        this.subscriptionService = subscriptionService;
        this.subscriptionRepo = subscriptionRepo;
        this.planLimits = planLimits;
        this.clock = clock;
        this.fx = fx;
    }

    @Test
    void newSellerGetsActiveFreePlan() {
        long s = fx.registerSeller();

        SubscriptionDto active = subscriptionService.getActive(s);

        assertThat(active.planCode()).isEqualTo(PlanCode.FREE);
        assertThat(active.endDate()).isNull();
        assertThat(active.pricePaid()).isEqualByComparingTo("0.00");
        assertThat(planLimits.productLimit(s)).isEqualTo(50);
        assertThat(planLimits.isMarketplaceVisible(s)).isFalse();
        assertThat(planLimits.hasFeature(s, PlanFeature.PROSALE)).isFalse();
    }

    @Test
    void purchaseWithoutFundsFailsAndKeepsFreePlan() {
        long s = fx.registerSeller();

        assertThatThrownBy(() -> subscriptionService.purchase(s, PlanCode.START, BillingPeriod.MONTHLY))
            .isInstanceOfSatisfying(BusinessRuleException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.INSUFFICIENT_FUNDS));

        assertThat(subscriptionService.getActive(s).planCode()).isEqualTo(PlanCode.FREE);
        assertThat(subscriptionRepo.findBySellerId(s)).hasSize(1);
    }

    @Test
    void purchaseCancelsPreviousAndCharges() {
        long s = fx.registerSeller();
        fx.topUp(s, "1000.00");
        Money revenueBefore = fx.ledger(REVENUE_SUBSCRIPTION);
        LocalDate today = LocalDate.now(clock);

        SubscriptionDto dto = subscriptionService.purchase(s, PlanCode.START, BillingPeriod.MONTHLY);

        assertThat(dto.planCode()).isEqualTo(PlanCode.START);
        assertThat(dto.endDate()).isEqualTo(today.plusMonths(1));
        assertThat(fx.balance(s)).isEqualTo(Money.of("10.00"));
        assertThat(fx.ledger(REVENUE_SUBSCRIPTION).minus(revenueBefore)).isEqualTo(Money.of("990.00"));

        List<Subscription> all = subscriptionRepo.findBySellerId(s);
        assertThat(all).hasSize(2);
        assertThat(all).anySatisfy(sub -> {
            assertThat(sub.getPlanCode()).isEqualTo(PlanCode.FREE);
            assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        });
        assertThat(all).anySatisfy(sub -> {
            assertThat(sub.getPlanCode()).isEqualTo(PlanCode.START);
            assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        });
    }

    @Test
    void yearlyProPurchaseUsesYearlyPrice() {
        long s = fx.registerSeller();
        fx.topUp(s, "25000.00");
        LocalDate today = LocalDate.now(clock);

        SubscriptionDto dto = subscriptionService.purchase(s, PlanCode.PRO, BillingPeriod.YEARLY);

        assertThat(dto.endDate()).isEqualTo(today.plusYears(1));
        assertThat(fx.balance(s)).isEqualTo(Money.of("100.00"));
    }

    @Test
    void freePurchaseAndUnknownSellerAreRejected() {
        long s = fx.registerSeller();

        assertThatThrownBy(() -> subscriptionService.purchase(s, PlanCode.FREE, BillingPeriod.MONTHLY))
            .isInstanceOfSatisfying(BusinessRuleException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_PLAN_CHANGE));

        assertThatThrownBy(() -> subscriptionService.purchase(Long.MAX_VALUE, PlanCode.START, BillingPeriod.MONTHLY))
            .isInstanceOfSatisfying(NotFoundException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.SELLER_NOT_FOUND));
    }

    @Test
    void planLimitsPerPlan() {
        long start = fx.sellerWithPlan(PlanCode.START);
        assertThat(planLimits.productLimit(start)).isEqualTo(1000);
        assertThat(planLimits.isMarketplaceVisible(start)).isTrue();
        assertThat(planLimits.hasFeature(start, PlanFeature.PROSALE)).isTrue();
        assertThat(planLimits.hasFeature(start, PlanFeature.ONLINE_PAYMENTS)).isFalse();

        long pro = fx.sellerWithPlan(PlanCode.PRO);
        assertThat(planLimits.hasFeature(pro, PlanFeature.ONLINE_PAYMENTS)).isTrue();
        assertThat(planLimits.hasFeature(pro, PlanFeature.ANALYTICS)).isTrue();
        assertThat(planLimits.hasFeature(pro, PlanFeature.PRIORITY_SUPPORT)).isFalse();

        long business = fx.sellerWithPlan(PlanCode.BUSINESS);
        for (PlanFeature f : PlanFeature.values()) {
            assertThat(planLimits.hasFeature(business, f)).isTrue();
        }
    }

    @Test
    void listPlansReturnsFourPlansInOrder() {
        List<PlanDto> plans = subscriptionService.listPlans();

        assertThat(plans).extracting(PlanDto::code)
            .containsExactly(PlanCode.FREE, PlanCode.START, PlanCode.PRO, PlanCode.BUSINESS);
        assertThat(plans).extracting(PlanDto::monthlyPrice)
            .usingElementComparator(java.math.BigDecimal::compareTo)
            .containsExactly(new java.math.BigDecimal("0.00"), new java.math.BigDecimal("990.00"),
                new java.math.BigDecimal("2490.00"), new java.math.BigDecimal("5990.00"));
        assertThat(plans).extracting(PlanDto::yearlyPrice)
            .usingElementComparator(java.math.BigDecimal::compareTo)
            .containsExactly(new java.math.BigDecimal("0.00"), new java.math.BigDecimal("9900.00"),
                new java.math.BigDecimal("24900.00"), new java.math.BigDecimal("59900.00"));
        PlanDto pro = plans.get(2);
        assertThat(pro.features()).isNotEmpty();
    }
}
