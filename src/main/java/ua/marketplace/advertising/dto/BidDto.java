package ua.marketplace.advertising.dto;

import java.math.BigDecimal;

public record BidDto(Long id, Long campaignId, Long productId, BigDecimal bidAmount) {
}
