package ua.marketplace.order;

import ua.marketplace.common.money.Money;

public record OrderView(Long id, Long sellerId, Money total, PaymentMethod paymentMethod, OrderStatus status) {
}
