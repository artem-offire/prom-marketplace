package ua.marketplace.advertising;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import ua.marketplace.common.money.Money;
import ua.marketplace.common.money.MoneyConverter;

@Entity
@Table(name = "ad_impression")
public class AdImpression {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Long bidId;

    private Long campaignId;

    private Long productId;

    private Long categoryId;

    private int slotPosition;

    @Column(precision = 19, scale = 4)
    private BigDecimal adRank;

    @Convert(converter = MoneyConverter.class)
    @Column(precision = 19, scale = 2)
    private Money cpc;

    @Column(length = 100)
    private String sessionId;

    private Instant shownAt;

    protected AdImpression() {
    }

    public AdImpression(Long bidId, Long campaignId, Long productId, Long categoryId, int slotPosition,
                        BigDecimal adRank, Money cpc, String sessionId, Instant shownAt) {
        this.bidId = bidId;
        this.campaignId = campaignId;
        this.productId = productId;
        this.categoryId = categoryId;
        this.slotPosition = slotPosition;
        this.adRank = adRank;
        this.cpc = cpc;
        this.sessionId = sessionId;
        this.shownAt = shownAt;
    }

    public UUID getId() {
        return id;
    }

    public Long getBidId() {
        return bidId;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public int getSlotPosition() {
        return slotPosition;
    }

    public BigDecimal getAdRank() {
        return adRank;
    }

    public Money getCpc() {
        return cpc;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Instant getShownAt() {
        return shownAt;
    }
}
