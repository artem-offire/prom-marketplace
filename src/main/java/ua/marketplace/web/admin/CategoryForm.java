package ua.marketplace.web.admin;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import ua.marketplace.catalog.dto.CreateCategoryRequest;

public class CategoryForm {

    @NotBlank(message = "Вкажіть назву категорії")
    @Size(max = 200, message = "Не більше 200 символів")
    private String name;

    private Long parentId;

    @NotNull(message = "Вкажіть комісію")
    @DecimalMin(value = "0.00", message = "Комісія має бути від 0 до 100")
    @DecimalMax(value = "100.00", message = "Комісія має бути від 0 до 100")
    @Digits(integer = 3, fraction = 2, message = "Не більше двох знаків після коми")
    private BigDecimal commissionRate;

    @NotNull(message = "Вкажіть мінімальну ставку")
    @DecimalMin(value = "0.01", message = "Мінімальна ставка не менша за 0,01 грн")
    @Digits(integer = 15, fraction = 2, message = "Не більше двох знаків після коми")
    private BigDecimal minBid;

    public CreateCategoryRequest toRequest() {
        return new CreateCategoryRequest(name, parentId, commissionRate, minBid);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public BigDecimal getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(BigDecimal commissionRate) {
        this.commissionRate = commissionRate;
    }

    public BigDecimal getMinBid() {
        return minBid;
    }

    public void setMinBid(BigDecimal minBid) {
        this.minBid = minBid;
    }
}
