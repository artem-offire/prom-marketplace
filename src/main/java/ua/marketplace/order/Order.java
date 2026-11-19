package ua.marketplace.order;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ua.marketplace.common.money.Money;
import ua.marketplace.common.money.MoneyConverter;

/** Сутність названа MarketOrder, бо ORDER є зарезервованим словом у JPQL. Таблиця: orders. */
@Entity(name = "MarketOrder")
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long sellerId;

    @Column(length = 200)
    private String buyerName;

    @Column(length = 32)
    private String buyerPhone;

    @Column(length = 254)
    private String buyerEmail;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private OrderStatus status;

    @Convert(converter = MoneyConverter.class)
    @Column(precision = 19, scale = 2)
    private Money total;

    @Convert(converter = MoneyConverter.class)
    @Column(precision = 19, scale = 2)
    private Money commissionAmount;

    private Instant createdAt;

    private Instant completedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
    }

    public Order(Long sellerId, String buyerName, String buyerPhone, String buyerEmail, PaymentMethod method,
                 Instant now) {
        this.sellerId = sellerId;
        this.buyerName = buyerName;
        this.buyerPhone = buyerPhone;
        this.buyerEmail = buyerEmail;
        this.paymentMethod = method;
        this.status = OrderStatus.NEW;
        this.total = Money.ZERO;
        this.commissionAmount = Money.ZERO;
        this.createdAt = now;
    }

    public void addItem(OrderItem i) {
        items.add(i);
        total = total.plus(i.getLineTotal());
    }

    public void changeStatus(OrderStatus s) {
        this.status = s;
    }

    public void applyCommission(Money total) {
        this.commissionAmount = total;
    }

    public void markCompleted(Instant now) {
        this.completedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public String getBuyerPhone() {
        return buyerPhone;
    }

    public String getBuyerEmail() {
        return buyerEmail;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public Money getTotal() {
        return total;
    }

    public Money getCommissionAmount() {
        return commissionAmount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }
}
