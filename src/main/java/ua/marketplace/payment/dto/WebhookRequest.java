package ua.marketplace.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ua.marketplace.payment.WebhookResult;

public record WebhookRequest(
    @NotBlank @Size(max = 100) String providerRef,
    @NotNull WebhookResult result) {
}
