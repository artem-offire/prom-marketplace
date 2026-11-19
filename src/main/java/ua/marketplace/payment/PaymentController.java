package ua.marketplace.payment;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.marketplace.payment.dto.PaymentDto;
import ua.marketplace.payment.dto.WebhookRequest;

@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/orders/{id}/payments")
    public ResponseEntity<PaymentDto> init(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.init(id));
    }

    @PostMapping("/payments/webhook")
    public ResponseEntity<PaymentDto> webhook(@Valid @RequestBody WebhookRequest request) {
        return ResponseEntity.ok(paymentService.handleWebhook(request));
    }

    @PostMapping("/payments/{id}/refund")
    public ResponseEntity<PaymentDto> refund(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.refund(id));
    }

    @GetMapping("/payments/{id}")
    public ResponseEntity<PaymentDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.get(id));
    }
}
