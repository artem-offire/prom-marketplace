package ua.marketplace.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import ua.marketplace.catalog.dto.CreateProductRequest;
import ua.marketplace.catalog.dto.PatchProductRequest;
import ua.marketplace.catalog.dto.ProductDto;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.common.error.NotFoundException;
import ua.marketplace.subscription.PlanCode;
import ua.marketplace.support.IntegrationTest;
import ua.marketplace.support.TestFixtures;

@IntegrationTest
class CatalogReadMethodsTest {

    private final ProductService productService;
    private final MarketplaceQueryService queryService;
    private final TestFixtures fx;

    CatalogReadMethodsTest(ProductService productService, MarketplaceQueryService queryService, TestFixtures fx) {
        this.productService = productService;
        this.queryService = queryService;
        this.fx = fx;
    }

    @Test
    void getReturnsDtoAndUnknownIdIsNotFound() {
        long seller = fx.sellerWithPlan(PlanCode.START);
        long id = fx.createProduct(seller, fx.categoryId("Авто"), "Get-" + UUID.randomUUID(), "12.30");

        ProductDto dto = productService.get(id);

        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.sellerId()).isEqualTo(seller);
        assertThat(dto.price()).isEqualByComparingTo("12.30");
        assertThatThrownBy(() -> productService.get(Long.MAX_VALUE))
            .isInstanceOfSatisfying(NotFoundException.class,
                e -> assertThat(e.code()).isEqualTo(ErrorCode.PRODUCT_NOT_FOUND));
    }

    @Test
    void countUsedIgnoresArchived() {
        long seller = fx.sellerWithPlan(PlanCode.FREE);
        long cat = fx.categoryId("Авто");
        long first = fx.createProduct(seller, cat, "Used-A-" + UUID.randomUUID(), "10.00");
        fx.createProduct(seller, cat, "Used-B-" + UUID.randomUUID(), "10.00");
        fx.createProduct(seller, cat, "Used-C-" + UUID.randomUUID(), "10.00");
        assertThat(productService.countUsed(seller)).isEqualTo(3);

        productService.patch(first, new PatchProductRequest(null, null, null, null, null, ProductStatus.ARCHIVED));

        assertThat(productService.countUsed(seller)).isEqualTo(2);
    }

    @Test
    void findVisibleRequiresActiveStatusAndVisiblePlan() {
        long start = fx.sellerWithPlan(PlanCode.START);
        long free = fx.sellerWithPlan(PlanCode.FREE);
        long cat = fx.categoryId("Авто");
        long visible = fx.createProduct(start, cat, "Vis-" + UUID.randomUUID(), "10.00");
        long hidden = fx.createProduct(start, cat, "Hid-" + UUID.randomUUID(), "10.00");
        productService.patch(hidden, new PatchProductRequest(null, null, null, null, null, ProductStatus.HIDDEN));
        long draft = productService.create(start, new CreateProductRequest(cat, "Dr-" + UUID.randomUUID(), null,
            new BigDecimal("10.00"), 1, ProductStatus.DRAFT)).id();
        long ofFree = fx.createProduct(free, cat, "Free-" + UUID.randomUUID(), "10.00");

        assertThat(queryService.findVisible(visible)).isPresent();
        assertThat(queryService.findVisible(hidden)).isEmpty();
        assertThat(queryService.findVisible(draft)).isEmpty();
        assertThat(queryService.findVisible(ofFree)).isEmpty();
        assertThat(queryService.findVisible(Long.MAX_VALUE)).isEmpty();
    }
}
