package ar.edu.utn.frc.tup.piii.engine.match.states;

import ar.edu.utn.frc.tup.piii.engine.MatchStatus;
import ar.edu.utn.frc.tup.piii.engine.action.GameActionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MatchStatesTest {

    private final WaitingMatchState waitingState = new WaitingMatchState();
    private final SetupMatchState setupState = new SetupMatchState();
    private final ActiveMatchState activeState = new ActiveMatchState();
    private final FinishedMatchState finishedState = new FinishedMatchState();

    @Test
    void shouldReturnWAITINGStatus_whenWaitingMatchState() {
        assertEquals(MatchStatus.WAITING, waitingState.getStatus());
    }

    @Test
    void shouldNotBeActiveOrFinished_whenWaitingMatchState() {
        assertFalse(waitingState.isActive());
        assertFalse(waitingState.isFinished());
    }

    @Test
    void shouldRejectAllActions_whenWaitingMatchState() {
        for (GameActionType action : GameActionType.values()) {
            assertFalse(waitingState.canAcceptAction(action),
                    "WaitingMatchState should reject action: " + action);
        }
    }

    @Test
    void shouldReturnSETUPStatus_whenSetupMatchState() {
        assertEquals(MatchStatus.SETUP, setupState.getStatus());
    }

    @Test
    void shouldAcceptSetupActions_whenSetupMatchState() {
        assertTrue(setupState.canAcceptAction(GameActionType.SETUP_PLACE_ACTIVE));
        assertTrue(setupState.canAcceptAction(GameActionType.SETUP_PLACE_BENCH));
        assertTrue(setupState.canAcceptAction(GameActionType.SETUP_REMOVE_ACTIVE));
        assertTrue(setupState.canAcceptAction(GameActionType.SETUP_REMOVE_BENCH));
        assertTrue(setupState.canAcceptAction(GameActionType.CONFIRM_SETUP));
        assertTrue(setupState.canAcceptAction(GameActionType.RESOLVE_MULLIGAN_DRAW));
        assertTrue(setupState.canAcceptAction(GameActionType.RESOLVE_INITIAL_MULLIGAN));
    }

    @Test
    void shouldRejectGameActions_whenSetupMatchState() {
        assertFalse(setupState.canAcceptAction(GameActionType.DECLARE_ATTACK));
        assertFalse(setupState.canAcceptAction(GameActionType.END_TURN));
        assertFalse(setupState.canAcceptAction(GameActionType.ATTACH_ENERGY));
        assertFalse(setupState.canAcceptAction(GameActionType.PLAY_TRAINER));
    }

    @Test
    void shouldReturnACTIVEStatusAndAcceptAll_whenActiveMatchState() {
        assertEquals(MatchStatus.ACTIVE, activeState.getStatus());
        assertTrue(activeState.isActive());
        assertFalse(activeState.isFinished());
        for (GameActionType action : GameActionType.values()) {
            assertTrue(activeState.canAcceptAction(action),
                    "ActiveMatchState should accept action: " + action);
        }
    }

    @Test
    void shouldReturnFINISHEDStatusAndRejectAll_whenFinishedMatchState() {
        assertEquals(MatchStatus.FINISHED, finishedState.getStatus());
        assertFalse(finishedState.isActive());
        assertTrue(finishedState.isFinished());
        for (GameActionType action : GameActionType.values()) {
            assertFalse(finishedState.canAcceptAction(action),
                    "FinishedMatchState should reject action: " + action);
        }
    }
}
