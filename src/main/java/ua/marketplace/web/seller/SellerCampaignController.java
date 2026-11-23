package ua.marketplace.web.seller;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
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
import ua.marketplace.advertising.AdsProperties;
import ua.marketplace.advertising.CampaignService;
import ua.marketplace.advertising.CampaignStatus;
import ua.marketplace.advertising.dto.BidDto;
import ua.marketplace.advertising.dto.BidRequest;
import ua.marketplace.advertising.dto.CampaignDto;
import ua.marketplace.advertising.dto.PatchCampaignRequest;
import ua.marketplace.catalog.CategoryService;
import ua.marketplace.catalog.ProductService;
import ua.marketplace.catalog.ProductStatus;
import ua.marketplace.catalog.dto.CategoryDto;
import ua.marketplace.catalog.dto.ProductDto;
import ua.marketplace.common.error.BusinessRuleException;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.subscription.PlanFeature;
import ua.marketplace.subscription.PlanLimits;
import ua.marketplace.web.support.Flash;
import ua.marketplace.web.support.UiErrors;
import ua.marketplace.web.support.UiFormat;

@Controller
public class SellerCampaignController {

    private final CabinetContext cabinet;
    private final CampaignService campaignService;
    private final ProductService productService;
    private final CategoryService categoryService;
    private final PlanLimits planLimits;
    private final AdsProperties adsProperties;
    private final UiErrors uiErrors;
    private final UiFormat fmt;

    public SellerCampaignController(CabinetContext cabinet, CampaignService campaignService,
                                    ProductService productService, CategoryService categoryService,
                                    PlanLimits planLimits, AdsProperties adsProperties, UiErrors uiErrors,
                                    UiFormat fmt) {
        this.cabinet = cabinet;
        this.campaignService = campaignService;
        this.productService = productService;
        this.categoryService = categoryService;
        this.planLimits = planLimits;
        this.adsProperties = adsProperties;
        this.uiErrors = uiErrors;
        this.fmt = fmt;
    }

    // ---------------------------------------------------------------- список і створення

    @GetMapping("/cabinet/{sellerId}/campaigns")
    public String list(@PathVariable Long sellerId, Model model) {
        cabinet.load(sellerId, model);
        model.addAttribute("form", new CampaignForm());
        populateList(model, sellerId);
        return "cabinet/campaigns";
    }

    @PostMapping("/cabinet/{sellerId}/campaigns")
    public String create(@PathVariable Long sellerId, @Valid @ModelAttribute("form") CampaignForm form,
                         BindingResult br, Model model, RedirectAttributes redirect) {
        cabinet.load(sellerId, model);
        if (!br.hasErrors()) {
            try {
                CampaignDto created = campaignService.create(sellerId, form.toRequest());
                redirect.addFlashAttribute(Flash.SUCCESS, "Кампанію створено. Додайте ставки на товари");
                return "redirect:/cabinet/" + sellerId + "/campaigns/" + created.id();
            } catch (BusinessRuleException e) {
                if (e.code() == ErrorCode.DAILY_BUDGET_TOO_LOW) {
                    br.rejectValue("dailyBudget", e.code().name(),
                        "Мінімальний денний бюджет: " + fmt.money(adsProperties.minDailyBudget()));
                } else {
                    br.reject(e.code().name(), uiErrors.message(e.code()));
                }
            }
        }
        populateList(model, sellerId);
        return "cabinet/campaigns";
    }

    private void populateList(Model model, Long sellerId) {
        model.addAttribute("canAdvertise", planLimits.hasFeature(sellerId, PlanFeature.PROSALE));
        model.addAttribute("campaigns", campaignService.listBySeller(sellerId));
        model.addAttribute("minBudget", adsProperties.minDailyBudget());
    }

    // ---------------------------------------------------------------- деталі кампанії

    @GetMapping("/cabinet/{sellerId}/campaigns/{campaignId}")
    public String detail(@PathVariable Long sellerId, @PathVariable Long campaignId, Model model) {
        cabinet.load(sellerId, model);
        CampaignDto campaign = cabinet.campaign(sellerId, campaignId);
        BudgetForm budgetForm = new BudgetForm();
        budgetForm.setDailyBudget(campaign.dailyBudget());
        model.addAttribute("budgetForm", budgetForm);
        BidsForm bidsForm = new BidsForm();
        bidsForm.setRows(currentRows(sellerId, campaignId));
        model.addAttribute("bidsForm", bidsForm);
        populateDetail(model, campaign);
        return "cabinet/campaign";
    }

    @PostMapping("/cabinet/{sellerId}/campaigns/{campaignId}/budget")
    public String budget(@PathVariable Long sellerId, @PathVariable Long campaignId,
                         @Valid @ModelAttribute("budgetForm") BudgetForm form, BindingResult br, Model model,
                         RedirectAttributes redirect) {
        cabinet.load(sellerId, model);
        CampaignDto campaign = cabinet.campaign(sellerId, campaignId);
        if (!br.hasErrors()) {
            try {
                campaignService.patch(campaignId, new PatchCampaignRequest(form.getDailyBudget(), null));
                redirect.addFlashAttribute(Flash.SUCCESS, "Денний бюджет оновлено");
                return "redirect:/cabinet/" + sellerId + "/campaigns/" + campaignId;
            } catch (BusinessRuleException e) {
                if (e.code() == ErrorCode.DAILY_BUDGET_TOO_LOW) {
                    br.rejectValue("dailyBudget", e.code().name(),
                        "Мінімальний денний бюджет: " + fmt.money(adsProperties.minDailyBudget()));
                } else {
                    br.reject(e.code().name(), uiErrors.message(e.code()));
                }
            }
        }
        BidsForm bidsForm = new BidsForm();
        bidsForm.setRows(currentRows(sellerId, campaignId));
        model.addAttribute("bidsForm", bidsForm);
        populateDetail(model, campaign);
        return "cabinet/campaign";
    }

    @PostMapping("/cabinet/{sellerId}/campaigns/{campaignId}/status")
    public String status(@PathVariable Long sellerId, @PathVariable Long campaignId,
                         @RequestParam CampaignStatus status, RedirectAttributes redirect) {
        cabinet.campaign(sellerId, campaignId);
        try {
            campaignService.patch(campaignId, new PatchCampaignRequest(null, status));
            redirect.addFlashAttribute(Flash.SUCCESS,
                status == CampaignStatus.PAUSED ? "Кампанію призупинено" : "Кампанію поновлено");
        } catch (BusinessRuleException e) {
            redirect.addFlashAttribute(Flash.ERROR, uiErrors.message(e.code()));
        }
        return "redirect:/cabinet/" + sellerId + "/campaigns/" + campaignId;
    }

    @PostMapping("/cabinet/{sellerId}/campaigns/{campaignId}/bids")
    public String bids(@PathVariable Long sellerId, @PathVariable Long campaignId,
                       @Valid @ModelAttribute("bidsForm") BidsForm form, BindingResult br, Model model,
                       RedirectAttributes redirect) {
        cabinet.load(sellerId, model);
        CampaignDto campaign = cabinet.campaign(sellerId, campaignId);
        Map<Long, ProductDto> products = new HashMap<>();
        for (ProductDto p : activeProducts(sellerId)) {
            products.put(p.id(), p);
        }
        Map<Long, CategoryDto> categories = new HashMap<>();
        for (CategoryDto c : categoryService.list()) {
            categories.put(c.id(), c);
        }

        List<BidRequest> requests = new ArrayList<>();
        for (int i = 0; i < form.getRows().size(); i++) {
            BidsForm.BidRow row = form.getRows().get(i);
            ProductDto product = row.getProductId() == null ? null : products.get(row.getProductId());
            if (product != null) {
                CategoryDto category = categories.get(product.categoryId());
                row.setTitle(product.title());
                row.setMinBid(category == null ? null : category.minBid());
            }
            if (row.getAmount() == null) {
                continue;
            }
            if (product != null && row.getMinBid() != null && row.getAmount().compareTo(row.getMinBid()) < 0
                    && !br.hasFieldErrors("rows[" + i + "].amount")) {
                br.rejectValue("rows[" + i + "].amount", ErrorCode.BID_BELOW_MINIMUM.name(),
                    "Ставка нижча за мінімальну: " + fmt.money(row.getMinBid()));
            }
            requests.add(new BidRequest(row.getProductId(), row.getAmount()));
        }

        if (!br.hasErrors()) {
            try {
                campaignService.replaceBids(campaignId, requests);
                redirect.addFlashAttribute(Flash.SUCCESS, "Ставки збережено");
                return "redirect:/cabinet/" + sellerId + "/campaigns/" + campaignId;
            } catch (BusinessRuleException e) {
                br.reject(e.code().name(), uiErrors.message(e.code()));
            }
        }
        BudgetForm budgetForm = new BudgetForm();
        budgetForm.setDailyBudget(campaign.dailyBudget());
        model.addAttribute("budgetForm", budgetForm);
        populateDetail(model, campaign);
        return "cabinet/campaign";
    }

    private void populateDetail(Model model, CampaignDto campaign) {
        model.addAttribute("campaign", campaign);
        model.addAttribute("minBudget", adsProperties.minDailyBudget());
    }

    private List<ProductDto> activeProducts(Long sellerId) {
        return productService.listBySeller(sellerId, 0, 100).content().stream()
            .filter(p -> p.status() == ProductStatus.ACTIVE)
            .toList();
    }

    /** Рядок на кожен активний товар (до 100): поточна ставка кампанії або порожньо. */
    private List<BidsForm.BidRow> currentRows(Long sellerId, Long campaignId) {
        Map<Long, BidDto> bidByProduct = new HashMap<>();
        for (BidDto b : campaignService.listBids(campaignId)) {
            bidByProduct.put(b.productId(), b);
        }
        Map<Long, CategoryDto> categories = new HashMap<>();
        for (CategoryDto c : categoryService.list()) {
            categories.put(c.id(), c);
        }
        List<BidsForm.BidRow> rows = new ArrayList<>();
        for (ProductDto p : activeProducts(sellerId)) {
            BidsForm.BidRow row = new BidsForm.BidRow();
            row.setProductId(p.id());
            row.setTitle(p.title());
            CategoryDto category = categories.get(p.categoryId());
            row.setMinBid(category == null ? null : category.minBid());
            BidDto bid = bidByProduct.get(p.id());
            row.setAmount(bid == null ? null : bid.bidAmount());
            rows.add(row);
        }
        return rows;
    }
}
