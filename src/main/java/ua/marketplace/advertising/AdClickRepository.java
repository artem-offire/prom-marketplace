package ua.marketplace.advertising;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdClickRepository extends JpaRepository<AdClick, Long> {

    boolean existsByImpressionId(UUID impressionId);
}
