package ua.marketplace.catalog;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.marketplace.catalog.dto.CategoryDto;
import ua.marketplace.catalog.dto.CreateCategoryRequest;
import ua.marketplace.catalog.dto.UpdateCategoryPricingRequest;

@RestController
@RequestMapping("/api/v1")
public class CategoryAdminController {

    private final CategoryService categoryService;

    public CategoryAdminController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDto>> list() {
        return ResponseEntity.ok(categoryService.list());
    }

    @PostMapping("/admin/categories")
    public ResponseEntity<CategoryDto> create(@Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
    }

    @PutMapping("/admin/categories/{id}/pricing")
    public ResponseEntity<CategoryDto> updatePricing(@PathVariable Long id,
                                                     @Valid @RequestBody UpdateCategoryPricingRequest request) {
        return ResponseEntity.ok(categoryService.updatePricing(id, request));
    }
}
