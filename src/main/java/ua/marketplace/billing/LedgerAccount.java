package ua.marketplace.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ua.marketplace.common.money.Money;
import ua.marketplace.common.money.MoneyConverter;

@Entity
@Table(name = "ledger_account")
public class LedgerAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String code;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private AccountType accountType;

    private Long sellerId;

    @Convert(converter = MoneyConverter.class)
    @Column(precision = 19, scale = 2)
    private Money balance;

    @Version
    private Long version;

    protected LedgerAccount() {
    }

    public LedgerAccount(String code, AccountType accountType, Long sellerId) {
        this.code = code;
        this.accountType = accountType;
        this.sellerId = sellerId;
        this.balance = Money.ZERO;
    }

    /** Зі знаком: для ASSET дебет додає, для LIABILITY і REVENUE кредит додає. */
    public Money delta(EntryDirection d, Money amount) {
        boolean increases = accountType == AccountType.ASSET
            ? d == EntryDirection.DEBIT
            : d == EntryDirection.CREDIT;
        return increases ? amount : amount.negate();
    }

    public void changeBalance(Money newBalance) {
        this.balance = newBalance;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public Money getBalance() {
        return balance;
    }

    public Long getVersion() {
        return version;
    }
}
