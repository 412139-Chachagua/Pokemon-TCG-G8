package ar.edu.utn.frc.tup.piii.engine.action;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GameErrorTest {

    @Test
    void defaultConstructor_createsEmptyFields() {
        GameError error = new GameError();

        assertNull(error.getCode());
        assertNull(error.getMessage());
        assertNull(error.getDetails());
    }

    @Test
    void twoArgConstructor_setsCodeAndMessage() {
        GameError error = new GameError("NOT_FOUND", "Resource not found");

        assertEquals("NOT_FOUND", error.getCode());
        assertEquals("Resource not found", error.getMessage());
        assertNull(error.getDetails());
    }

    @Test
    void threeArgConstructor_setsAllFields() {
        Map<String, Object> details = Map.of("key", "value");
        GameError error = new GameError("VALIDATION_ERROR", "Invalid input", details);

        assertEquals("VALIDATION_ERROR", error.getCode());
        assertEquals("Invalid input", error.getMessage());
        assertEquals(details, error.getDetails());
    }

    @Test
    void settersAndGetters_roundTrip() {
        GameError error = new GameError();
        error.setCode("ERR");
        error.setMessage("msg");
        error.setDetails(Map.of());

        assertEquals("ERR", error.getCode());
        assertEquals("msg", error.getMessage());
        assertNotNull(error.getDetails());
    }
}
