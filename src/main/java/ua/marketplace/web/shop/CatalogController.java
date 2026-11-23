package ua.marketplace.web.shop;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ua.marketplace.advertising.AdAuctionService;
import ua.marketplace.catalog.CategoryService;
import ua.marketplace.catalog.MarketplaceQueryService;
import ua.marketplace.catalog.dto.CategoryDto;
import ua.marketplace.catalog.dto.ProductDto;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.common.error.NotFoundException;
import ua.marketplace.common.web.PageResponse;
import ua.marketplace.web.support.CategoryNames;
import ua.marketplace.web.support.PageModel;

@Controller
public class CatalogController {

    private static final int PAGE_SIZE = 20;
    private static final int AD_SLOTS = 3;

    private final CategoryService categoryService;
    private final MarketplaceQueryService queryService;
    private final AdAuctionService auctionService;

    public CatalogController(CategoryService categoryService, MarketplaceQueryService queryService,
                             AdAuctionService auctionService) {
        this.categoryService = categoryService;
        this.queryService = queryService;
        this.auctionService = auctionService;
    }

    @GetMapping("/catalog")
    public String catalog(@RequestParam(required = false) Long category,
                          @RequestParam(required = false) String q,
                          @RequestParam(defaultValue = "0") int page,
                          HttpSession session, Model model) {
        List<CategoryDto> categories = categoryService.list();
        CategoryDto selected = null;
        if (category != null) {
            selected = categories.stream().filter(c -> c.id().equals(category)).findFirst()
                .orElseThrow(() -> new NotFoundException(ErrorCode.CATEGORY_NOT_FOUND,
                    "Category not found: " + category));
        }

        List<AdCard> ads = new ArrayList<>();
        if (selected != null && page <= 0) {
            for (var p : auctionService.placements(selected.id(), AD_SLOTS, session.getId())) {
                ads.add(AdCard.of(p, selected.name()));
            }
        }

        PageResponse<ProductDto> results = queryService.search(category, q, page, PAGE_SIZE);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("category", category);
        params.put("q", q);

        model.addAttribute("categories", categories);
        model.addAttribute("categoryNames", CategoryNames.byId(categories));
        model.addAttribute("selectedCategory", category);
        model.addAttribute("q", q);
        model.addAttribute("ads", ads);
        model.addAttribute("results", results);
        model.addAttribute("pager", PageModel.of(results, "/catalog", params));
        return "shop/catalog";
    }
}
