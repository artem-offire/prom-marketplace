package ua.marketplace.common.error;

/** Порушення бізнес-правила (409) або некоректне значення (400 з кодом VALIDATION_ERROR). */
public class BusinessRuleException extends RuntimeException {

    private final ErrorCode code;

    public BusinessRuleException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
