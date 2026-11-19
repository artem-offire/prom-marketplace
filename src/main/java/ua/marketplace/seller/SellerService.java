package ua.marketplace.seller;

import java.time.Clock;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.marketplace.common.error.BusinessRuleException;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.common.error.NotFoundException;
import ua.marketplace.seller.dto.RegisterSellerRequest;
import ua.marketplace.seller.dto.SellerDto;

@Service
public class SellerService {

    private final SellerRepository repo;
    private final ApplicationEventPublisher publisher;
    private final Clock clock;

    public SellerService(SellerRepository repo, ApplicationEventPublisher publisher, Clock clock) {
        this.repo = repo;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Transactional
    public SellerDto register(RegisterSellerRequest r) {
        if (repo.existsByEmailIgnoreCase(r.email().trim())) {
            throw new BusinessRuleException(ErrorCode.EMAIL_ALREADY_EXISTS, "Seller with this email already exists");
        }
        Seller s = repo.save(new Seller(r.name().trim(), r.email(), r.phone(), Instant.now(clock)));
        publisher.publishEvent(new SellerRegisteredEvent(s.getId()));
        return toDto(s);
    }

    @Transactional(readOnly = true)
    public SellerDto get(Long id) {
        Seller s = repo.findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.SELLER_NOT_FOUND, "Seller not found: " + id));
        return toDto(s);
    }

    private SellerDto toDto(Seller s) {
        return new SellerDto(s.getId(), s.getName(), s.getEmail(), s.getPhone(), s.getStatus(), s.getCreatedAt());
    }
}
