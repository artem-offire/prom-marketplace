package ua.marketplace.web.seller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.marketplace.common.error.BusinessRuleException;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.common.error.NotFoundException;
import ua.marketplace.seller.SellerService;
import ua.marketplace.seller.dto.SellerDto;
import ua.marketplace.web.support.Flash;
import ua.marketplace.web.support.UiErrors;

@Controller
public class CabinetEntryController {

    private final SellerService sellerService;
    private final UiErrors uiErrors;

    public CabinetEntryController(SellerService sellerService, UiErrors uiErrors) {
        this.sellerService = sellerService;
        this.uiErrors = uiErrors;
    }

    @GetMapping("/cabinet")
    public String entry(Model model) {
        model.addAttribute("form", new RegisterSellerForm());
        return "cabinet/entry";
    }

    @GetMapping("/cabinet/open")
    public String open(@RequestParam(required = false) String sellerId, RedirectAttributes redirect) {
        Long id = parse(sellerId);
        if (id == null) {
            redirect.addFlashAttribute(Flash.ERROR, "Вкажіть номер продавця цифрами");
            return "redirect:/cabinet";
        }
        try {
            sellerService.get(id);
        } catch (NotFoundException e) {
            redirect.addFlashAttribute(Flash.ERROR, "Продавця з номером " + id + " не знайдено");
            return "redirect:/cabinet";
        }
        return "redirect:/cabinet/" + id;
    }

    @PostMapping("/cabinet/register")
    public String register(@Valid @ModelAttribute("form") RegisterSellerForm form, BindingResult br,
                           RedirectAttributes redirect) {
        if (!br.hasErrors()) {
            try {
                SellerDto seller = sellerService.register(form.toRequest());
                redirect.addFlashAttribute(Flash.SUCCESS, "Кабінет створено, пакет FREE");
                return "redirect:/cabinet/" + seller.id();
            } catch (BusinessRuleException e) {
                if (e.code() == ErrorCode.EMAIL_ALREADY_EXISTS) {
                    br.rejectValue("email", e.code().name(), uiErrors.message(e.code()));
                } else {
                    br.reject(e.code().name(), uiErrors.message(e.code()));
                }
            }
        }
        return "cabinet/entry";
    }

    private static Long parse(String value) {
        if (value == null || !value.trim().matches("\\d{1,18}")) {
            return null;
        }
        return Long.valueOf(value.trim());
    }
}
