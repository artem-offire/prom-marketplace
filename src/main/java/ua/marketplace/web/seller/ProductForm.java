package ua.marketplace.web.seller;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import ua.marketplace.catalog.ProductStatus;
import ua.marketplace.catalog.dto.CreateProductRequest;
import ua.marketplace.catalog.dto.PatchProductRequest;
import ua.marketplace.catalog.dto.ProductDto;

public class ProductForm {

    @NotNull(message = "Оберіть категорію")
    private Long categoryId;

    @NotBlank(message = "Вкажіть назву")
    @Size(max = 300, message = "Не більше 300 символів")
    private String title;

    @Size(max = 4000, message = "Не більше 4000 символів")
    private String description;

    @NotNull(message = "Вкажіть ціну")
    @DecimalMin(value = "0.01", message = "Ціна має бути не менше 0,01")
    @Digits(integer = 15, fraction = 2, message = "Не більше двох знаків після коми")
    private BigDecimal price;

    @NotNull(message = "Вкажіть залишок")
    @Min(value = 0, message = "Залишок не може бути меншим за нуль")
    @Max(value = 1000000, message = "Не більше 1 000 000")
    private Integer stock = 0;

    @NotNull(message = "Оберіть статус")
    private ProductStatus status = ProductStatus.ACTIVE;

    public static ProductForm from(ProductDto p) {
        ProductForm f = new ProductForm();
        f.categoryId = p.categoryId();
        f.title = p.title();
        f.description = p.description();
        f.price = p.price();
        f.stock = p.stock();
        f.status = p.status();
        return f;
    }

    public CreateProductRequest toCreateRequest() {
        return new CreateProductRequest(categoryId, title, description, price, stock, status);
    }

    /** Порожній опис передається як "", щоб його можна було очистити (null означає "не змінювати"). */
    public PatchProductRequest toPatchRequest() {
        return new PatchProductRequest(categoryId, title, description == null ? "" : description, price, stock,
            status);
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }
}
