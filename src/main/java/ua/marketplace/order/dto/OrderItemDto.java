package ua.marketplace.order.dto;

import java.math.BigDecimal;

public record OrderItemDto(Long id, Long productId, String title, BigDecimal unitPrice, int quantity,
                           BigDecimal lineTotal, BigDecimal commissionRate, BigDecimal commissionAmount) {
}
