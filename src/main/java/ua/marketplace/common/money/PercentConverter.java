package ua.marketplace.common.money;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.math.BigDecimal;

@Converter
public class PercentConverter implements AttributeConverter<Percent, BigDecimal> {

    @Override
    public BigDecimal convertToDatabaseColumn(Percent p) {
        return p == null ? null : p.value();
    }

    @Override
    public Percent convertToEntityAttribute(BigDecimal b) {
        return b == null ? null : Percent.of(b);
    }
}
