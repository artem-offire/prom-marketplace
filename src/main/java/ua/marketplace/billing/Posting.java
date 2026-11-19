package ua.marketplace.billing;

import java.util.List;

public record Posting(TransactionType type, String idempotencyKey, String referenceType, String referenceId,
                      String description, boolean allowNegative, List<PostingLine> lines) {
}
