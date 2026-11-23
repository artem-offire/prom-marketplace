package ua.marketplace.web.shop;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ua.marketplace.catalog.CategoryService;
import ua.marketplace.catalog.MarketplaceQueryService;
import ua.marketplace.catalog.dto.CategoryDto;
import ua.marketplace.catalog.dto.ProductDto;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.common.error.NotFoundException;

@Controller
public class ProductPageController {

    private final MarketplaceQueryService queryService;
    private final CategoryService categoryService;

    public ProductPageController(MarketplaceQueryService queryService, CategoryService categoryService) {
        this.queryService = queryService;
        this.categoryService = categoryService;
    }

    @GetMapping("/products/{id}")
    public String product(@PathVariable Long id, Model model) {
        ProductDto product = queryService.findVisible(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found: " + id));
        String categoryName = categoryService.list().stream()
            .filter(c -> c.id().equals(product.categoryId()))
            .map(CategoryDto::name)
            .findFirst().orElse("");
        model.addAttribute("product", product);
        model.addAttribute("categoryName", categoryName);
        model.addAttribute("maxQuantity", Math.max(1, Math.min(product.stock(), 99)));
        return "shop/product";
    }
}
