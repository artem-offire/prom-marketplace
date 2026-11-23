package ua.marketplace.web.support;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ua.marketplace.catalog.dto.CategoryDto;

public final class CategoryNames {

    private CategoryNames() {
    }

    public static Map<Long, String> byId(List<CategoryDto> categories) {
        Map<Long, String> names = new LinkedHashMap<>();
        for (CategoryDto c : categories) {
            names.put(c.id(), c.name());
        }
        return names;
    }
}
