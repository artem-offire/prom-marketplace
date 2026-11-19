package ua.marketplace.catalog;

import java.math.BigDecimal;
import ua.marketplace.common.money.Money;

public record ProductView(Long id, Long sellerId, Long categoryId, String title, Money price, int stock,
                          ProductStatus status, BigDecimal qualityScore) {
}
