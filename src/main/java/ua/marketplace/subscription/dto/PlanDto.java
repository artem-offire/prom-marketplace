package ua.marketplace.subscription.dto;

import java.math.BigDecimal;
import java.util.List;
import ua.marketplace.subscription.PlanCode;
import ua.marketplace.subscription.PlanFeature;

public record PlanDto(PlanCode code, String name, BigDecimal monthlyPrice, BigDecimal yearlyPrice,
                      int productLimit, boolean marketplaceVisible, List<PlanFeature> features) {
}
