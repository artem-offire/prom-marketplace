package ua.marketplace.web.shop;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import ua.marketplace.order.PaymentMethod;
import ua.marketplace.order.dto.CheckoutItem;
import ua.marketplace.order.dto.CheckoutRequest;

public class CheckoutForm {

    private Long productId;

    @NotNull(message = "Вкажіть кількість")
    @Min(value = 1, message = "Мінімальна кількість: 1")
    @Max(value = 999, message = "Максимальна кількість: 999")
    private Integer quantity = 1;

    @NotBlank(message = "Вкажіть ім’я")
    @Size(max = 200, message = "Не більше 200 символів")
    private String buyerName;

    @NotBlank(message = "Вкажіть телефон")
    @Pattern(regexp = "^\\+?[0-9 ()\\-]{7,20}$", message = "Вкажіть телефон у форматі +380501234567")
    private String buyerPhone;

    @Email(message = "Вкажіть коректну пошту")
    @Size(max = 254, message = "Не більше 254 символів")
    private String buyerEmail;

    @NotNull(message = "Оберіть спосіб оплати")
    private PaymentMethod paymentMethod = PaymentMethod.CASH_ON_DELIVERY;

    /** Один товар на замовлення; телефон нормалізується до цифр і плюса. */
    public CheckoutRequest toRequest() {
        String phone = buyerPhone.replaceAll("[\\s()\\-]", "");
        return new CheckoutRequest(buyerName, phone, buyerEmail, paymentMethod,
            List.of(new CheckoutItem(productId, quantity)));
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public String getBuyerPhone() {
        return buyerPhone;
    }

    public void setBuyerPhone(String buyerPhone) {
        this.buyerPhone = buyerPhone;
    }

    public String getBuyerEmail() {
        return buyerEmail;
    }

    public void setBuyerEmail(String buyerEmail) {
        this.buyerEmail = buyerEmail;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
