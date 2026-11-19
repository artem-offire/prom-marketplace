package ua.marketplace.advertising;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.marketplace.advertising.dto.ClickRequest;
import ua.marketplace.advertising.dto.ClickResponse;
import ua.marketplace.advertising.dto.PlacementDto;

@RestController
@RequestMapping("/api/v1")
public class AdServingController {

    private final AdAuctionService auctionService;
    private final ClickChargingService clickService;

    public AdServingController(AdAuctionService auctionService, ClickChargingService clickService) {
        this.auctionService = auctionService;
        this.clickService = clickService;
    }

    @GetMapping("/ads/placements")
    public ResponseEntity<List<PlacementDto>> placements(
            @RequestParam Long categoryId,
            @RequestParam(required = false) Integer slots,
            @RequestParam(required = false) String sessionId) {
        return ResponseEntity.ok(auctionService.placements(categoryId, slots, sessionId));
    }

    @PostMapping("/ads/clicks")
    public ResponseEntity<ClickResponse> click(@Valid @RequestBody ClickRequest request) {
        return ResponseEntity.ok(clickService.registerClick(request.impressionId(), request.sessionId()));
    }
}
