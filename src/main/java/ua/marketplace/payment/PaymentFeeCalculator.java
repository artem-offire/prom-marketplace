package ua.marketplace.payment;

import ua.marketplace.common.money.Money;
import ua.marketplace.common.money.Percent;

/** Чисті функції розрахунку комісії за платіж. */
public final class PaymentFeeCalculator {

    private PaymentFeeCalculator() {
    }

    public static Money fee(Money amount, Percent rate) {
        return amount.percent(rate);
    }

    public static Money payable(Money amount, Money fee) {
        return amount.minus(fee);
    }
}
