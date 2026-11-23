package ua.marketplace.web.shop;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.marketplace.catalog.CategoryService;
import ua.marketplace.catalog.MarketplaceQueryService;
import ua.marketplace.catalog.dto.CategoryDto;
import ua.marketplace.catalog.dto.ProductDto;
import ua.marketplace.common.error.BusinessRuleException;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.common.error.NotFoundException;
import ua.marketplace.order.OrderService;
import ua.marketplace.order.PaymentMethod;
import ua.marketplace.order.dto.OrderDto;
import ua.marketplace.payment.PaymentService;
import ua.marketplace.subscription.PlanFeature;
import ua.marketplace.subscription.PlanLimits;
import ua.marketplace.web.support.Flash;
import ua.marketplace.web.support.UiErrors;

@Controller
public class CheckoutController {

    private final MarketplaceQueryService queryService;
    private final CategoryService categoryService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final PlanLimits planLimits;
    private final UiErrors uiErrors;

    public CheckoutController(MarketplaceQueryService queryService, CategoryService categoryService,
                              OrderService orderService, PaymentService paymentService, PlanLimits planLimits,
                              UiErrors uiErrors) {
        this.queryService = queryService;
        this.categoryService = categoryService;
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.planLimits = planLimits;
        this.uiErrors = uiErrors;
    }

    @GetMapping("/checkout")
    public String form(@RequestParam Long productId, @RequestParam(defaultValue = "1") int quantity, Model model) {
        ProductDto product = visible(productId);
        CheckoutForm form = new CheckoutForm();
        form.setProductId(productId);
        form.setQuantity(Math.max(1, quantity));
        populate(model, product, form.getQuantity());
        model.addAttribute("form", form);
        return "shop/checkout";
    }

    @PostMapping("/checkout")
    public String submit(@Valid @ModelAttribute("form") CheckoutForm form, BindingResult br, Model model,
                         RedirectAttributes redirect) {
        if (form.getProductId() == null) {
            throw new NotFoundException(ErrorCode.PRODUCT_NOT_FOUND, "Product not specified");
        }
        ProductDto product = visible(form.getProductId());
        boolean onlineAllowed = onlineAllowed(product);
        if (form.getPaymentMethod() == PaymentMethod.ONLINE && !onlineAllowed) {
            br.rejectValue("paymentMethod", "online.not.allowed", "Онлайн-оплата недоступна для цього продавця");
        }
        if (form.getQuantity() != null && form.getQuantity() > product.stock()) {
            br.rejectValue("quantity", "quantity.too.big", "Доступно лише " + product.stock() + " шт.");
        }
        if (!br.hasErrors()) {
            try {
                OrderDto order = orderService.checkout(form.toRequest());
                if (order.paymentMethod() == PaymentMethod.ONLINE) {
                    paymentService.init(order.id());
                }
                redirect.addFlashAttribute(Flash.SUCCESS, "Замовлення №" + order.id() + " оформлено");
                return "redirect:/orders/" + order.id();
            } catch (BusinessRuleException e) {
                br.reject(e.code().name(), uiErrors.message(e.code()));
            }
        }
        populate(model, product, form.getQuantity() == null ? 1 : Math.max(1, form.getQuantity()));
        return "shop/checkout";
    }

    private ProductDto visible(Long productId) {
        return queryService.findVisible(productId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found: " + productId));
    }

    private boolean onlineAllowed(ProductDto product) {
        return planLimits.hasFeature(product.sellerId(), PlanFeature.ONLINE_PAYMENTS);
    }

    private void populate(Model model, ProductDto product, int quantity) {
        boolean online = onlineAllowed(product);
        String categoryName = categoryService.list().stream()
            .filter(c -> c.id().equals(product.categoryId()))
            .map(CategoryDto::name).findFirst().orElse("");
        List<PaymentMethod> methods = online
            ? List.of(PaymentMethod.CASH_ON_DELIVERY, PaymentMethod.ONLINE)
            : List.of(PaymentMethod.CASH_ON_DELIVERY);
        model.addAttribute("product", product);
        model.addAttribute("categoryName", categoryName);
        model.addAttribute("paymentMethods", methods);
        model.addAttribute("onlineAllowed", online);
        model.addAttribute("total", product.price().multiply(BigDecimal.valueOf(quantity)));
    }
}
