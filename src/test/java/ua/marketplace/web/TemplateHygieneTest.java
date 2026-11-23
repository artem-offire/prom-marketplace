package ua.marketplace.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Статична перевірка шаблонів, стилів і повідомлень (запускається з кореня проєкту). */
class TemplateHygieneTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");
    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final String FORBIDDEN_WORD = "цен" + "тр";

    private List<Path> files(Path root) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile).toList();
        }
    }

    @Test
    void templatesExist() throws IOException {
        List<String> expected = List.of(
                "layout.html",
                "fragments/header.html", "fragments/footer.html", "fragments/flash.html", "fragments/pager.html",
                "fragments/temp-access-banner.html", "fragments/form-errors.html", "fragments/product-card.html",
                "fragments/cabinet-nav.html",
                "error/404.html", "error/4xx.html", "error/5xx.html",
                "shop/home.html", "shop/catalog.html", "shop/product.html", "shop/checkout.html", "shop/order.html",
                "cabinet/entry.html", "cabinet/dashboard.html", "cabinet/products.html", "cabinet/product-form.html",
                "cabinet/plan.html", "cabinet/balance.html", "cabinet/campaigns.html", "cabinet/campaign.html",
                "cabinet/orders.html", "cabinet/order.html",
                "admin/categories.html");
        List<String> actual = files(TEMPLATES).stream()
                .map(p -> TEMPLATES.relativize(p).toString().replace('\\', '/'))
                .toList();
        assertThat(actual).containsAll(expected);
    }

    @Test
    void noUnescapedOutput() throws IOException {
        for (Path file : files(TEMPLATES)) {
            assertThat(Files.readString(file)).as(file.toString()).doesNotContain("th:utext");
        }
    }

    @Test
    void everyFormUsesThAction() throws IOException {
        Pattern form = Pattern.compile("<form\\b[^>]*>", Pattern.DOTALL);
        for (Path file : files(TEMPLATES)) {
            Matcher m = form.matcher(Files.readString(file));
            while (m.find()) {
                assertThat(m.group()).as(file + ": " + m.group()).contains("th:action=");
            }
        }
    }

    @Test
    void noForbiddenTextInTemplatesStylesAndMessages() throws IOException {
        Pattern asciiApostropheInWord = Pattern.compile("[а-яіїєґА-ЯІЇЄҐ]'[а-яіїєґА-ЯІЇЄҐ]");
        for (Path file : files(RESOURCES)) {
            String name = file.toString();
            if (!(name.endsWith(".html") || name.endsWith(".css") || name.endsWith(".svg")
                    || name.endsWith(".properties") || name.endsWith(".yml"))) {
                continue;
            }
            String text = Files.readString(file);
            assertThat(text).as(name).doesNotContain("\u2014").doesNotContain("\u2013");
            assertThat(text.toLowerCase()).as(name).doesNotContain(FORBIDDEN_WORD);
            assertThat(asciiApostropheInWord.matcher(text).find()).as(name + " містить ASCII-апостроф у слові").isFalse();
        }
    }
}
