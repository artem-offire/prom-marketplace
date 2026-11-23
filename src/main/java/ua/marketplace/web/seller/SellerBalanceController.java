package ua.marketplace.web.seller;

import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.marketplace.billing.BalanceService;
import ua.marketplace.billing.TransactionType;
import ua.marketplace.billing.dto.LedgerEntryDto;
import ua.marketplace.billing.dto.TopUpResponse;
import ua.marketplace.common.error.BusinessRuleException;
import ua.marketplace.common.web.PageResponse;
import ua.marketplace.web.support.Flash;
import ua.marketplace.web.support.PageModel;
import ua.marketplace.web.support.UiErrors;
import ua.marketplace.web.support.UiFormat;

@Controller
public class SellerBalanceController {

    private static final int PAGE_SIZE = 20;

    private final CabinetContext cabinet;
    private final BalanceService balanceService;
    private final UiErrors uiErrors;
    private final UiFormat fmt;

    public SellerBalanceController(CabinetContext cabinet, BalanceService balanceService, UiErrors uiErrors,
                                   UiFormat fmt) {
        this.cabinet = cabinet;
        this.balanceService = balanceService;
        this.uiErrors = uiErrors;
        this.fmt = fmt;
    }

    @GetMapping("/cabinet/{sellerId}/balance")
    public String balance(@PathVariable Long sellerId, @RequestParam(required = false) TransactionType type,
                          @RequestParam(defaultValue = "0") int page, Model model) {
        cabinet.load(sellerId, model);
        TopUpForm form = new TopUpForm();
        form.setIdempotencyKey(UUID.randomUUID().toString());
        model.addAttribute("form", form);
        populate(model, sellerId, type, page);
        return "cabinet/balance";
    }

    @PostMapping("/cabinet/{sellerId}/balance/top-up")
    public String topUp(@PathVariable Long sellerId, @Valid @ModelAttribute("form") TopUpForm form,
                        BindingResult br, Model model, RedirectAttributes redirect) {
        cabinet.load(sellerId, model);
        if (!br.hasErrors()) {
            try {
                TopUpResponse response = balanceService.topUp(sellerId, form.toRequest());
                redirect.addFlashAttribute(Flash.SUCCESS, response.replayed()
                    ? "Поповнення вже зараховано"
                    : "Баланс поповнено на " + fmt.money(form.getAmount()));
                return "redirect:/cabinet/" + sellerId + "/balance";
            } catch (BusinessRuleException e) {
                br.reject(e.code().name(), uiErrors.message(e.code()));
            }
        }
        populate(model, sellerId, null, 0);
        return "cabinet/balance";
    }

    private void populate(Model model, Long sellerId, TransactionType type, int page) {
        PageResponse<LedgerEntryDto> entries = balanceService.listEntries(sellerId, type, page, PAGE_SIZE);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("type", type == null ? null : type.name());
        model.addAttribute("balance", balanceService.getBalance(sellerId));
        model.addAttribute("entries", entries);
        model.addAttribute("pager", PageModel.of(entries, "/cabinet/" + sellerId + "/balance", params));
        model.addAttribute("types", TransactionType.values());
        model.addAttribute("selectedType", type);
    }
}
