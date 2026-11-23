package ua.marketplace.web.shop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import ua.marketplace.subscription.PlanCode;
import ua.marketplace.support.Html;
import ua.marketplace.support.IntegrationTest;
import ua.marketplace.support.TestFixtures;

@IntegrationTest
@AutoConfigureMockMvc
class CatalogPageTest {

    private final MockMvc mvc;
    private final TestFixtures fx;

    CatalogPageTest(MockMvc mvc, TestFixtures fx) {
        this.mvc = mvc;
        this.fx = fx;
    }

    @Test
    void categoryAndSearchFilterProducts() throws Exception {
        long seller = fx.sellerWithPlan(PlanCode.START);
        String title = "Чайник-" + UUID.randomUUID();
        long product = fx.createProduct(seller, fx.categoryId("Електроніка"), title, "100.00");

        String hit = Html.body(mvc.perform(get("/catalog")
            .param("category", String.valueOf(fx.categoryId("Електроніка"))).param("q", title))
            .andExpect(status().isOk()).andReturn());
        assertThat(hit).contains(title).contains("href=\"/products/" + product + "\"");

        String miss = Html.body(mvc.perform(get("/catalog")
            .param("category", String.valueOf(fx.categoryId("Авто"))).param("q", title))
            .andExpect(status().isOk()).andReturn());
        assertThat(miss).contains("Нічого не знайдено за запитом «" + title + "»")
            .doesNotContain("href=\"/products/" + product + "\"");
        Html.assertClean(miss);
    }

    @Test
    void invalidPageAndUnknownCategoryAreErrors() throws Exception {
        String bad = Html.body(mvc.perform(get("/catalog").param("page", "abc"))
            .andExpect(status().isBadRequest()).andReturn());
        assertThat(bad).contains("Некоректний запит");

        String unknown = Html.body(mvc.perform(get("/catalog").param("category", "999999999"))
            .andExpect(status().isNotFound()).andReturn());
        assertThat(unknown).contains("Сторінку не знайдено");
    }

    @Test
    void productPageShowsFormattedPriceAndBuyForm() throws Exception {
        long seller = fx.sellerWithPlan(PlanCode.START);
        String title = "Картка-" + UUID.randomUUID();
        long product = fx.createProduct(seller, fx.categoryId("Одяг і взуття"), title, "1234.56");

        String html = Html.body(mvc.perform(get("/products/" + product)).andExpect(status().isOk()).andReturn());

        assertThat(html).contains(title).contains(Html.uah("1 234,56")).contains("В наявності")
            .contains("action=\"/checkout\"").contains("Одяг і взуття");
        Html.assertClean(html);
    }

    @Test
    void productOfFreeSellerIsNotFound() throws Exception {
        long seller = fx.sellerWithPlan(PlanCode.FREE);
        long product = fx.createProduct(seller, fx.categoryId("Авто"), "Прихований-" + UUID.randomUUID(), "10.00");

        String html = Html.body(mvc.perform(get("/products/" + product))
            .andExpect(status().isNotFound()).andReturn());

        assertThat(html).contains("Сторінку не знайдено");
    }

    @Test
    void pagerAppearsAfterTwentyProducts() throws Exception {
        long seller = fx.sellerWithPlan(PlanCode.START);
        long cat = fx.categoryId("Дім і сад");
        String prefix = "Пейджер-" + UUID.randomUUID();
        for (int i = 0; i < 21; i++) {
            fx.createProduct(seller, cat, prefix + "-" + i, "10.00");
        }

        String first = Html.body(mvc.perform(get("/catalog").param("q", prefix))
            .andExpect(status().isOk()).andReturn());
        assertThat(first).contains("Сторінка").contains("page=1\" rel=\"next\"").doesNotContain("rel=\"prev\"");

        String second = Html.body(mvc.perform(get("/catalog").param("q", prefix).param("page", "1"))
            .andExpect(status().isOk()).andReturn());
        assertThat(second).contains("rel=\"prev\"").doesNotContain("rel=\"next\"");
    }
}
