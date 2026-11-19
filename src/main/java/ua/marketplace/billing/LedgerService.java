package ua.marketplace.billing;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.marketplace.common.error.BusinessRuleException;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.common.error.NotFoundException;
import ua.marketplace.common.money.Money;

@Service
public class LedgerService {

    private final LedgerAccountRepository accountRepo;
    private final LedgerTransactionRepository txRepo;
    private final LedgerEntryRepository entryRepo;
    private final Clock clock;

    public LedgerService(LedgerAccountRepository accountRepo, LedgerTransactionRepository txRepo,
                         LedgerEntryRepository entryRepo, Clock clock) {
        this.accountRepo = accountRepo;
        this.txRepo = txRepo;
        this.entryRepo = entryRepo;
        this.clock = clock;
    }

    @Transactional
    public PostedTransaction post(Posting p) {
        validate(p);

        Set<String> codes = new LinkedHashSet<>();
        for (PostingLine line : p.lines()) {
            codes.add(line.accountCode());
        }
        List<LedgerAccount> accounts = accountRepo.lockByCodes(codes);
        if (accounts.size() != codes.size()) {
            throw new NotFoundException(ErrorCode.ACCOUNT_NOT_FOUND, "Ledger account not found");
        }
        Map<String, LedgerAccount> byCode = new HashMap<>();
        for (LedgerAccount a : accounts) {
            byCode.put(a.getCode(), a);
        }

        Optional<LedgerTransaction> existing = txRepo.findByIdempotencyKey(p.idempotencyKey());
        if (existing.isPresent()) {
            List<String> expected = fingerprint(p.lines());
            List<String> actual = new ArrayList<>();
            for (LedgerEntry e : entryRepo.findFetchedByTransactionId(existing.get().getId())) {
                actual.add(lineKey(e.getAccount().getCode(), e.getDirection(), e.getAmount()));
            }
            Collections.sort(actual);
            if (expected.equals(actual)) {
                return new PostedTransaction(existing.get().getId(), true);
            }
            throw new BusinessRuleException(ErrorCode.IDEMPOTENCY_CONFLICT,
                "Idempotency key reused with different postings: " + p.idempotencyKey());
        }

        // Фаза 1: розрахунок без мутації сутностей.
        Map<String, Money> projected = new HashMap<>();
        for (LedgerAccount a : accounts) {
            projected.put(a.getCode(), a.getBalance());
        }
        for (PostingLine line : p.lines()) {
            LedgerAccount account = byCode.get(line.accountCode());
            projected.put(line.accountCode(),
                projected.get(line.accountCode()).plus(account.delta(line.direction(), line.amount())));
        }

        if (!p.allowNegative()) {
            for (PostingLine line : p.lines()) {
                if (line.direction() == EntryDirection.DEBIT
                        && line.accountCode().startsWith(AccountCodes.SELLER_BALANCE_PREFIX)
                        && projected.get(line.accountCode()).isNegative()) {
                    throw new BusinessRuleException(ErrorCode.INSUFFICIENT_FUNDS, "Insufficient funds");
                }
            }
        }

        // Фаза 2: запис.
        LedgerTransaction tx = txRepo.save(new LedgerTransaction(
            p.type(), p.idempotencyKey(), p.referenceType(), p.referenceId(), p.description(), Instant.now(clock)));
        for (PostingLine line : p.lines()) {
            entryRepo.save(new LedgerEntry(tx, byCode.get(line.accountCode()), line.direction(), line.amount()));
        }
        for (LedgerAccount a : accounts) {
            a.changeBalance(projected.get(a.getCode()));
        }
        return new PostedTransaction(tx.getId(), false);
    }

    private void validate(Posting p) {
        if (p.lines() == null || p.lines().size() < 2) {
            throw new IllegalStateException("Posting must have at least two lines");
        }
        if (p.idempotencyKey() == null || p.idempotencyKey().isBlank()) {
            throw new IllegalStateException("Idempotency key must not be empty");
        }
        Money debit = Money.ZERO;
        Money credit = Money.ZERO;
        for (PostingLine line : p.lines()) {
            if (line.amount() == null || !line.amount().isPositive()) {
                throw new IllegalStateException("Posting line amount must be positive");
            }
            if (line.direction() == EntryDirection.DEBIT) {
                debit = debit.plus(line.amount());
            } else {
                credit = credit.plus(line.amount());
            }
        }
        if (debit.compareTo(credit) != 0) {
            throw new IllegalStateException("Posting is not balanced: debit " + debit + ", credit " + credit);
        }
    }

    private static List<String> fingerprint(List<PostingLine> lines) {
        List<String> result = new ArrayList<>();
        for (PostingLine line : lines) {
            result.add(lineKey(line.accountCode(), line.direction(), line.amount()));
        }
        Collections.sort(result);
        return result;
    }

    private static String lineKey(String code, EntryDirection direction, Money amount) {
        return code + "|" + direction + "|" + amount.amount().toPlainString();
    }
}
