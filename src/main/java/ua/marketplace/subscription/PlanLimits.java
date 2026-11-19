package ua.marketplace.subscription;

import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.common.error.NotFoundException;

/** Публічне API ліміт-правил пакета для інших модулів. */
@Service
public class PlanLimits {

    private final SubscriptionRepository subscriptionRepo;
    private final PlanRepository planRepo;

    public PlanLimits(SubscriptionRepository subscriptionRepo, PlanRepository planRepo) {
        this.subscriptionRepo = subscriptionRepo;
        this.planRepo = planRepo;
    }

    @Transactional(readOnly = true)
    public int productLimit(Long sellerId) {
        return activePlan(sellerId).getProductLimit();
    }

    @Transactional(readOnly = true)
    public boolean isMarketplaceVisible(Long sellerId) {
        return activePlan(sellerId).isMarketplaceVisible();
    }

    @Transactional(readOnly = true)
    public boolean hasFeature(Long sellerId, PlanFeature f) {
        return activePlan(sellerId).getFeatures().contains(f);
    }

    @Transactional(readOnly = true)
    public Set<Long> visibleSellerIds() {
        return new HashSet<>(subscriptionRepo.findVisibleSellerIds());
    }

    private Plan activePlan(Long sellerId) {
        Subscription sub = subscriptionRepo.findBySellerIdAndStatus(sellerId, SubscriptionStatus.ACTIVE)
            .orElseThrow(() -> new NotFoundException(ErrorCode.SELLER_NOT_FOUND, "Seller not found: " + sellerId));
        return planRepo.findById(sub.getPlanCode())
            .orElseThrow(() -> new NotFoundException(ErrorCode.PLAN_NOT_FOUND, "Plan not found: " + sub.getPlanCode()));
    }
}
