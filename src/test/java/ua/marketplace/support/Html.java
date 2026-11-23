package ua.marketplace.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.springframework.test.web.servlet.MvcResult;

/** Допоміжні методи для перевірок HTML-відповідей MockMvc. */
public final class Html {

    public static final String NBSP = "\u00A0";

    private Html() {
    }

    /** Тіло відповіді, прочитане як UTF-8 незалежно від заголовків. */
    public static String body(MvcResult result) {
        return new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }

    /** Те, що Thymeleaf робить з тексту в th:text: екранує п'ять символів. */
    public static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
    }

    /** "1 234,56" -> "1 234,56 грн" з NBSP у групах і перед "грн". */
    public static String uah(String amountWithSpaces) {
        return amountWithSpaces.replace(" ", NBSP) + NBSP + "грн";
    }

    /** "5,00" -> "5,00 %" з NBSP перед знаком відсотка. */
    public static String percent(String value) {
        return value + NBSP + "%";
    }

    /** Загальна перевірка якості сторінки: немає невирішених ключів повідомлень і сирих виразів. */
    public static void assertClean(String html) {
        assertThat(html).doesNotContain("??").doesNotContain("${").doesNotContain(" th:");
    }

    /** Ідентифікатор в кінці адреси редиректу: "/orders/42" -> 42. */
    public static long lastId(String redirectedUrl) {
        return Long.parseLong(redirectedUrl.substring(redirectedUrl.lastIndexOf('/') + 1));
    }
}
