package ua.marketplace.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Сума в UAH з фіксованою точністю два знаки після коми. */
public final class Money implements Comparable<Money> {

    private final BigDecimal amount;

    private Money(BigDecimal a) {
        this.amount = a.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal v) {
        return new Money(Objects.requireNonNull(v, "amount"));
    }

    public static Money of(String v) {
        return of(new BigDecimal(v));
    }

    public static Money of(long v) {
        return new Money(BigDecimal.valueOf(v));
    }

    public static final Money ZERO = Money.of(BigDecimal.ZERO);

    public static Money max(Money a, Money b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    public static Money min(Money a, Money b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    public BigDecimal amount() {
        return amount;
    }

    public Money plus(Money o) {
        return new Money(amount.add(o.amount));
    }

    public Money minus(Money o) {
        return new Money(amount.subtract(o.amount));
    }

    public Money negate() {
        return new Money(amount.negate());
    }

    public Money times(int qty) {
        return new Money(amount.multiply(BigDecimal.valueOf(qty)));
    }

    public Money percent(Percent p) {
        return new Money(amount.multiply(p.value()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isGreaterThanOrEqualTo(Money o) {
        return compareTo(o) >= 0;
    }

    @Override
    public int compareTo(Money o) {
        return amount.compareTo(o.amount);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Money m && amount.equals(m.amount);
    }

    @Override
    public int hashCode() {
        return amount.hashCode();
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }
}
