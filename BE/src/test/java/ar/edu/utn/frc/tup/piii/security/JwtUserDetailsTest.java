package ar.edu.utn.frc.tup.piii.security;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtUserDetailsTest {

    @Test
    void createWithAllFields_returnsRecord() {
        UUID userId = UUID.randomUUID();
        String role = "USER";
        UUID playerId = UUID.randomUUID();

        JwtUserDetails details = new JwtUserDetails(userId, role, playerId);

        assertEquals(userId, details.userId());
        assertEquals(role, details.role());
        assertEquals(playerId, details.playerId());
    }

    @Test
    void createWithNullPlayerId_returnsNull() {
        UUID userId = UUID.randomUUID();
        JwtUserDetails details = new JwtUserDetails(userId, "USER", null);

        assertEquals(userId, details.userId());
        assertNull(details.playerId());
    }

    @Test
    void equals_sameValues_returnsTrue() {
        UUID userId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();

        JwtUserDetails d1 = new JwtUserDetails(userId, "USER", playerId);
        JwtUserDetails d2 = new JwtUserDetails(userId, "USER", playerId);

        assertEquals(d1, d2);
        assertEquals(d1.hashCode(), d2.hashCode());
    }

    @Test
    void equals_differentValues_returnsFalse() {
        JwtUserDetails d1 = new JwtUserDetails(UUID.randomUUID(), "USER", UUID.randomUUID());
        JwtUserDetails d2 = new JwtUserDetails(UUID.randomUUID(), "ADMIN", UUID.randomUUID());

        assertNotEquals(d1, d2);
    }

    @Test
    void toString_containsFields() {
        UUID userId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        JwtUserDetails details = new JwtUserDetails(userId, "USER", playerId);

        String str = details.toString();

        assertTrue(str.contains(userId.toString()));
        assertTrue(str.contains("USER"));
        assertTrue(str.contains(playerId.toString()));
    }
}
