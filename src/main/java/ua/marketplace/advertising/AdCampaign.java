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
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ua.marketplace.common.money.Money;
import ua.marketplace.common.money.MoneyConverter;

@Entity
@Table(name = "ad_campaign")
public class AdCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long sellerId;

    @Column(length = 200)
    private String name;

    @Convert(converter = MoneyConverter.class)
    @Column(precision = 19, scale = 2)
    private Money dailyBudget;

    @Convert(converter = MoneyConverter.class)
    @Column(precision = 19, scale = 2)
    private Money spentToday;

    private LocalDate spentDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private CampaignStatus status;

    private Instant createdAt;

    protected AdCampaign() {
    }

    public AdCampaign(Long sellerId, String name, Money dailyBudget, LocalDate today, Instant now) {
        this.sellerId = sellerId;
        this.name = name;
        this.dailyBudget = dailyBudget;
        this.spentToday = Money.ZERO;
        this.spentDate = today;
        this.status = CampaignStatus.ACTIVE;
        this.createdAt = now;
    }

    public Money spentOn(LocalDate today) {
        return spentDate.equals(today) ? spentToday : Money.ZERO;
    }

    public Money remaining(LocalDate today) {
        return dailyBudget.minus(spentOn(today));
    }

    public void registerSpend(LocalDate today, Money amount) {
        if (!spentDate.equals(today)) {
            spentToday = Money.ZERO;
            spentDate = today;
        }
        spentToday = spentToday.plus(amount);
    }

    public void changeBudget(Money b) {
        this.dailyBudget = b;
    }

    public void changeStatus(CampaignStatus s) {
        this.status = s;
    }

    public Long getId() {
        return id;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public String getName() {
        return name;
    }

    public Money getDailyBudget() {
        return dailyBudget;
    }

    public Money getSpentToday() {
        return spentToday;
    }

    public LocalDate getSpentDate() {
        return spentDate;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
