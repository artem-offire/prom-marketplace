package ua.marketplace.web.seller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Форма ставок: один рядок на активний товар продавця. Порожня ставка означає "без ставки". */
public class BidsForm {

    @Valid
    private List<BidRow> rows = new ArrayList<>();

    public List<BidRow> getRows() {
        return rows;
    }

    public void setRows(List<BidRow> rows) {
        this.rows = rows;
    }

    public static class BidRow {

        private Long productId;

        @DecimalMin(value = "0.01", message = "Ставка має бути не менше 0,01 грн")
        @Digits(integer = 15, fraction = 2, message = "Не більше двох знаків після коми")
        private BigDecimal amount;

        /** Лише для показу; не береться з запиту. */
        private String title;

        /** Лише для показу: мінімальна ставка категорії. */
        private BigDecimal minBid;

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public BigDecimal getMinBid() {
            return minBid;
        }

        public void setMinBid(BigDecimal minBid) {
            this.minBid = minBid;
        }
    }
}
