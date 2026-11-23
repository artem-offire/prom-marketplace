package ua.marketplace.web.seller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import ua.marketplace.order.OrderService;
import ua.marketplace.order.OrderStatus;
import ua.marketplace.order.dto.OrderDto;
import ua.marketplace.subscription.PlanCode;
import ua.marketplace.support.Html;
import ua.marketplace.support.IntegrationTest;
import ua.marketplace.support.TestFixtures;

@IntegrationTest
@AutoConfigureMockMvc
class SellerOrdersPageTest {

    private final MockMvc mvc;
    private final TestFixtures fx;
    private final OrderService orderService;

    SellerOrdersPageTest(MockMvc mvc, TestFixtures fx, OrderService orderService) {
        this.mvc = mvc;
        this.fx = fx;
        this.orderService = orderService;
    }

    private long product(long seller) {
        return fx.createProduct(seller, fx.categoryId("Електроніка"), "Заказ-" + UUID.randomUUID(), "100.00");
    }

    @Test
    void listShowsOrderAndFilters() throws Exception {
        long seller = fx.sellerWithPlan(PlanCode.START);
        OrderDto order = fx.placeCashOrder(product(seller), 1);

        String all = Html.body(mvc.perform(get("/cabinet/" + seller + "/orders")).andExpect(status().isOk()).andReturn());
        assertThat(all).contains("№" + order.id()).contains("Нове");
        Html.assertClean(all);

        String shipped = Html.body(mvc.perform(get("/cabinet/" + seller + "/orders").param("status", "SHIPPED"))
            .andExpect(status().isOk()).andReturn());
        assertThat(shipped).contains("Немає замовлень з таким статусом");
    }

    @Test
    void cashOrderMovesThroughConfirmAndShip() throws Exception {
        long seller = fx.sellerWithPlan(PlanCode.START);
        OrderDto order = fx.placeCashOrder(product(seller), 1);
        String base = "/cabinet/" + seller + "/orders/" + order.id();

        String detail = Html.body(mvc.perform(get(base)).andExpect(status().isOk()).andReturn());
        assertThat(detail).contains("+38050***33").doesNotContain("+380501112233");

        mvc.perform(post(base + "/confirm")).andExpect(flash().attributeExists("flashSuccess"));
        assertThat(orderService.get(order.id()).status()).isEqualTo(OrderStatus.CONFIRMED);
        mvc.perform(post(base + "/ship")).andExpect(flash().attributeExists("flashSuccess"));
        assertThat(orderService.get(order.id()).status()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void forbiddenTransitionFlashesError() throws Exception {
        long seller = fx.sellerWithPlan(PlanCode.START);
        OrderDto order = fx.placeCashOrder(product(seller), 1);

        mvc.perform(post("/cabinet/" + seller + "/orders/" + order.id() + "/ship"))
            .andExpect(flash().attributeExists("flashError"));
        assertThat(orderService.get(order.id()).status()).isEqualTo(OrderStatus.NEW);
    }

    @Test
    void unknownActionAndForeignOrderAreNotFound() throws Exception {
        long seller = fx.sellerWithPlan(PlanCode.START);
        long other = fx.sellerWithPlan(PlanCode.START);
        OrderDto order = fx.placeCashOrder(product(seller), 1);

        mvc.perform(post("/cabinet/" + seller + "/orders/" + order.id() + "/explode")).andExpect(status().isNotFound());
        mvc.perform(get("/cabinet/" + other + "/orders/" + order.id())).andExpect(status().isNotFound());
    }

    @Test
    void onlineOrderCannotBeConfirmedBeforePayment() throws Exception {
        long seller = fx.sellerWithPlan(PlanCode.PRO);
        OrderDto order = fx.placeOnlineOrder(product(seller), 1);

        String html = Html.body(mvc.perform(get("/cabinet/" + seller + "/orders/" + order.id()))
            .andExpect(status().isOk()).andReturn());

        assertThat(html).contains("Очікує оплати");
    }
}
