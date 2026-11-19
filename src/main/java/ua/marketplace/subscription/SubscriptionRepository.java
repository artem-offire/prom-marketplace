package ua.marketplace.subscription;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findBySellerIdAndStatus(Long sellerId, SubscriptionStatus status);

    List<Subscription> findBySellerId(Long sellerId);

    @Query("select s.sellerId from Subscription s, Plan p where p.code = s.planCode "
            + "and s.status = ua.marketplace.subscription.SubscriptionStatus.ACTIVE "
            + "and p.marketplaceVisible = true")
    List<Long> findVisibleSellerIds();
}
