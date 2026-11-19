package ua.marketplace.advertising;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import ua.marketplace.common.money.Money;
import ua.marketplace.common.money.MoneyConverter;

@Entity
@Table(name = "ad_bid")
public class AdBid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long campaignId;

    private Long productId;

    @Convert(converter = MoneyConverter.class)
    @Column(precision = 19, scale = 2)
    private Money bidAmount;

    private Instant createdAt;

    protected AdBid() {
    }

    public AdBid(Long campaignId, Long productId, Money bidAmount, Instant now) {
        this.campaignId = campaignId;
        this.productId = productId;
        this.bidAmount = bidAmount;
        this.createdAt = now;
    }

    public void changeAmount(Money amount, Instant now) {
        this.bidAmount = amount;
        this.createdAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getCampaignId() {
        return campaignId;
    }

    public Long getProductId() {
        return productId;
    }

    public Money getBidAmount() {
        return bidAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
