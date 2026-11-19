package ua.marketplace.subscription.dto;

import jakarta.validation.constraints.NotNull;
import ua.marketplace.subscription.BillingPeriod;
import ua.marketplace.subscription.PlanCode;

public record PurchaseSubscriptionRequest(@NotNull PlanCode planCode, @NotNull BillingPeriod period) {
}
