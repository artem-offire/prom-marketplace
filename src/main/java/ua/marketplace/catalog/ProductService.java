package ua.marketplace.catalog;

import static ua.marketplace.catalog.ProductStatus.ACTIVE;
import static ua.marketplace.catalog.ProductStatus.ARCHIVED;
import static ua.marketplace.catalog.ProductStatus.DRAFT;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.marketplace.catalog.dto.CreateProductRequest;
import ua.marketplace.catalog.dto.PatchProductRequest;
import ua.marketplace.catalog.dto.ProductDto;
import ua.marketplace.common.error.BusinessRuleException;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.common.error.NotFoundException;
import ua.marketplace.common.money.Money;
import ua.marketplace.common.web.PageResponse;
import ua.marketplace.common.web.Paging;
import ua.marketplace.subscription.PlanLimits;

@Service
public class ProductService {

    private final ProductRepository productRepo;
    private final CategoryRepository categoryRepo;
    private final PlanLimits planLimits;
    private final Clock clock;

    public ProductService(ProductRepository productRepo, CategoryRepository categoryRepo,
                          PlanLimits planLimits, Clock clock) {
        this.productRepo = productRepo;
        this.categoryRepo = categoryRepo;
        this.planLimits = planLimits;
        this.clock = clock;
    }

    @Transactional
    public ProductDto create(Long sellerId, CreateProductRequest r) {
        int limit = planLimits.productLimit(sellerId);
        long count = productRepo.countBySellerIdAndStatusNot(sellerId, ARCHIVED);
        if (count >= limit) {
            throw new BusinessRuleException(ErrorCode.PRODUCT_LIMIT_EXCEEDED,
                "Product limit of the plan reached: " + limit);
        }
        if (!categoryRepo.existsById(r.categoryId())) {
            throw new NotFoundException(ErrorCode.CATEGORY_NOT_FOUND, "Category not found: " + r.categoryId());
        }
        ProductStatus status = r.status() == null ? ACTIVE : r.status();
        if (status != DRAFT && status != ACTIVE) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_ERROR, "Initial status must be DRAFT or ACTIVE");
        }
        Product p = productRepo.save(new Product(sellerId, r.categoryId(), r.title().trim(), r.description(),
            Money.of(r.price()), r.stock(), status, Instant.now(clock)));
        return toDto(p);
    }

    @Transactional
    public ProductDto patch(Long productId, PatchProductRequest r) {
        Product product = productRepo.findById(productId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found: " + productId));

        if (r.categoryId() != null && !categoryRepo.existsById(r.categoryId())) {
            throw new NotFoundException(ErrorCode.CATEGORY_NOT_FOUND, "Category not found: " + r.categoryId());
        }

        if (product.getStatus() == ARCHIVED && r.status() != null && r.status() != ARCHIVED) {
            int limit = planLimits.productLimit(product.getSellerId());
            long count = productRepo.countBySellerIdAndStatusNot(product.getSellerId(), ARCHIVED);
            if (count >= limit) {
                throw new BusinessRuleException(ErrorCode.PRODUCT_LIMIT_EXCEEDED,
                    "Product limit of the plan reached: " + limit);
            }
        }

        String title = r.title() == null ? null : r.title().trim();
        if (title != null && title.isEmpty()) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_ERROR, "Title must not be blank");
        }

        product.update(r.categoryId(), title, r.description(),
            r.price() == null ? null : Money.of(r.price()), r.stock(), r.status(), Instant.now(clock));
        return toDto(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductDto> listBySeller(Long sellerId, int page, int size) {
        planLimits.productLimit(sellerId);
        return PageResponse.of(
            productRepo.findBySellerId(sellerId, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "id"))),
            this::toDto);
    }

    @Transactional(readOnly = true)
    public ProductView getView(Long id) {
        Product p = productRepo.findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found: " + id));
        return toView(p);
    }

    /** Відсутні ідентифікатори не дають помилки: виклик перевіряє розмір результату сам. */
    @Transactional(readOnly = true)
    public List<ProductView> findViews(Collection<Long> ids) {
        return productRepo.findByIdIn(ids).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public boolean isMarketplaceVisible(ProductView v) {
        return v.status() == ACTIVE && planLimits.isMarketplaceVisible(v.sellerId());
    }

    private ProductView toView(Product p) {
        return new ProductView(p.getId(), p.getSellerId(), p.getCategoryId(), p.getTitle(), p.getPrice(),
            p.getStock(), p.getStatus(), p.getQualityScore());
    }

    private ProductDto toDto(Product p) {
        return new ProductDto(p.getId(), p.getSellerId(), p.getCategoryId(), p.getTitle(), p.getDescription(),
            p.getPrice().amount(), p.getStock(), p.getStatus(), p.getQualityScore(), p.getCreatedAt());
    }
}
