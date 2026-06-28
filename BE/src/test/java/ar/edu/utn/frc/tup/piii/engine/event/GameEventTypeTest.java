package ar.edu.utn.frc.tup.piii.engine.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameEventTypeTest {

    @Test
    void shouldContainAllGameEventTypes() {
        assertNotNull(GameEventType.valueOf("SETUP_ACTIVE_PLACED"));
        assertNotNull(GameEventType.valueOf("SETUP_BENCH_PLACED"));
        assertNotNull(GameEventType.valueOf("SETUP_ACTIVE_REMOVED"));
        assertNotNull(GameEventType.valueOf("SETUP_BENCH_REMOVED"));
        assertNotNull(GameEventType.valueOf("SETUP_CONFIRMED"));
        assertNotNull(GameEventType.valueOf("SETUP_COMPLETED"));
        assertNotNull(GameEventType.valueOf("CARD_DRAWN"));
        assertNotNull(GameEventType.valueOf("VICTORY_DECIDED"));
        assertNotNull(GameEventType.valueOf("POKEMON_PLACED_ON_BENCH"));
        assertNotNull(GameEventType.valueOf("ENERGY_ATTACHED"));
        assertNotNull(GameEventType.valueOf("POKEMON_EVOLVED"));
        assertNotNull(GameEventType.valueOf("TRAINER_PLAYED"));
        assertNotNull(GameEventType.valueOf("RETREAT_EXECUTED"));
        assertNotNull(GameEventType.valueOf("DAMAGE_APPLIED"));
        assertNotNull(GameEventType.valueOf("KNOCKOUT_OCCURRED"));
        assertNotNull(GameEventType.valueOf("ATTACK_DECLARED"));
        assertNotNull(GameEventType.valueOf("PHASE_CHANGED"));
        assertNotNull(GameEventType.valueOf("STATE_UPDATED"));
        assertNotNull(GameEventType.valueOf("PRIZE_TAKEN"));
        assertNotNull(GameEventType.valueOf("MULLIGAN_REVEALED"));
        assertNotNull(GameEventType.valueOf("INITIAL_MULLIGAN_NEEDED"));
        assertNotNull(GameEventType.valueOf("INITIAL_MULLIGAN_RESOLVED"));
        assertNotNull(GameEventType.valueOf("MULLIGAN_DRAW_OPPORTUNITY"));
        assertNotNull(GameEventType.valueOf("MULLIGAN_DRAW_RESOLVED"));
        assertNotNull(GameEventType.valueOf("TRAINER_EFFECT_RESOLVED"));
        assertNotNull(GameEventType.valueOf("STADIUM_PLAYED"));
        assertNotNull(GameEventType.valueOf("STADIUM_REMOVED"));
        assertNotNull(GameEventType.valueOf("TOOL_ATTACHED"));
        assertNotNull(GameEventType.valueOf("CARDS_DRAWN"));
        assertNotNull(GameEventType.valueOf("POKEMON_HEALED"));
        assertNotNull(GameEventType.valueOf("POKEMON_SEARCHED"));
        assertNotNull(GameEventType.valueOf("STATUS_APPLIED"));
        assertNotNull(GameEventType.valueOf("ENERGY_DISCARDED"));
        assertNotNull(GameEventType.valueOf("BENCH_DAMAGE"));
        assertNotNull(GameEventType.valueOf("ATTACK_EFFECT_RESOLVED"));
        assertNotNull(GameEventType.valueOf("ABILITY_USED"));
        assertNotNull(GameEventType.valueOf("ABILITY_BLOCKED"));
        assertNotNull(GameEventType.valueOf("KO_REPLACEMENT_REQUIRED"));
        assertNotNull(GameEventType.valueOf("KO_REPLACEMENT_DONE"));
        assertNotNull(GameEventType.valueOf("SUDDEN_DEATH_STARTED"));
        assertNotNull(GameEventType.valueOf("CONFUSION_SELF_HIT"));
        assertNotNull(GameEventType.valueOf("COIN_FLIP_RESULT"));
        assertNotNull(GameEventType.valueOf("ATTACK_CANCELED"));
        assertNotNull(GameEventType.valueOf("RECOIL_OCCURRED"));
        assertNotNull(GameEventType.valueOf("SWITCH_EXECUTED"));
        assertNotNull(GameEventType.valueOf("STATUS_REMOVED"));
        assertNotNull(GameEventType.valueOf("POKEMON_REVIVED"));
        assertNotNull(GameEventType.valueOf("ENERGY_SEARCHED"));
        assertNotNull(GameEventType.valueOf("OPPONENT_HAND_SHUFFLED"));
        assertNotNull(GameEventType.valueOf("POKEMON_RETURNED_TO_DECK"));
        assertNotNull(GameEventType.valueOf("OPPONENT_ENERGY_DISCARDED"));
        assertNotNull(GameEventType.valueOf("DECK_ORDERED"));
        assertNotNull(GameEventType.valueOf("DECK_PEEKED"));
        assertNotNull(GameEventType.valueOf("OPPONENT_RANDOM_DISCARD"));
        assertNotNull(GameEventType.valueOf("PLAYER_RECONNECTED"));
    }

    @Test
    void shouldHaveCorrectCount() {
        assertEquals(55, GameEventType.values().length);
    }
}
