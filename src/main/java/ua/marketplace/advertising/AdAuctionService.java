package ua.marketplace.advertising;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.marketplace.advertising.AuctionRanker.Candidate;
import ua.marketplace.advertising.AuctionRanker.Ranked;
import ua.marketplace.advertising.dto.PlacementDto;
import ua.marketplace.billing.BalanceService;
import ua.marketplace.catalog.ProductService;
import ua.marketplace.catalog.ProductView;
import ua.marketplace.common.money.Money;

@Service
public class AdAuctionService {

    private final AdBidRepository bidRepo;
    private final AdCampaignRepository campaignRepo;
    private final AdImpressionRepository impressionRepo;
    private final ProductService productService;
    private final BalanceService balanceService;
    private final AdsProperties props;
    private final Clock clock;

    public AdAuctionService(AdBidRepository bidRepo, AdCampaignRepository campaignRepo,
                            AdImpressionRepository impressionRepo, ProductService productService,
                            BalanceService balanceService, AdsProperties props, Clock clock) {
        this.bidRepo = bidRepo;
        this.campaignRepo = campaignRepo;
        this.impressionRepo = impressionRepo;
        this.productService = productService;
        this.balanceService = balanceService;
        this.props = props;
        this.clock = clock;
    }

    @Transactional
    public List<PlacementDto> placements(Long categoryId, Integer slots, String sessionId) {
        int n = slots == null ? props.defaultSlots() : slots;
        n = Math.max(1, Math.min(n, props.maxSlots()));

        LocalDate today = LocalDate.now(clock);
        Instant now = Instant.now(clock);

        List<AdBid> bids = bidRepo.findAllInActiveCampaigns();
        if (bids.isEmpty()) {
            return List.of();
        }

        Set<Long> campaignIds = bids.stream().map(AdBid::getCampaignId).collect(Collectors.toSet());
        Map<Long, AdCampaign> campaigns = new HashMap<>();
        for (AdCampaign c : campaignRepo.findByIdIn(campaignIds)) {
            campaigns.put(c.getId(), c);
        }

        Set<Long> productIds = bids.stream().map(AdBid::getProductId).collect(Collectors.toSet());
        Map<Long, ProductView> products = new HashMap<>();
        for (ProductView v : productService.findViews(productIds)) {
            products.put(v.id(), v);
        }

        Map<Long, Money> balances = new HashMap<>();
        List<Candidate> candidates = new ArrayList<>();
        for (AdBid b : bids) {
            AdCampaign campaign = campaigns.get(b.getCampaignId());
            ProductView pv = products.get(b.getProductId());
            if (campaign == null || pv == null) {
                continue;
            }
            if (!pv.categoryId().equals(categoryId)) {
                continue;
            }
            if (!productService.isMarketplaceVisible(pv)) {
                continue;
            }
            if (campaign.remaining(today).compareTo(b.getBidAmount()) < 0) {
                continue;
            }
            Long sellerId = campaign.getSellerId();
            Money balance = balances.computeIfAbsent(sellerId, balanceService::balanceOf);
            if (balance.compareTo(b.getBidAmount()) < 0) {
                continue;
            }
            candidates.add(new Candidate(b.getId(), b.getCampaignId(), b.getProductId(), pv.categoryId(),
                b.getBidAmount(), pv.qualityScore(), b.getCreatedAt()));
        }

        List<Ranked> ranked = AuctionRanker.rank(candidates, n);
        List<PlacementDto> result = new ArrayList<>();
        for (Ranked r : ranked) {
            Candidate c = r.candidate();
            ProductView pv = products.get(c.productId());
            AdImpression impression = impressionRepo.save(new AdImpression(
                c.bidId(), c.campaignId(), c.productId(), c.categoryId(), r.position(), r.adRank(),
                c.bidAmount(), sessionId, now));
            result.add(new PlacementDto(impression.getId(), r.position(), c.productId(), pv.sellerId(),
                pv.title(), pv.price().amount()));
        }
        return result;
    }
}
