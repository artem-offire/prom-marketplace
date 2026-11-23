package ua.marketplace.web.seller;

import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import ua.marketplace.advertising.CampaignService;
import ua.marketplace.advertising.dto.CampaignDto;
import ua.marketplace.catalog.ProductService;
import ua.marketplace.catalog.dto.ProductDto;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.common.error.NotFoundException;
import ua.marketplace.order.OrderService;
import ua.marketplace.order.dto.OrderDto;
import ua.marketplace.seller.SellerService;
import ua.marketplace.seller.dto.SellerDto;
import ua.marketplace.subscription.SubscriptionService;
import ua.marketplace.subscription.dto.SubscriptionDto;

/** Завантажує контекст кабінету і перевіряє, що вкладені ідентифікатори належать продавцю (інакше 404). */
@Component
public class CabinetContext {

    private final SellerService sellerService;
    private final SubscriptionService subscriptionService;
    private final ProductService productService;
    private final CampaignService campaignService;
    private final OrderService orderService;

    public CabinetContext(SellerService sellerService, SubscriptionService subscriptionService,
                          ProductService productService, CampaignService campaignService,
                          OrderService orderService) {
        this.sellerService = sellerService;
        this.subscriptionService = subscriptionService;
        this.productService = productService;
        this.campaignService = campaignService;
        this.orderService = orderService;
    }

    /** Кладе в модель seller і subscription; неіснуючий продавець дає NotFoundException (404). */
    public SellerDto load(Long sellerId, Model model) {
        SellerDto seller = sellerService.get(sellerId);
        SubscriptionDto subscription = subscriptionService.getActive(sellerId);
        model.addAttribute("seller", seller);
        model.addAttribute("subscription", subscription);
        return seller;
    }

    public ProductDto product(Long sellerId, Long productId) {
        ProductDto product = productService.get(productId);
        if (!sellerId.equals(product.sellerId())) {
            throw new NotFoundException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found: " + productId);
        }
        return product;
    }

    public CampaignDto campaign(Long sellerId, Long campaignId) {
        CampaignDto campaign = campaignService.get(campaignId);
        if (!sellerId.equals(campaign.sellerId())) {
            throw new NotFoundException(ErrorCode.CAMPAIGN_NOT_FOUND, "Campaign not found: " + campaignId);
        }
        return campaign;
    }

    public OrderDto order(Long sellerId, Long orderId) {
        OrderDto order = orderService.get(orderId);
        if (!sellerId.equals(order.sellerId())) {
            throw new NotFoundException(ErrorCode.ORDER_NOT_FOUND, "Order not found: " + orderId);
        }
        return order;
    }
}
