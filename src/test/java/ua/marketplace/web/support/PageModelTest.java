package ua.marketplace.web.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ua.marketplace.common.web.PageResponse;

class PageModelTest {

    @Test
    void middlePageHasBothLinksAndKeepsEncodedFilters() {
        PageResponse<String> page = new PageResponse<>(List.of(), 1, 20, 50, 3);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("category", 3L);
        params.put("q", "чай & кава");
        params.put("empty", null);

        PageModel model = PageModel.of(page, "/catalog", params);

        assertThat(model.hasPrev()).isTrue();
        assertThat(model.hasNext()).isTrue();
        assertThat(model.prevUrl()).isEqualTo("/catalog?category=3&q=%D1%87%D0%B0%D0%B9+%26+%D0%BA%D0%B0%D0%B2%D0%B0&page=0");
        assertThat(model.nextUrl()).endsWith("&page=2");
        assertThat(model.totalPages()).isEqualTo(3);
    }

    @Test
    void firstAndLastPagesHaveOneLink() {
        PageModel first = PageModel.of(new PageResponse<String>(List.of(), 0, 20, 50, 3), "/x", Map.of());
        PageModel last = PageModel.of(new PageResponse<String>(List.of(), 2, 20, 50, 3), "/x", Map.of());

        assertThat(first.hasPrev()).isFalse();
        assertThat(first.prevUrl()).isNull();
        assertThat(first.nextUrl()).isEqualTo("/x?page=1");
        assertThat(last.hasNext()).isFalse();
        assertThat(last.nextUrl()).isNull();
        assertThat(last.prevUrl()).isEqualTo("/x?page=1");
    }
}
