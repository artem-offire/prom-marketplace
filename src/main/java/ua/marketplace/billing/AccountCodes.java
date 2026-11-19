package ua.marketplace.billing;

public final class AccountCodes {

    public static final String PLATFORM_CLEARING = "PLATFORM_CLEARING";
    public static final String REVENUE_SUBSCRIPTION = "REVENUE_SUBSCRIPTION";
    public static final String REVENUE_ADS = "REVENUE_ADS";
    public static final String REVENUE_COMMISSION = "REVENUE_COMMISSION";
    public static final String REVENUE_PAYMENT_FEE = "REVENUE_PAYMENT_FEE";
    public static final String SELLER_BALANCE_PREFIX = "SELLER_BALANCE:";
    public static final String SELLER_PAYABLE_PREFIX = "SELLER_PAYABLE:";

    private AccountCodes() {
    }

    public static String sellerBalance(Long sellerId) {
        return SELLER_BALANCE_PREFIX + sellerId;
    }

    public static String sellerPayable(Long sellerId) {
        return SELLER_PAYABLE_PREFIX + sellerId;
    }
}
