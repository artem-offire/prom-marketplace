package ua.marketplace.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import ua.marketplace.payment.PaymentStatus;

public record PaymentDto(Long id, Long orderId, BigDecimal amount, BigDecimal feeRate, BigDecimal feeAmount,
                         PaymentStatus status, String providerRef, Instant createdAt, Instant capturedAt) {
}
