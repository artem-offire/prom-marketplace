package ua.marketplace.web.support;

import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;

/**
 * Приймає "1 234,56", "1234,56", "1234.56" (крапка це десятковий роздільник).
 * Порожнє значення дає null. "1.234,56" і "abc" це помилка.
 */
public class UaDecimalEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) {
        if (text == null || text.isBlank()) {
            setValue(null);
            return;
        }
        String s = text.trim()
            .replace("\u00A0", "")
            .replace("\u202F", "")
            .replace(" ", "")
            .replace(',', '.');
        if (!s.matches("[+-]?\\d+(\\.\\d+)?")) {
            throw new IllegalArgumentException("Not a number: " + text);
        }
        setValue(new BigDecimal(s));
    }

    @Override
    public String getAsText() {
        Object value = getValue();
        return value == null ? "" : ((BigDecimal) value).toPlainString().replace('.', ',');
    }
}
