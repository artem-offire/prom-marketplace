package ua.marketplace.advertising;

import static ua.marketplace.subscription.PlanFeature.PROSALE;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.marketplace.advertising.dto.BidDto;
import ua.marketplace.advertising.dto.BidRequest;
import ua.marketplace.advertising.dto.CampaignDto;
import ua.marketplace.advertising.dto.CreateCampaignRequest;
import ua.marketplace.advertising.dto.PatchCampaignRequest;
import ua.marketplace.catalog.CategoryService;
import ua.marketplace.catalog.CategoryView;
import ua.marketplace.catalog.ProductService;
import ua.marketplace.catalog.ProductView;
import ua.marketplace.common.error.BusinessRuleException;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.common.error.NotFoundException;
import ua.marketplace.common.money.Money;
import ua.marketplace.subscription.PlanLimits;

@Service
public class CampaignService {

    private final AdCampaignRepository campaignRepo;
    private final AdBidRepository bidRepo;
    private final PlanLimits planLimits;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final AdsProperties props;
    private final Clock clock;

    public CampaignService(AdCampaignRepository campaignRepo, AdBidRepository bidRepo, PlanLimits planLimits,
                           ProductService productService, CategoryService categoryService,
                           AdsProperties props, Clock clock) {
        this.campaignRepo = campaignRepo;
        this.bidRepo = bidRepo;
        this.planLimits = planLimits;
        this.productService = productService;
        this.categoryService = categoryService;
        this.props = props;
        this.clock = clock;
    }

    @Transactional
    public CampaignDto create(Long sellerId, CreateCampaignRequest r) {
        if (!planLimits.hasFeature(sellerId, PROSALE)) {
            throw new BusinessRuleException(ErrorCode.FEATURE_NOT_IN_PLAN, "Plan does not include PROSALE");
        }
        Money budget = Money.of(r.dailyBudget());
        checkMinBudget(budget);
        AdCampaign c = campaignRepo.save(new AdCampaign(sellerId, r.name().trim(), budget,
            LocalDate.now(clock), Instant.now(clock)));
        return toDto(c);
    }

    @Transactional
    public CampaignDto patch(Long campaignId, PatchCampaignRequest r) {
        AdCampaign c = findCampaign(campaignId);
        if (r.dailyBudget() != null) {
            Money budget = Money.of(r.dailyBudget());
            checkMinBudget(budget);
            c.changeBudget(budget);
        }
        if (r.status() != null) {
            c.changeStatus(r.status());
        }
        return toDto(c);
    }

    @Transactional
    public List<BidDto> replaceBids(Long campaignId, List<BidRequest> bids) {
        AdCampaign campaign = findCampaign(campaignId);
        List<BidRequest> requests = bids == null ? List.of() : bids;

        // Усі перевірки виконуються до будь-яких змін.
        Set<Long> seenProducts = new HashSet<>();
        Map<Long, Money> validated = new HashMap<>();
        for (BidRequest r : requests) {
            if (r.productId() == null || r.bidAmount() == null || r.bidAmount().signum() <= 0) {
                throw new BusinessRuleException(ErrorCode.VALIDATION_ERROR, "Invalid bid");
            }
            if (!seenProducts.add(r.productId())) {
                throw new BusinessRuleException(ErrorCode.VALIDATION_ERROR,
                    "Duplicate product in bids: " + r.productId());
            }
            ProductView pv = productService.getView(r.productId());
            if (!pv.sellerId().equals(campaign.getSellerId())) {
                throw new BusinessRuleException(ErrorCode.PRODUCT_NOT_OWNED,
                    "Product does not belong to the seller: " + r.productId());
            }
            CategoryView cv = categoryService.getView(pv.categoryId());
            Money bid = Money.of(r.bidAmount());
            if (bid.compareTo(cv.minBid()) < 0) {
                throw new BusinessRuleException(ErrorCode.BID_BELOW_MINIMUM,
                    "Bid below category minimum " + cv.minBid());
            }
            validated.put(r.productId(), bid);
        }

        Instant now = Instant.now(clock);
        List<AdBid> current = bidRepo.findByCampaignId(campaignId);
        Map<Long, AdBid> byProduct = new HashMap<>();
        for (AdBid b : current) {
            byProduct.put(b.getProductId(), b);
        }
        for (Map.Entry<Long, Money> e : validated.entrySet()) {
            AdBid existing = byProduct.get(e.getKey());
            if (existing != null) {
                if (!existing.getBidAmount().equals(e.getValue())) {
                    existing.changeAmount(e.getValue(), now);
                }
            } else {
                bidRepo.save(new AdBid(campaignId, e.getKey(), e.getValue(), now));
            }
        }
        for (AdBid b : current) {
            if (!validated.containsKey(b.getProductId())) {
                bidRepo.delete(b);
            }
        }
        return bidRepo.findByCampaignId(campaignId).stream().map(this::toBidDto).toList();
    }

    @Transactional(readOnly = true)
    public List<CampaignDto> listBySeller(Long sellerId) {
        return campaignRepo.findBySellerIdOrderByIdDesc(sellerId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public CampaignDto get(Long campaignId) {
        return toDto(findCampaign(campaignId));
    }

    @Transactional(readOnly = true)
    public List<BidDto> listBids(Long campaignId) {
        findCampaign(campaignId);
        return bidRepo.findByCampaignId(campaignId).stream()
            .map(this::toBidDto)
            .sorted(Comparator.comparing(BidDto::id))
            .toList();
    }

    private void checkMinBudget(Money budget) {
        Money min = Money.of(props.minDailyBudget());
        if (budget.compareTo(min) < 0) {
            throw new BusinessRuleException(ErrorCode.DAILY_BUDGET_TOO_LOW, "Daily budget must be at least " + min);
        }
    }

    private AdCampaign findCampaign(Long id) {
        return campaignRepo.findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.CAMPAIGN_NOT_FOUND, "Campaign not found: " + id));
    }

    private CampaignDto toDto(AdCampaign c) {
        LocalDate today = LocalDate.now(clock);
        return new CampaignDto(c.getId(), c.getSellerId(), c.getName(), c.getDailyBudget().amount(),
            c.spentOn(today).amount(), c.remaining(today).amount(), c.getStatus());
    }

    private BidDto toBidDto(AdBid b) {
        return new BidDto(b.getId(), b.getCampaignId(), b.getProductId(), b.getBidAmount().amount());
    }
}
