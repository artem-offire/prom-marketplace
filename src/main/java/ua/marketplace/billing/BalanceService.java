package ua.marketplace.billing;

import static ua.marketplace.billing.AccountCodes.PLATFORM_CLEARING;
import static ua.marketplace.billing.AccountCodes.sellerBalance;
import static ua.marketplace.billing.AccountCodes.sellerPayable;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.marketplace.billing.dto.BalanceDto;
import ua.marketplace.billing.dto.LedgerEntryDto;
import ua.marketplace.billing.dto.TopUpRequest;
import ua.marketplace.billing.dto.TopUpResponse;
import ua.marketplace.common.error.BusinessRuleException;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.common.error.NotFoundException;
import ua.marketplace.common.money.Money;
import ua.marketplace.common.web.PageResponse;

@Service
public class BalanceService {

    private final LedgerAccountRepository accountRepo;
    private final LedgerEntryRepository entryRepo;
    private final LedgerService ledger;

    public BalanceService(LedgerAccountRepository accountRepo, LedgerEntryRepository entryRepo,
                          LedgerService ledger) {
        this.accountRepo = accountRepo;
        this.entryRepo = entryRepo;
        this.ledger = ledger;
    }

    @Transactional
    public void openSellerAccounts(Long sellerId) {
        if (!accountRepo.existsByCode(sellerBalance(sellerId))) {
            accountRepo.save(new LedgerAccount(sellerBalance(sellerId), AccountType.LIABILITY, sellerId));
        }
        if (!accountRepo.existsByCode(sellerPayable(sellerId))) {
            accountRepo.save(new LedgerAccount(sellerPayable(sellerId), AccountType.LIABILITY, sellerId));
        }
    }

    @Transactional(readOnly = true)
    public Money balanceOf(Long sellerId) {
        return accountRepo.findByCode(sellerBalance(sellerId))
            .orElseThrow(() -> new NotFoundException(ErrorCode.SELLER_NOT_FOUND, "Seller not found: " + sellerId))
            .getBalance();
    }

    @Transactional(readOnly = true)
    public Money payableOf(Long sellerId) {
        return accountRepo.findByCode(sellerPayable(sellerId))
            .orElseThrow(() -> new NotFoundException(ErrorCode.SELLER_NOT_FOUND, "Seller not found: " + sellerId))
            .getBalance();
    }

    @Transactional(readOnly = true)
    public BalanceDto getBalance(Long sellerId) {
        return new BalanceDto(balanceOf(sellerId).amount(), payableOf(sellerId).amount(), "UAH");
    }

    @Transactional
    public TopUpResponse topUp(Long sellerId, TopUpRequest r) {
        balanceOf(sellerId);
        Money amount = Money.of(r.amount());
        if (!amount.isPositive()) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_ERROR, "Amount must be positive");
        }
        PostedTransaction pt = ledger.post(new Posting(
            TransactionType.TOP_UP,
            "TOPUP:" + sellerId + ":" + r.idempotencyKey(),
            "SELLER",
            String.valueOf(sellerId),
            "Поповнення балансу",
            false,
            List.of(
                PostingLine.debit(PLATFORM_CLEARING, amount),
                PostingLine.credit(sellerBalance(sellerId), amount))));
        return new TopUpResponse(pt.transactionId(), balanceOf(sellerId).amount(), pt.replayed());
    }

    @Transactional(readOnly = true)
    public PageResponse<LedgerEntryDto> listEntries(Long sellerId, TransactionType type, int page, int size) {
        balanceOf(sellerId);
        PageRequest p = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<LedgerEntry> result = type == null
            ? entryRepo.findBySeller(sellerId, p)
            : entryRepo.findBySellerAndType(sellerId, type, p);
        return PageResponse.of(result, this::toDto);
    }

    private LedgerEntryDto toDto(LedgerEntry e) {
        LedgerTransaction tx = e.getTransaction();
        return new LedgerEntryDto(tx.getId(), tx.getTxType(), tx.getCreatedAt(), e.getAccount().getCode(),
            e.getDirection(), e.getAmount().amount(), tx.getDescription());
    }
}
