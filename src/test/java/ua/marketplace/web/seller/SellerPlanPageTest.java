package ua.marketplace.web.seller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import ua.marketplace.subscription.PlanCode;
import ua.marketplace.subscription.SubscriptionService;
import ua.marketplace.support.Html;
import ua.marketplace.support.IntegrationTest;
import ua.marketplace.support.TestFixtures;

@IntegrationTest
@AutoConfigureMockMvc
class SellerPlanPageTest {

    private final MockMvc mvc;
    private final TestFixtures fx;
    private final SubscriptionService subscriptionService;

    SellerPlanPageTest(MockMvc mvc, TestFixtures fx, SubscriptionService subscriptionService) {
        this.mvc = mvc;
        this.fx = fx;
        this.subscriptionService = subscriptionService;
    }

    @Test
    void plansPageListsAllPlans() throws Exception {
        long seller = fx.sellerWithPlan(PlanCode.FREE);

        String html = Html.body(mvc.perform(get("/cabinet/" + seller + "/plan")).andExpect(status().isOk()).andReturn());

        assertThat(html).contains("FREE").contains("START").contains("PRO").contains("BUSINESS")
            .contains(Html.uah("990,00"));
        Html.assertClean(html);
    }

    @Test
    void purchaseFromFreeActivatesPlan() throws Exception {
        long seller = fx.registerSeller();
        fx.topUp(seller, "1000.00");

        mvc.perform(post("/cabinet/" + seller + "/plan/purchase").param("planCode", "START").param("period", "MONTHLY"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/cabinet/" + seller + "/plan"))
            .andExpect(flash().attributeExists("flashSuccess"));

        assertThat(subscriptionService.getActive(seller).planCode()).isEqualTo(PlanCode.START);
    }

    @Test
    void purchaseWithoutMoneyFlashesError() throws Exception {
        long seller = fx.registerSeller();

        mvc.perform(post("/cabinet/" + seller + "/plan/purchase").param("planCode", "START").param("period", "MONTHLY"))
            .andExpect(flash().attributeExists("flashError"));

        assertThat(subscriptionService.getActive(seller).planCode()).isEqualTo(PlanCode.FREE);
    }

    @Test
    void purchaseFromPaidPlanIsBlockedWithExplanation() throws Exception {
        long seller = fx.sellerWithPlan(PlanCode.START);
        fx.topUp(seller, "5000.00");

        mvc.perform(post("/cabinet/" + seller + "/plan/purchase").param("planCode", "PRO").param("period", "MONTHLY"))
            .andExpect(flash().attribute("flashError",
                "Зміна пакета з перерахунком стане доступною найближчим часом"));

        assertThat(subscriptionService.getActive(seller).planCode()).isEqualTo(PlanCode.START);
        String html = Html.body(mvc.perform(get("/cabinet/" + seller + "/plan")).andExpect(status().isOk()).andReturn());
        assertThat(html).contains("Зміна пакета з перерахунком стане доступною найближчим часом");
    }
}
