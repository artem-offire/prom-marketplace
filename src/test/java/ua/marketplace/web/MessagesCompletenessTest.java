package ua.marketplace.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.ResourceBundle;
import org.junit.jupiter.api.Test;
import ua.marketplace.advertising.CampaignStatus;
import ua.marketplace.billing.EntryDirection;
import ua.marketplace.billing.TransactionType;
import ua.marketplace.catalog.ProductStatus;
import ua.marketplace.common.error.ErrorCode;
import ua.marketplace.order.OrderAction;
import ua.marketplace.order.OrderStatus;
import ua.marketplace.order.PaymentMethod;
import ua.marketplace.payment.PaymentStatus;
import ua.marketplace.subscription.BillingPeriod;
import ua.marketplace.subscription.PlanCode;
import ua.marketplace.subscription.PlanFeature;

class MessagesCompletenessTest {

    private final ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.ROOT);

    private void assertAllKeys(String prefix, Enum<?>[] values) {
        for (Enum<?> value : values) {
            assertThat(bundle.containsKey(prefix + value.name())).as(prefix + value.name()).isTrue();
        }
    }

    @Test
    void everyErrorCodeAndEnumHasALabel() {
        assertAllKeys("error.", ErrorCode.values());
        assertAllKeys("order.status.", OrderStatus.values());
        assertAllKeys("order.action.", OrderAction.values());
        assertAllKeys("order.action.done.", OrderAction.values());
        assertAllKeys("payment.method.", PaymentMethod.values());
        assertAllKeys("payment.status.", PaymentStatus.values());
        assertAllKeys("product.status.", ProductStatus.values());
        assertAllKeys("campaign.status.", CampaignStatus.values());
        assertAllKeys("plan.code.", PlanCode.values());
        assertAllKeys("plan.feature.", PlanFeature.values());
        assertAllKeys("billing.period.", BillingPeriod.values());
        assertAllKeys("tx.", TransactionType.values());
        assertAllKeys("tx.direction.", EntryDirection.values());
    }

    @Test
    void valuesHaveNoAsciiApostropheAndNoForbiddenCharacters() {
        for (String key : bundle.keySet()) {
            String value = bundle.getString(key);
            assertThat(value).as(key).doesNotContain("'").doesNotContain("\u2014").doesNotContain("\u2013");
            assertThat(value.toLowerCase()).as(key).doesNotContain("цен" + "тр");
        }
    }
}
