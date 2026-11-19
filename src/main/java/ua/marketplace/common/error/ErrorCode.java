package ua.marketplace.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST),
    SELLER_NOT_FOUND(HttpStatus.NOT_FOUND),
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND),
    PLAN_NOT_FOUND(HttpStatus.NOT_FOUND),
    SUBSCRIPTION_NOT_FOUND(HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND),
    CAMPAIGN_NOT_FOUND(HttpStatus.NOT_FOUND),
    IMPRESSION_NOT_FOUND(HttpStatus.NOT_FOUND),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT),
    INSUFFICIENT_FUNDS(HttpStatus.CONFLICT),
    IDEMPOTENCY_CONFLICT(HttpStatus.CONFLICT),
    INVALID_PLAN_CHANGE(HttpStatus.CONFLICT),
    PRODUCT_LIMIT_EXCEEDED(HttpStatus.CONFLICT),
    FEATURE_NOT_IN_PLAN(HttpStatus.CONFLICT),
    DAILY_BUDGET_TOO_LOW(HttpStatus.CONFLICT),
    BID_BELOW_MINIMUM(HttpStatus.CONFLICT),
    PRODUCT_NOT_OWNED(HttpStatus.CONFLICT),
    PRODUCT_NOT_AVAILABLE(HttpStatus.CONFLICT),
    MIXED_SELLERS(HttpStatus.CONFLICT),
    INVALID_ORDER_TRANSITION(HttpStatus.CONFLICT),
    PAYMENT_REQUIRED(HttpStatus.CONFLICT),
    PAYMENT_ALREADY_EXISTS(HttpStatus.CONFLICT),
    INVALID_PAYMENT_STATE(HttpStatus.CONFLICT),
    ORDER_NOT_PAYABLE(HttpStatus.CONFLICT),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT),
    DATA_CONFLICT(HttpStatus.CONFLICT),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
