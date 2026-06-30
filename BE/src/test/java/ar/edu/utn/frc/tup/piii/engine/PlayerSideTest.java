package ar.edu.utn.frc.tup.piii.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerSideTest {

    @Test
    void shouldContainAllExpectedValues() {
        assertNotNull(PlayerSide.valueOf("PLAYER_ONE"));
        assertNotNull(PlayerSide.valueOf("PLAYER_TWO"));
    }

    @Test
    void shouldHaveCorrectCount() {
        assertEquals(2, PlayerSide.values().length);
    }

    @Test
    void shouldThrowForInvalidValue() {
        assertThrows(IllegalArgumentException.class,
                () -> PlayerSide.valueOf("PLAYER_THREE"));
    }
}
