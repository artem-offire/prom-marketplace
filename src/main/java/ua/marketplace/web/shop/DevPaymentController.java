package ua.marketplace.web.shop;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.marketplace.common.error.BusinessRuleException;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.common.error.NotFoundException;
import ua.marketplace.payment.PaymentService;
import ua.marketplace.payment.WebhookResult;
import ua.marketplace.payment.dto.PaymentDto;
import ua.marketplace.payment.dto.WebhookRequest;
import ua.marketplace.web.support.Flash;
import ua.marketplace.web.support.UiErrors;

/** Імітація відповіді платіжної системи. Існує лише коли app.ui.dev-tools=true. */
@Controller
@ConditionalOnProperty(name = "app.ui.dev-tools", havingValue = "true")
public class DevPaymentController {

    private final PaymentService paymentService;
    private final UiErrors uiErrors;

    public DevPaymentController(PaymentService paymentService, UiErrors uiErrors) {
        this.paymentService = paymentService;
        this.uiErrors = uiErrors;
    }

    @PostMapping("/dev/payments/{paymentId}/{result}")
    public String simulate(@PathVariable Long paymentId, @PathVariable String result,
                           RedirectAttributes redirect) {
        WebhookResult webhook = switch (result) {
            case "success" -> WebhookResult.SUCCESS;
            case "failure" -> WebhookResult.FAILURE;
            default -> throw new NotFoundException(ErrorCode.PAYMENT_NOT_FOUND, "Unknown result: " + result);
        };
        PaymentDto payment = paymentService.get(paymentId);
        try {
            paymentService.handleWebhook(new WebhookRequest(payment.providerRef(), webhook));
            redirect.addFlashAttribute(Flash.SUCCESS, webhook == WebhookResult.SUCCESS
                ? "Оплату отримано (режим розробки)"
                : "Оплату відхилено (режим розробки)");
        } catch (BusinessRuleException e) {
            redirect.addFlashAttribute(Flash.ERROR, uiErrors.message(e.code()));
        }
        return "redirect:/orders/" + payment.orderId();
    }
}
