package ua.marketplace.web.shop;

import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ua.marketplace.advertising.AdAuctionService;
import ua.marketplace.catalog.CategoryService;
import ua.marketplace.catalog.MarketplaceQueryService;
import ua.marketplace.catalog.dto.CategoryDto;
import ua.marketplace.catalog.dto.ProductDto;
import ua.marketplace.web.support.CategoryNames;

@Controller
public class HomeController {

    private static final int NEW_PRODUCTS = 8;
    private static final int AD_CATEGORIES = 4;

    private final CategoryService categoryService;
    private final MarketplaceQueryService queryService;
    private final AdAuctionService auctionService;

    public HomeController(CategoryService categoryService, MarketplaceQueryService queryService,
                          AdAuctionService auctionService) {
        this.categoryService = categoryService;
        this.queryService = queryService;
        this.auctionService = auctionService;
    }

    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        List<CategoryDto> categories = categoryService.list();
        List<AdCard> ads = new ArrayList<>();
        categories.stream()
            .filter(c -> c.parentId() == null)
            .limit(AD_CATEGORIES)
            .forEach(c -> auctionService.placements(c.id(), 1, session.getId())
                .forEach(p -> ads.add(AdCard.of(p, c.name()))));
        List<ProductDto> newProducts = queryService.search(null, null, 0, NEW_PRODUCTS).content();
        Map<Long, String> names = CategoryNames.byId(categories);

        model.addAttribute("categories", categories);
        model.addAttribute("categoryNames", names);
        model.addAttribute("ads", ads);
        model.addAttribute("newProducts", newProducts);
        return "shop/home";
    }
}
