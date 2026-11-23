package ua.marketplace.support;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import ua.marketplace.advertising.CampaignService;
import ua.marketplace.advertising.dto.BidRequest;
import ua.marketplace.advertising.dto.CreateCampaignRequest;
import ua.marketplace.billing.BalanceService;
import ua.marketplace.billing.LedgerAccountRepository;
import ua.marketplace.billing.dto.TopUpRequest;
import ua.marketplace.catalog.CategoryService;
import ua.marketplace.catalog.ProductService;
import ua.marketplace.catalog.ProductStatus;
import ua.marketplace.catalog.dto.CreateCategoryRequest;
import ua.marketplace.catalog.dto.CreateProductRequest;
import ua.marketplace.common.money.Money;
import ua.marketplace.order.OrderService;
import ua.marketplace.order.PaymentMethod;
import ua.marketplace.order.dto.CheckoutItem;
import ua.marketplace.order.dto.CheckoutRequest;
import ua.marketplace.order.dto.OrderDto;
import ua.marketplace.payment.PaymentService;
import ua.marketplace.payment.dto.PaymentDto;
import ua.marketplace.seller.SellerService;
import ua.marketplace.seller.dto.RegisterSellerRequest;
import ua.marketplace.subscription.BillingPeriod;
import ua.marketplace.subscription.PlanCode;
import ua.marketplace.subscription.SubscriptionService;

@Component
public class TestFixtures {

    private final SellerService sellerService;
    private final BalanceService balanceService;
    private final SubscriptionService subscriptionService;
    private final CategoryService categoryService;
    private final ProductService productService;
    private final LedgerAccountRepository accountRepo;
    private final CampaignService campaignService;
    private final OrderService orderService;
    private final PaymentService paymentService;

    public TestFixtures(SellerService sellerService, BalanceService balanceService,
                        SubscriptionService subscriptionService, CategoryService categoryService,
                        ProductService productService, LedgerAccountRepository accountRepo,
                        CampaignService campaignService, OrderService orderService,
                        PaymentService paymentService) {
        this.sellerService = sellerService;
        this.balanceService = balanceService;
        this.subscriptionService = subscriptionService;
        this.categoryService = categoryService;
        this.productService = productService;
        this.accountRepo = accountRepo;
        this.campaignService = campaignService;
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    public long registerSeller() {
        return sellerService.register(new RegisterSellerRequest("Test Seller",
            "seller-" + UUID.randomUUID() + "@test.local", null)).id();
    }

    public void topUp(long sellerId, String amount) {
        balanceService.topUp(sellerId, new TopUpRequest(new BigDecimal(amount), UUID.randomUUID().toString()));
    }

    /** Для не-FREE пакета поповнює рівно на місячну ціну і купує MONTHLY; баланс після цього 0.00. */
    public long sellerWithPlan(PlanCode code) {
        long id = registerSeller();
        if (code != PlanCode.FREE) {
            String price = switch (code) {
                case START -> "990.00";
                case PRO -> "2490.00";
                case BUSINESS -> "5990.00";
                default -> throw new IllegalArgumentException("Unexpected plan " + code);
            };
            topUp(id, price);
            subscriptionService.purchase(id, code, BillingPeriod.MONTHLY);
        }
        return id;
    }

    public long categoryId(String name) {
        return categoryService.list().stream()
            .filter(c -> c.name().equals(name))
            .findFirst()
            .orElseThrow()
            .id();
    }

    public long createProduct(long sellerId, long categoryId, String title, String price) {
        return productService.create(sellerId, new CreateProductRequest(categoryId, title, null,
            new BigDecimal(price), 10, ProductStatus.ACTIVE)).id();
    }

    /** Нова категорія верхнього рівня: мінімальна ставка 1.00, комісія 5.00. */
    public long freshCategory() {
        return categoryService.create(new CreateCategoryRequest("Тест-" + UUID.randomUUID(), null,
            new BigDecimal("5.00"), new BigDecimal("1.00"))).id();
    }

    public long createCampaign(long sellerId, String dailyBudget) {
        return campaignService.create(sellerId, new CreateCampaignRequest("Кампанія", new BigDecimal(dailyBudget))).id();
    }

    public void bid(long campaignId, long productId, String amount) {
        campaignService.replaceBids(campaignId, List.of(new BidRequest(productId, new BigDecimal(amount))));
    }

    public OrderDto placeCashOrder(long productId, int quantity) {
        return orderService.checkout(new CheckoutRequest("Покупець", "+380501112233", null,
            PaymentMethod.CASH_ON_DELIVERY, List.of(new CheckoutItem(productId, quantity))));
    }

    /** Онлайн-замовлення без платежу (платіж створює тест через initPayment або UI). */
    public OrderDto placeOnlineOrder(long productId, int quantity) {
        return orderService.checkout(new CheckoutRequest("Покупець", "+380501112233", null,
            PaymentMethod.ONLINE, List.of(new CheckoutItem(productId, quantity))));
    }

    public PaymentDto initPayment(long orderId) {
        return paymentService.init(orderId);
    }

    public Money balance(long sellerId) {
        return balanceService.balanceOf(sellerId);
    }

    public Money ledger(String code) {
        return accountRepo.findByCode(code).orElseThrow().getBalance();
    }
}
