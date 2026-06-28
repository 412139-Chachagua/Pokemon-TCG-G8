package ar.edu.utn.frc.tup.piii.configs;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;

class RestTemplateConfigTest {

    private final RestTemplateConfig config = new RestTemplateConfig();

    @Test
    void shouldCreateRestTemplate() {
        RestTemplate restTemplate = config.restTemplate();
        assertNotNull(restTemplate);
    }

    @Test
    void shouldCreateRestTemplateWithTimeouts() {
        RestTemplate restTemplate = config.restTemplate();
        assertNotNull(restTemplate);
        assertNotNull(restTemplate.getRequestFactory());
    }
}
