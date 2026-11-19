package ua.marketplace.subscription;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ua.marketplace.common.money.Money;
import ua.marketplace.common.money.MoneyConverter;

@Entity
@Table(name = "plan")
public class Plan {

    @Id
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private PlanCode code;

    @Column(length = 100)
    private String name;

    @Convert(converter = MoneyConverter.class)
    @Column(precision = 19, scale = 2)
    private Money monthlyPrice;

    @Convert(converter = MoneyConverter.class)
    @Column(precision = 19, scale = 2)
    private Money yearlyPrice;

    private int productLimit;

    private boolean marketplaceVisible;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "plan_feature", joinColumns = @JoinColumn(name = "plan_code"))
    @Column(name = "feature")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private Set<PlanFeature> features = new HashSet<>();

    protected Plan() {
    }

    public Money priceFor(BillingPeriod p) {
        return p == BillingPeriod.MONTHLY ? monthlyPrice : yearlyPrice;
    }

    public PlanCode getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Money getMonthlyPrice() {
        return monthlyPrice;
    }

    public Money getYearlyPrice() {
        return yearlyPrice;
    }

    public int getProductLimit() {
        return productLimit;
    }

    public boolean isMarketplaceVisible() {
        return marketplaceVisible;
    }

    public Set<PlanFeature> getFeatures() {
        return features;
    }
}
