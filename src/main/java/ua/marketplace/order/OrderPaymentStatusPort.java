package ua.marketplace.order;

/** Порт, який реалізує модуль payment. Модуль order не імпортує нічого з payment. */
public interface OrderPaymentStatusPort {

    boolean isCaptured(Long orderId);
}
