package ar.edu.utn.frc.tup.piii.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void passwordEncoder_beanIsBCrypt() {
        SecurityConfig config = new SecurityConfig(jwtAuthenticationFilter);

        PasswordEncoder encoder = config.passwordEncoder();

        assertNotNull(encoder);
        assertInstanceOf(BCryptPasswordEncoder.class, encoder);
    }

    @Test
    void corsConfigurationSource_beanIsCreated() {
        SecurityConfig config = new SecurityConfig(jwtAuthenticationFilter);

        CorsConfigurationSource source = config.corsConfigurationSource();

        assertNotNull(source);
    }
}
