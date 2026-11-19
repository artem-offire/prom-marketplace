package ua.marketplace.catalog.dto;

import java.math.BigDecimal;
import java.time.Instant;
import ua.marketplace.catalog.ProductStatus;

public record ProductDto(Long id, Long sellerId, Long categoryId, String title, String description,
                         BigDecimal price, int stock, ProductStatus status, BigDecimal qualityScore,
                         Instant createdAt) {
}
