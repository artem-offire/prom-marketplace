package ua.marketplace.billing;

import ua.marketplace.common.money.Money;

public record PostingLine(String accountCode, EntryDirection direction, Money amount) {

    public static PostingLine debit(String code, Money amount) {
        return new PostingLine(code, EntryDirection.DEBIT, amount);
    }

    public static PostingLine credit(String code, Money amount) {
        return new PostingLine(code, EntryDirection.CREDIT, amount);
    }
}
