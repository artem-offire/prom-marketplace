package ua.marketplace.advertising;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ua.marketplace.common.money.Money;
import ua.marketplace.common.money.MoneyConverter;

@Entity
@Table(name = "ad_click")
public class AdClick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID impressionId;

    @Column(length = 100)
    private String sessionId;

    @Convert(converter = MoneyConverter.class)
    @Column(precision = 19, scale = 2)
    private Money chargedAmount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private ClickStatus status;

    private UUID ledgerTransactionId;

    private Instant clickedAt;

    protected AdClick() {
    }

    public AdClick(UUID impressionId, String sessionId, Money chargedAmount, ClickStatus status,
                   UUID ledgerTransactionId, Instant clickedAt) {
        this.impressionId = impressionId;
        this.sessionId = sessionId;
        this.chargedAmount = chargedAmount;
        this.status = status;
        this.ledgerTransactionId = ledgerTransactionId;
        this.clickedAt = clickedAt;
    }

    public Long getId() {
        return id;
    }

    public UUID getImpressionId() {
        return impressionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Money getChargedAmount() {
        return chargedAmount;
    }

    public ClickStatus getStatus() {
        return status;
    }

    public UUID getLedgerTransactionId() {
        return ledgerTransactionId;
    }

    public Instant getClickedAt() {
        return clickedAt;
    }
}
