package ua.marketplace.subscription;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ua.marketplace.common.money.Money;
import ua.marketplace.common.money.MoneyConverter;

@Entity
@Table(name = "subscription")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long sellerId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private PlanCode planCode;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private BillingPeriod billingPeriod;

    @Convert(converter = MoneyConverter.class)
    @Column(precision = 19, scale = 2)
    private Money pricePaid;

    private LocalDate startDate;

    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private SubscriptionStatus status;

    private boolean autoRenew;

    protected Subscription() {
    }

    public Subscription(Long sellerId, PlanCode planCode, BillingPeriod billingPeriod, Money pricePaid,
                        LocalDate startDate, LocalDate endDate) {
        this.sellerId = sellerId;
        this.planCode = planCode;
        this.billingPeriod = billingPeriod;
        this.pricePaid = pricePaid;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = SubscriptionStatus.ACTIVE;
        this.autoRenew = false;
    }

    public void cancel() {
        this.status = SubscriptionStatus.CANCELLED;
    }

    public Long getId() {
        return id;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public PlanCode getPlanCode() {
        return planCode;
    }

    public BillingPeriod getBillingPeriod() {
        return billingPeriod;
    }

    public Money getPricePaid() {
        return pricePaid;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }
}
