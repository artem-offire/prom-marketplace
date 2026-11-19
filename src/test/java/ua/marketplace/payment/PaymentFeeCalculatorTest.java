package ua.marketplace.payment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ua.marketplace.common.money.Money;
import ua.marketplace.common.money.Percent;

class PaymentFeeCalculatorTest {

    private static final Percent RATE = Percent.of("3.50");

    @ParameterizedTest
    @CsvSource({"1234.56,43.21,1191.35", "1000.00,35.00,965.00", "0.01,0.00,0.01", "0.15,0.01,0.14",
        "99.99,3.50,96.49"})
    void feeAndPayableRoundHalfUp(String amount, String fee, String payable) {
        Money f = PaymentFeeCalculator.fee(Money.of(amount), RATE);
        assertThat(f).isEqualTo(Money.of(fee));
        assertThat(PaymentFeeCalculator.payable(Money.of(amount), f)).isEqualTo(Money.of(payable));
    }
}
