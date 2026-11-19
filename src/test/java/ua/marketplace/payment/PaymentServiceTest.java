package ua.marketplace.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ua.marketplace.billing.AccountCodes.PLATFORM_CLEARING;
import static ua.marketplace.billing.AccountCodes.REVENUE_PAYMENT_FEE;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ua.marketplace.billing.BalanceService;
import ua.marketplace.common.error.BusinessRuleException;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.common.error.NotFoundException;
import ua.marketplace.common.money.Money;
import ua.marketplace.order.OrderAction;
import ua.marketplace.order.OrderService;
import ua.marketplace.order.OrderStatus;
import ua.marketplace.order.PaymentMethod;
import ua.marketplace.order.dto.CheckoutItem;
import ua.marketplace.order.dto.CheckoutRequest;
import ua.marketplace.order.dto.OrderDto;
import ua.marketplace.payment.dto.PaymentDto;
import ua.marketplace.payment.dto.WebhookRequest;
import ua.marketplace.subscription.PlanCode;
import ua.marketplace.support.IntegrationTest;
import ua.marketplace.support.TestFixtures;

@IntegrationTest
class PaymentServiceTest {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final BalanceService balanceService;
    private final TestFixtures fx;

    PaymentServiceTest(PaymentService paymentService, OrderService orderService, BalanceService balanceService,
                       TestFixtures fx) {
        this.paymentService = paymentService;
        this.orderService = orderService;
        this.balanceService = balanceService;
        this.fx = fx;
    }

    private long proSeller() {
        return fx.sellerWithPlan(PlanCode.PRO);
    }

    private OrderDto onlineOrder(long seller, String price) {
        long product = fx.createProduct(seller, fx.categoryId("Електроніка"), "Онлайн-" + UUID.randomUUID(), price);
        return orderService.checkout(new CheckoutRequest("Покупець", "+380501112233", null, PaymentMethod.ONLINE,
            List.of(new CheckoutItem(product, 1))));
    }

    @Test
    void initCreatesPendingPaymentWithFee() {
        long seller = proSeller();
        OrderDto order = onlineOrder(seller, "1234.56");

        PaymentDto p = paymentService.init(order.id());

        assertThat(p.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(p.feeAmount()).isEqualByComparingTo("43.21");
        assertThat(p.feeRate()).isEqualByComparingTo("3.50");
        assertThat(p.providerRef()).startsWith("stub-");
    }

    @Test
    void captureConfirmAndRefundFlow() {
        long seller = proSeller();
        OrderDto order = onlineOrder(seller, "1234.56");
        Money clearingBefore = fx.ledger(PLATFORM_CLEARING);
        Money feeBefore = fx.ledger(REVENUE_PAYMENT_FEE);
        PaymentDto payment = paymentService.init(order.id());

        assertThatThrownBy(() -> orderService.apply(order.id(), OrderAction.CONFIRM))
            .isInstanceOfSatisfying(BusinessRuleException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.PAYMENT_REQUIRED));

        PaymentDto captured = paymentService.handleWebhook(
            new WebhookRequest(payment.providerRef(), WebhookResult.SUCCESS));
        assertThat(captured.status()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(balanceService.payableOf(seller)).isEqualTo(Money.of("1191.35"));
        assertThat(fx.ledger(REVENUE_PAYMENT_FEE).minus(feeBefore)).isEqualTo(Money.of("43.21"));
        assertThat(fx.ledger(PLATFORM_CLEARING).minus(clearingBefore)).isEqualTo(Money.of("1234.56"));

        PaymentDto again = paymentService.handleWebhook(
            new WebhookRequest(payment.providerRef(), WebhookResult.SUCCESS));
        assertThat(again.status()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(balanceService.payableOf(seller)).isEqualTo(Money.of("1191.35"));

        assertThatThrownBy(() -> paymentService.handleWebhook(
            new WebhookRequest(payment.providerRef(), WebhookResult.FAILURE)))
            .isInstanceOfSatisfying(BusinessRuleException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_PAYMENT_STATE));

        OrderDto confirmed = orderService.apply(order.id(), OrderAction.CONFIRM);
        assertThat(confirmed.status()).isEqualTo(OrderStatus.CONFIRMED);

        PaymentDto refunded = paymentService.refund(payment.id());
        assertThat(refunded.status()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(balanceService.payableOf(seller)).isEqualTo(Money.of("-43.21"));
        assertThat(fx.ledger(REVENUE_PAYMENT_FEE).minus(feeBefore)).isEqualTo(Money.of("43.21"));
        assertThat(fx.ledger(PLATFORM_CLEARING).minus(clearingBefore)).isEqualTo(Money.ZERO);

        assertThatThrownBy(() -> paymentService.refund(payment.id()))
            .isInstanceOfSatisfying(BusinessRuleException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_PAYMENT_STATE));
    }

    @Test
    void failedPaymentCanBeRestarted() {
        long seller = proSeller();
        OrderDto order = onlineOrder(seller, "100.00");
        PaymentDto first = paymentService.init(order.id());

        PaymentDto failed = paymentService.handleWebhook(new WebhookRequest(first.providerRef(),
            WebhookResult.FAILURE));
        assertThat(failed.status()).isEqualTo(PaymentStatus.FAILED);

        PaymentDto retry = paymentService.init(order.id());
        assertThat(retry.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(retry.id()).isEqualTo(first.id());
        assertThat(retry.providerRef()).isNotEqualTo(first.providerRef());
    }

    @Test
    void initRejectionsAndUnknownReferences() {
        long seller = proSeller();
        long product = fx.createProduct(seller, fx.categoryId("Електроніка"), "Накладений-" + UUID.randomUUID(),
            "100.00");
        OrderDto cod = orderService.checkout(new CheckoutRequest("Покупець", "+380501112233", null,
            PaymentMethod.CASH_ON_DELIVERY, List.of(new CheckoutItem(product, 1))));
        assertThatThrownBy(() -> paymentService.init(cod.id()))
            .isInstanceOfSatisfying(BusinessRuleException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.ORDER_NOT_PAYABLE));

        OrderDto online = onlineOrder(seller, "100.00");
        PaymentDto captured = paymentService.init(online.id());
        paymentService.handleWebhook(new WebhookRequest(captured.providerRef(), WebhookResult.SUCCESS));
        assertThatThrownBy(() -> paymentService.init(online.id()))
            .isInstanceOfSatisfying(BusinessRuleException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.PAYMENT_ALREADY_EXISTS));

        assertThatThrownBy(() -> paymentService.handleWebhook(new WebhookRequest("unknown-ref-" + UUID.randomUUID(),
            WebhookResult.SUCCESS)))
            .isInstanceOfSatisfying(NotFoundException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.PAYMENT_NOT_FOUND));
    }
}
