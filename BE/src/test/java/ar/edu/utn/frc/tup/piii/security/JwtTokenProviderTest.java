package ar.edu.utn.frc.tup.piii.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;
    private String secret;
    private long expirationMs;

    @BeforeEach
    void setUp() {
        secret = Base64.getEncoder().encodeToString("mySecretKeyForTestingPurposesAtLeast32Bytes!".getBytes());
        expirationMs = 3600000;
        provider = new JwtTokenProvider(secret, expirationMs);
    }

    @Test
    void generateToken_allFields_returnsValidToken() {
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        String role = "USER";
        UUID playerId = UUID.randomUUID();

        String token = provider.generateToken(userId, email, role, playerId);

        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);
        assertTrue(provider.validateToken(token));
    }

    @Test
    void generateToken_withDisplayName_returnsValidToken() {
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        String role = "USER";
        UUID playerId = UUID.randomUUID();
        String displayName = "TestPlayer";

        String token = provider.generateToken(userId, email, role, playerId, displayName);

        assertNotNull(token);
        assertTrue(provider.validateToken(token));
    }

    @Test
    void validateToken_validToken_returnsTrue() {
        UUID userId = UUID.randomUUID();
        String token = provider.generateToken(userId, "email@test.com", "USER", UUID.randomUUID());

        assertTrue(provider.validateToken(token));
    }

    @Test
    void validateToken_expiredToken_returnsFalse() throws InterruptedException {
        JwtTokenProvider shortLived = new JwtTokenProvider(secret, 1);
        UUID userId = UUID.randomUUID();
        String token = shortLived.generateToken(userId, "email@test.com", "USER", UUID.randomUUID());

        Thread.sleep(5);

        assertFalse(shortLived.validateToken(token));
    }

    @Test
    void validateToken_malformedToken_returnsFalse() {
        String secret2 = Base64.getEncoder().encodeToString("anotherSecretKeyForTestingPurposesAtLeast32Bytes!!".getBytes());
        JwtTokenProvider otherProvider = new JwtTokenProvider(secret2, 3600000);
        UUID userId = UUID.randomUUID();
        String token = otherProvider.generateToken(userId, "email@test.com", "USER", UUID.randomUUID());

        assertFalse(provider.validateToken(token));
    }

    @Test
    void validateToken_nullToken_returnsFalse() {
        assertFalse(provider.validateToken(null));
    }

    @Test
    void validateToken_emptyToken_returnsFalse() {
        assertFalse(provider.validateToken(""));
    }

    @Test
    void getUserIdFromToken_returnsCorrectUUID() {
        UUID userId = UUID.randomUUID();
        String token = provider.generateToken(userId, "email@test.com", "USER", UUID.randomUUID());

        UUID extracted = provider.getUserIdFromToken(token);

        assertEquals(userId, extracted);
    }

    @Test
    void getUserDetailsFromToken_returnsCorrectDetails() {
        UUID userId = UUID.randomUUID();
        String role = "ADMIN";
        UUID playerId = UUID.randomUUID();
        String token = provider.generateToken(userId, "email@test.com", role, playerId);

        JwtUserDetails details = provider.getUserDetailsFromToken(token);

        assertEquals(userId, details.userId());
        assertEquals(role, details.role());
        assertEquals(playerId, details.playerId());
    }

    @Test
    void getUserDetailsFromToken_withNullPlayerId_returnsNullPlayerId() {
        SecretKey customKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        String token = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", "USER")
                .claim("playerId", null)
                .signWith(customKey)
                .compact();

        JwtUserDetails details = provider.getUserDetailsFromToken(token);

        assertNotNull(details);
        assertNull(details.playerId());
    }

    @Test
    void differentTokensForDifferentUsers_areNotEqual() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        String token1 = provider.generateToken(userId1, "a@test.com", "USER", UUID.randomUUID());
        String token2 = provider.generateToken(userId2, "b@test.com", "USER", UUID.randomUUID());

        assertNotEquals(token1, token2);
    }
}
