package ua.marketplace.web.seller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.marketplace.catalog.CategoryService;
import ua.marketplace.catalog.ProductService;
import ua.marketplace.catalog.ProductStatus;
import ua.marketplace.catalog.dto.CategoryDto;
import ua.marketplace.catalog.dto.ProductDto;
import ua.marketplace.common.error.BusinessRuleException;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.common.error.NotFoundException;
import ua.marketplace.common.web.PageResponse;
import ua.marketplace.subscription.PlanLimits;
import ua.marketplace.web.support.CategoryNames;
import ua.marketplace.web.support.Flash;
import ua.marketplace.web.support.PageModel;
import ua.marketplace.web.support.UiErrors;

@Controller
public class SellerProductController {

    private static final int PAGE_SIZE = 20;
    private static final List<ProductStatus> CREATE_STATUSES = List.of(ProductStatus.DRAFT, ProductStatus.ACTIVE);
    private static final List<ProductStatus> EDIT_STATUSES = List.of(ProductStatus.values());

    private final CabinetContext cabinet;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final PlanLimits planLimits;
    private final UiErrors uiErrors;

    public SellerProductController(CabinetContext cabinet, ProductService productService,
                                   CategoryService categoryService, PlanLimits planLimits, UiErrors uiErrors) {
        this.cabinet = cabinet;
        this.productService = productService;
        this.categoryService = categoryService;
        this.planLimits = planLimits;
        this.uiErrors = uiErrors;
    }

    @GetMapping("/cabinet/{sellerId}/products")
    public String list(@PathVariable Long sellerId, @RequestParam(defaultValue = "0") int page, Model model) {
        cabinet.load(sellerId, model);
        PageResponse<ProductDto> products = productService.listBySeller(sellerId, page, PAGE_SIZE);
        long used = productService.countUsed(sellerId);
        int limit = planLimits.productLimit(sellerId);
        model.addAttribute("products", products);
        model.addAttribute("pager", PageModel.of(products, "/cabinet/" + sellerId + "/products", Map.of()));
        model.addAttribute("used", used);
        model.addAttribute("limit", limit);
        model.addAttribute("canAdd", used < limit);
        model.addAttribute("categoryNames", CategoryNames.byId(categoryService.list()));
        return "cabinet/products";
    }

    @GetMapping("/cabinet/{sellerId}/products/new")
    public String newForm(@PathVariable Long sellerId, Model model) {
        cabinet.load(sellerId, model);
        model.addAttribute("form", new ProductForm());
        populateForm(model, false, null, false);
        return "cabinet/product-form";
    }

    @PostMapping("/cabinet/{sellerId}/products/new")
    public String create(@PathVariable Long sellerId, @Valid @ModelAttribute("form") ProductForm form,
                         BindingResult br, Model model, RedirectAttributes redirect) {
        cabinet.load(sellerId, model);
        boolean limitReached = false;
        if (!br.hasErrors()) {
            try {
                productService.create(sellerId, form.toCreateRequest());
                redirect.addFlashAttribute(Flash.SUCCESS, "Товар додано");
                return "redirect:/cabinet/" + sellerId + "/products";
            } catch (BusinessRuleException e) {
                if (e.code() == ErrorCode.PRODUCT_LIMIT_EXCEEDED) {
                    limitReached = true;
                }
                br.reject(e.code().name(), uiErrors.message(e.code()));
            } catch (NotFoundException e) {
                if (e.code() == ErrorCode.CATEGORY_NOT_FOUND) {
                    br.rejectValue("categoryId", e.code().name(), uiErrors.message(e.code()));
                } else {
                    throw e;
                }
            }
        }
        populateForm(model, false, null, limitReached);
        return "cabinet/product-form";
    }

    @GetMapping("/cabinet/{sellerId}/products/{productId}/edit")
    public String editForm(@PathVariable Long sellerId, @PathVariable Long productId, Model model) {
        cabinet.load(sellerId, model);
        ProductDto product = cabinet.product(sellerId, productId);
        model.addAttribute("form", ProductForm.from(product));
        populateForm(model, true, productId, false);
        return "cabinet/product-form";
    }

    @PostMapping("/cabinet/{sellerId}/products/{productId}/edit")
    public String edit(@PathVariable Long sellerId, @PathVariable Long productId,
                       @Valid @ModelAttribute("form") ProductForm form, BindingResult br, Model model,
                       RedirectAttributes redirect) {
        cabinet.load(sellerId, model);
        cabinet.product(sellerId, productId);
        boolean limitReached = false;
        if (!br.hasErrors()) {
            try {
                productService.patch(productId, form.toPatchRequest());
                redirect.addFlashAttribute(Flash.SUCCESS, "Зміни збережено");
                return "redirect:/cabinet/" + sellerId + "/products";
            } catch (BusinessRuleException e) {
                if (e.code() == ErrorCode.PRODUCT_LIMIT_EXCEEDED) {
                    limitReached = true;
                }
                br.reject(e.code().name(), uiErrors.message(e.code()));
            } catch (NotFoundException e) {
                if (e.code() == ErrorCode.CATEGORY_NOT_FOUND) {
                    br.rejectValue("categoryId", e.code().name(), uiErrors.message(e.code()));
                } else {
                    throw e;
                }
            }
        }
        populateForm(model, true, productId, limitReached);
        return "cabinet/product-form";
    }

    private void populateForm(Model model, boolean editing, Long productId, boolean limitReached) {
        List<CategoryDto> categories = categoryService.list();
        model.addAttribute("categories", categories);
        model.addAttribute("statusOptions", editing ? EDIT_STATUSES : CREATE_STATUSES);
        model.addAttribute("editing", editing);
        model.addAttribute("productId", productId);
        model.addAttribute("limitReached", limitReached);
    }
}
