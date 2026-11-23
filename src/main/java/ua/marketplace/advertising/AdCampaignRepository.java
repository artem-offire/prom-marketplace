package ua.marketplace.advertising;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdCampaignRepository extends JpaRepository<AdCampaign, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from AdCampaign c where c.id = :id")
    Optional<AdCampaign> findForUpdate(@Param("id") Long id);

    List<AdCampaign> findByIdIn(Collection<Long> ids);

    List<AdCampaign> findBySellerIdOrderByIdDesc(Long sellerId);
}
