package ar.edu.utn.frc.tup.piii.engine.turn.states;

import ar.edu.utn.frc.tup.piii.engine.turn.TurnPhase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TurnStateTest {

    @Test
    void drawTurnState_phaseAndPermissions() {
        DrawTurnState state = new DrawTurnState();

        assertEquals(TurnPhase.DRAW, state.getPhase());
        assertTrue(state.canDraw());
        assertFalse(state.canPlay());
        assertFalse(state.canAttack());
        assertTrue(state.canEndTurn());
        assertTrue(state.canPlaceBasic());

        TurnState advanced = state.advance();
        assertInstanceOf(MainTurnState.class, advanced);
    }

    @Test
    void mainTurnState_phaseAndPermissions() {
        MainTurnState state = new MainTurnState();

        assertEquals(TurnPhase.MAIN, state.getPhase());
        assertFalse(state.canDraw());
        assertTrue(state.canPlay());
        assertTrue(state.canAttack());
        assertTrue(state.canEndTurn());
        assertTrue(state.canPlaceBasic());

        TurnState advanced = state.advance();
        assertInstanceOf(AttackTurnState.class, advanced);
    }

    @Test
    void attackTurnState_phaseAndPermissions() {
        AttackTurnState state = new AttackTurnState();

        assertEquals(TurnPhase.ATTACK, state.getPhase());
        assertFalse(state.canDraw());
        assertFalse(state.canPlay());
        assertFalse(state.canAttack());
        assertFalse(state.canEndTurn());
        assertFalse(state.canPlaceBasic());

        TurnState advanced = state.advance();
        assertInstanceOf(BetweenTurnsTurnState.class, advanced);
    }

    @Test
    void betweenTurnsTurnState_phaseAndPermissions() {
        BetweenTurnsTurnState state = new BetweenTurnsTurnState();

        assertEquals(TurnPhase.BETWEEN_TURNS, state.getPhase());
        assertFalse(state.canDraw());
        assertFalse(state.canPlay());
        assertFalse(state.canAttack());
        assertFalse(state.canEndTurn());
        assertFalse(state.canPlaceBasic());

        TurnState advanced = state.advance();
        assertSame(state, advanced); // self-loop
    }
}
