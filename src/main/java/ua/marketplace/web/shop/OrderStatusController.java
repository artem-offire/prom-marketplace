package ua.marketplace.web.shop;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.marketplace.common.error.BusinessRuleException;
import ua.marketplace.order.OrderService;
import ua.marketplace.order.OrderStatus;
import ua.marketplace.order.dto.OrderDto;
import ua.marketplace.payment.PaymentService;
import ua.marketplace.payment.dto.PaymentDto;
import ua.marketplace.web.support.Flash;
import ua.marketplace.web.support.UiErrors;

@Controller
public class OrderStatusController {

    private static final List<OrderStatus> STEPS =
        List.of(OrderStatus.NEW, OrderStatus.CONFIRMED, OrderStatus.SHIPPED, OrderStatus.COMPLETED);

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final UiErrors uiErrors;

    public OrderStatusController(OrderService orderService, PaymentService paymentService, UiErrors uiErrors) {
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.uiErrors = uiErrors;
    }

    @GetMapping("/orders/{id}")
    public String order(@PathVariable Long id, Model model) {
        OrderDto order = orderService.get(id);
        PaymentDto payment = paymentService.findByOrder(id).orElse(null);
        model.addAttribute("order", order);
        model.addAttribute("payment", payment);
        model.addAttribute("steps", STEPS.stream().map(Enum::name).toList());
        model.addAttribute("stepIndex", STEPS.indexOf(order.status()));
        return "shop/order";
    }

    @PostMapping("/orders/{id}/pay")
    public String pay(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            paymentService.init(id);
            redirect.addFlashAttribute(Flash.SUCCESS, "Платіж створено. Очікуємо оплату");
        } catch (BusinessRuleException e) {
            redirect.addFlashAttribute(Flash.ERROR, uiErrors.message(e.code()));
        }
        return "redirect:/orders/" + id;
    }
}
