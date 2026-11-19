package ua.marketplace.advertising.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateCampaignRequest(
    @NotBlank @Size(max = 200) String name,
    @NotNull @DecimalMin("0.01") @Digits(integer = 15, fraction = 2) BigDecimal dailyBudget) {
}
