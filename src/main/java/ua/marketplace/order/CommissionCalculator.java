package ua.marketplace.order;

import java.util.List;
import ua.marketplace.common.money.Money;
import ua.marketplace.common.money.Percent;

/** Чисті функції розрахунку комісії. Округлення виконується по кожному рядку. */
public final class CommissionCalculator {

    private CommissionCalculator() {
    }

    public static Money lineCommission(Money lineTotal, Percent rate) {
        return lineTotal.percent(rate);
    }

    public static Money sum(List<Money> parts) {
        Money total = Money.ZERO;
        for (Money m : parts) {
            total = total.plus(m);
        }
        return total;
    }
}
