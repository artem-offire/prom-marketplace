package ua.marketplace.order;

import java.util.EnumSet;
import java.util.Set;
import ua.marketplace.common.error.BusinessRuleException;
import ua.marketplace.common.error.ErrorCode;

/** Чиста таблиця переходів замовлення. Усе, чого немає в таблиці, є помилкою. */
public final class OrderStateMachine {

    private OrderStateMachine() {
    }

    public static OrderStatus next(OrderStatus current, OrderAction action) {
        OrderStatus result = transition(current, action);
        if (result == null) {
            throw new BusinessRuleException(ErrorCode.INVALID_ORDER_TRANSITION,
                "Cannot " + action + " order in status " + current);
        }
        return result;
    }

    /** Дії, дозволені зі статусу: похідна від тієї самої таблиці переходів. */
    public static Set<OrderAction> allowedActions(OrderStatus current) {
        Set<OrderAction> result = EnumSet.noneOf(OrderAction.class);
        for (OrderAction action : OrderAction.values()) {
            if (transition(current, action) != null) {
                result.add(action);
            }
        }
        return result;
    }

    public static boolean commissionRefundRequired(OrderStatus current, OrderAction action) {
        return (action == OrderAction.CANCEL && (current == OrderStatus.CONFIRMED || current == OrderStatus.SHIPPED))
            || (action == OrderAction.RETURN && current == OrderStatus.COMPLETED);
    }

    private static OrderStatus transition(OrderStatus current, OrderAction action) {
        return switch (current) {
            case NEW -> switch (action) {
                case CONFIRM -> OrderStatus.CONFIRMED;
                case CANCEL -> OrderStatus.CANCELLED;
                default -> null;
            };
            case CONFIRMED -> switch (action) {
                case SHIP -> OrderStatus.SHIPPED;
                case CANCEL -> OrderStatus.CANCELLED;
                default -> null;
            };
            case SHIPPED -> switch (action) {
                case COMPLETE -> OrderStatus.COMPLETED;
                case CANCEL -> OrderStatus.CANCELLED;
                default -> null;
            };
            case COMPLETED -> action == OrderAction.RETURN ? OrderStatus.RETURNED : null;
            default -> null;
        };
    }
}
