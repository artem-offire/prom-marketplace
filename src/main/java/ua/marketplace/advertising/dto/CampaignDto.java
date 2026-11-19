package ua.marketplace.advertising.dto;

import java.math.BigDecimal;
import ua.marketplace.advertising.CampaignStatus;

public record CampaignDto(Long id, Long sellerId, String name, BigDecimal dailyBudget, BigDecimal spentToday,
                          BigDecimal remainingBudget, CampaignStatus status) {
}
