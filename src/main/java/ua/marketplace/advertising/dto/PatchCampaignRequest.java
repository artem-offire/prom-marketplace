package ua.marketplace.advertising.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;
import ua.marketplace.advertising.CampaignStatus;

public record PatchCampaignRequest(
    @DecimalMin("0.01") @Digits(integer = 15, fraction = 2) BigDecimal dailyBudget,
    CampaignStatus status) {
}
