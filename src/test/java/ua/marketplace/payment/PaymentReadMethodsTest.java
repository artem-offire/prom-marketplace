package ua.marketplace.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import ua.marketplace.order.dto.OrderDto;
import ua.marketplace.payment.dto.PaymentDto;
import ua.marketplace.subscription.PlanCode;
import ua.marketplace.support.IntegrationTest;
import ua.marketplace.support.TestFixtures;

@IntegrationTest
class PaymentReadMethodsTest {

    private final PaymentService paymentService;
    private final TestFixtures fx;

    PaymentReadMethodsTest(PaymentService paymentService, TestFixtures fx) {
        this.paymentService = paymentService;
        this.fx = fx;
    }

    @Test
    void findByOrderIsEmptyUntilInitAndThenPending() {
        long seller = fx.sellerWithPlan(PlanCode.PRO);
        long product = fx.createProduct(seller, fx.categoryId("Електроніка"), "Pay-" + UUID.randomUUID(), "100.00");
        OrderDto order = fx.placeOnlineOrder(product, 1);

        assertThat(paymentService.findByOrder(order.id())).isEmpty();

        PaymentDto created = paymentService.init(order.id());

        PaymentDto found = paymentService.findByOrder(order.id()).orElseThrow();
        assertThat(found.id()).isEqualTo(created.id());
        assertThat(found.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(paymentService.findByOrder(Long.MAX_VALUE)).isEmpty();
    }
}
