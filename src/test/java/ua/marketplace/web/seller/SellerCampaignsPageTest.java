package ua.marketplace.web.seller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import ua.marketplace.advertising.CampaignService;
import ua.marketplace.advertising.CampaignStatus;
import ua.marketplace.subscription.PlanCode;
import ua.marketplace.support.Html;
import ua.marketplace.support.IntegrationTest;
import ua.marketplace.support.TestFixtures;

@IntegrationTest
@AutoConfigureMockMvc
class SellerCampaignsPageTest {

    private final MockMvc mvc;
    private final TestFixtures fx;
    private final CampaignService campaignService;

    SellerCampaignsPageTest(MockMvc mvc, TestFixtures fx, CampaignService campaignService) {
        this.mvc = mvc;
        this.fx = fx;
        this.campaignService = campaignService;
    }

    @Test
    void freeSellerSeesAdvertisingHint() throws Exception {
        long seller = fx.sellerWithPlan(PlanCode.FREE);

        String html = Html.body(mvc.perform(get("/cabinet/" + seller + "/campaigns")).andExpect(status().isOk()).andReturn());

        assertThat(html).contains("Реклама доступна з пакета START");
        Html.assertClean(html);
    }

    @Test
    void createCampaignRedirectsToDetails() throws Exception {
        long seller = fx.sellerWithPlan(PlanCode.PRO);

        mvc.perform(post("/cabinet/" + seller + "/campaigns").param("name", "Весна").param("dailyBudget", "150,00"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/cabinet/" + seller + "/campaigns/*"))
            .andExpect(flash().attributeExists("flashSuccess"));

        assertThat(campaignService.listBySeller(seller)).hasSize(1);
    }

    @Test
    void tooLowBudgetShowsMinimum() throws Exception {
        long seller = fx.sellerWithPlan(PlanCode.PRO);

        String html = Html.body(mvc.perform(post("/cabinet/" + seller + "/campaigns")
            .param("name", "Мало").param("dailyBudget", "0,50")).andExpect(status().isOk()).andReturn());

        assertThat(html).contains("Мінімальний денний бюджет");
        assertThat(campaignService.listBySeller(seller)).isEmpty();
    }

    @Test
    void pauseAndResumeCampaign() throws Exception {
        long seller = fx.sellerWithPlan(PlanCode.PRO);
        long campaign = fx.createCampaign(seller, "100.00");

        mvc.perform(post("/cabinet/" + seller + "/campaigns/" + campaign + "/status").param("status", "PAUSED"))
            .andExpect(flash().attribute("flashSuccess", "Кампанію призупинено"));
        assertThat(campaignService.get(campaign).status()).isEqualTo(CampaignStatus.PAUSED);
        String html = Html.body(mvc.perform(get("/cabinet/" + seller + "/campaigns/" + campaign))
            .andExpect(status().isOk()).andReturn());
        assertThat(html).contains("Призупинено");

        mvc.perform(post("/cabinet/" + seller + "/campaigns/" + campaign + "/status").param("status", "ACTIVE"))
            .andExpect(flash().attribute("flashSuccess", "Кампанію поновлено"));
        assertThat(campaignService.get(campaign).status()).isEqualTo(CampaignStatus.ACTIVE);
    }

    @Test
    void bidsAreSavedAndBelowMinimumIsRejected() throws Exception {
        long seller = fx.sellerWithPlan(PlanCode.PRO);
        fx.topUp(seller, "100.00");
        long product = fx.createProduct(seller, fx.categoryId("Електроніка"), "Ставка-" + UUID.randomUUID(), "50.00");
        long campaign = fx.createCampaign(seller, "100.00");
        String url = "/cabinet/" + seller + "/campaigns/" + campaign + "/bids";

        String rejected = Html.body(mvc.perform(post(url).param("rows[0].productId", String.valueOf(product))
            .param("rows[0].amount", "0,01")).andExpect(status().isOk()).andReturn());
        assertThat(rejected).contains("Ставка нижча за мінімальну");
        assertThat(campaignService.listBids(campaign)).isEmpty();

        mvc.perform(post(url).param("rows[0].productId", String.valueOf(product)).param("rows[0].amount", "5,00"))
            .andExpect(status().is3xxRedirection()).andExpect(flash().attribute("flashSuccess", "Ставки збережено"));
        assertThat(campaignService.listBids(campaign)).hasSize(1);
    }

    @Test
    void foreignCampaignIsNotFound() throws Exception {
        long owner = fx.sellerWithPlan(PlanCode.PRO);
        long other = fx.sellerWithPlan(PlanCode.PRO);
        long campaign = fx.createCampaign(owner, "100.00");

        mvc.perform(get("/cabinet/" + other + "/campaigns/" + campaign)).andExpect(status().isNotFound());
    }
}
