package ua.marketplace.web.seller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ua.marketplace.seller.dto.RegisterSellerRequest;

public class RegisterSellerForm {

    @NotBlank(message = "Вкажіть назву магазину або ім’я")
    @Size(max = 200, message = "Не більше 200 символів")
    private String name;

    @NotBlank(message = "Вкажіть пошту")
    @Email(message = "Вкажіть коректну пошту")
    @Size(max = 254, message = "Не більше 254 символів")
    private String email;

    @Size(max = 32, message = "Не більше 32 символів")
    private String phone;

    public RegisterSellerRequest toRequest() {
        return new RegisterSellerRequest(name, email, phone);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
