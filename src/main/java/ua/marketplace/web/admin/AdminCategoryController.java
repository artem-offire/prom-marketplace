package ua.marketplace.web.admin;

import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.marketplace.catalog.CategoryService;
import ua.marketplace.catalog.dto.CategoryDto;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.common.error.NotFoundException;
import ua.marketplace.web.support.Flash;
import ua.marketplace.web.support.UiErrors;

@Controller
public class AdminCategoryController {

    private final CategoryService categoryService;
    private final UiErrors uiErrors;

    public AdminCategoryController(CategoryService categoryService, UiErrors uiErrors) {
        this.categoryService = categoryService;
        this.uiErrors = uiErrors;
    }

    @GetMapping("/admin/categories")
    public String list(Model model) {
        model.addAttribute("form", new CategoryForm());
        populate(model);
        return "admin/categories";
    }

    @PostMapping("/admin/categories")
    public String create(@Valid @ModelAttribute("form") CategoryForm form, BindingResult br, Model model,
                         RedirectAttributes redirect) {
        if (!br.hasErrors()) {
            try {
                categoryService.create(form.toRequest());
                redirect.addFlashAttribute(Flash.SUCCESS, "Категорію додано");
                return "redirect:/admin/categories";
            } catch (NotFoundException e) {
                br.rejectValue("parentId", e.code().name(), uiErrors.message(ErrorCode.CATEGORY_NOT_FOUND));
            }
        }
        populate(model);
        return "admin/categories";
    }

    /** Інлайн-форма в рядку таблиці: помилка показується над таблицею, значення рядка зберігаються. */
    @PostMapping("/admin/categories/{id}/pricing")
    public String pricing(@PathVariable Long id, @Valid @ModelAttribute("pricingForm") CategoryPricingForm form,
                          BindingResult br, Model model, RedirectAttributes redirect) {
        if (!br.hasErrors()) {
            try {
                categoryService.updatePricing(id, form.toRequest());
                redirect.addFlashAttribute(Flash.SUCCESS, "Тарифи категорії оновлено");
                return "redirect:/admin/categories";
            } catch (NotFoundException e) {
                throw e;
            }
        }
        Map<String, String> raw = new LinkedHashMap<>();
        raw.put("commissionRate", raw(br, "commissionRate"));
        raw.put("minBid", raw(br, "minBid"));
        model.addAttribute("form", new CategoryForm());
        model.addAttribute("pricingErrors", uiErrors.messages(br));
        model.addAttribute("pricingId", id);
        model.addAttribute("pricingRaw", raw);
        populate(model);
        return "admin/categories";
    }

    private void populate(Model model) {
        List<CategoryDto> categories = categoryService.list();
        model.addAttribute("categories", categories);
    }

    private static String raw(BindingResult br, String field) {
        Object value = br.getFieldValue(field);
        return value == null ? "" : value.toString();
    }
}
