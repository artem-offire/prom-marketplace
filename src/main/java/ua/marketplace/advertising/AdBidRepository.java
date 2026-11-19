package ua.marketplace.advertising;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AdBidRepository extends JpaRepository<AdBid, Long> {

    List<AdBid> findByCampaignId(Long campaignId);

    @Query("select b from AdBid b where b.campaignId in "
            + "(select c.id from AdCampaign c where c.status = ua.marketplace.advertising.CampaignStatus.ACTIVE)")
    List<AdBid> findAllInActiveCampaigns();
}
