package ua.marketplace.web.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class UiFormatTest {

    private static final String N = "\u00A0";

    private final UiFormat fmt = new UiFormat(Clock.system(ZoneId.of("Europe/Kyiv")));

    @ParameterizedTest
    @CsvSource({
        "1234.56, 1N234,56",
        "0, 0,00",
        "1000000, 1N000N000,00",
        "-12.5, -12,50",
        "999.995, 1N000,00",
        "0.005, 0,01"})
    void moneyUsesNbspGroupsCommaAndSuffix(String input, String expectedNumber) {
        String expected = expectedNumber.replace("N", N) + N + "грн";
        assertThat(fmt.money(new BigDecimal(input))).isEqualTo(expected);
    }

    @Test
    void percentAndNull() {
        assertThat(fmt.percent(new BigDecimal("5"))).isEqualTo("5,00" + N + "%");
        assertThat(fmt.percent(new BigDecimal("7.5"))).isEqualTo("7,50" + N + "%");
        assertThat(fmt.money(null)).isEmpty();
        assertThat(fmt.percent(null)).isEmpty();
    }

    @Test
    void plainHasNoGroupsAndUsesComma() {
        assertThat(fmt.plain(new BigDecimal("1234.5"))).isEqualTo("1234,50");
        assertThat(fmt.plain(null)).isEmpty();
    }

    @Test
    void dateAndDateTimeUseKyivZone() {
        assertThat(fmt.dateTime(Instant.parse("2026-12-04T10:15:30Z"))).isEqualTo("04.12.2026 12:15");
        assertThat(fmt.dateTime(Instant.parse("2026-07-04T10:15:30Z"))).isEqualTo("04.07.2026 13:15");
        assertThat(fmt.date(LocalDate.of(2026, 12, 4))).isEqualTo("04.12.2026");
        assertThat(fmt.dateTime(null)).isEmpty();
    }

    @Test
    void phoneIsMasked() {
        assertThat(fmt.maskPhone("+380501112233")).isEqualTo("+38050***33");
        assertThat(fmt.maskPhone("+380 50 111 22 33")).isEqualTo("+38050***33");
        assertThat(fmt.maskPhone("12345")).isEqualTo("***");
        assertThat(fmt.maskPhone(null)).isEmpty();
    }

    @Test
    void accountCodesAreLabelled() {
        assertThat(fmt.accountLabel("SELLER_BALANCE:7")).isEqualTo("Баланс");
        assertThat(fmt.accountLabel("SELLER_PAYABLE:7")).isEqualTo("До виплати");
        assertThat(fmt.accountLabel("REVENUE_ADS")).isEqualTo("REVENUE_ADS");
    }
}
