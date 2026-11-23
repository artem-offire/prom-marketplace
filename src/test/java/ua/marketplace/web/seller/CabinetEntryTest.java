package ua.marketplace.web.seller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import ua.marketplace.support.Html;
import ua.marketplace.support.IntegrationTest;
import ua.marketplace.support.TestFixtures;

@IntegrationTest
@AutoConfigureMockMvc
class CabinetEntryTest {

    private final MockMvc mvc;
    private final TestFixtures fx;

    CabinetEntryTest(MockMvc mvc, TestFixtures fx) {
        this.mvc = mvc;
        this.fx = fx;
    }

    @Test
    void entryPageShowsBannerAndForms() throws Exception {
        String html = Html.body(mvc.perform(get("/cabinet")).andExpect(status().isOk()).andReturn());

        assertThat(html).contains("Тимчасовий вхід без пароля").contains("action=\"/cabinet/register\"")
            .contains("action=\"/cabinet/open\"");
        Html.assertClean(html);
    }

    @Test
    void registerCreatesSellerAndRedirectsToDashboard() throws Exception {
        mvc.perform(post("/cabinet/register").param("name", "Магазин").param("phone", "")
                .param("email", "ui-" + UUID.randomUUID() + "@test.local"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/cabinet/*"))
            .andExpect(flash().attributeExists("flashSuccess"));
    }

    @Test
    void registerValidatesFields() throws Exception {
        String html = Html.body(mvc.perform(post("/cabinet/register").param("name", "").param("email", "not-an-email"))
            .andExpect(status().isOk()).andReturn());

        assertThat(html).contains("Вкажіть назву магазину або ім\u2019я");
    }

    @Test
    void duplicateEmailShowsFieldError() throws Exception {
        String email = "dup-" + UUID.randomUUID() + "@test.local";
        mvc.perform(post("/cabinet/register").param("name", "Один").param("email", email))
            .andExpect(status().is3xxRedirection());

        String html = Html.body(mvc.perform(post("/cabinet/register").param("name", "Два").param("email", email))
            .andExpect(status().isOk()).andReturn());

        assertThat(html).contains("Продавця з такою поштою вже зареєстровано");
    }

    @Test
    void openByNumberRedirectsOrFlashesError() throws Exception {
        long seller = fx.registerSeller();

        mvc.perform(get("/cabinet/open").param("sellerId", String.valueOf(seller)))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/cabinet/" + seller));
        mvc.perform(get("/cabinet/open").param("sellerId", "abc"))
            .andExpect(redirectedUrl("/cabinet")).andExpect(flash().attributeExists("flashError"));
        mvc.perform(get("/cabinet/open").param("sellerId", "999999999"))
            .andExpect(redirectedUrl("/cabinet")).andExpect(flash().attributeExists("flashError"));
    }

    @Test
    void unknownSellerCabinetIsNotFound() throws Exception {
        mvc.perform(get("/cabinet/999999999")).andExpect(status().isNotFound());
        mvc.perform(get("/cabinet/999999999/products")).andExpect(status().isNotFound());
    }
}
