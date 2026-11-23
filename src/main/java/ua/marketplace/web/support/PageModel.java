package ua.marketplace.web.support;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import ua.marketplace.common.web.PageResponse;

/** Дані пейджера: сторінка нумерується з нуля, посилання зберігають інші параметри фільтра. */
public record PageModel(int page, int totalPages, long totalElements, boolean hasPrev, boolean hasNext,
                        String prevUrl, String nextUrl) {

    public static PageModel of(PageResponse<?> response, String path, Map<String, ?> params) {
        boolean hasPrev = response.page() > 0;
        boolean hasNext = response.page() + 1 < response.totalPages();
        return new PageModel(response.page(), response.totalPages(), response.totalElements(), hasPrev, hasNext,
            hasPrev ? url(path, params, response.page() - 1) : null,
            hasNext ? url(path, params, response.page() + 1) : null);
    }

    private static String url(String path, Map<String, ?> params, int page) {
        StringBuilder sb = new StringBuilder(path).append('?');
        for (Map.Entry<String, ?> e : params.entrySet()) {
            if (e.getValue() != null) {
                sb.append(e.getKey()).append('=')
                    .append(URLEncoder.encode(e.getValue().toString(), StandardCharsets.UTF_8)).append('&');
            }
        }
        return sb.append("page=").append(page).toString();
    }
}
