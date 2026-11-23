package ua.marketplace.web.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ua.marketplace.order.OrderService;
import ua.marketplace.order.OrderStatus;
import ua.marketplace.order.dto.OrderDto;
import ua.marketplace.payment.PaymentService;
import ua.marketplace.payment.PaymentStatus;
import ua.marketplace.subscription.PlanCode;
import ua.marketplace.support.Html;
import ua.marketplace.support.IntegrationTest;
import ua.marketplace.support.TestFixtures;

@IntegrationTest
@AutoConfigureMockMvc
class CheckoutPageTest {

    private final MockMvc mvc;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final TestFixtures fx;

    CheckoutPageTest(MockMvc mvc, OrderService orderService, PaymentService paymentService, TestFixtures fx) {
        this.mvc = mvc;
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.fx = fx;
    }

    private long product(PlanCode plan, String price) {
        long seller = fx.sellerWithPlan(plan);
        return fx.createProduct(seller, fx.categoryId("Електроніка"), "Замовлення-" + UUID.randomUUID(), price);
    }

    private MockHttpServletRequestBuilder checkout(long productId, String name, String method) {
        return post("/checkout").param("productId", String.valueOf(productId)).param("quantity", "1")
            .param("buyerName", name).param("buyerPhone", "+380 50 111 22 33").param("buyerEmail", "")
            .param("paymentMethod", method);
    }

    @Test
    void formShowsSummaryWithTotal() throws Exception {
        long product = product(PlanCode.START, "1000.00");

        String html = Html.body(mvc.perform(get("/checkout").param("productId", String.valueOf(product))
            .param("quantity", "2")).andExpect(status().isOk()).andReturn());

        assertThat(html).contains("Оформлення замовлення").contains(Html.uah("2 000,00"))
            .contains("value=\"CASH_ON_DELIVERY\"");
        Html.assertClean(html);
    }

    @Test
    void sellerWithoutOnlinePaymentsHasNoOnlineOption() throws Exception {
        long product = product(PlanCode.START, "100.00");

        String html = Html.body(mvc.perform(get("/checkout").param("productId", String.valueOf(product)))
            .andExpect(status().isOk()).andReturn());

        assertThat(html).doesNotContain("value=\"ONLINE\"");
    }

    @Test
    void blankNameRerendersFormWithMessage() throws Exception {
        long product = product(PlanCode.START, "100.00");

        String html = Html.body(mvc.perform(checkout(product, "", "CASH_ON_DELIVERY"))
            .andExpect(status().isOk()).andReturn());

        assertThat(html).contains("Вкажіть ім\u2019я").contains("Оформлення замовлення");
    }

    @Test
    void quantityAboveStockIsRejected() throws Exception {
        long product = product(PlanCode.START, "100.00");

        String html = Html.body(mvc.perform(checkout(product, "Іван", "CASH_ON_DELIVERY").param("quantity", "11"))
            .andExpect(status().isOk()).andReturn());

        assertThat(html).contains("Доступно лише 10 шт.");
    }

    @Test
    void onlineMethodForSellerWithoutFeatureIsRejected() throws Exception {
        long product = product(PlanCode.START, "100.00");

        String html = Html.body(mvc.perform(checkout(product, "Іван", "ONLINE"))
            .andExpect(status().isOk()).andReturn());

        assertThat(html).contains("Онлайн-оплата недоступна для цього продавця");
    }

    @Test
    void cashOrderRedirectsToStatusPageWithMaskedPhone() throws Exception {
        long product = product(PlanCode.START, "100.00");

        MvcResult r = mvc.perform(checkout(product, "Іван Петренко", "CASH_ON_DELIVERY"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/orders/*"))
            .andExpect(flash().attributeExists("flashSuccess"))
            .andReturn();
        long orderId = Html.lastId(r.getResponse().getRedirectedUrl());

        OrderDto order = orderService.get(orderId);
        assertThat(order.status()).isEqualTo(OrderStatus.NEW);
        assertThat(order.buyerPhone()).isEqualTo("+380501112233");

        String html = Html.body(mvc.perform(get("/orders/" + orderId)).andExpect(status().isOk()).andReturn());
        assertThat(html).contains("Замовлення №" + orderId).contains("Нове").contains("+38050***33")
            .doesNotContain("+380501112233");
        Html.assertClean(html);
    }

    @Test
    void onlineOrderIsPaidThroughDevTools() throws Exception {
        long product = product(PlanCode.PRO, "100.00");

        MvcResult r = mvc.perform(checkout(product, "Іван", "ONLINE"))
            .andExpect(status().is3xxRedirection())
            .andReturn();
        long orderId = Html.lastId(r.getResponse().getRedirectedUrl());

        String pending = Html.body(mvc.perform(get("/orders/" + orderId)).andExpect(status().isOk()).andReturn());
        assertThat(pending).contains("Очікуємо оплату").contains("Режим розробки");

        long paymentId = paymentService.findByOrder(orderId).orElseThrow().id();
        mvc.perform(post("/dev/payments/{id}/success", paymentId))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/orders/" + orderId));

        assertThat(paymentService.findByOrder(orderId).orElseThrow().status()).isEqualTo(PaymentStatus.CAPTURED);
        String paid = Html.body(mvc.perform(get("/orders/" + orderId)).andExpect(status().isOk()).andReturn());
        assertThat(paid).contains("Оплачено").doesNotContain("Режим розробки");
    }

    @Test
    void failedPaymentCanBeRetriedFromStatusPage() throws Exception {
        long product = product(PlanCode.PRO, "100.00");
        OrderDto order = fx.placeOnlineOrder(product, 1);
        long paymentId = fx.initPayment(order.id()).id();

        mvc.perform(post("/dev/payments/{id}/failure", paymentId)).andExpect(status().is3xxRedirection());

        String failed = Html.body(mvc.perform(get("/orders/" + order.id())).andExpect(status().isOk()).andReturn());
        assertThat(failed).contains("Оплата не пройшла").contains("Оплатити ще раз");

        mvc.perform(post("/orders/{id}/pay", order.id()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/orders/" + order.id()));
        assertThat(paymentService.findByOrder(order.id()).orElseThrow().status()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void unknownOrderIsNotFound() throws Exception {
        mvc.perform(get("/orders/999999999")).andExpect(status().isNotFound());
    }
}
