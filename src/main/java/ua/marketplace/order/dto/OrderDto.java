package ua.marketplace.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import ua.marketplace.order.OrderStatus;
import ua.marketplace.order.PaymentMethod;

public record OrderDto(Long id, Long sellerId, String buyerName, String buyerPhone, String buyerEmail,
                       PaymentMethod paymentMethod, OrderStatus status, BigDecimal total,
                       BigDecimal commissionAmount, Instant createdAt, Instant completedAt,
                       List<OrderItemDto> items) {
}
