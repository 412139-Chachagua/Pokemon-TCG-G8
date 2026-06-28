package ar.edu.utn.frc.tup.piii.engine.action;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GameActionTest {

    @Test
    void getPayloadString_existingKey_returnsValue() {
        GameAction action = new GameAction();
        action.setPayload(Map.of("key1", "value1"));

        assertEquals("value1", action.getPayloadString("key1"));
    }

    @Test
    void getPayloadString_missingKey_returnsNull() {
        GameAction action = new GameAction();
        action.setPayload(Map.of("key1", "value1"));

        assertNull(action.getPayloadString("nonexistent"));
    }

    @Test
    void getPayloadString_nullPayload_returnsNull() {
        GameAction action = new GameAction();
        action.setPayload(null);

        assertNull(action.getPayloadString("key"));
    }

    @Test
    void getPayloadInt_validNumber_returnsInt() {
        GameAction action = new GameAction();
        action.setPayload(Map.of("count", 42));

        assertEquals(42, action.getPayloadInt("count"));
    }

    @Test
    void getPayloadInt_stringNumber_parsesInt() {
        GameAction action = new GameAction();
        action.setPayload(Map.of("count", "42"));

        assertEquals(42, action.getPayloadInt("count"));
    }

    @Test
    void getPayloadInt_malformedString_returnsNull() {
        GameAction action = new GameAction();
        action.setPayload(Map.of("count", "not-a-number"));

        assertNull(action.getPayloadInt("count"));
    }

    @Test
    void getPayloadInt_nullPayload_returnsNull() {
        GameAction action = new GameAction();
        action.setPayload(null);

        assertNull(action.getPayloadInt("count"));
    }

    @Test
    void settersAndGetters_roundTrip() {
        GameAction action = new GameAction();
        UUID playerId = UUID.randomUUID();
        action.setType(GameActionType.DRAW_CARD);
        action.setPlayerId(playerId);
        action.setClientRequestId("req-1");
        action.setPayload(Map.of("k", "v"));

        assertEquals(GameActionType.DRAW_CARD, action.getType());
        assertEquals(playerId, action.getPlayerId());
        assertEquals("req-1", action.getClientRequestId());
        assertEquals(Map.of("k", "v"), action.getPayload());
    }
}
