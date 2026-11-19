package ua.marketplace.billing.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import ua.marketplace.billing.EntryDirection;
import ua.marketplace.billing.TransactionType;

public record LedgerEntryDto(UUID transactionId, TransactionType type, Instant createdAt, String accountCode,
                             EntryDirection direction, BigDecimal amount, String description) {
}
