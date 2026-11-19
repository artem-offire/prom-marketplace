package ua.marketplace.advertising;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.marketplace.advertising.dto.BidDto;
import ua.marketplace.advertising.dto.BidRequest;
import ua.marketplace.advertising.dto.CampaignDto;
import ua.marketplace.advertising.dto.CreateCampaignRequest;
import ua.marketplace.advertising.dto.PatchCampaignRequest;

@RestController
@RequestMapping("/api/v1")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @PostMapping("/sellers/{id}/campaigns")
    public ResponseEntity<CampaignDto> create(@PathVariable Long id,
                                              @Valid @RequestBody CreateCampaignRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(campaignService.create(id, request));
    }

    @PatchMapping("/campaigns/{id}")
    public ResponseEntity<CampaignDto> patch(@PathVariable Long id,
                                             @Valid @RequestBody PatchCampaignRequest request) {
        return ResponseEntity.ok(campaignService.patch(id, request));
    }

    @PutMapping("/campaigns/{id}/bids")
    public ResponseEntity<List<BidDto>> replaceBids(@PathVariable Long id,
                                                    @RequestBody List<BidRequest> bids) {
        return ResponseEntity.ok(campaignService.replaceBids(id, bids));
    }
}
