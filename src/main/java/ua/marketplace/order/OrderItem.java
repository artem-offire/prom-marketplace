package ua.marketplace.order;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import ua.marketplace.common.money.Money;
import ua.marketplace.common.money.MoneyConverter;
import ua.marketplace.common.money.Percent;
import ua.marketplace.common.money.PercentConverter;

@Entity
@Table(name = "order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    private Long productId;

    private Long categoryId;

    @Column(length = 300)
    private String titleSnapshot;

    @Convert(converter = MoneyConverter.class)
    @Column(precision = 19, scale = 2)
    private Money unitPrice;

    private int quantity;

    @Convert(converter = MoneyConverter.class)
    @Column(precision = 19, scale = 2)
    private Money lineTotal;

    @Convert(converter = PercentConverter.class)
    @Column(precision = 5, scale = 2)
    private Percent commissionRateSnapshot;

    @Convert(converter = MoneyConverter.class)
    @Column(precision = 19, scale = 2)
    private Money commissionAmount;

    protected OrderItem() {
    }

    public OrderItem(Order order, Long productId, Long categoryId, String titleSnapshot, Money unitPrice,
                     int quantity, Percent commissionRateSnapshot) {
        this.order = order;
        this.productId = productId;
        this.categoryId = categoryId;
        this.titleSnapshot = titleSnapshot;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.lineTotal = unitPrice.times(quantity);
        this.commissionRateSnapshot = commissionRateSnapshot;
        this.commissionAmount = Money.ZERO;
    }

    public void applyCommission(Money m) {
        this.commissionAmount = m;
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public Long getProductId() {
        return productId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getTitleSnapshot() {
        return titleSnapshot;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public Money getLineTotal() {
        return lineTotal;
    }

    public Percent getCommissionRateSnapshot() {
        return commissionRateSnapshot;
    }

    public Money getCommissionAmount() {
        return commissionAmount;
    }
}
