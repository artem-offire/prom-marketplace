package ua.marketplace.billing;

import java.util.UUID;

public record PostedTransaction(UUID transactionId, boolean replayed) {
}
