package ua.marketplace.payment;

import static ua.marketplace.billing.AccountCodes.PLATFORM_CLEARING;
import static ua.marketplace.billing.AccountCodes.REVENUE_PAYMENT_FEE;
import static ua.marketplace.billing.AccountCodes.sellerPayable;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.marketplace.billing.LedgerService;
import ua.marketplace.billing.Posting;
import ua.marketplace.billing.PostingLine;
import ua.marketplace.billing.TransactionType;
import ua.marketplace.common.error.BusinessRuleException;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.common.error.NotFoundException;
import ua.marketplace.common.money.Money;
import ua.marketplace.common.money.Percent;
import ua.marketplace.order.OrderService;
import ua.marketplace.order.OrderStatus;
import ua.marketplace.order.OrderView;
import ua.marketplace.order.PaymentMethod;
import ua.marketplace.payment.dto.PaymentDto;
import ua.marketplace.payment.dto.WebhookRequest;

@Service
public class PaymentService {

    private final PaymentRepository repo;
    private final PaymentGateway gateway;
    private final PaymentProperties props;
    private final OrderService orderService;
    private final LedgerService ledger;
    private final Clock clock;

    public PaymentService(PaymentRepository repo, PaymentGateway gateway, PaymentProperties props,
                          OrderService orderService, LedgerService ledger, Clock clock) {
        this.repo = repo;
        this.gateway = gateway;
        this.props = props;
        this.orderService = orderService;
        this.ledger = ledger;
        this.clock = clock;
    }

    @Transactional
    public PaymentDto init(Long orderId) {
        OrderView ov = orderService.getView(orderId);
        if (ov.paymentMethod() != PaymentMethod.ONLINE || ov.status() != OrderStatus.NEW) {
            throw new BusinessRuleException(ErrorCode.ORDER_NOT_PAYABLE, "Order is not payable");
        }
        Percent rate = Percent.of(props.feePercent());
        Money fee = PaymentFeeCalculator.fee(ov.total(), rate);

        Optional<Payment> existing = repo.findByOrderId(orderId);
        Payment p;
        if (existing.isPresent()) {
            if (existing.get().getStatus() != PaymentStatus.FAILED) {
                throw new BusinessRuleException(ErrorCode.PAYMENT_ALREADY_EXISTS,
                    "Payment already exists for order: " + orderId);
            }
            p = existing.get();
            p.restart(ov.total(), rate, fee);
        } else {
            p = repo.saveAndFlush(new Payment(orderId, ov.total(), rate, fee, Instant.now(clock)));
        }
        String ref = gateway.createPayment(p.getId(), p.getAmount(), "Order " + orderId);
        p.assignProviderRef(ref);
        return toDto(p);
    }

    @Transactional
    public PaymentDto handleWebhook(WebhookRequest r) {
        Payment p = repo.findByProviderRef(r.providerRef())
            .orElseThrow(() -> new NotFoundException(ErrorCode.PAYMENT_NOT_FOUND,
                "Payment not found for provider reference"));

        if (p.getStatus() != PaymentStatus.PENDING) {
            boolean sameResult = (p.getStatus() == PaymentStatus.CAPTURED && r.result() == WebhookResult.SUCCESS)
                || (p.getStatus() == PaymentStatus.FAILED && r.result() == WebhookResult.FAILURE);
            if (sameResult) {
                return toDto(p);
            }
            throw new BusinessRuleException(ErrorCode.INVALID_PAYMENT_STATE,
                "Payment is in status " + p.getStatus());
        }

        if (r.result() == WebhookResult.FAILURE) {
            p.markFailed();
            return toDto(p);
        }

        OrderView ov = orderService.getView(p.getOrderId());
        Money payable = PaymentFeeCalculator.payable(p.getAmount(), p.getFeeAmount());
        List<PostingLine> lines = new ArrayList<>();
        lines.add(PostingLine.debit(PLATFORM_CLEARING, p.getAmount()));
        lines.add(PostingLine.credit(sellerPayable(ov.sellerId()), payable));
        if (p.getFeeAmount().isPositive()) {
            lines.add(PostingLine.credit(REVENUE_PAYMENT_FEE, p.getFeeAmount()));
        }
        ledger.post(new Posting(
            TransactionType.ONLINE_PAYMENT_CAPTURE,
            "PAY_CAPTURE:" + p.getId(),
            "PAYMENT",
            String.valueOf(p.getId()),
            "Оплата замовлення",
            false,
            lines));
        p.markCaptured(Instant.now(clock));
        return toDto(p);
    }

    @Transactional
    public PaymentDto refund(Long paymentId) {
        Payment p = findPayment(paymentId);
        if (p.getStatus() != PaymentStatus.CAPTURED) {
            throw new BusinessRuleException(ErrorCode.INVALID_PAYMENT_STATE,
                "Only a captured payment can be refunded");
        }
        OrderView ov = orderService.getView(p.getOrderId());
        ledger.post(new Posting(
            TransactionType.ONLINE_PAYMENT_REFUND,
            "PAY_REFUND:" + p.getId(),
            "PAYMENT",
            String.valueOf(p.getId()),
            "Повернення оплати",
            false,
            List.of(
                PostingLine.debit(sellerPayable(ov.sellerId()), p.getAmount()),
                PostingLine.credit(PLATFORM_CLEARING, p.getAmount()))));
        p.markRefunded();
        return toDto(p);
    }

    @Transactional(readOnly = true)
    public PaymentDto get(Long id) {
        return toDto(findPayment(id));
    }

    @Transactional(readOnly = true)
    public Optional<PaymentDto> findByOrder(Long orderId) {
        return repo.findByOrderId(orderId).map(this::toDto);
    }

    private Payment findPayment(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.PAYMENT_NOT_FOUND, "Payment not found: " + id));
    }

    private PaymentDto toDto(Payment p) {
        return new PaymentDto(p.getId(), p.getOrderId(), p.getAmount().amount(), p.getFeeRate().value(),
            p.getFeeAmount().amount(), p.getStatus(), p.getProviderRef(), p.getCreatedAt(), p.getCapturedAt());
    }
}
