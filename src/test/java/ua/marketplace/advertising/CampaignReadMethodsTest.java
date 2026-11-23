package ua.marketplace.advertising;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ua.marketplace.advertising.dto.BidDto;
import ua.marketplace.advertising.dto.BidRequest;
import ua.marketplace.advertising.dto.CampaignDto;
import ua.marketplace.advertising.dto.CreateCampaignRequest;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.common.error.NotFoundException;
import ua.marketplace.subscription.PlanCode;
import ua.marketplace.support.IntegrationTest;
import ua.marketplace.support.TestFixtures;

@IntegrationTest
class CampaignReadMethodsTest {

    private final CampaignService campaignService;
    private final TestFixtures fx;

    CampaignReadMethodsTest(CampaignService campaignService, TestFixtures fx) {
        this.campaignService = campaignService;
        this.fx = fx;
    }

    @Test
    void listBySellerReturnsNewestFirstAndOnlyOwn() {
        long seller = fx.sellerWithPlan(PlanCode.START);
        long other = fx.sellerWithPlan(PlanCode.START);
        long a = campaignService.create(seller, new CreateCampaignRequest("A", new BigDecimal("60.00"))).id();
        long b = campaignService.create(seller, new CreateCampaignRequest("B", new BigDecimal("70.00"))).id();
        campaignService.create(other, new CreateCampaignRequest("C", new BigDecimal("80.00")));

        List<CampaignDto> list = campaignService.listBySeller(seller);

        assertThat(list).extracting(CampaignDto::id).containsExactly(b, a);
        assertThat(campaignService.listBySeller(fx.registerSeller())).isEmpty();
    }

    @Test
    void getAndListBidsWorkAndUnknownCampaignIsNotFound() {
        long seller = fx.sellerWithPlan(PlanCode.START);
        long product = fx.createProduct(seller, fx.categoryId("Електроніка"), "Bid-" + UUID.randomUUID(), "100.00");
        long id = campaignService.create(seller, new CreateCampaignRequest("Кампанія", new BigDecimal("100.00"))).id();
        campaignService.replaceBids(id, List.of(new BidRequest(product, new BigDecimal("2.50"))));

        CampaignDto dto = campaignService.get(id);
        List<BidDto> bids = campaignService.listBids(id);

        assertThat(dto.sellerId()).isEqualTo(seller);
        assertThat(dto.dailyBudget()).isEqualByComparingTo("100.00");
        assertThat(bids).hasSize(1);
        assertThat(bids.get(0).productId()).isEqualTo(product);
        assertThat(bids.get(0).bidAmount()).isEqualByComparingTo("2.50");
        assertThatThrownBy(() -> campaignService.get(Long.MAX_VALUE))
            .isInstanceOfSatisfying(NotFoundException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.CAMPAIGN_NOT_FOUND));
        assertThatThrownBy(() -> campaignService.listBids(Long.MAX_VALUE))
            .isInstanceOfSatisfying(NotFoundException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.CAMPAIGN_NOT_FOUND));
    }
}
