package ua.marketplace.web.shop;

import java.math.BigDecimal;
import java.util.UUID;
import ua.marketplace.advertising.dto.PlacementDto;

/** Рекламна картка для головної та каталогу. */
public record AdCard(UUID impressionId, Long productId, String title, BigDecimal price, String categoryName) {

    public static AdCard of(PlacementDto p, String categoryName) {
        return new AdCard(p.impressionId(), p.productId(), p.title(), p.price(), categoryName);
    }
}
