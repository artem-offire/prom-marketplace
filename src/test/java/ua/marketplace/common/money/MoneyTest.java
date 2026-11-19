package ua.marketplace.common.money;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MoneyTest {

    @ParameterizedTest
    @CsvSource({"0.005,0.01", "0.004,0.00", "0.015,0.02", "-0.005,-0.01", "1,1.00"})
    void roundsHalfUpToTwoDecimals(String input, String expected) {
        assertThat(Money.of(input)).isEqualTo(Money.of(expected));
        assertThat(Money.of(input).amount().scale()).isEqualTo(2);
        assertThat(Money.of(input).amount()).isEqualByComparingTo(expected);
    }

    @Test
    void equalsIgnoresInputScale() {
        assertThat(Money.of("1.0")).isEqualTo(Money.of("1.00"));
    }

    @ParameterizedTest
    @CsvSource({"1234.56,3.50,43.21", "999.99,33.33,333.30", "0.01,5.00,0.00", "0.10,5.00,0.01",
        "100.00,0.00,0.00", "100.00,100.00,100.00"})
    void percentRoundsHalfUp(String amount, String rate, String expected) {
        assertThat(Money.of(amount).percent(Percent.of(rate))).isEqualTo(Money.of(expected));
    }

    @Test
    void arithmeticAndComparison() {
        assertThat(Money.of("19.99").times(3)).isEqualTo(Money.of("59.97"));
        assertThat(Money.of("10.00").plus(Money.of("0.25"))).isEqualTo(Money.of("10.25"));
        assertThat(Money.of("10.00").minus(Money.of("12.00"))).isEqualTo(Money.of("-2.00"));
        assertThat(Money.of("5.00").negate()).isEqualTo(Money.of("-5.00"));
        assertThat(Money.of("-0.01").isNegative()).isTrue();
        assertThat(Money.of("0.01").isPositive()).isTrue();
        assertThat(Money.of("2.00").compareTo(Money.of("10.00"))).isNegative();
        assertThat(Money.of(new BigDecimal("3"))).isEqualTo(Money.of("3.00"));
    }
}
