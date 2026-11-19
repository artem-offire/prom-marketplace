package ua.marketplace.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Відсоток у діапазоні 0..100 з двома знаками після коми. */
public final class Percent {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final BigDecimal value;

    private Percent(BigDecimal value) {
        this.value = value;
    }

    public static Percent of(BigDecimal v) {
        BigDecimal scaled = Objects.requireNonNull(v, "value").setScale(2, RoundingMode.HALF_UP);
        if (scaled.signum() < 0 || scaled.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException("Percent must be between 0 and 100");
        }
        return new Percent(scaled);
    }

    public static Percent of(String v) {
        return of(new BigDecimal(v));
    }

    public static final Percent ZERO = Percent.of(BigDecimal.ZERO);

    public BigDecimal value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Percent p && value.equals(p.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value.toPlainString();
    }
}
