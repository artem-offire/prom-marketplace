package ua.marketplace.catalog.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import ua.marketplace.catalog.ProductStatus;

public record CreateProductRequest(
    @NotNull Long categoryId,
    @NotBlank @Size(max = 300) String title,
    @Size(max = 4000) String description,
    @NotNull @DecimalMin("0.01") @Digits(integer = 15, fraction = 2) BigDecimal price,
    @Min(0) int stock,
    ProductStatus status) {
}
