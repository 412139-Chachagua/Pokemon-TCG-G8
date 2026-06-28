package ar.edu.utn.frc.tup.piii.engine.action;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameActionTypeTest {

    @Test
    void shouldContainAllGameActionTypes() {
        assertNotNull(GameActionType.valueOf("PUT_BASIC_ON_BENCH"));
        assertNotNull(GameActionType.valueOf("ATTACH_ENERGY"));
        assertNotNull(GameActionType.valueOf("EVOLVE_POKEMON"));
        assertNotNull(GameActionType.valueOf("PLAY_TRAINER"));
        assertNotNull(GameActionType.valueOf("RETREAT_ACTIVE"));
        assertNotNull(GameActionType.valueOf("DECLARE_ATTACK"));
        assertNotNull(GameActionType.valueOf("END_TURN"));
        assertNotNull(GameActionType.valueOf("DRAW_CARD"));
        assertNotNull(GameActionType.valueOf("TAKE_PRIZE_CARD"));
        assertNotNull(GameActionType.valueOf("ATTACH_TOOL"));
        assertNotNull(GameActionType.valueOf("USE_ABILITY"));
        assertNotNull(GameActionType.valueOf("CHOOSE_KO_REPLACEMENT"));
        assertNotNull(GameActionType.valueOf("SETUP_PLACE_ACTIVE"));
        assertNotNull(GameActionType.valueOf("SETUP_PLACE_BENCH"));
        assertNotNull(GameActionType.valueOf("SETUP_REMOVE_ACTIVE"));
        assertNotNull(GameActionType.valueOf("SETUP_REMOVE_BENCH"));
        assertNotNull(GameActionType.valueOf("CONFIRM_SETUP"));
        assertNotNull(GameActionType.valueOf("RESOLVE_MULLIGAN_DRAW"));
        assertNotNull(GameActionType.valueOf("RESOLVE_INITIAL_MULLIGAN"));
    }

    @Test
    void shouldHaveCorrectCount() {
        assertEquals(19, GameActionType.values().length);
    }
}
