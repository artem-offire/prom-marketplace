package ua.marketplace.common.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PercentTest {

    @Test
    void parsesAndKeepsTwoDecimals() {
        assertThat(Percent.of("3.5").value()).isEqualByComparingTo("3.50");
        assertThat(Percent.of("3.5").value().scale()).isEqualTo(2);
        assertThat(Percent.of("100.00").value()).isEqualByComparingTo("100.00");
        assertThat(Percent.of("3.555").value()).isEqualByComparingTo("3.56");
    }

    @Test
    void rejectsOutOfRange() {
        assertThatThrownBy(() -> Percent.of("100.01")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Percent.of("-0.01")).isInstanceOf(IllegalArgumentException.class);
    }
}
