package ua.marketplace.billing;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.marketplace.billing.dto.BalanceDto;
import ua.marketplace.billing.dto.LedgerEntryDto;
import ua.marketplace.billing.dto.TopUpRequest;
import ua.marketplace.billing.dto.TopUpResponse;
import ua.marketplace.common.web.PageResponse;

@RestController
@RequestMapping("/api/v1")
public class BillingController {

    private final BalanceService balanceService;

    public BillingController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping("/sellers/{id}/balance")
    public ResponseEntity<BalanceDto> balance(@PathVariable Long id) {
        return ResponseEntity.ok(balanceService.getBalance(id));
    }

    @PostMapping("/sellers/{id}/balance/top-ups")
    public ResponseEntity<TopUpResponse> topUp(@PathVariable Long id, @Valid @RequestBody TopUpRequest request) {
        TopUpResponse response = balanceService.topUp(id, request);
        return ResponseEntity.status(response.replayed() ? HttpStatus.OK : HttpStatus.CREATED).body(response);
    }

    @GetMapping("/sellers/{id}/transactions")
    public ResponseEntity<PageResponse<LedgerEntryDto>> transactions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TransactionType type) {
        return ResponseEntity.ok(balanceService.listEntries(id, type, page, size));
    }
}
