package ua.marketplace.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ua.marketplace.common.money.Money;
import ua.marketplace.common.money.MoneyConverter;
import ua.marketplace.common.money.Percent;
import ua.marketplace.common.money.PercentConverter;

@Entity
@Table(name = "category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200)
    private String name;

    private Long parentId;

    @Convert(converter = PercentConverter.class)
    @Column(precision = 5, scale = 2)
    private Percent commissionRate;

    @Convert(converter = MoneyConverter.class)
    @Column(precision = 19, scale = 2)
    private Money minBid;

    protected Category() {
    }

    public Category(String name, Long parentId, Percent commissionRate, Money minBid) {
        this.name = name;
        this.parentId = parentId;
        this.commissionRate = commissionRate;
        this.minBid = minBid;
    }

    public void updatePricing(Percent commissionRate, Money minBid) {
        this.commissionRate = commissionRate;
        this.minBid = minBid;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getParentId() {
        return parentId;
    }

    public Percent getCommissionRate() {
        return commissionRate;
    }

    public Money getMinBid() {
        return minBid;
    }
}
