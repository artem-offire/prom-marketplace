package ua.marketplace.order;

public record OrderCancelledEvent(Long orderId, Long sellerId, OrderStatus previousStatus, OrderAction action) {
}
