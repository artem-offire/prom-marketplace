package ua.marketplace.advertising.dto;

import java.math.BigDecimal;
import ua.marketplace.advertising.ClickStatus;

public record ClickResponse(Long clickId, ClickStatus status, BigDecimal chargedAmount) {
}
