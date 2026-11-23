package ua.marketplace.web.shop;

import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ua.marketplace.advertising.ClickChargingService;
import ua.marketplace.common.error.NotFoundException;

@Controller
public class AdClickController {

    private static final Logger log = LoggerFactory.getLogger(AdClickController.class);

    private final ClickChargingService clickService;

    public AdClickController(ClickChargingService clickService) {
        this.clickService = clickService;
    }

    /** Завжди веде на картку товару: помилка реєстрації кліка не повинна ламати перехід покупця. */
    @PostMapping("/ads/{impressionId}/click")
    public String click(@PathVariable UUID impressionId, @RequestParam Long productId, HttpSession session) {
        try {
            clickService.registerClick(impressionId, session.getId());
        } catch (NotFoundException e) {
            log.debug("Click ignored: {}", e.getMessage());
        }
        return "redirect:/products/" + productId;
    }
}
