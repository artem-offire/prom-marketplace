package ua.marketplace.web.seller;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import ua.marketplace.advertising.dto.CreateCampaignRequest;

public class CampaignForm {

    @NotBlank(message = "Вкажіть назву кампанії")
    @Size(max = 200, message = "Не більше 200 символів")
    private String name;

    @NotNull(message = "Вкажіть денний бюджет")
    @DecimalMin(value = "0.01", message = "Бюджет має бути не менше 0,01 грн")
    @Digits(integer = 15, fraction = 2, message = "Не більше двох знаків після коми")
    private BigDecimal dailyBudget;

    public CreateCampaignRequest toRequest() {
        return new CreateCampaignRequest(name, dailyBudget);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getDailyBudget() {
        return dailyBudget;
    }

    public void setDailyBudget(BigDecimal dailyBudget) {
        this.dailyBudget = dailyBudget;
    }
}
