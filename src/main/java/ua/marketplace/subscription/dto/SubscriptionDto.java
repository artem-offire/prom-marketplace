package ua.marketplace.subscription.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import ua.marketplace.subscription.BillingPeriod;
import ua.marketplace.subscription.PlanCode;
import ua.marketplace.subscription.SubscriptionStatus;

public record SubscriptionDto(Long id, Long sellerId, PlanCode planCode, BillingPeriod billingPeriod,
                              BigDecimal pricePaid, LocalDate startDate, LocalDate endDate,
                              SubscriptionStatus status) {
}
