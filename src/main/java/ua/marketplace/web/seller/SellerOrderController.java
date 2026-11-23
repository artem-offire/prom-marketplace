package ua.marketplace.web.seller;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.marketplace.common.error.BusinessRuleException;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.common.error.NotFoundException;
import ua.marketplace.common.web.PageResponse;
import ua.marketplace.order.OrderAction;
import ua.marketplace.order.OrderService;
import ua.marketplace.order.OrderStateMachine;
import ua.marketplace.order.OrderStatus;
import ua.marketplace.order.PaymentMethod;
import ua.marketplace.order.dto.OrderDto;
import ua.marketplace.payment.PaymentService;
import ua.marketplace.payment.PaymentStatus;
import ua.marketplace.payment.dto.PaymentDto;
import ua.marketplace.web.support.Flash;
import ua.marketplace.web.support.PageModel;
import ua.marketplace.web.support.UiErrors;

@Controller
public class SellerOrderController {

    private static final int PAGE_SIZE = 20;

    private final CabinetContext cabinet;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final UiErrors uiErrors;

    public SellerOrderController(CabinetContext cabinet, OrderService orderService, PaymentService paymentService,
                                 UiErrors uiErrors) {
        this.cabinet = cabinet;
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.uiErrors = uiErrors;
    }

    @GetMapping("/cabinet/{sellerId}/orders")
    public String list(@PathVariable Long sellerId, @RequestParam(required = false) OrderStatus status,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        cabinet.load(sellerId, model);
        PageResponse<OrderDto> orders = orderService.listBySeller(sellerId, status, page, PAGE_SIZE);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("status", status == null ? null : status.name());
        model.addAttribute("orders", orders);
        model.addAttribute("pager", PageModel.of(orders, "/cabinet/" + sellerId + "/orders", params));
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("selectedStatus", status);
        return "cabinet/orders";
    }

    @GetMapping("/cabinet/{sellerId}/orders/{orderId}")
    public String detail(@PathVariable Long sellerId, @PathVariable Long orderId, Model model) {
        cabinet.load(sellerId, model);
        OrderDto order = cabinet.order(sellerId, orderId);
        PaymentDto payment = paymentService.findByOrder(orderId).orElse(null);
        Set<OrderAction> actions = OrderStateMachine.allowedActions(order.status());
        boolean confirmBlocked = order.paymentMethod() == PaymentMethod.ONLINE
            && (payment == null || payment.status() != PaymentStatus.CAPTURED);
        model.addAttribute("order", order);
        model.addAttribute("payment", payment);
        model.addAttribute("actions", actions);
        model.addAttribute("confirmBlocked", confirmBlocked);
        return "cabinet/order";
    }

    @PostMapping("/cabinet/{sellerId}/orders/{orderId}/{action}")
    public String apply(@PathVariable Long sellerId, @PathVariable Long orderId, @PathVariable String action,
                        RedirectAttributes redirect) {
        cabinet.order(sellerId, orderId);
        OrderAction orderAction = switch (action) {
            case "confirm" -> OrderAction.CONFIRM;
            case "ship" -> OrderAction.SHIP;
            case "complete" -> OrderAction.COMPLETE;
            case "cancel" -> OrderAction.CANCEL;
            case "return" -> OrderAction.RETURN;
            default -> throw new NotFoundException(ErrorCode.ORDER_NOT_FOUND, "Unknown action: " + action);
        };
        try {
            orderService.apply(orderId, orderAction);
            redirect.addFlashAttribute(Flash.SUCCESS, uiErrors.text("order.action.done." + orderAction.name()));
        } catch (BusinessRuleException e) {
            redirect.addFlashAttribute(Flash.ERROR, uiErrors.message(e.code()));
        }
        return "redirect:/cabinet/" + sellerId + "/orders/" + orderId;
    }
}
