package ua.marketplace.web.seller;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class BudgetForm {

    @NotNull(message = "Вкажіть денний бюджет")
    @DecimalMin(value = "0.01", message = "Бюджет має бути не менше 0,01 грн")
    @Digits(integer = 15, fraction = 2, message = "Не більше двох знаків після коми")
    private BigDecimal dailyBudget;

    public BigDecimal getDailyBudget() {
        return dailyBudget;
    }

    public void setDailyBudget(BigDecimal dailyBudget) {
        this.dailyBudget = dailyBudget;
    }
}
