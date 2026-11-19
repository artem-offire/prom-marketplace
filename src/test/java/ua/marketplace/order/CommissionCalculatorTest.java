package ua.marketplace.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import ua.marketplace.common.money.Money;
import ua.marketplace.common.money.Percent;

class CommissionCalculatorTest {

    @Test
    void roundsEachLineThenSumsWithoutRoundingTotal() {
        Money a = CommissionCalculator.lineCommission(Money.of("333.33"), Percent.of("5.00"));
        Money b = CommissionCalculator.lineCommission(Money.of("199.99"), Percent.of("10.00"));
        Money c = CommissionCalculator.lineCommission(Money.of("10.10"), Percent.of("8.00"));

        assertThat(a).isEqualTo(Money.of("16.67"));
        assertThat(b).isEqualTo(Money.of("20.00"));
        assertThat(c).isEqualTo(Money.of("0.81"));
        assertThat(CommissionCalculator.sum(List.of(a, b, c))).isEqualTo(Money.of("37.48"));
    }

    @Test
    void edgeAmounts() {
        assertThat(CommissionCalculator.lineCommission(Money.of("1000.00"), Percent.of("5.00")))
            .isEqualTo(Money.of("50.00"));
        assertThat(CommissionCalculator.lineCommission(Money.of("0.01"), Percent.of("5.00")))
            .isEqualTo(Money.of("0.00"));
        assertThat(CommissionCalculator.lineCommission(Money.of("0.10"), Percent.of("5.00")))
            .isEqualTo(Money.of("0.01"));
        assertThat(CommissionCalculator.lineCommission(Money.of("2000.00"), Percent.of("5.00")))
            .isEqualTo(Money.of("100.00"));
    }
}
