package ua.marketplace;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ua.marketplace.support.IntegrationTest;

@IntegrationTest
@AutoConfigureMockMvc
class ApiFlowTest {

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    private final MockMvc mvc;

    ApiFlowTest(MockMvc mvc) {
        this.mvc = mvc;
    }

    private String registerBody(String email) {
        return "{\"name\":\"Тест\",\"email\":\"" + email + "\"}";
    }

    private long registerSeller() throws Exception {
        String email = "api-" + UUID.randomUUID() + "@test.local";
        MvcResult r = mvc.perform(post("/api/v1/sellers").contentType(JSON).content(registerBody(email)))
            .andExpect(status().isCreated())
            .andReturn();
        String body = r.getResponse().getContentAsString();
        return Long.parseLong(body.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    @Test
    void registerThenFetchSeller() throws Exception {
        String email = "flow-" + UUID.randomUUID() + "@test.local";
        MvcResult created = mvc.perform(post("/api/v1/sellers").contentType(JSON).content(registerBody(email)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", containsString("/api/v1/sellers/")))
            .andReturn();
        String location = created.getResponse().getHeader("Location");

        mvc.perform(get(location)).andExpect(status().isOk());
    }

    @Test
    void duplicateEmailIgnoringCaseIsConflict() throws Exception {
        String email = "dup-" + UUID.randomUUID() + "@test.local";
        mvc.perform(post("/api/v1/sellers").contentType(JSON).content(registerBody(email)))
            .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/sellers").contentType(JSON).content(registerBody(email.toUpperCase())))
            .andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void invalidEmailIsValidationError() throws Exception {
        mvc.perform(post("/api/v1/sellers").contentType(JSON).content(registerBody("не-пошта")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void unknownSellerIsNotFound() throws Exception {
        mvc.perform(get("/api/v1/sellers/999999999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("SELLER_NOT_FOUND"));
    }

    @Test
    void balanceTopUpAndIdempotency() throws Exception {
        long id = registerSeller();

        mvc.perform(get("/api/v1/sellers/" + id + "/balance"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("\"balance\":0.00")))
            .andExpect(content().string(containsString("\"payable\":0.00")));

        String zero = "{\"amount\":0,\"idempotencyKey\":\"k-zero\"}";
        mvc.perform(post("/api/v1/sellers/" + id + "/balance/top-ups").contentType(JSON).content(zero))
            .andExpect(status().isBadRequest());

        String tooPrecise = "{\"amount\":10.001,\"idempotencyKey\":\"k-precise\"}";
        mvc.perform(post("/api/v1/sellers/" + id + "/balance/top-ups").contentType(JSON).content(tooPrecise))
            .andExpect(status().isBadRequest());

        String key = "k-" + UUID.randomUUID();
        String topUp = "{\"amount\":100.00,\"idempotencyKey\":\"" + key + "\"}";
        mvc.perform(post("/api/v1/sellers/" + id + "/balance/top-ups").contentType(JSON).content(topUp))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/sellers/" + id + "/balance/top-ups").contentType(JSON).content(topUp))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.replayed").value(true));
    }

    @Test
    void plansAndSubscriptionRules() throws Exception {
        mvc.perform(get("/api/v1/plans"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4));

        long id = registerSeller();
        String topUp = "{\"amount\":100.00,\"idempotencyKey\":\"sub-" + UUID.randomUUID() + "\"}";
        mvc.perform(post("/api/v1/sellers/" + id + "/balance/top-ups").contentType(JSON).content(topUp))
            .andExpect(status().isCreated());

        String purchase = "{\"planCode\":\"START\",\"period\":\"MONTHLY\"}";
        mvc.perform(post("/api/v1/sellers/" + id + "/subscription").contentType(JSON).content(purchase))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("INSUFFICIENT_FUNDS"));

        String unknownPlan = "{\"planCode\":\"GOLD\",\"period\":\"MONTHLY\"}";
        mvc.perform(post("/api/v1/sellers/" + id + "/subscription").contentType(JSON).content(unknownPlan))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        mvc.perform(post("/api/v1/sellers/" + id + "/subscription").contentType(JSON).content("{not json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }
}
