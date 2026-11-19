package ua.marketplace.payment;

import java.util.UUID;
import org.springframework.stereotype.Component;
import ua.marketplace.common.money.Money;

@Component
public class StubPaymentGateway implements PaymentGateway {

    @Override
    public String createPayment(Long paymentId, Money amount, String description) {
        return "stub-" + UUID.randomUUID();
    }
}
