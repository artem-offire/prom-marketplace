package ua.marketplace.billing.dto;

import java.math.BigDecimal;

public record BalanceDto(BigDecimal balance, BigDecimal payable, String currency) {
}
