package ua.marketplace.advertising.dto;

import java.math.BigDecimal;

/** Валідація полів виконується вручну в сервісі. */
public record BidRequest(Long productId, BigDecimal bidAmount) {
}
