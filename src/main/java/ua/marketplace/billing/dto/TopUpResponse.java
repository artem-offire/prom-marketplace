package ua.marketplace.billing.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TopUpResponse(UUID transactionId, BigDecimal balance, boolean replayed) {
}
