package ua.marketplace.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ua.marketplace.support.Html;
import ua.marketplace.support.IntegrationTest;

@IntegrationTest
@AutoConfigureMockMvc
class ErrorHandlingTest {

    private final MockMvc mvc;

    ErrorHandlingTest(MockMvc mvc) {
        this.mvc = mvc;
    }

    @Test
    void apiStillReturnsProblemJson() throws Exception {
        mvc.perform(get("/api/v1/sellers/999999999"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.code").value("SELLER_NOT_FOUND"));
        mvc.perform(get("/api/v1/sellers/abc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void unknownProductRendersHtmlNotFoundPage() throws Exception {
        MvcResult r = mvc.perform(get("/products/999999999"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith("text/html"))
            .andReturn();
        String html = Html.body(r);
        assertThat(html).contains("Сторінку не знайдено").contains("Товар не знайдено");
        Html.assertClean(html);
    }

    @Test
    void badParameterRendersHtmlBadRequestPage() throws Exception {
        MvcResult r = mvc.perform(get("/products/abc")).andExpect(status().isBadRequest()).andReturn();
        assertThat(Html.body(r)).contains("Некоректний запит");
        MvcResult r2 = mvc.perform(get("/checkout")).andExpect(status().isBadRequest()).andReturn();
        assertThat(Html.body(r2)).contains("Некоректний запит");
    }

    @Test
    void unknownPathIsNotFound() throws Exception {
        mvc.perform(get("/no-such-page")).andExpect(status().isNotFound());
    }
}
