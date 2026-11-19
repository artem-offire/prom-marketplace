package ua.marketplace.catalog;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.marketplace.catalog.dto.CreateProductRequest;
import ua.marketplace.catalog.dto.PatchProductRequest;
import ua.marketplace.catalog.dto.ProductDto;
import ua.marketplace.common.web.PageResponse;

@RestController
@RequestMapping("/api/v1")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/sellers/{id}/products")
    public ResponseEntity<ProductDto> create(@PathVariable Long id,
                                             @Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(id, request));
    }

    @PatchMapping("/products/{id}")
    public ResponseEntity<ProductDto> patch(@PathVariable Long id,
                                            @Valid @RequestBody PatchProductRequest request) {
        return ResponseEntity.ok(productService.patch(id, request));
    }

    @GetMapping("/sellers/{id}/products")
    public ResponseEntity<PageResponse<ProductDto>> listBySeller(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(productService.listBySeller(id, page, size));
    }
}
