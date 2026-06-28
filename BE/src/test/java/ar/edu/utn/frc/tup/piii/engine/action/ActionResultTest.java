package ar.edu.utn.frc.tup.piii.engine.action;

import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ActionResultTest {

    @Test
    void defaultConstructor_createsEmptyFields() {
        ActionResult result = new ActionResult();

        assertFalse(result.isSuccess());
        assertNull(result.getClientRequestId());
        assertNull(result.getPublicState());
        assertNull(result.getPrivateState());
        assertNull(result.getEvents());
        assertNull(result.getError());
    }

    @Test
    void fullConstructor_setsAllFields() {
        GameError error = new GameError("ERR", "msg");
        List<GameEvent> events = List.of(
                new GameEvent("TEST", UUID.randomUUID(), 1, null, "test", null));

        ActionResult result = new ActionResult(true, "req-1", "public", "private", events, error);

        assertTrue(result.isSuccess());
        assertEquals("req-1", result.getClientRequestId());
        assertEquals("public", result.getPublicState());
        assertEquals("private", result.getPrivateState());
        assertEquals(1, result.getEvents().size());
        assertSame(error, result.getError());
    }

    @Test
    void settersAndGetters_roundTrip() {
        ActionResult result = new ActionResult();
        result.setSuccess(true);
        result.setClientRequestId("req-2");
        result.setPublicState("public");
        result.setPrivateState("private");
        result.setEvents(new ArrayList<>());
        result.setError(new GameError("E", "m"));

        assertTrue(result.isSuccess());
        assertEquals("req-2", result.getClientRequestId());
        assertEquals("public", result.getPublicState());
        assertEquals("private", result.getPrivateState());
        assertNotNull(result.getEvents());
        assertNotNull(result.getError());
    }
}
