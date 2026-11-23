package ua.marketplace.web.seller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ua.marketplace.advertising.CampaignService;
import ua.marketplace.advertising.CampaignStatus;
import ua.marketplace.billing.BalanceService;
import ua.marketplace.catalog.ProductService;
import ua.marketplace.order.OrderService;
import ua.marketplace.order.OrderStatus;
import ua.marketplace.subscription.PlanLimits;

@Controller
public class DashboardController {

    private final CabinetContext cabinet;
    private final BalanceService balanceService;
    private final ProductService productService;
    private final PlanLimits planLimits;
    private final OrderService orderService;
    private final CampaignService campaignService;

    public DashboardController(CabinetContext cabinet, BalanceService balanceService, ProductService productService,
                               PlanLimits planLimits, OrderService orderService, CampaignService campaignService) {
        this.cabinet = cabinet;
        this.balanceService = balanceService;
        this.productService = productService;
        this.planLimits = planLimits;
        this.orderService = orderService;
        this.campaignService = campaignService;
    }

    @GetMapping("/cabinet/{sellerId}")
    public String dashboard(@PathVariable Long sellerId, Model model) {
        cabinet.load(sellerId, model);
        model.addAttribute("balance", balanceService.getBalance(sellerId));
        model.addAttribute("used", productService.countUsed(sellerId));
        model.addAttribute("limit", planLimits.productLimit(sellerId));
        model.addAttribute("visible", planLimits.isMarketplaceVisible(sellerId));
        model.addAttribute("newOrders", orderService.listBySeller(sellerId, OrderStatus.NEW, 0, 1).totalElements());
        model.addAttribute("activeCampaigns", campaignService.listBySeller(sellerId).stream()
            .filter(c -> c.status() == CampaignStatus.ACTIVE).count());
        return "cabinet/dashboard";
    }
}
