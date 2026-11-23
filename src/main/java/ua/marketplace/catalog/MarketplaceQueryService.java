package ua.marketplace.catalog;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.marketplace.catalog.dto.ProductDto;
import ua.marketplace.common.web.PageResponse;
import ua.marketplace.common.web.Paging;
import ua.marketplace.subscription.PlanLimits;

@Service
public class MarketplaceQueryService {

    private final ProductRepository productRepo;
    private final PlanLimits planLimits;

    public MarketplaceQueryService(ProductRepository productRepo, PlanLimits planLimits) {
        this.productRepo = productRepo;
        this.planLimits = planLimits;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductDto> search(Long categoryId, String q, int page, int size) {
        Set<Long> sellerIds = planLimits.visibleSellerIds();
        if (sellerIds.isEmpty()) {
            return PageResponse.empty(page, size);
        }
        String pattern = "%" + (q == null ? "" : q.trim().toLowerCase(Locale.ROOT)) + "%";
        Pageable p = Paging.of(page, size,
            Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        Page<Product> result = categoryId == null
            ? productRepo.searchVisible(sellerIds, pattern, p)
            : productRepo.searchVisibleInCategory(sellerIds, categoryId, pattern, p);
        return PageResponse.of(result, this::toDto);
    }

    /** Товар видно покупцеві: статус ACTIVE і пакет продавця дозволяє показ у вітрині. */
    @Transactional(readOnly = true)
    public Optional<ProductDto> findVisible(Long productId) {
        return productRepo.findById(productId)
            .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
            .filter(p -> planLimits.isMarketplaceVisible(p.getSellerId()))
            .map(this::toDto);
    }

    private ProductDto toDto(Product p) {
        return new ProductDto(p.getId(), p.getSellerId(), p.getCategoryId(), p.getTitle(), p.getDescription(),
            p.getPrice().amount(), p.getStock(), p.getStatus(), p.getQualityScore(), p.getCreatedAt());
    }
}
