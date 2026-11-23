package ua.marketplace.web.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class UaDecimalEditorTest {

    @ParameterizedTest
    @CsvSource({"'1 234,56', 1234.56", "'1234,5', 1234.5", "'1234.56', 1234.56", "'  7 ', 7", "'0,01', 0.01",
        "'1\u00A0000,50', 1000.50"})
    void parsesSupportedFormats(String text, String expected) {
        UaDecimalEditor editor = new UaDecimalEditor();
        editor.setAsText(text);
        assertThat((BigDecimal) editor.getValue()).isEqualByComparingTo(expected);
    }

    @Test
    void blankBecomesNull() {
        UaDecimalEditor editor = new UaDecimalEditor();
        editor.setAsText("   ");
        assertThat(editor.getValue()).isNull();
        assertThat(editor.getAsText()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"abc", "'1.234,56'", "'12,3,4'", "'1e5'"})
    void rejectsGarbage(String text) {
        UaDecimalEditor editor = new UaDecimalEditor();
        assertThatThrownBy(() -> editor.setAsText(text)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rendersWithCommaAndNoGroups() {
        UaDecimalEditor editor = new UaDecimalEditor();
        editor.setValue(new BigDecimal("1234.50"));
        assertThat(editor.getAsText()).isEqualTo("1234,50");
    }
}
