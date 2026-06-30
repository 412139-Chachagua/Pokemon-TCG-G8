package ar.edu.utn.frc.tup.piii.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MatchStatusTest {

    @Test
    void shouldContainAllExpectedValues() {
        assertNotNull(MatchStatus.valueOf("WAITING"));
        assertNotNull(MatchStatus.valueOf("SETUP"));
        assertNotNull(MatchStatus.valueOf("ACTIVE"));
        assertNotNull(MatchStatus.valueOf("FINISHED"));
    }

    @Test
    void shouldHaveCorrectCount() {
        assertEquals(4, MatchStatus.values().length);
    }

    @Test
    void shouldThrowForInvalidValue() {
        assertThrows(IllegalArgumentException.class,
                () -> MatchStatus.valueOf("PAUSED"));
    }
}
