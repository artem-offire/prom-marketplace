package ua.marketplace.catalog;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.marketplace.catalog.dto.CategoryDto;
import ua.marketplace.catalog.dto.CreateCategoryRequest;
import ua.marketplace.catalog.dto.UpdateCategoryPricingRequest;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.common.error.NotFoundException;
import ua.marketplace.common.money.Money;
import ua.marketplace.common.money.Percent;

@Service
public class CategoryService {

    private final CategoryRepository repo;

    public CategoryService(CategoryRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> list() {
        return repo.findAllByOrderByIdAsc().stream().map(this::toDto).toList();
    }

    @Transactional
    public CategoryDto create(CreateCategoryRequest r) {
        if (r.parentId() != null && !repo.existsById(r.parentId())) {
            throw new NotFoundException(ErrorCode.CATEGORY_NOT_FOUND, "Parent category not found: " + r.parentId());
        }
        Category c = repo.save(new Category(r.name().trim(), r.parentId(),
            Percent.of(r.commissionRate()), Money.of(r.minBid())));
        return toDto(c);
    }

    @Transactional
    public CategoryDto updatePricing(Long id, UpdateCategoryPricingRequest r) {
        Category c = repo.findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.CATEGORY_NOT_FOUND, "Category not found: " + id));
        c.updatePricing(Percent.of(r.commissionRate()), Money.of(r.minBid()));
        return toDto(c);
    }

    @Transactional(readOnly = true)
    public CategoryView getView(Long id) {
        Category c = repo.findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorCode.CATEGORY_NOT_FOUND, "Category not found: " + id));
        return new CategoryView(c.getId(), c.getName(), c.getCommissionRate(), c.getMinBid());
    }

    private CategoryDto toDto(Category c) {
        return new CategoryDto(c.getId(), c.getName(), c.getParentId(),
            c.getCommissionRate().value(), c.getMinBid().amount());
    }
}
