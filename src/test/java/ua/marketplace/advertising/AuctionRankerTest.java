package ua.marketplace.advertising;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import ua.marketplace.advertising.AuctionRanker.Candidate;
import ua.marketplace.advertising.AuctionRanker.Ranked;
import ua.marketplace.common.money.Money;

class AuctionRankerTest {

    private static final Instant T10 = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant T09 = Instant.parse("2026-01-01T09:00:00Z");

    private static Candidate c(long bidId, long productId, String bid, String qs, Instant created) {
        return new Candidate(bidId, 1L, productId, 1L, Money.of(bid), new BigDecimal(qs), created);
    }

    @Test
    void ranksByAdRankAndAssignsPositions() {
        Candidate a = c(1, 1, "2.00", "0.50", T10);
        Candidate b = c(2, 2, "1.50", "0.90", T10);
        Candidate cc = c(3, 3, "3.00", "0.30", T10);

        List<Ranked> ranked = AuctionRanker.rank(List.of(a, cc, b), 3);

        assertThat(ranked).extracting(r -> r.candidate().bidId()).containsExactly(2L, 1L, 3L);
        assertThat(ranked).extracting(Ranked::position).containsExactly(1, 2, 3);
        assertThat(ranked.get(0).adRank()).isEqualByComparingTo("1.3500");
    }

    @Test
    void respectsSlots() {
        Candidate a = c(1, 1, "2.00", "0.50", T10);
        Candidate b = c(2, 2, "1.50", "0.90", T10);
        Candidate cc = c(3, 3, "3.00", "0.30", T10);

        List<Ranked> ranked = AuctionRanker.rank(List.of(a, b, cc), 2);

        assertThat(ranked).extracting(r -> r.candidate().bidId()).containsExactly(2L, 1L);
    }

    @Test
    void tieGoesToOlderBidThenLowerBidId() {
        Candidate x = c(10, 10, "2.00", "0.50", T10);
        Candidate y = c(11, 11, "1.00", "1.00", T09);
        assertThat(AuctionRanker.rank(List.of(x, y), 2))
            .extracting(r -> r.candidate().bidId()).containsExactly(11L, 10L);

        Candidate p = c(5, 5, "2.00", "0.50", T10);
        Candidate q = c(4, 6, "1.00", "1.00", T10);
        assertThat(AuctionRanker.rank(List.of(p, q), 2))
            .extracting(r -> r.candidate().bidId()).containsExactly(4L, 5L);
    }

    @Test
    void deduplicatesByProduct() {
        Candidate first = c(20, 7, "1.50", "0.90", T10);
        Candidate second = c(21, 7, "1.00", "1.00", T10);

        List<Ranked> ranked = AuctionRanker.rank(List.of(second, first), 5);

        assertThat(ranked).hasSize(1);
        assertThat(ranked.get(0).candidate().bidId()).isEqualTo(20L);
    }

    @Test
    void emptyInputAndNonPositiveSlotsGiveEmpty() {
        assertThat(AuctionRanker.rank(List.of(), 3)).isEmpty();
        assertThat(AuctionRanker.rank(List.of(c(1, 1, "2.00", "0.50", T10)), 0)).isEmpty();
    }

    @Test
    void slotsAboveCandidateCountReturnsAll() {
        List<Ranked> ranked = AuctionRanker.rank(List.of(c(1, 1, "2.00", "0.50", T10),
            c(2, 2, "1.50", "0.90", T10)), 10);
        assertThat(ranked).hasSize(2);
    }

    @Test
    void adRankRoundsToFourDecimals() {
        assertThat(AuctionRanker.adRank(Money.of("1.33"), new BigDecimal("0.33")))
            .isEqualByComparingTo("0.4389");
    }
}
