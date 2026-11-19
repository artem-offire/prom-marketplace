package ua.marketplace.subscription;

import static ua.marketplace.billing.AccountCodes.REVENUE_SUBSCRIPTION;
import static ua.marketplace.billing.AccountCodes.sellerBalance;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.marketplace.billing.LedgerService;
import ua.marketplace.billing.Posting;
import ua.marketplace.billing.PostingLine;
import ua.marketplace.billing.TransactionType;
import ua.marketplace.common.error.BusinessRuleException;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.common.error.NotFoundException;
import ua.marketplace.common.money.Money;
import ua.marketplace.subscription.dto.PlanDto;
import ua.marketplace.subscription.dto.SubscriptionDto;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepo;
    private final PlanRepository planRepo;
    private final LedgerService ledger;
    private final Clock clock;

    public SubscriptionService(SubscriptionRepository subscriptionRepo, PlanRepository planRepo,
                               LedgerService ledger, Clock clock) {
        this.subscriptionRepo = subscriptionRepo;
        this.planRepo = planRepo;
        this.ledger = ledger;
        this.clock = clock;
    }

    @Transactional
    public void assignFree(Long sellerId) {
        if (subscriptionRepo.findBySellerIdAndStatus(sellerId, SubscriptionStatus.ACTIVE).isPresent()) {
            return;
        }
        subscriptionRepo.save(new Subscription(sellerId, PlanCode.FREE, BillingPeriod.MONTHLY, Money.ZERO,
            LocalDate.now(clock), null));
    }

    @Transactional(readOnly = true)
    public List<PlanDto> listPlans() {
        return planRepo.findAllByOrderByMonthlyPriceAsc().stream()
            .map(p -> new PlanDto(
                p.getCode(),
                p.getName(),
                p.getMonthlyPrice().amount(),
                p.getYearlyPrice().amount(),
                p.getProductLimit(),
                p.isMarketplaceVisible(),
                p.getFeatures().stream().sorted(Comparator.comparing(PlanFeature::name)).toList()))
            .toList();
    }

    @Transactional(readOnly = true)
    public SubscriptionDto getActive(Long sellerId) {
        return subscriptionRepo.findBySellerIdAndStatus(sellerId, SubscriptionStatus.ACTIVE)
            .map(this::toDto)
            .orElseThrow(() -> new NotFoundException(ErrorCode.SELLER_NOT_FOUND, "Seller not found: " + sellerId));
    }

    @Transactional
    public SubscriptionDto purchase(Long sellerId, PlanCode planCode, BillingPeriod period) {
        if (planCode == PlanCode.FREE) {
            throw new BusinessRuleException(ErrorCode.INVALID_PLAN_CHANGE, "FREE plan cannot be purchased");
        }
        Plan plan = planRepo.findById(planCode)
            .orElseThrow(() -> new NotFoundException(ErrorCode.PLAN_NOT_FOUND, "Plan not found: " + planCode));
        Subscription current = subscriptionRepo.findBySellerIdAndStatus(sellerId, SubscriptionStatus.ACTIVE)
            .orElseThrow(() -> new NotFoundException(ErrorCode.SELLER_NOT_FOUND, "Seller not found: " + sellerId));

        Money price = plan.priceFor(period);
        LocalDate start = LocalDate.now(clock);
        LocalDate end = period == BillingPeriod.MONTHLY ? start.plusMonths(1) : start.plusYears(1);

        Subscription next = subscriptionRepo.save(new Subscription(sellerId, planCode, period, price, start, end));

        ledger.post(new Posting(
            TransactionType.SUBSCRIPTION_CHARGE,
            "SUB:" + next.getId(),
            "SUBSCRIPTION",
            String.valueOf(next.getId()),
            "Оплата пакета " + planCode,
            false,
            List.of(
                PostingLine.debit(sellerBalance(sellerId), price),
                PostingLine.credit(REVENUE_SUBSCRIPTION, price))));

        current.cancel();
        return toDto(next);
    }

    private SubscriptionDto toDto(Subscription s) {
        return new SubscriptionDto(s.getId(), s.getSellerId(), s.getPlanCode(), s.getBillingPeriod(),
            s.getPricePaid().amount(), s.getStartDate(), s.getEndDate(), s.getStatus());
    }
}
