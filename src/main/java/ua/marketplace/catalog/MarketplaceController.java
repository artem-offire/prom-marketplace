package ua.marketplace.catalog;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.marketplace.catalog.dto.ProductDto;
import ua.marketplace.common.web.PageResponse;

@RestController
@RequestMapping("/api/v1")
public class MarketplaceController {

    private final MarketplaceQueryService queryService;

    public MarketplaceController(MarketplaceQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/marketplace/products")
    public ResponseEntity<PageResponse<ProductDto>> search(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(queryService.search(categoryId, q, page, size));
    }
}
