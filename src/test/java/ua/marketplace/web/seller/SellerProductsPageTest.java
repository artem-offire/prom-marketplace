package ua.marketplace.web.seller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ua.marketplace.catalog.ProductService;
import ua.marketplace.subscription.PlanCode;
import ua.marketplace.support.Html;
import ua.marketplace.support.IntegrationTest;
import ua.marketplace.support.TestFixtures;

@IntegrationTest
@AutoConfigureMockMvc
class SellerProductsPageTest {

    private final MockMvc mvc;
    private final TestFixtures fx;
    private final ProductService productService;

    SellerProductsPageTest(MockMvc mvc, TestFixtures fx, ProductService productService) {
        this.mvc = mvc;
        this.fx = fx;
        this.productService = productService;
    }

    private MockHttpServletRequestBuilder create(long seller, String title, String price) throws Exception {
        return post("/cabinet/" + seller + "/products/new")
            .param("categoryId", String.valueOf(fx.categoryId("Електроніка")))
            .param("title", title).param("description", "").param("price", price)
            .param("stock", "5").param("status", "ACTIVE");
    }

    @Test
    void createAcceptsUkrainianDecimalAndListsProduct() throws Exception {
        long seller = fx.sellerWithPlan(PlanCode.START);
        String title = "Новий-" + UUID.randomUUID();

        mvc.perform(create(seller, title, "1 234,50"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/cabinet/" + seller + "/products"));

        String html = Html.body(mvc.perform(get("/cabinet/" + seller + "/products"))
            .andExpect(status().isOk()).andReturn());
        assertThat(html).contains(title).contains(Html.uah("1 234,50"));
        Html.assertClean(html);
    }

    @Test
    void invalidPriceAndTitleShowFieldMessages() throws Exception {
        long seller = fx.sellerWithPlan(PlanCode.START);

        String html = Html.body(mvc.perform(create(seller, "", "abc")).andExpect(status().isOk()).andReturn());

        assertThat(html).contains("Вкажіть назву").contains("Некоректне число");

        String zero = Html.body(mvc.perform(create(seller, "Назва", "0")).andExpect(status().isOk()).andReturn());
        assertThat(zero).contains("Ціна має бути не менше 0,01");
    }

    @Test
    void editFormIsPrefilledAndSaves() throws Exception {
        long seller = fx.sellerWithPlan(PlanCode.START);
        String title = "Редагувати-" + UUID.randomUUID();
        long product = fx.createProduct(seller, fx.categoryId("Авто"), title, "10.00");

        String form = Html.body(mvc.perform(get("/cabinet/" + seller + "/products/" + product + "/edit"))
            .andExpect(status().isOk()).andReturn());
        assertThat(form).contains(title).contains("value=\"10,00\"");

        mvc.perform(post("/cabinet/" + seller + "/products/" + product + "/edit")
                .param("categoryId", String.valueOf(fx.categoryId("Авто"))).param("title", title + "!")
                .param("description", "").param("price", "20,00").param("stock", "3").param("status", "ACTIVE"))
            .andExpect(redirectedUrl("/cabinet/" + seller + "/products"));
        assertThat(productService.get(product).title()).isEqualTo(title + "!");
    }

    @Test
    void foreignProductIsNotFound() throws Exception {
        long owner = fx.sellerWithPlan(PlanCode.START);
        long other = fx.sellerWithPlan(PlanCode.START);
        long product = fx.createProduct(owner, fx.categoryId("Авто"), "Чужий-" + UUID.randomUUID(), "10.00");

        mvc.perform(get("/cabinet/" + other + "/products/" + product + "/edit")).andExpect(status().isNotFound());
    }

    @Test
    void productLimitOnFreePlanShowsConflictMessage() throws Exception {
        long seller = fx.sellerWithPlan(PlanCode.FREE);
        long category = fx.categoryId("Авто");
        for (int i = 0; i < 50; i++) {
            fx.createProduct(seller, category, "Ліміт-" + i, "1.00");
        }

        String html = Html.body(mvc.perform(create(seller, "П51", "5")).andExpect(status().isOk()).andReturn());

        assertThat(html).contains("Досягнуто ліміт товарів пакета");
    }
}
