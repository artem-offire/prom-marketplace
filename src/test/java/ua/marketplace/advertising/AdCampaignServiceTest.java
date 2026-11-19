package ua.marketplace.advertising;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ua.marketplace.advertising.dto.BidDto;
import ua.marketplace.advertising.dto.BidRequest;
import ua.marketplace.advertising.dto.CampaignDto;
import ua.marketplace.advertising.dto.CreateCampaignRequest;
import ua.marketplace.advertising.dto.PlacementDto;
import ua.marketplace.catalog.CategoryService;
import ua.marketplace.catalog.dto.CreateCategoryRequest;
import ua.marketplace.common.error.BusinessRuleException;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.common.money.Money;
import ua.marketplace.subscription.PlanCode;
import ua.marketplace.support.IntegrationTest;
import ua.marketplace.support.TestFixtures;

@IntegrationTest
class AdCampaignServiceTest {

    private final CampaignService campaignService;
    private final AdAuctionService auctionService;
    private final AdCampaignRepository campaignRepo;
    private final AdBidRepository bidRepo;
    private final CategoryService categoryService;
    private final Clock clock;
    private final TestFixtures fx;

    AdCampaignServiceTest(CampaignService campaignService, AdAuctionService auctionService,
                          AdCampaignRepository campaignRepo, AdBidRepository bidRepo,
                          CategoryService categoryService, Clock clock, TestFixtures fx) {
        this.campaignService = campaignService;
        this.auctionService = auctionService;
        this.campaignRepo = campaignRepo;
        this.bidRepo = bidRepo;
        this.categoryService = categoryService;
        this.clock = clock;
        this.fx = fx;
    }

    @Test
    void freeSellerCannotCreateCampaign() {
        long free = fx.sellerWithPlan(PlanCode.FREE);
        assertThatThrownBy(() -> campaignService.create(free, new CreateCampaignRequest("Кампанія",
            new BigDecimal("100.00"))))
            .isInstanceOfSatisfying(BusinessRuleException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.FEATURE_NOT_IN_PLAN));
    }

    @Test
    void dailyBudgetMinimumIsEnforced() {
        long start = fx.sellerWithPlan(PlanCode.START);
        assertThatThrownBy(() -> campaignService.create(start, new CreateCampaignRequest("Кампанія",
            new BigDecimal("49.99"))))
            .isInstanceOfSatisfying(BusinessRuleException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.DAILY_BUDGET_TOO_LOW));

        CampaignDto ok = campaignService.create(start, new CreateCampaignRequest("Кампанія",
            new BigDecimal("50.00")));
        assertThat(ok.dailyBudget()).isEqualByComparingTo("50.00");
    }

    @Test
    void bidMinimumAndOwnershipAreEnforced() {
        long start = fx.sellerWithPlan(PlanCode.START);
        long other = fx.sellerWithPlan(PlanCode.START);
        long electronics = fx.categoryId("Електроніка");
        long productId = fx.createProduct(start, electronics, "Товар-" + UUID.randomUUID(), "100.00");
        long foreignProductId = fx.createProduct(other, electronics, "Чужий-" + UUID.randomUUID(), "100.00");
        long campaignId = campaignService.create(start, new CreateCampaignRequest("Кампанія",
            new BigDecimal("100.00"))).id();

        assertThatThrownBy(() -> campaignService.replaceBids(campaignId,
            List.of(new BidRequest(productId, new BigDecimal("1.99")))))
            .isInstanceOfSatisfying(BusinessRuleException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.BID_BELOW_MINIMUM));

        List<BidDto> ok = campaignService.replaceBids(campaignId,
            List.of(new BidRequest(productId, new BigDecimal("2.00"))));
        assertThat(ok).hasSize(1);

        assertThatThrownBy(() -> campaignService.replaceBids(campaignId,
            List.of(new BidRequest(foreignProductId, new BigDecimal("2.00")))))
            .isInstanceOfSatisfying(BusinessRuleException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.PRODUCT_NOT_OWNED));
    }

    @Test
    void emptyBidListRemovesBidsAndChangedAmountRefreshesCreatedAt() throws InterruptedException {
        long start = fx.sellerWithPlan(PlanCode.START);
        long productId = fx.createProduct(start, fx.categoryId("Електроніка"), "Товар-" + UUID.randomUUID(), "100.00");
        long campaignId = campaignService.create(start, new CreateCampaignRequest("Кампанія",
            new BigDecimal("100.00"))).id();

        campaignService.replaceBids(campaignId, List.of(new BidRequest(productId, new BigDecimal("2.00"))));
        var before = bidRepo.findByCampaignId(campaignId).get(0).getCreatedAt();

        Thread.sleep(5);
        campaignService.replaceBids(campaignId, List.of(new BidRequest(productId, new BigDecimal("3.00"))));
        var after = bidRepo.findByCampaignId(campaignId).get(0);
        assertThat(after.getCreatedAt()).isAfter(before);

        campaignService.replaceBids(campaignId, List.of());
        assertThat(bidRepo.findByCampaignId(campaignId)).isEmpty();
    }

    @Test
    void placementsSkipPausedCampaignsAndUnfundedSellers() {
        long category = freshCategory();
        long seller = fx.sellerWithPlan(PlanCode.START);
        fx.topUp(seller, "100.00");
        long productId = fx.createProduct(seller, category, "Показ-" + UUID.randomUUID(), "100.00");
        long campaignId = campaignService.create(seller, new CreateCampaignRequest("Кампанія",
            new BigDecimal("100.00"))).id();
        campaignService.replaceBids(campaignId, List.of(new BidRequest(productId, new BigDecimal("2.00"))));

        assertThat(auctionService.placements(category, 10, "s1")).hasSize(1);

        campaignService.patch(campaignId, new ua.marketplace.advertising.dto.PatchCampaignRequest(null,
            CampaignStatus.PAUSED));
        assertThat(auctionService.placements(category, 10, "s1")).isEmpty();

        campaignService.patch(campaignId, new ua.marketplace.advertising.dto.PatchCampaignRequest(null,
            CampaignStatus.ACTIVE));
        long poor = fx.sellerWithPlan(PlanCode.START);
        fx.topUp(poor, "1.00");
        long poorProduct = fx.createProduct(poor, category, "Бідний-" + UUID.randomUUID(), "100.00");
        long poorCampaign = campaignService.create(poor, new CreateCampaignRequest("Бідна",
            new BigDecimal("100.00"))).id();
        campaignService.replaceBids(poorCampaign, List.of(new BidRequest(poorProduct, new BigDecimal("2.00"))));
        List<PlacementDto> placements = auctionService.placements(category, 10, "s2");
        assertThat(placements).extracting(PlacementDto::productId).doesNotContain(poorProduct);
        assertThat(placements).extracting(PlacementDto::productId).contains(productId);
    }

    @Test
    void exhaustedDailyBudgetHidesCampaign() {
        long category = freshCategory();
        long seller = fx.sellerWithPlan(PlanCode.START);
        fx.topUp(seller, "100.00");
        long productId = fx.createProduct(seller, category, "Бюджет-" + UUID.randomUUID(), "100.00");
        long campaignId = campaignService.create(seller, new CreateCampaignRequest("Кампанія",
            new BigDecimal("50.00"))).id();
        campaignService.replaceBids(campaignId, List.of(new BidRequest(productId, new BigDecimal("2.00"))));

        AdCampaign campaign = campaignRepo.findById(campaignId).orElseThrow();
        campaign.registerSpend(LocalDate.now(clock), Money.of("50.00"));
        campaignRepo.save(campaign);

        assertThat(auctionService.placements(category, 10, "s3")).isEmpty();
    }

    @Test
    void placementsReturnImpressionIdPerPosition() {
        long category = freshCategory();
        long seller = fx.sellerWithPlan(PlanCode.START);
        fx.topUp(seller, "100.00");
        long productId = fx.createProduct(seller, category, "Позиція-" + UUID.randomUUID(), "100.00");
        long campaignId = campaignService.create(seller, new CreateCampaignRequest("Кампанія",
            new BigDecimal("100.00"))).id();
        campaignService.replaceBids(campaignId, List.of(new BidRequest(productId, new BigDecimal("2.00"))));

        List<PlacementDto> placements = auctionService.placements(category, 10, "s4");

        assertThat(placements).hasSize(1);
        assertThat(placements.get(0).impressionId()).isNotNull();
        assertThat(placements.get(0).position()).isEqualTo(1);
    }

    private long freshCategory() {
        return categoryService.create(new CreateCategoryRequest("Тест-" + UUID.randomUUID(), null,
            new BigDecimal("5.00"), new BigDecimal("1.00"))).id();
    }
}
