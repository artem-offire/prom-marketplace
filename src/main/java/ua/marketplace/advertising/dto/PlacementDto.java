package ua.marketplace.advertising.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PlacementDto(UUID impressionId, int position, Long productId, Long sellerId, String title,
                           BigDecimal price) {
}
