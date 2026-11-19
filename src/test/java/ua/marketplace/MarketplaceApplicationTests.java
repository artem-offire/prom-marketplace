package ua.marketplace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import ua.marketplace.support.IntegrationTest;

@IntegrationTest
@AutoConfigureMockMvc
class MarketplaceApplicationTests {

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbc;

    MarketplaceApplicationTests(MockMvc mockMvc, JdbcTemplate jdbc) {
        this.mockMvc = mockMvc;
        this.jdbc = jdbc;
    }

    @Test
    void contextStartsAndHealthIsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"status\":\"UP\"")));
    }

    @Test
    void migrationsAndSeedDataArePresent() {
        Integer migrations = jdbc.queryForObject(
            "select count(*) from flyway_schema_history where success = TRUE", Integer.class);
        assertThat(migrations).isEqualTo(7);

        Integer plans = jdbc.queryForObject("select count(*) from plan", Integer.class);
        assertThat(plans).isEqualTo(4);

        // Категорії, засіяні міграцією; тести можуть додавати власні категорії.
        Integer seededCategories = jdbc.queryForObject(
            "select count(*) from category where name in ('Електроніка', 'Одяг і взуття', 'Дім і сад', 'Авто')",
            Integer.class);
        assertThat(seededCategories).isEqualTo(4);

        Integer platformAccounts = jdbc.queryForObject(
            "select count(*) from ledger_account where seller_id is null", Integer.class);
        assertThat(platformAccounts).isEqualTo(5);
    }
}
