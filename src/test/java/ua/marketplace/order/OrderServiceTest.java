package ua.marketplace.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ua.marketplace.billing.AccountCodes.REVENUE_COMMISSION;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.RecordApplicationEvents;
import ua.marketplace.catalog.CategoryService;
import ua.marketplace.catalog.dto.CreateCategoryRequest;
import ua.marketplace.catalog.dto.UpdateCategoryPricingRequest;
import ua.marketplace.common.error.BusinessRuleException;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.common.error.NotFoundException;
import ua.marketplace.common.money.Money;
import ua.marketplace.order.dto.CheckoutItem;
import ua.marketplace.order.dto.CheckoutRequest;
import ua.marketplace.order.dto.OrderDto;
import ua.marketplace.order.dto.OrderItemDto;
import ua.marketplace.subscription.PlanCode;
import ua.marketplace.support.IntegrationTest;
import ua.marketplace.support.TestFixtures;

@IntegrationTest
@RecordApplicationEvents
class OrderServiceTest {

    private final OrderService orderService;
    private final CategoryService categoryService;
    private final TestFixtures fx;

    @Autowired
    private ApplicationEvents events;

    OrderServiceTest(OrderService orderService, CategoryService categoryService, TestFixtures fx) {
        this.orderService = orderService;
        this.categoryService = categoryService;
        this.fx = fx;
    }

    private CheckoutRequest cod(List<CheckoutItem> items) {
        return new CheckoutRequest("Покупець", "+380501112233", null, PaymentMethod.CASH_ON_DELIVERY, items);
    }

    @Test
    void confirmCancelFlowRestoresCommission() {
        long seller = fx.sellerWithPlan(PlanCode.PRO);
        long product = fx.createProduct(seller, fx.categoryId("Електроніка"), "Ноутбук-" + UUID.randomUUID(),
            "1000.00");
        Money revenueBefore = fx.ledger(REVENUE_COMMISSION);

        OrderDto order = orderService.checkout(cod(List.of(new CheckoutItem(product, 2))));
        assertThat(order.total()).isEqualByComparingTo("2000.00");
        assertThat(order.status()).isEqualTo(OrderStatus.NEW);
        assertThat(order.commissionAmount()).isEqualByComparingTo("0.00");

        OrderDto confirmed = orderService.apply(order.id(), OrderAction.CONFIRM);
        assertThat(confirmed.commissionAmount()).isEqualByComparingTo("100.00");
        assertThat(confirmed.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(fx.balance(seller)).isEqualTo(Money.of("-100.00"));
        assertThat(fx.ledger(REVENUE_COMMISSION).minus(revenueBefore)).isEqualTo(Money.of("100.00"));

        OrderDto cancelled = orderService.apply(order.id(), OrderAction.CANCEL);
        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(fx.balance(seller)).isEqualTo(Money.ZERO);
        assertThat(fx.ledger(REVENUE_COMMISSION).minus(revenueBefore)).isEqualTo(Money.ZERO);
        assertThat(events.stream(OrderCancelledEvent.class)).hasSize(1);

        assertThatThrownBy(() -> orderService.apply(order.id(), OrderAction.CANCEL))
            .isInstanceOfSatisfying(BusinessRuleException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_ORDER_TRANSITION));
    }

    @Test
    void cancelFromNewPostsNothingButPublishesEvent() {
        long seller = fx.sellerWithPlan(PlanCode.PRO);
        long product = fx.createProduct(seller, fx.categoryId("Електроніка"), "Кабель-" + UUID.randomUUID(),
            "50.00");
        OrderDto order = orderService.checkout(cod(List.of(new CheckoutItem(product, 1))));

        OrderDto cancelled = orderService.apply(order.id(), OrderAction.CANCEL);

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(fx.balance(seller)).isEqualTo(Money.ZERO);
        assertThat(events.stream(OrderCancelledEvent.class)).hasSize(1);
    }

    @Test
    void fullPathAndReturnRefundsOnce() {
        long seller = fx.sellerWithPlan(PlanCode.PRO);
        long product = fx.createProduct(seller, fx.categoryId("Електроніка"), "Телевізор-" + UUID.randomUUID(),
            "1000.00");
        OrderDto order = orderService.checkout(cod(List.of(new CheckoutItem(product, 1))));

        orderService.apply(order.id(), OrderAction.CONFIRM);
        orderService.apply(order.id(), OrderAction.SHIP);
        OrderDto completed = orderService.apply(order.id(), OrderAction.COMPLETE);
        assertThat(completed.completedAt()).isNotNull();

        OrderDto returned = orderService.apply(order.id(), OrderAction.RETURN);
        assertThat(returned.status()).isEqualTo(OrderStatus.RETURNED);
        assertThat(fx.balance(seller)).isEqualTo(Money.ZERO);

        assertThatThrownBy(() -> orderService.apply(order.id(), OrderAction.RETURN))
            .isInstanceOfSatisfying(BusinessRuleException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_ORDER_TRANSITION));
    }

    @Test
    void commissionIsCalculatedPerLine() {
        long seller = fx.sellerWithPlan(PlanCode.PRO);
        long e1 = fx.createProduct(seller, fx.categoryId("Електроніка"), "E-" + UUID.randomUUID(), "333.33");
        long c1 = fx.createProduct(seller, fx.categoryId("Одяг і взуття"), "C-" + UUID.randomUUID(), "199.99");
        long h1 = fx.createProduct(seller, fx.categoryId("Дім і сад"), "H-" + UUID.randomUUID(), "10.10");

        OrderDto order = orderService.checkout(cod(List.of(
            new CheckoutItem(e1, 1), new CheckoutItem(c1, 1), new CheckoutItem(h1, 1))));
        OrderDto confirmed = orderService.apply(order.id(), OrderAction.CONFIRM);

        assertThat(confirmed.commissionAmount()).isEqualByComparingTo("37.48");
        for (OrderItemDto item : confirmed.items()) {
            if (item.productId().equals(e1)) {
                assertThat(item.commissionAmount()).isEqualByComparingTo("16.67");
            } else if (item.productId().equals(c1)) {
                assertThat(item.commissionAmount()).isEqualByComparingTo("20.00");
            } else if (item.productId().equals(h1)) {
                assertThat(item.commissionAmount()).isEqualByComparingTo("0.81");
            }
        }
    }

    @Test
    void commissionUsesSnapshotRateAtCheckout() {
        long seller = fx.sellerWithPlan(PlanCode.PRO);
        long catId = categoryService.create(new CreateCategoryRequest(
            "Снапшот-" + UUID.randomUUID(), null, new BigDecimal("5.00"), new BigDecimal("1.00"))).id();
        long product = fx.createProduct(seller, catId, "Знімок-" + UUID.randomUUID(), "1000.00");
        OrderDto order = orderService.checkout(cod(List.of(new CheckoutItem(product, 1))));

        categoryService.updatePricing(catId,
            new UpdateCategoryPricingRequest(new BigDecimal("12.50"), new BigDecimal("1.00")));

        OrderDto confirmed = orderService.apply(order.id(), OrderAction.CONFIRM);
        assertThat(confirmed.commissionAmount()).isEqualByComparingTo("50.00");
    }

    @Test
    void checkoutRejectionsAndConfirmPaymentRule() {
        long pro1 = fx.sellerWithPlan(PlanCode.PRO);
        long pro2 = fx.sellerWithPlan(PlanCode.PRO);
        long start = fx.sellerWithPlan(PlanCode.START);
        long free = fx.sellerWithPlan(PlanCode.FREE);
        long cat = fx.categoryId("Електроніка");
        long p1 = fx.createProduct(pro1, cat, "A-" + UUID.randomUUID(), "100.00");
        long p2 = fx.createProduct(pro2, cat, "B-" + UUID.randomUUID(), "100.00");
        long pStart = fx.createProduct(start, cat, "S-" + UUID.randomUUID(), "100.00");
        long pFree = fx.createProduct(free, cat, "F-" + UUID.randomUUID(), "100.00");

        assertThatThrownBy(() -> orderService.checkout(cod(List.of(new CheckoutItem(p1, 1), new CheckoutItem(p2, 1)))))
            .isInstanceOfSatisfying(BusinessRuleException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.MIXED_SELLERS));

        assertThatThrownBy(() -> orderService.checkout(cod(List.of(new CheckoutItem(pFree, 1)))))
            .isInstanceOfSatisfying(BusinessRuleException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.PRODUCT_NOT_AVAILABLE));

        assertThatThrownBy(() -> orderService.checkout(cod(List.of(new CheckoutItem(Long.MAX_VALUE, 1)))))
            .isInstanceOfSatisfying(NotFoundException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND));

        assertThatThrownBy(() -> orderService.checkout(cod(List.of(new CheckoutItem(p1, 1), new CheckoutItem(p1, 2)))))
            .isInstanceOfSatisfying(BusinessRuleException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_ERROR));

        assertThatThrownBy(() -> orderService.checkout(new CheckoutRequest("Покупець", "+380501112233", null,
            PaymentMethod.ONLINE, List.of(new CheckoutItem(pStart, 1)))))
            .isInstanceOfSatisfying(BusinessRuleException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.FEATURE_NOT_IN_PLAN));

        OrderDto online = orderService.checkout(new CheckoutRequest("Покупець", "+380501112233", null,
            PaymentMethod.ONLINE, List.of(new CheckoutItem(p1, 1))));
        assertThatThrownBy(() -> orderService.apply(online.id(), OrderAction.CONFIRM))
            .isInstanceOfSatisfying(BusinessRuleException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.PAYMENT_REQUIRED));
    }

    @Test
    void zeroCommissionPostsNothing() {
        long seller = fx.sellerWithPlan(PlanCode.PRO);
        long product = fx.createProduct(seller, fx.categoryId("Електроніка"), "Копійка-" + UUID.randomUUID(), "0.01");
        OrderDto order = orderService.checkout(cod(List.of(new CheckoutItem(product, 1))));

        OrderDto confirmed = orderService.apply(order.id(), OrderAction.CONFIRM);

        assertThat(confirmed.commissionAmount()).isEqualByComparingTo("0.00");
        assertThat(fx.balance(seller)).isEqualTo(Money.ZERO);
    }
}
