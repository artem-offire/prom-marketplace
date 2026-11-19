package ua.marketplace.advertising;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdImpressionRepository extends JpaRepository<AdImpression, UUID> {
}
