package ua.marketplace.subscription;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.marketplace.subscription.dto.PurchaseSubscriptionRequest;
import ua.marketplace.subscription.dto.SubscriptionDto;

@RestController
@RequestMapping("/api/v1")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/sellers/{id}/subscription")
    public ResponseEntity<SubscriptionDto> active(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.getActive(id));
    }

    @PostMapping("/sellers/{id}/subscription")
    public ResponseEntity<SubscriptionDto> purchase(@PathVariable Long id,
                                                    @Valid @RequestBody PurchaseSubscriptionRequest request) {
        SubscriptionDto dto = subscriptionService.purchase(id, request.planCode(), request.period());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }
}
