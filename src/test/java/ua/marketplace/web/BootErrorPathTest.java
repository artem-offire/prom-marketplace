package ua.marketplace.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/** Реальний сервер: перевіряє шлях Boot (BasicErrorController) і шаблони error/*. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BootErrorPathTest {

    private final TestRestTemplate rest;

    BootErrorPathTest(TestRestTemplate rest) {
        this.rest = rest;
    }

    private ResponseEntity<String> get(String path, MediaType accept) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(accept));
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    @Test
    void unknownPageForBrowserIsHtml404() {
        ResponseEntity<String> r = get("/no-such-page", MediaType.TEXT_HTML);

        assertThat(r.getStatusCode().value()).isEqualTo(404);
        assertThat(r.getHeaders().getContentType().toString()).startsWith("text/html");
        assertThat(r.getBody()).contains("Сторінку не знайдено");
    }

    @Test
    void unknownApiPathForJsonClientIsPlainBootJson() {
        ResponseEntity<String> r = get("/api/v1/no-such-endpoint", MediaType.APPLICATION_JSON);

        assertThat(r.getStatusCode().value()).isEqualTo(404);
        assertThat(r.getHeaders().getContentType().toString()).startsWith("application/json");
    }
}
