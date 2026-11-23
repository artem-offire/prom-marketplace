package ua.marketplace.web.admin;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import ua.marketplace.catalog.dto.UpdateCategoryPricingRequest;

public class CategoryPricingForm {

    @NotNull(message = "Вкажіть комісію")
    @DecimalMin(value = "0.00", message = "Комісія має бути від 0 до 100")
    @DecimalMax(value = "100.00", message = "Комісія має бути від 0 до 100")
    @Digits(integer = 3, fraction = 2, message = "Не більше двох знаків після коми")
    private BigDecimal commissionRate;

    @NotNull(message = "Вкажіть мінімальну ставку")
    @DecimalMin(value = "0.01", message = "Мінімальна ставка не менша за 0,01 грн")
    @Digits(integer = 15, fraction = 2, message = "Не більше двох знаків після коми")
    private BigDecimal minBid;

    public UpdateCategoryPricingRequest toRequest() {
        return new UpdateCategoryPricingRequest(commissionRate, minBid);
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
