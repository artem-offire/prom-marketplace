package ua.marketplace.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CheckoutItem(@NotNull Long productId, @Min(1) int quantity) {
}
