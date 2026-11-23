package ua.marketplace.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class OrderStateMachineAllowedActionsTest {

    @Test
    void expectedActionsPerStatus() {
        assertThat(OrderStateMachine.allowedActions(OrderStatus.NEW))
            .containsExactlyInAnyOrder(OrderAction.CONFIRM, OrderAction.CANCEL);
        assertThat(OrderStateMachine.allowedActions(OrderStatus.CONFIRMED))
            .containsExactlyInAnyOrder(OrderAction.SHIP, OrderAction.CANCEL);
        assertThat(OrderStateMachine.allowedActions(OrderStatus.SHIPPED))
            .containsExactlyInAnyOrder(OrderAction.COMPLETE, OrderAction.CANCEL);
        assertThat(OrderStateMachine.allowedActions(OrderStatus.COMPLETED))
            .containsExactly(OrderAction.RETURN);
        assertThat(OrderStateMachine.allowedActions(OrderStatus.CANCELLED)).isEmpty();
        assertThat(OrderStateMachine.allowedActions(OrderStatus.RETURNED)).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(OrderStatus.class)
    void allowedActionsAgreeWithNext(OrderStatus status) {
        Set<OrderAction> allowed = OrderStateMachine.allowedActions(status);
        for (OrderAction action : OrderAction.values()) {
            boolean works;
            try {
                OrderStateMachine.next(status, action);
                works = true;
            } catch (RuntimeException e) {
                works = false;
            }
            assertThat(allowed.contains(action)).as(status + "/" + action).isEqualTo(works);
        }
        assertThat(allowed).isSubsetOf(EnumSet.allOf(OrderAction.class));
    }
}
