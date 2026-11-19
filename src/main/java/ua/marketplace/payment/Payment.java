package ua.marketplace.payment;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ua.marketplace.common.money.Money;
import ua.marketplace.common.money.MoneyConverter;
import ua.marketplace.common.money.Percent;
import ua.marketplace.common.money.PercentConverter;

@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    @Convert(converter = MoneyConverter.class)
    @Column(precision = 19, scale = 2)
    private Money amount;

    @Convert(converter = PercentConverter.class)
    @Column(precision = 5, scale = 2)
    private Percent feeRate;

    @Convert(converter = MoneyConverter.class)
    @Column(precision = 19, scale = 2)
    private Money feeAmount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private PaymentStatus status;

    @Column(length = 100)
    private String providerRef;

    private Instant createdAt;

    private Instant capturedAt;

    protected Payment() {
    }

    public Payment(Long orderId, Money amount, Percent feeRate, Money feeAmount, Instant now) {
        this.orderId = orderId;
        this.amount = amount;
        this.feeRate = feeRate;
        this.feeAmount = feeAmount;
        this.status = PaymentStatus.PENDING;
        this.createdAt = now;
    }

    public void restart(Money amount, Percent feeRate, Money feeAmount) {
        this.amount = amount;
        this.feeRate = feeRate;
        this.feeAmount = feeAmount;
        this.status = PaymentStatus.PENDING;
        this.providerRef = null;
        this.capturedAt = null;
    }

    public void assignProviderRef(String ref) {
        this.providerRef = ref;
    }

    public void markCaptured(Instant now) {
        this.status = PaymentStatus.CAPTURED;
        this.capturedAt = now;
    }

    public void markFailed() {
        this.status = PaymentStatus.FAILED;
    }

    public void markRefunded() {
        this.status = PaymentStatus.REFUNDED;
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Money getAmount() {
        return amount;
    }

    public Percent getFeeRate() {
        return feeRate;
    }

    public Money getFeeAmount() {
        return feeAmount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getProviderRef() {
        return providerRef;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCapturedAt() {
        return capturedAt;
    }
}
