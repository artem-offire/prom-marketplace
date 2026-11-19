package ua.marketplace.subscription;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.marketplace.subscription.dto.PlanDto;

@RestController
@RequestMapping("/api/v1")
public class PlanController {

    private final SubscriptionService subscriptionService;

    public PlanController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/plans")
    public ResponseEntity<List<PlanDto>> plans() {
        return ResponseEntity.ok(subscriptionService.listPlans());
    }
}
