package ua.marketplace.web.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ua.marketplace.order.dto.OrderDto;
import ua.marketplace.payment.PaymentService;
import ua.marketplace.payment.PaymentStatus;
import ua.marketplace.subscription.PlanCode;
import ua.marketplace.support.Html;
import ua.marketplace.support.IntegrationTest;
import ua.marketplace.support.TestFixtures;

/** Окремий контекст: app.ui.dev-tools=false. */
@IntegrationTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.ui.dev-tools=false")
class DevToolsDisabledTest {

    private final MockMvc mvc;
    private final PaymentService paymentService;
    private final TestFixtures fx;

    DevToolsDisabledTest(MockMvc mvc, PaymentService paymentService, TestFixtures fx) {
        this.mvc = mvc;
        this.paymentService = paymentService;
        this.fx = fx;
    }

    @Test
    void devEndpointIsGoneAndButtonsAreHidden() throws Exception {
        long seller = fx.sellerWithPlan(PlanCode.PRO);
        long product = fx.createProduct(seller, fx.categoryId("Авто"), "Dev-" + UUID.randomUUID(), "100.00");
        OrderDto order = fx.placeOnlineOrder(product, 1);
        long paymentId = fx.initPayment(order.id()).id();

        String html = Html.body(mvc.perform(get("/orders/" + order.id())).andExpect(status().isOk()).andReturn());
        assertThat(html).contains("Очікуємо оплату").doesNotContain("/dev/payments").doesNotContain("Режим розробки");

        mvc.perform(post("/dev/payments/{id}/success", paymentId)).andExpect(status().is4xxClientError());
        assertThat(paymentService.get(paymentId).status()).isEqualTo(PaymentStatus.PENDING);
    }
}
