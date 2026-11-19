package ua.marketplace.subscription;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ua.marketplace.seller.SellerRegisteredEvent;

@Component
public class FreePlanAssignmentListener {

    private final SubscriptionService subscriptionService;

    public FreePlanAssignmentListener(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @EventListener
    public void on(SellerRegisteredEvent e) {
        subscriptionService.assignFree(e.sellerId());
    }
}
