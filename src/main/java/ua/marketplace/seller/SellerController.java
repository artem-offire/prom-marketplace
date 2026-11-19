package ua.marketplace.seller;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.marketplace.seller.dto.RegisterSellerRequest;
import ua.marketplace.seller.dto.SellerDto;

@RestController
@RequestMapping("/api/v1")
public class SellerController {

    private final SellerService sellerService;

    public SellerController(SellerService sellerService) {
        this.sellerService = sellerService;
    }

    @PostMapping("/sellers")
    public ResponseEntity<SellerDto> register(@Valid @RequestBody RegisterSellerRequest request) {
        SellerDto dto = sellerService.register(request);
        return ResponseEntity.created(URI.create("/api/v1/sellers/" + dto.id())).body(dto);
    }

    @GetMapping("/sellers/{id}")
    public ResponseEntity<SellerDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(sellerService.get(id));
    }
}
