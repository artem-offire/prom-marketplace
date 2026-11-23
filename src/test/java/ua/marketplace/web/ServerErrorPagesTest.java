package ua.marketplace.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ua.marketplace.catalog.CategoryService;
import ua.marketplace.common.error.BusinessRuleException;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.support.Html;
import ua.marketplace.support.IntegrationTest;

@IntegrationTest
@AutoConfigureMockMvc
class ServerErrorPagesTest {

    @MockitoBean
    CategoryService categoryService;

    private final MockMvc mvc;

    ServerErrorPagesTest(MockMvc mvc) {
        this.mvc = mvc;
    }

    @Test
    void unexpectedExceptionRendersGenericPageWithoutDetails() throws Exception {
        when(categoryService.list()).thenThrow(new IllegalStateException("boom-secret-detail"));

        MvcResult r = mvc.perform(get("/catalog")).andExpect(status().isInternalServerError()).andReturn();

        String html = Html.body(r);
        assertThat(html).contains("Щось пішло не так").contains("Внутрішня помилка, спробуйте пізніше");
        assertThat(html).doesNotContain("boom-secret-detail").doesNotContain("IllegalStateException");
        Html.assertClean(html);
    }

    @Test
    void businessRuleExceptionRendersConflictPageWithUkrainianMessage() throws Exception {
        when(categoryService.list()).thenThrow(new BusinessRuleException(ErrorCode.DATA_CONFLICT, "english detail"));

        MvcResult r = mvc.perform(get("/catalog")).andExpect(status().isConflict()).andReturn();

        String html = Html.body(r);
        assertThat(html).contains("Дію не виконано").contains("Конфлікт даних, повторіть дію");
        assertThat(html).doesNotContain("english detail");
    }
}
