package ua.marketplace.payment;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ua.marketplace.order.OrderPaymentStatusPort;

@Component
public class PaymentStatusAdapter implements OrderPaymentStatusPort {

    private final PaymentRepository repo;

    public PaymentStatusAdapter(PaymentRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCaptured(Long orderId) {
        return repo.existsByOrderIdAndStatus(orderId, PaymentStatus.CAPTURED);
    }
}
