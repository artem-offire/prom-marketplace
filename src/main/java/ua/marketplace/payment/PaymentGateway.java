package ua.marketplace.payment;

import ua.marketplace.common.money.Money;

public interface PaymentGateway {

    /** Повертає providerRef. */
    String createPayment(Long paymentId, Money amount, String description);
}
