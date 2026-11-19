package ua.marketplace.order;

import static ua.marketplace.billing.AccountCodes.REVENUE_COMMISSION;
import static ua.marketplace.billing.AccountCodes.sellerBalance;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.marketplace.billing.LedgerService;
import ua.marketplace.billing.Posting;
import ua.marketplace.billing.PostingLine;
import ua.marketplace.billing.TransactionType;
import ua.marketplace.catalog.CategoryService;
import ua.marketplace.catalog.CategoryView;
import ua.marketplace.catalog.ProductService;
import ua.marketplace.catalog.ProductView;
import ua.marketplace.common.error.BusinessRuleException;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.common.error.NotFoundException;
import ua.marketplace.common.money.Money;
import ua.marketplace.common.web.PageResponse;
import ua.marketplace.common.web.Paging;
import ua.marketplace.order.dto.CheckoutItem;
import ua.marketplace.order.dto.CheckoutRequest;
import ua.marketplace.order.dto.OrderDto;
import ua.marketplace.order.dto.OrderItemDto;
import ua.marketplace.subscription.PlanFeature;
import ua.marketplace.subscription.PlanLimits;

@Service
public class OrderService {

    private final OrderRepository orderRepo;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final PlanLimits planLimits;
    private final LedgerService ledger;
    private final OrderPaymentStatusPort paymentStatusPort;
    private final ApplicationEventPublisher publisher;
    private final Clock clock;

    public OrderService(OrderRepository orderRepo, ProductService productService, CategoryService categoryService,
                        PlanLimits planLimits, LedgerService ledger, OrderPaymentStatusPort paymentStatusPort,
                        ApplicationEventPublisher publisher, Clock clock) {
        this.orderRepo = orderRepo;
        this.productService = productService;
        this.categoryService = categoryService;
        this.planLimits = planLimits;
        this.ledger = ledger;
        this.paymentStatusPort = paymentStatusPort;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Transactional
    public OrderDto checkout(CheckoutRequest r) {
        Set<Long> seen = new HashSet<>();
        List<Long> ids = new ArrayList<>();
        for (CheckoutItem item : r.items()) {
            if (!seen.add(item.productId())) {
                throw new BusinessRuleException(ErrorCode.VALIDATION_ERROR, "Duplicate product in items");
            }
            ids.add(item.productId());
        }

        List<ProductView> views = productService.findViews(ids);
        if (views.size() != ids.size()) {
            throw new NotFoundException(ErrorCode.PRODUCT_NOT_FOUND, "Product not found in checkout");
        }
        Map<Long, ProductView> byId = new HashMap<>();
        for (ProductView v : views) {
            byId.put(v.id(), v);
        }

        Set<Long> sellers = new HashSet<>();
        for (ProductView v : views) {
            sellers.add(v.sellerId());
        }
        if (sellers.size() > 1) {
            throw new BusinessRuleException(ErrorCode.MIXED_SELLERS, "All items must belong to one seller");
        }
        Long sellerId = views.get(0).sellerId();

        for (ProductView v : views) {
            if (!productService.isMarketplaceVisible(v)) {
                throw new BusinessRuleException(ErrorCode.PRODUCT_NOT_AVAILABLE, "Product not available: " + v.id());
            }
        }

        if (r.paymentMethod() == PaymentMethod.ONLINE
                && !planLimits.hasFeature(sellerId, PlanFeature.ONLINE_PAYMENTS)) {
            throw new BusinessRuleException(ErrorCode.FEATURE_NOT_IN_PLAN, "Plan does not include ONLINE_PAYMENTS");
        }

        Order o = new Order(sellerId, r.buyerName().trim(), r.buyerPhone(), r.buyerEmail(), r.paymentMethod(),
            Instant.now(clock));
        for (CheckoutItem item : r.items()) {
            ProductView view = byId.get(item.productId());
            CategoryView cv = categoryService.getView(view.categoryId());
            o.addItem(new OrderItem(o, view.id(), view.categoryId(), view.title(), view.price(), item.quantity(),
                cv.commissionRate()));
        }
        Order saved = orderRepo.save(o);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public OrderDto get(Long id) {
        return toDto(findOrder(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderDto> listBySeller(Long sellerId, OrderStatus status, int page, int size) {
        var pageable = Paging.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        var result = status == null
            ? orderRepo.findBySellerId(sellerId, pageable)
            : orderRepo.findBySellerIdAndStatus(sellerId, status, pageable);
        return PageResponse.of(result, this::toDto);
    }

    @Transactional(readOnly = true)
    public OrderView getView(Long id) {
        Order o = findOrder(id);
        return new OrderView(o.getId(), o.getSellerId(), o.getTotal(), o.getPaymentMethod(), o.getStatus());
    }

    /** Єдина точка переходів замовлення. */
    @Transactional
    public OrderDto apply(Long orderId, OrderAction action) {
        Order o = orderRepo.findForUpdate(orderId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.ORDER_NOT_FOUND, "Order not found: " + orderId));
        OrderStatus from = o.getStatus();
        OrderStatus to = OrderStateMachine.next(from, action);

        if (action == OrderAction.CONFIRM) {
            if (o.getPaymentMethod() == PaymentMethod.ONLINE && !paymentStatusPort.isCaptured(o.getId())) {
                throw new BusinessRuleException(ErrorCode.PAYMENT_REQUIRED,
                    "Online order must be paid before confirmation");
            }
            List<Money> commissions = new ArrayList<>();
            for (OrderItem item : o.getItems()) {
                Money c = CommissionCalculator.lineCommission(item.getLineTotal(), item.getCommissionRateSnapshot());
                item.applyCommission(c);
                commissions.add(c);
            }
            Money commission = CommissionCalculator.sum(commissions);
            o.applyCommission(commission);
            if (commission.isPositive()) {
                ledger.post(new Posting(
                    TransactionType.ORDER_COMMISSION,
                    "COMMISSION:" + orderId,
                    "ORDER",
                    String.valueOf(orderId),
                    "Комісія за замовлення",
                    true,
                    List.of(
                        PostingLine.debit(sellerBalance(o.getSellerId()), commission),
                        PostingLine.credit(REVENUE_COMMISSION, commission))));
            }
        }

        if (OrderStateMachine.commissionRefundRequired(from, action) && o.getCommissionAmount().isPositive()) {
            Money c = o.getCommissionAmount();
            ledger.post(new Posting(
                TransactionType.ORDER_COMMISSION_REFUND,
                "COMMISSION_REFUND:" + orderId,
                "ORDER",
                String.valueOf(orderId),
                "Повернення комісії за замовлення",
                false,
                List.of(
                    PostingLine.debit(REVENUE_COMMISSION, c),
                    PostingLine.credit(sellerBalance(o.getSellerId()), c))));
        }

        if (action == OrderAction.COMPLETE) {
            o.markCompleted(Instant.now(clock));
        }
        o.changeStatus(to);

        if (action == OrderAction.CANCEL || action == OrderAction.RETURN) {
            publisher.publishEvent(new OrderCancelledEvent(orderId, o.getSellerId(), from, action));
        }
        return toDto(o);
    }

    private Order findOrder(Long id) {
        return orderRepo.findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.ORDER_NOT_FOUND, "Order not found: " + id));
    }

    private OrderDto toDto(Order o) {
        List<OrderItemDto> items = o.getItems().stream()
            .map(i -> new OrderItemDto(i.getId(), i.getProductId(), i.getTitleSnapshot(),
                i.getUnitPrice().amount(), i.getQuantity(), i.getLineTotal().amount(),
                i.getCommissionRateSnapshot().value(), i.getCommissionAmount().amount()))
            .toList();
        return new OrderDto(o.getId(), o.getSellerId(), o.getBuyerName(), o.getBuyerPhone(), o.getBuyerEmail(),
            o.getPaymentMethod(), o.getStatus(), o.getTotal().amount(), o.getCommissionAmount().amount(),
            o.getCreatedAt(), o.getCompletedAt(), items);
    }
}
