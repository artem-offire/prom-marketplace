package ua.marketplace.catalog.dto;

import java.math.BigDecimal;

public record CategoryDto(Long id, String name, Long parentId, BigDecimal commissionRate, BigDecimal minBid) {
}
