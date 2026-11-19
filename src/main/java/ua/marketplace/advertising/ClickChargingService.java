package ua.marketplace.advertising;

import static ua.marketplace.billing.AccountCodes.REVENUE_ADS;
import static ua.marketplace.billing.AccountCodes.sellerBalance;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.marketplace.advertising.dto.ClickResponse;
import ua.marketplace.billing.BalanceService;
import ua.marketplace.billing.LedgerService;
import ua.marketplace.billing.PostedTransaction;
import ua.marketplace.billing.Posting;
import ua.marketplace.billing.PostingLine;
import ua.marketplace.billing.TransactionType;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.common.error.NotFoundException;
import ua.marketplace.common.money.Money;

@Service
public class ClickChargingService {

    private final AdImpressionRepository impressionRepo;
    private final AdClickRepository clickRepo;
    private final AdCampaignRepository campaignRepo;
    private final LedgerService ledger;
    private final BalanceService balanceService;
    private final Clock clock;

    public ClickChargingService(AdImpressionRepository impressionRepo, AdClickRepository clickRepo,
                                AdCampaignRepository campaignRepo, LedgerService ledger,
                                BalanceService balanceService, Clock clock) {
        this.impressionRepo = impressionRepo;
        this.clickRepo = clickRepo;
        this.campaignRepo = campaignRepo;
        this.ledger = ledger;
        this.balanceService = balanceService;
        this.clock = clock;
    }

    @Transactional
    public ClickResponse registerClick(UUID impressionId, String sessionId) {
        AdImpression imp = impressionRepo.findById(impressionId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.IMPRESSION_NOT_FOUND,
                "Impression not found: " + impressionId));
        // Блокування рядка кампанії серіалізує клики по одній кампанії.
        AdCampaign campaign = campaignRepo.findForUpdate(imp.getCampaignId())
            .orElseThrow(() -> new NotFoundException(ErrorCode.CAMPAIGN_NOT_FOUND,
                "Campaign not found: " + imp.getCampaignId()));
        Instant now = Instant.now(clock);
        LocalDate today = LocalDate.now(clock);

        if (clickRepo.existsByImpressionId(impressionId)) {
            AdClick dup = clickRepo.save(new AdClick(impressionId, sessionId, Money.ZERO,
                ClickStatus.DUPLICATE, null, now));
            return new ClickResponse(dup.getId(), ClickStatus.DUPLICATE, Money.ZERO.amount());
        }

        Money cpc = imp.getCpc();
        if (campaign.remaining(today).compareTo(cpc) < 0) {
            AdClick c = clickRepo.save(new AdClick(impressionId, sessionId, Money.ZERO,
                ClickStatus.UNCHARGED_BUDGET, null, now));
            return new ClickResponse(c.getId(), ClickStatus.UNCHARGED_BUDGET, Money.ZERO.amount());
        }

        // Баланс перевіряється заздалегідь, щоб не ловити INSUFFICIENT_FUNDS з ledger.post.
        if (balanceService.balanceOf(campaign.getSellerId()).compareTo(cpc) < 0) {
            AdClick c = clickRepo.save(new AdClick(impressionId, sessionId, Money.ZERO,
                ClickStatus.UNCHARGED_NO_FUNDS, null, now));
            return new ClickResponse(c.getId(), ClickStatus.UNCHARGED_NO_FUNDS, Money.ZERO.amount());
        }

        PostedTransaction pt = ledger.post(new Posting(
            TransactionType.AD_CLICK_CHARGE,
            "CLICK:" + impressionId,
            "AD_IMPRESSION",
            impressionId.toString(),
            "Клік по оголошенню",
            false,
            List.of(
                PostingLine.debit(sellerBalance(campaign.getSellerId()), cpc),
                PostingLine.credit(REVENUE_ADS, cpc))));

        campaign.registerSpend(today, cpc);
        AdClick click = clickRepo.save(new AdClick(impressionId, sessionId, cpc, ClickStatus.CHARGED,
            pt.transactionId(), now));
        return new ClickResponse(click.getId(), ClickStatus.CHARGED, cpc.amount());
    }
}
