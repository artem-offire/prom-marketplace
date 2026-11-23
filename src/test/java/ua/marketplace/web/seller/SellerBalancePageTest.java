package ua.marketplace.web.seller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import ua.marketplace.common.money.Money;
import ua.marketplace.support.Html;
import ua.marketplace.support.IntegrationTest;
import ua.marketplace.support.TestFixtures;

@IntegrationTest
@AutoConfigureMockMvc
class SellerBalancePageTest {

    private final MockMvc mvc;
    private final TestFixtures fx;

    SellerBalancePageTest(MockMvc mvc, TestFixtures fx) {
        this.mvc = mvc;
        this.fx = fx;
    }

    @Test
    void topUpCreditsBalanceAndShowsLedgerRow() throws Exception {
        long seller = fx.registerSeller();

        mvc.perform(post("/cabinet/" + seller + "/balance/top-up").param("amount", "1 500,25")
                .param("idempotencyKey", UUID.randomUUID().toString()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/cabinet/" + seller + "/balance"))
            .andExpect(flash().attributeExists("flashSuccess"));

        assertThat(fx.balance(seller)).isEqualTo(Money.of("1500.25"));
        String html = Html.body(mvc.perform(get("/cabinet/" + seller + "/balance")).andExpect(status().isOk()).andReturn());
        assertThat(html).contains(Html.uah("1 500,25"));
        Html.assertClean(html);
    }

    @Test
    void repeatedKeyIsNotCreditedTwice() throws Exception {
        long seller = fx.registerSeller();
        String key = UUID.randomUUID().toString();

        mvc.perform(post("/cabinet/" + seller + "/balance/top-up").param("amount", "100").param("idempotencyKey", key));
        mvc.perform(post("/cabinet/" + seller + "/balance/top-up").param("amount", "100").param("idempotencyKey", key))
            .andExpect(flash().attribute("flashSuccess", "Поповнення вже зараховано"));

        assertThat(fx.balance(seller)).isEqualTo(Money.of("100.00"));
    }

    @Test
    void invalidAmountRerendersWithMessages() throws Exception {
        long seller = fx.registerSeller();

        String zero = Html.body(mvc.perform(post("/cabinet/" + seller + "/balance/top-up").param("amount", "0")
            .param("idempotencyKey", UUID.randomUUID().toString())).andExpect(status().isOk()).andReturn());
        assertThat(zero).contains("Сума має бути не менше 0,01");

        String garbage = Html.body(mvc.perform(post("/cabinet/" + seller + "/balance/top-up").param("amount", "abc")
            .param("idempotencyKey", UUID.randomUUID().toString())).andExpect(status().isOk()).andReturn());
        assertThat(garbage).contains("Некоректне число");
        assertThat(fx.balance(seller)).isEqualTo(Money.of("0.00"));
    }

    @Test
    void typeFilterAndUnknownTypeValue() throws Exception {
        long seller = fx.registerSeller();
        fx.topUp(seller, "10.00");

        mvc.perform(get("/cabinet/" + seller + "/balance").param("type", "TOP_UP")).andExpect(status().isOk());
        mvc.perform(get("/cabinet/" + seller + "/balance").param("type", "NOPE")).andExpect(status().isBadRequest());
    }
}
