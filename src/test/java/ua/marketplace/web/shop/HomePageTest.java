package ua.marketplace.web.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ua.marketplace.subscription.PlanCode;
import ua.marketplace.support.Html;
import ua.marketplace.support.IntegrationTest;
import ua.marketplace.support.TestFixtures;

@IntegrationTest
@AutoConfigureMockMvc
class HomePageTest {

    private final MockMvc mvc;
    private final TestFixtures fx;

    HomePageTest(MockMvc mvc, TestFixtures fx) {
        this.mvc = mvc;
        this.fx = fx;
    }

    @Test
    void homeShowsBrandCategoriesAndFooter() throws Exception {
        MvcResult r = mvc.perform(get("/")).andExpect(status().isOk()).andReturn();

        String html = Html.body(r);
        assertThat(html).contains("lang=\"uk\"").contains("Ринок").contains("навчальний маркетплейс")
            .contains("Електроніка").contains("Нові товари")
            .contains("Навчальний проєкт. Тарифи, ставки й комісії ілюстративні.");
        Html.assertClean(html);
    }

    @Test
    void newProductOfStartSellerIsListed() throws Exception {
        long seller = fx.sellerWithPlan(PlanCode.START);
        String title = "Новинка-" + UUID.randomUUID();
        fx.createProduct(seller, fx.categoryId("Електроніка"), title, "250.00");

        String html = Html.body(mvc.perform(get("/")).andExpect(status().isOk()).andReturn());

        assertThat(html).contains(title).contains(Html.uah("250,00"));
    }

    @Test
    void activeCampaignShowsAdBlockWithLabel() throws Exception {
        long seller = fx.sellerWithPlan(PlanCode.START);
        fx.topUp(seller, "100.00");
        long product = fx.createProduct(seller, fx.categoryId("Електроніка"), "Реклама-" + UUID.randomUUID(), "99.00");
        long campaign = fx.createCampaign(seller, "100.00");
        fx.bid(campaign, product, "50.00");

        String html = Html.body(mvc.perform(get("/")).andExpect(status().isOk()).andReturn());

        assertThat(html).contains("Рекомендовані").contains("badge-ad").contains("Реклама");
    }
}
