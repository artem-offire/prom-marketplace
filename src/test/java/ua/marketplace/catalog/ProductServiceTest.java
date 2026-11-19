package ua.marketplace.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ua.marketplace.catalog.dto.CategoryDto;
import ua.marketplace.catalog.dto.CreateCategoryRequest;
import ua.marketplace.catalog.dto.CreateProductRequest;
import ua.marketplace.catalog.dto.PatchProductRequest;
import ua.marketplace.catalog.dto.ProductDto;
import ua.marketplace.catalog.dto.UpdateCategoryPricingRequest;
import ua.marketplace.common.error.BusinessRuleException;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.common.error.NotFoundException;
import ua.marketplace.subscription.PlanCode;
import ua.marketplace.subscription.SubscriptionService;
import ua.marketplace.subscription.BillingPeriod;
import ua.marketplace.support.IntegrationTest;
import ua.marketplace.support.TestFixtures;

@IntegrationTest
class ProductServiceTest {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final MarketplaceQueryService queryService;
    private final SubscriptionService subscriptionService;
    private final TestFixtures fx;

    ProductServiceTest(ProductService productService, CategoryService categoryService,
                       MarketplaceQueryService queryService, SubscriptionService subscriptionService,
                       TestFixtures fx) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.queryService = queryService;
        this.subscriptionService = subscriptionService;
        this.fx = fx;
    }

    @Test
    void freeLimitIsEnforcedAndArchivingFreesSlot() {
        long free = fx.sellerWithPlan(PlanCode.FREE);
        long cat = fx.categoryId("Електроніка");
        ProductDto first = null;
        for (int i = 0; i < 50; i++) {
            ProductDto p = productService.create(free, new CreateProductRequest(cat, "Товар " + i, null,
                new BigDecimal("10.00"), 1, ProductStatus.ACTIVE));
            if (first == null) {
                first = p;
            }
        }
        ProductDto extra = null;
        final long catId = cat;
        assertThatThrownBy(() -> productService.create(free, new CreateProductRequest(catId, "Зайвий", null,
            new BigDecimal("10.00"), 1, ProductStatus.ACTIVE)))
            .isInstanceOfSatisfying(BusinessRuleException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.PRODUCT_LIMIT_EXCEEDED));

        productService.patch(first.id(), new PatchProductRequest(null, null, null, null, null, ProductStatus.ARCHIVED));
        extra = productService.create(free, new CreateProductRequest(cat, "Після архівації", null,
            new BigDecimal("10.00"), 1, ProductStatus.ACTIVE));
        assertThat(extra.id()).isNotNull();

        final ProductDto archived = first;
        assertThatThrownBy(() -> productService.patch(archived.id(),
            new PatchProductRequest(null, null, null, null, null, ProductStatus.ACTIVE)))
            .isInstanceOfSatisfying(BusinessRuleException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.PRODUCT_LIMIT_EXCEEDED));
    }

    @Test
    void productVisibleOnlyAfterPlanAllowsMarketplace() {
        long free = fx.sellerWithPlan(PlanCode.FREE);
        long cat = fx.categoryId("Електроніка");
        String title = "Унікальний-" + UUID.randomUUID();
        fx.createProduct(free, cat, title, "100.00");

        assertThat(productService.listBySeller(free, 0, 20).content()).anyMatch(p -> p.title().equals(title));
        assertThat(queryService.search(null, title, 0, 20).content()).isEmpty();

        fx.topUp(free, "990.00");
        subscriptionService.purchase(free, PlanCode.START, BillingPeriod.MONTHLY);

        assertThat(queryService.search(null, title, 0, 20).content()).anyMatch(p -> p.title().equals(title));
    }

    @Test
    void categoryFilterAndCaseInsensitiveSearch() {
        long seller = fx.sellerWithPlan(PlanCode.START);
        long electronics = fx.categoryId("Електроніка");
        long clothing = fx.categoryId("Одяг і взуття");
        String title = "Фільтр-" + UUID.randomUUID();
        fx.createProduct(seller, electronics, title, "100.00");

        assertThat(queryService.search(electronics, title, 0, 20).content()).anyMatch(p -> p.title().equals(title));
        assertThat(queryService.search(clothing, title, 0, 20).content()).noneMatch(p -> p.title().equals(title));
        assertThat(queryService.search(null, title.toUpperCase(), 0, 20).content())
            .anyMatch(p -> p.title().equals(title));
    }

    @Test
    void draftAndHiddenAreNotShownInMarketplace() {
        long seller = fx.sellerWithPlan(PlanCode.START);
        long cat = fx.categoryId("Електроніка");
        String draftTitle = "Чернетка-" + UUID.randomUUID();
        productService.create(seller, new CreateProductRequest(cat, draftTitle, null,
            new BigDecimal("10.00"), 1, ProductStatus.DRAFT));
        String hiddenTitle = "Прихований-" + UUID.randomUUID();
        ProductDto hidden = productService.create(seller, new CreateProductRequest(cat, hiddenTitle, null,
            new BigDecimal("10.00"), 1, ProductStatus.ACTIVE));
        productService.patch(hidden.id(), new PatchProductRequest(null, null, null, null, null, ProductStatus.HIDDEN));

        assertThat(queryService.search(null, draftTitle, 0, 20).content()).isEmpty();
        assertThat(queryService.search(null, hiddenTitle, 0, 20).content()).isEmpty();
    }

    @Test
    void adminUpdatesCategoryPricing() {
        CategoryDto created = categoryService.create(new CreateCategoryRequest("Тест-" + UUID.randomUUID(),
            null, new BigDecimal("5.00"), new BigDecimal("1.00")));

        categoryService.updatePricing(created.id(),
            new UpdateCategoryPricingRequest(new BigDecimal("12.50"), new BigDecimal("2.50")));

        CategoryView view = categoryService.getView(created.id());
        assertThat(view.commissionRate().value()).isEqualByComparingTo("12.50");
        assertThat(view.minBid().amount()).isEqualByComparingTo("2.50");

        assertThatThrownBy(() -> categoryService.updatePricing(created.id(),
            new UpdateCategoryPricingRequest(new BigDecimal("100.01"), new BigDecimal("2.50"))))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> categoryService.updatePricing(Long.MAX_VALUE,
            new UpdateCategoryPricingRequest(new BigDecimal("1.00"), new BigDecimal("1.00"))))
            .isInstanceOfSatisfying(NotFoundException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.CATEGORY_NOT_FOUND));
    }

    @Test
    void createWithUnknownCategoryOrSellerFails() {
        long seller = fx.sellerWithPlan(PlanCode.FREE);

        assertThatThrownBy(() -> productService.create(seller, new CreateProductRequest(Long.MAX_VALUE, "X", null,
            new BigDecimal("1.00"), 1, ProductStatus.ACTIVE)))
            .isInstanceOfSatisfying(NotFoundException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.CATEGORY_NOT_FOUND));

        assertThatThrownBy(() -> productService.create(Long.MAX_VALUE, new CreateProductRequest(
            fx.categoryId("Авто"), "X", null, new BigDecimal("1.00"), 1, ProductStatus.ACTIVE)))
            .isInstanceOfSatisfying(NotFoundException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.SELLER_NOT_FOUND));
    }
}
