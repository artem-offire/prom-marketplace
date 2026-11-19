package ua.marketplace.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ua.marketplace.common.money.Money;
import ua.marketplace.common.money.MoneyConverter;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long sellerId;

    private Long categoryId;

    @Column(length = 300)
    private String title;

    @Column(length = 4000)
    private String description;

    @Convert(converter = MoneyConverter.class)
    @Column(precision = 19, scale = 2)
    private Money price;

    private int stock;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private ProductStatus status;

    @Column(precision = 3, scale = 2)
    private BigDecimal qualityScore;

    private Instant createdAt;

    private Instant updatedAt;

    protected Product() {
    }

    public Product(Long sellerId, Long categoryId, String title, String description, Money price, int stock,
                   ProductStatus status, Instant now) {
        this.sellerId = sellerId;
        this.categoryId = categoryId;
        this.title = title;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.status = status;
        this.qualityScore = new BigDecimal("0.50");
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Кожен непорожній (не null) аргумент замінює відповідне поле. */
    public void update(Long categoryId, String title, String description, Money price, Integer stock,
                       ProductStatus status, Instant now) {
        if (categoryId != null) {
            this.categoryId = categoryId;
        }
        if (title != null) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        if (price != null) {
            this.price = price;
        }
        if (stock != null) {
            this.stock = stock;
        }
        if (status != null) {
            this.status = status;
        }
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Money getPrice() {
        return price;
    }

    public int getStock() {
        return stock;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public BigDecimal getQualityScore() {
        return qualityScore;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
