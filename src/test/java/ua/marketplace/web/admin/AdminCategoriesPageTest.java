package ua.marketplace.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import ua.marketplace.catalog.CategoryService;
import ua.marketplace.support.Html;
import ua.marketplace.support.IntegrationTest;
import ua.marketplace.support.TestFixtures;

@IntegrationTest
@AutoConfigureMockMvc
class AdminCategoriesPageTest {

    private final MockMvc mvc;
    private final TestFixtures fx;
    private final CategoryService categoryService;

    AdminCategoriesPageTest(MockMvc mvc, TestFixtures fx, CategoryService categoryService) {
        this.mvc = mvc;
        this.fx = fx;
        this.categoryService = categoryService;
    }

    @Test
    void listShowsSeededCategories() throws Exception {
        String html = Html.body(mvc.perform(get("/admin/categories")).andExpect(status().isOk()).andReturn());

        assertThat(html).contains("Електроніка").contains("Тарифи");
        Html.assertClean(html);
    }

    @Test
    void createCategoryWithPricing() throws Exception {
        String name = "Нова-" + UUID.randomUUID();

        mvc.perform(post("/admin/categories").param("name", name).param("parentId", "")
                .param("commissionRate", "7,5").param("minBid", "2,00"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/categories"))
            .andExpect(flash().attribute("flashSuccess", "Категорію додано"));

        long id = fx.categoryId(name);
        assertThat(categoryService.list().stream().filter(c -> c.id() == id).findFirst().orElseThrow().commissionRate())
            .isEqualByComparingTo(new BigDecimal("7.50"));
    }

    @Test
    void invalidCategoryRerendersWithMessages() throws Exception {
        String html = Html.body(mvc.perform(post("/admin/categories").param("name", "").param("commissionRate", "150")
            .param("minBid", "0")).andExpect(status().isOk()).andReturn());

        assertThat(html).contains("Вкажіть назву категорії").contains("Комісія має бути від 0 до 100");
    }

    @Test
    void pricingUpdateAndValidationAbovePricingTable() throws Exception {
        long id = fx.freshCategory();

        mvc.perform(post("/admin/categories/" + id + "/pricing").param("commissionRate", "9,00").param("minBid", "3,00"))
            .andExpect(redirectedUrl("/admin/categories"))
            .andExpect(flash().attribute("flashSuccess", "Тарифи категорії оновлено"));

        String html = Html.body(mvc.perform(post("/admin/categories/" + id + "/pricing")
            .param("commissionRate", "101").param("minBid", "3,00")).andExpect(status().isOk()).andReturn());
        assertThat(html).contains("Комісія має бути від 0 до 100");
    }

    @Test
    void pricingOfUnknownCategoryIsNotFound() throws Exception {
        mvc.perform(post("/admin/categories/999999999/pricing").param("commissionRate", "5").param("minBid", "1"))
            .andExpect(status().isNotFound());
    }
}
