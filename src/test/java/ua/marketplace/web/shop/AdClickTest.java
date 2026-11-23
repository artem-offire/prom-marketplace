package ua.marketplace.web.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import ua.marketplace.advertising.AdAuctionService;
import ua.marketplace.advertising.dto.PlacementDto;
import ua.marketplace.common.money.Money;
import ua.marketplace.subscription.PlanCode;
import ua.marketplace.support.IntegrationTest;
import ua.marketplace.support.TestFixtures;

@IntegrationTest
@AutoConfigureMockMvc
class AdClickTest {

    private final MockMvc mvc;
    private final AdAuctionService auctionService;
    private final TestFixtures fx;

    AdClickTest(MockMvc mvc, AdAuctionService auctionService, TestFixtures fx) {
        this.mvc = mvc;
        this.auctionService = auctionService;
        this.fx = fx;
    }

    @Test
    void clickChargesBidAndRedirectsToProduct() throws Exception {
        long category = fx.freshCategory();
        long seller = fx.sellerWithPlan(PlanCode.START);
        fx.topUp(seller, "100.00");
        long product = fx.createProduct(seller, category, "Клік-" + UUID.randomUUID(), "100.00");
        fx.bid(fx.createCampaign(seller, "100.00"), product, "3.00");
        PlacementDto placement = auctionService.placements(category, 1, "test-session").get(0);
        Money before = fx.balance(seller);

        mvc.perform(post("/ads/{id}/click", placement.impressionId()).param("productId", String.valueOf(product)))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/products/" + product));

        assertThat(fx.balance(seller)).isEqualTo(before.minus(Money.of("3.00")));
    }

    @Test
    void unknownImpressionStillRedirects() throws Exception {
        mvc.perform(post("/ads/{id}/click", UUID.randomUUID()).param("productId", "42"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/products/42"));
    }

    @Test
    void missingProductIdIsBadRequest() throws Exception {
        mvc.perform(post("/ads/{id}/click", UUID.randomUUID())).andExpect(status().isBadRequest());
    }
}
