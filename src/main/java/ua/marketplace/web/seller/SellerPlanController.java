package ua.marketplace.web.seller;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.marketplace.billing.BalanceService;
import ua.marketplace.billing.dto.BalanceDto;
import ua.marketplace.common.error.BusinessRuleException;
import ua.marketplace.subscription.BillingPeriod;
import ua.marketplace.subscription.PlanCode;
import ua.marketplace.subscription.SubscriptionService;
import ua.marketplace.subscription.dto.PlanDto;
import ua.marketplace.subscription.dto.SubscriptionDto;
import ua.marketplace.web.support.Flash;
import ua.marketplace.web.support.UiErrors;

@Controller
public class SellerPlanController {

    private final CabinetContext cabinet;
    private final SubscriptionService subscriptionService;
    private final BalanceService balanceService;
    private final UiErrors uiErrors;

    public SellerPlanController(CabinetContext cabinet, SubscriptionService subscriptionService,
                                BalanceService balanceService, UiErrors uiErrors) {
        this.cabinet = cabinet;
        this.subscriptionService = subscriptionService;
        this.balanceService = balanceService;
        this.uiErrors = uiErrors;
    }

    /** Рядок порівняльної таблиці. missing* це нестача коштів для купівлі (0, якщо вистачає). */
    public record PlanRow(PlanDto plan, BigDecimal yearlySaving, boolean current, BigDecimal missingMonthly,
                          BigDecimal missingYearly) {
    }

    @GetMapping("/cabinet/{sellerId}/plan")
    public String plans(@PathVariable Long sellerId, Model model) {
        cabinet.load(sellerId, model);
        SubscriptionDto subscription = subscriptionService.getActive(sellerId);
        BalanceDto balance = balanceService.getBalance(sellerId);
        List<PlanRow> rows = subscriptionService.listPlans().stream().map(p -> new PlanRow(
            p,
            p.monthlyPrice().multiply(BigDecimal.valueOf(12)).subtract(p.yearlyPrice()),
            p.code() == subscription.planCode(),
            shortfall(p.monthlyPrice(), balance.balance()),
            shortfall(p.yearlyPrice(), balance.balance()))).toList();
        model.addAttribute("rows", rows);
        model.addAttribute("balance", balance);
        model.addAttribute("onFree", subscription.planCode() == PlanCode.FREE);
        return "cabinet/plan";
    }

    @PostMapping("/cabinet/{sellerId}/plan/purchase")
    public String purchase(@PathVariable Long sellerId, @RequestParam PlanCode planCode,
                           @RequestParam BillingPeriod period, RedirectAttributes redirect) {
        SubscriptionDto current = subscriptionService.getActive(sellerId);
        String back = "redirect:/cabinet/" + sellerId + "/plan";
        if (current.planCode() != PlanCode.FREE) {
            redirect.addFlashAttribute(Flash.ERROR,
                "Зміна пакета з перерахунком стане доступною найближчим часом");
            return back;
        }
        try {
            subscriptionService.purchase(sellerId, planCode, period);
            redirect.addFlashAttribute(Flash.SUCCESS, "Пакет " + planCode.name() + " активовано");
        } catch (BusinessRuleException e) {
            redirect.addFlashAttribute(Flash.ERROR, uiErrors.message(e.code()));
        }
        return back;
    }

    private static BigDecimal shortfall(BigDecimal price, BigDecimal balance) {
        BigDecimal missing = price.subtract(balance);
        return missing.signum() > 0 ? missing : BigDecimal.ZERO.setScale(2);
    }
}
