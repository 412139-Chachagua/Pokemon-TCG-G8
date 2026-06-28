package ar.edu.utn.frc.tup.piii.engine.victory;

import ar.edu.utn.frc.tup.piii.engine.turn.TurnPhase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FinishReasonTest {

    @Test
    void shouldContainAllFinishReasons() {
        assertNotNull(FinishReason.valueOf("KNOCKOUT"));
        assertNotNull(FinishReason.valueOf("PRIZES"));
        assertNotNull(FinishReason.valueOf("DECK_OUT"));
        assertNotNull(FinishReason.valueOf("CONCEDE"));
        assertNotNull(FinishReason.valueOf("SUDDEN_DEATH"));
    }

    @Test
    void shouldHaveCorrectCount() {
        assertEquals(5, FinishReason.values().length);
    }

    @Test
    void shouldContainAllTurnPhases() {
        assertNotNull(TurnPhase.valueOf("DRAW"));
        assertNotNull(TurnPhase.valueOf("MAIN"));
        assertNotNull(TurnPhase.valueOf("ATTACK"));
        assertNotNull(TurnPhase.valueOf("BETWEEN_TURNS"));
    }

    @Test
    void shouldHaveCorrectTurnPhaseCount() {
        assertEquals(4, TurnPhase.values().length);
    }
}
