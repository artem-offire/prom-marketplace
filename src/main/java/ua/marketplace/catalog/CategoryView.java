package ua.marketplace.catalog;

import ua.marketplace.common.money.Money;
import ua.marketplace.common.money.Percent;

public record CategoryView(Long id, String name, Percent commissionRate, Money minBid) {
}
