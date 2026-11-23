package ua.marketplace.web.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;
import ua.marketplace.billing.AccountCodes;

/** Форматування для шаблонів: ${@fmt.money(x)}. Розділювач груп і пробіл перед "грн" це NBSP. */
@Component("fmt")
public class UiFormat {

    public static final String NBSP = "\u00A0";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final ZoneId zone;

    public UiFormat(Clock clock) {
        this.zone = clock.getZone();
    }

    /** 1234.56 -> "1 234,56 грн" (NBSP), 0 -> "0,00 грн", -12.5 -> "-12,50 грн". */
    public String money(BigDecimal value) {
        return value == null ? "" : number(value) + NBSP + "грн";
    }

    /** 5 -> "5,00 %" (NBSP перед знаком). */
    public String percent(BigDecimal value) {
        return value == null ? "" : number(value) + NBSP + "%";
    }

    /** Число з групами і комою: "1 234,56". */
    public String number(BigDecimal value) {
        if (value == null) {
            return "";
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('\u00A0');
        symbols.setDecimalSeparator(',');
        DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(value);
    }

    /** Значення для поля вводу без груп: 1234.5 -> "1234,50". */
    public String plain(BigDecimal value) {
        return value == null ? "" : value.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    public String dateTime(Instant instant) {
        return instant == null ? "" : DATE_TIME.withZone(zone).format(instant);
    }

    public String date(LocalDate date) {
        return date == null ? "" : DATE.format(date);
    }

    /** "+380501112233" -> "+38050***33". */
    public String maskPhone(String phone) {
        if (phone == null) {
            return "";
        }
        String s = phone.replaceAll("\\s+", "");
        if (s.length() < 9) {
            return "***";
        }
        return s.substring(0, 6) + "***" + s.substring(s.length() - 2);
    }

    /** "ivan@example.com" -> "i***@example.com"; рядок без "@" повністю приховується. */
    public String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        String s = email.strip();
        int at = s.lastIndexOf('@');
        if (at < 1) {
            return "***";
        }
        return s.charAt(0) + "***" + s.substring(at);
    }

    /** Код рахунку в журналі -> підпис для продавця. */
    public String accountLabel(String code) {
        if (code == null) {
            return "";
        }
        if (code.startsWith(AccountCodes.SELLER_BALANCE_PREFIX)) {
            return "Баланс";
        }
        if (code.startsWith(AccountCodes.SELLER_PAYABLE_PREFIX)) {
            return "До виплати";
        }
        return code;
    }
}
