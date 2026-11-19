package ua.marketplace.order;

import ua.marketplace.common.error.BusinessRuleException;
import ua.marketplace.common.error.ErrorCode;

/** Чиста таблиця переходів замовлення. Усе, чого немає в таблиці, є помилкою. */
public final class OrderStateMachine {

    private OrderStateMachine() {
    }

    public static OrderStatus next(OrderStatus current, OrderAction action) {
        OrderStatus result = switch (current) {
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
        if (result == null) {
            throw new BusinessRuleException(ErrorCode.INVALID_ORDER_TRANSITION,
                "Cannot " + action + " order in status " + current);
        }
        return result;
    }

    public static boolean commissionRefundRequired(OrderStatus current, OrderAction action) {
        return (action == OrderAction.CANCEL && (current == OrderStatus.CONFIRMED || current == OrderStatus.SHIPPED))
            || (action == OrderAction.RETURN && current == OrderStatus.COMPLETED);
    }
}
