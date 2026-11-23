package ua.marketplace.web.seller;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import ua.marketplace.billing.dto.TopUpRequest;

public class TopUpForm {

    @NotNull(message = "Вкажіть суму")
    @DecimalMin(value = "0.01", message = "Сума має бути не менше 0,01 грн")
    @Digits(integer = 15, fraction = 2, message = "Не більше двох знаків після коми")
    private BigDecimal amount;

    @NotBlank(message = "Ключ операції відсутній, оновіть сторінку")
    @Size(max = 100, message = "Ключ операції задовгий")
    private String idempotencyKey;

    public TopUpRequest toRequest() {
        return new TopUpRequest(amount, idempotencyKey);
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}
