package ua.marketplace.advertising;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ua.marketplace.billing.AccountCodes.REVENUE_ADS;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ua.marketplace.advertising.dto.BidRequest;
import ua.marketplace.advertising.dto.ClickResponse;
import ua.marketplace.advertising.dto.CreateCampaignRequest;
import ua.marketplace.advertising.dto.PlacementDto;
import ua.marketplace.catalog.CategoryService;
import ua.marketplace.catalog.dto.CreateCategoryRequest;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.common.error.NotFoundException;
import ua.marketplace.common.money.Money;
import ua.marketplace.subscription.PlanCode;
import ua.marketplace.support.IntegrationTest;
import ua.marketplace.support.TestFixtures;

/**
 * Кожен сценарій працює у власній категорії, щоб аукціон не залежав від ставок інших тестів.
 */
@IntegrationTest
class ClickChargingServiceTest {

    private final CampaignService campaignService;
    private final AdAuctionService auctionService;
    private final ClickChargingService clickService;
    private final AdCampaignRepository campaignRepo;
    private final CategoryService categoryService;
    private final Clock clock;
    private final TestFixtures fx;

    ClickChargingServiceTest(CampaignService campaignService, AdAuctionService auctionService,
                             ClickChargingService clickService, AdCampaignRepository campaignRepo,
                             CategoryService categoryService, Clock clock, TestFixtures fx) {
        this.campaignService = campaignService;
        this.auctionService = auctionService;
        this.clickService = clickService;
        this.campaignRepo = campaignRepo;
        this.categoryService = categoryService;
        this.clock = clock;
        this.fx = fx;
    }

    @Test
    void chargesOnceDuplicatesAndStopsWithoutFunds() {
        long category = freshCategory();
        long seller = fx.sellerWithPlan(PlanCode.PRO);
        fx.topUp(seller, "5.00");
        long campaignId = setupCampaign(seller, category, "100.00", "3.00");
        Money adsBefore = fx.ledger(REVENUE_ADS);

        UUID imp1 = impression(category, "a");
        UUID imp2 = impression(category, "b");
        assertThat(imp1).isNotEqualTo(imp2);

        ClickResponse first = clickService.registerClick(imp1, "a");
        assertThat(first.status()).isEqualTo(ClickStatus.CHARGED);
        assertThat(first.chargedAmount()).isEqualByComparingTo("3.00");
        assertThat(fx.balance(seller)).isEqualTo(Money.of("2.00"));
        assertThat(fx.ledger(REVENUE_ADS).minus(adsBefore)).isEqualTo(Money.of("3.00"));
        assertThat(campaignRepo.findById(campaignId).orElseThrow().getSpentToday()).isEqualTo(Money.of("3.00"));

        ClickResponse dup = clickService.registerClick(imp1, "a");
        assertThat(dup.status()).isEqualTo(ClickStatus.DUPLICATE);
        assertThat(dup.chargedAmount()).isEqualByComparingTo("0.00");
        assertThat(fx.balance(seller)).isEqualTo(Money.of("2.00"));

        ClickResponse noFunds = clickService.registerClick(imp2, "b");
        assertThat(noFunds.status()).isEqualTo(ClickStatus.UNCHARGED_NO_FUNDS);
        assertThat(fx.balance(seller)).isEqualTo(Money.of("2.00"));
    }

    @Test
    void dailyBudgetLimitsClicks() {
        long category = freshCategory();
        long seller = fx.sellerWithPlan(PlanCode.PRO);
        fx.topUp(seller, "1000.00");
        setupCampaign(seller, category, "50.00", "20.00");

        UUID imp1 = impression(category, "x");
        UUID imp2 = impression(category, "x");
        UUID imp3 = impression(category, "x");

        assertThat(clickService.registerClick(imp1, "x").status()).isEqualTo(ClickStatus.CHARGED);
        assertThat(clickService.registerClick(imp2, "x").status()).isEqualTo(ClickStatus.CHARGED);
        assertThat(clickService.registerClick(imp3, "x").status()).isEqualTo(ClickStatus.UNCHARGED_BUDGET);
    }

    @Test
    void unknownImpressionIsNotFound() {
        assertThatThrownBy(() -> clickService.registerClick(UUID.randomUUID(), "s"))
            .isInstanceOfSatisfying(NotFoundException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.IMPRESSION_NOT_FOUND));
    }

    @Test
    void spentCounterResetsOnNewDay() {
        long category = freshCategory();
        long seller = fx.sellerWithPlan(PlanCode.PRO);
        fx.topUp(seller, "5.00");
        long campaignId = setupCampaign(seller, category, "100.00", "3.00");

        UUID imp = impression(category, "d");

        AdCampaign campaign = campaignRepo.findById(campaignId).orElseThrow();
        campaign.registerSpend(LocalDate.now(clock).minusDays(1), Money.of("100.00"));
        campaignRepo.save(campaign);

        ClickResponse click = clickService.registerClick(imp, "d");
        assertThat(click.status()).isEqualTo(ClickStatus.CHARGED);
        assertThat(campaignRepo.findById(campaignId).orElseThrow().getSpentToday())
            .isEqualTo(Money.of("3.00"));
    }

    private long setupCampaign(long seller, long category, String budget, String bid) {
        long productId = fx.createProduct(seller, category, "Клік-" + UUID.randomUUID(), "100.00");
        long campaignId = campaignService.create(seller, new CreateCampaignRequest("Кампанія",
            new BigDecimal(budget))).id();
        campaignService.replaceBids(campaignId, List.of(new BidRequest(productId, new BigDecimal(bid))));
        return campaignId;
    }

    private UUID impression(long category, String session) {
        List<PlacementDto> placements = auctionService.placements(category, 10, session);
        assertThat(placements).isNotEmpty();
        return placements.get(0).impressionId();
    }

    private long freshCategory() {
        return categoryService.create(new CreateCategoryRequest("Тест-" + UUID.randomUUID(), null,
            new BigDecimal("5.00"), new BigDecimal("1.00"))).id();
    }
}
