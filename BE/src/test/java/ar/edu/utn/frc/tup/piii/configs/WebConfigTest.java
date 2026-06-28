package ar.edu.utn.frc.tup.piii.configs;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import static org.junit.jupiter.api.Assertions.*;

class WebConfigTest {

    private final WebConfig webConfig = new WebConfig();

    @Test
    void shouldAllowCorsForLocalhost4200() {
        CorsRegistry registry = new CorsRegistry();
        assertDoesNotThrow(() -> webConfig.addCorsMappings(registry));
    }
}
