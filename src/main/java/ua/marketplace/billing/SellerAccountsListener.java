package ua.marketplace.billing;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ua.marketplace.seller.SellerRegisteredEvent;

@Component
public class SellerAccountsListener {

    private final BalanceService balanceService;

    public SellerAccountsListener(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @EventListener
    public void on(SellerRegisteredEvent e) {
        balanceService.openSellerAccounts(e.sellerId());
    }
}
