package ar.edu.utn.frc.tup.piii.engine.event;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GameEventTest {

    @Test
    void constructor_setsAllFields() {
        UUID matchId = UUID.randomUUID();
        Instant now = Instant.now();
        Map<String, Object> payload = Map.of("key", "value");

        GameEvent event = new GameEvent("CARD_DRAWN", matchId, 3, now, "Player drew a card", payload);

        assertEquals("CARD_DRAWN", event.getType());
        assertEquals(matchId, event.getMatchId());
        assertEquals(3, event.getTurnNumber());
        assertEquals(now, event.getCreatedAt());
        assertEquals("Player drew a card", event.getMessage());
        assertEquals(payload, event.getPayload());
    }

    @Test
    void constructor_withNullFields() {
        GameEvent event = new GameEvent("TEST", null, 0, null, null, null);

        assertEquals("TEST", event.getType());
        assertNull(event.getMatchId());
        assertEquals(0, event.getTurnNumber());
        assertNull(event.getCreatedAt());
        assertNull(event.getMessage());
        assertNull(event.getPayload());
    }
}
