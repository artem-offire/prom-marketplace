package ua.marketplace.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import ua.marketplace.common.error.BusinessRuleException;
import ua.marketplace.common.error.ErrorCode;

class OrderStateMachineTest {

    private static final Map<String, OrderStatus> ALLOWED = Map.of(
        "NEW/CONFIRM", OrderStatus.CONFIRMED,
        "NEW/CANCEL", OrderStatus.CANCELLED,
        "CONFIRMED/SHIP", OrderStatus.SHIPPED,
        "CONFIRMED/CANCEL", OrderStatus.CANCELLED,
        "SHIPPED/COMPLETE", OrderStatus.COMPLETED,
        "SHIPPED/CANCEL", OrderStatus.CANCELLED,
        "COMPLETED/RETURN", OrderStatus.RETURNED);

    static Stream<Arguments> allPairs() {
        return Arrays.stream(OrderStatus.values())
            .flatMap(s -> Arrays.stream(OrderAction.values()).map(a -> Arguments.of(s, a)));
    }

    @ParameterizedTest
    @MethodSource("allPairs")
    void onlyTableTransitionsAreAllowed(OrderStatus status, OrderAction action) {
        String key = status + "/" + action;
        if (ALLOWED.containsKey(key)) {
            assertThat(OrderStateMachine.next(status, action)).isEqualTo(ALLOWED.get(key));
        } else {
            assertThatThrownBy(() -> OrderStateMachine.next(status, action))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                    e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_ORDER_TRANSITION));
        }
    }

    @Test
    void exactlySevenTransitionsExist() {
        long allowed = Arrays.stream(OrderStatus.values())
            .flatMap(s -> Arrays.stream(OrderAction.values()).map(a -> s + "/" + a))
            .filter(ALLOWED::containsKey)
            .count();
        assertThat(allowed).isEqualTo(7);
    }

    @ParameterizedTest
    @CsvSource({"CONFIRMED,CANCEL,true", "SHIPPED,CANCEL,true", "COMPLETED,RETURN,true",
        "NEW,CANCEL,false", "CONFIRMED,SHIP,false", "SHIPPED,COMPLETE,false", "NEW,CONFIRM,false"})
    void commissionRefundRequiredOnlyForReversals(OrderStatus status, OrderAction action, boolean expected) {
        assertThat(OrderStateMachine.commissionRefundRequired(status, action)).isEqualTo(expected);
    }
}
