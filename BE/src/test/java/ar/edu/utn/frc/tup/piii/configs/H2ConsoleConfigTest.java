package ar.edu.utn.frc.tup.piii.configs;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.ServletRegistrationBean;

import static org.junit.jupiter.api.Assertions.*;

class H2ConsoleConfigTest {

    private final H2ConsoleConfig config = new H2ConsoleConfig();

    @Test
    void shouldCreateH2ConsoleServletRegistration() {
        ServletRegistrationBean<?> registration = config.h2ConsoleServletRegistration();
        assertNotNull(registration);
        assertTrue(registration.getUrlMappings().contains("/h2-console/*"));
    }
}
