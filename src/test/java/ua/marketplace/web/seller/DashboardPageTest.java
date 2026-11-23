package ua.marketplace.web.seller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import ua.marketplace.subscription.PlanCode;
import ua.marketplace.support.Html;
import ua.marketplace.support.IntegrationTest;
import ua.marketplace.support.TestFixtures;

@IntegrationTest
@AutoConfigureMockMvc
class DashboardPageTest {

    private final MockMvc mvc;
    private final TestFixtures fx;

    DashboardPageTest(MockMvc mvc, TestFixtures fx) {
        this.mvc = mvc;
        this.fx = fx;
    }

    @Test
    void freeSellerSeesHiddenCatalogWarningAndUsage() throws Exception {
        long seller = fx.sellerWithPlan(PlanCode.FREE);

        String html = Html.body(mvc.perform(get("/cabinet/" + seller)).andExpect(status().isOk()).andReturn());

        assertThat(html).contains("Товари не показуються у вітрині на пакеті FREE").contains("0 з 50")
            .contains(Html.uah("0,00"));
        Html.assertClean(html);
    }

    @Test
    void startSellerHasNoWarningAndShowsBalance() throws Exception {
        long seller = fx.sellerWithPlan(PlanCode.START);
        fx.topUp(seller, "1234.56");

        String html = Html.body(mvc.perform(get("/cabinet/" + seller)).andExpect(status().isOk()).andReturn());

        assertThat(html).doesNotContain("Товари не показуються у вітрині").contains(Html.uah("1 234,56"))
            .contains("0 з 1000");
    }
}
