package ua.marketplace.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import ua.marketplace.order.PaymentMethod;

public record CheckoutRequest(
    @NotBlank @Size(max = 200) String buyerName,
    @NotBlank @Size(max = 32) String buyerPhone,
    @Email @Size(max = 254) String buyerEmail,
    @NotNull PaymentMethod paymentMethod,
    @NotEmpty @Valid List<CheckoutItem> items) {
}
