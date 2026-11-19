package ua.marketplace.common.money;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.math.BigDecimal;

@Converter
public class MoneyConverter implements AttributeConverter<Money, BigDecimal> {

    @Override
    public BigDecimal convertToDatabaseColumn(Money m) {
        return m == null ? null : m.amount();
    }

    @Override
    public Money convertToEntityAttribute(BigDecimal b) {
        return b == null ? null : Money.of(b);
    }
}
