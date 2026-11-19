package ua.marketplace.catalog.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateCategoryRequest(
    @NotBlank @Size(max = 200) String name,
    Long parentId,
    @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal commissionRate,
    @NotNull @DecimalMin("0.01") BigDecimal minBid) {
}
