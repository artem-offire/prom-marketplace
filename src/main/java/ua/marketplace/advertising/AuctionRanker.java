package ua.marketplace.advertising;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ua.marketplace.common.money.Money;

/** Чистий розрахунок аукціону без залежностей від Spring. */
public final class AuctionRanker {

    public record Candidate(long bidId, long campaignId, long productId, long categoryId,
                            Money bidAmount, BigDecimal qualityScore, Instant bidCreatedAt) {
    }

    public record Ranked(Candidate candidate, BigDecimal adRank, int position) {
    }

    private AuctionRanker() {
    }

    public static BigDecimal adRank(Money bid, BigDecimal qualityScore) {
        return bid.amount().multiply(qualityScore).setScale(4, RoundingMode.HALF_UP);
    }

    public static List<Ranked> rank(List<Candidate> candidates, int slots) {
        if (slots <= 0 || candidates.isEmpty()) {
            return List.of();
        }
        List<Candidate> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator
            .comparing((Candidate c) -> adRank(c.bidAmount(), c.qualityScore()), Comparator.reverseOrder())
            .thenComparing(Candidate::bidCreatedAt)
            .thenComparingLong(Candidate::bidId));

        Set<Long> seenProducts = new HashSet<>();
        List<Ranked> result = new ArrayList<>();
        for (Candidate c : sorted) {
            if (result.size() >= slots) {
                break;
            }
            if (seenProducts.add(c.productId())) {
                result.add(new Ranked(c, adRank(c.bidAmount(), c.qualityScore()), result.size() + 1));
            }
        }
        return result;
    }
}
