package ua.marketplace.advertising;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ads")
public record AdsProperties(BigDecimal minDailyBudget, int defaultSlots, int maxSlots) {
}
