package ua.marketplace.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ledger_transaction")
public class LedgerTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private TransactionType txType;

    @Column(length = 150)
    private String idempotencyKey;

    @Column(length = 40)
    private String referenceType;

    @Column(length = 100)
    private String referenceId;

    @Column(length = 500)
    private String description;

    private Instant createdAt;

    protected LedgerTransaction() {
    }

    public LedgerTransaction(TransactionType txType, String idempotencyKey, String referenceType,
                             String referenceId, String description, Instant createdAt) {
        this.txType = txType;
        this.idempotencyKey = idempotencyKey;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.description = description;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public TransactionType getTxType() {
        return txType;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
