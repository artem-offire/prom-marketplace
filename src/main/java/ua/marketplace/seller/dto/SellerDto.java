package ua.marketplace.seller.dto;

import java.time.Instant;
import ua.marketplace.seller.SellerStatus;

public record SellerDto(Long id, String name, String email, String phone, SellerStatus status, Instant createdAt) {
}
