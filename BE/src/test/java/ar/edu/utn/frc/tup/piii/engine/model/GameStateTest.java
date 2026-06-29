package ar.edu.utn.frc.tup.piii.engine.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import ar.edu.utn.frc.tup.piii.engine.MatchStatus;
import ar.edu.utn.frc.tup.piii.engine.turn.TurnPhase;
import ar.edu.utn.frc.tup.piii.engine.match.states.ActiveMatchState;
import ar.edu.utn.frc.tup.piii.engine.match.states.FinishedMatchState;
import ar.edu.utn.frc.tup.piii.engine.match.states.MatchState;
import ar.edu.utn.frc.tup.piii.engine.match.states.SetupMatchState;
import ar.edu.utn.frc.tup.piii.engine.match.states.WaitingMatchState;
import ar.edu.utn.frc.tup.piii.engine.turn.states.TurnState;
import ar.edu.utn.frc.tup.piii.engine.turn.states.AttackTurnState;
import ar.edu.utn.frc.tup.piii.engine.turn.states.BetweenTurnsTurnState;
import ar.edu.utn.frc.tup.piii.engine.turn.states.DrawTurnState;
import ar.edu.utn.frc.tup.piii.engine.turn.states.MainTurnState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GameStateTest {

    private GameState state;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        state = new GameState();
        playerId = UUID.randomUUID();
    }

    @Test
    void shouldTrackFirstTurnCompletion() {
        assertFalse(state.hasPlayerCompletedFirstTurn(playerId));
        state.markPlayerCompletedFirstTurn(playerId);
        assertTrue(state.hasPlayerCompletedFirstTurn(playerId));
    }

    @Test
    void shouldTrackFirstTurnPerPlayer() {
        UUID p2 = UUID.randomUUID();
        state.markPlayerCompletedFirstTurn(playerId);
        assertTrue(state.hasPlayerCompletedFirstTurn(playerId));
        assertFalse(state.hasPlayerCompletedFirstTurn(p2));
    }

    @Test
    void shouldManageKOReplacementFlags() {
        assertFalse(state.isPendingKOReplacement());
        assertNull(state.getKnockedOutPlayerId());

        state.setPendingKOReplacement(true);
        state.setKnockedOutPlayerId(playerId);

        assertTrue(state.isPendingKOReplacement());
        assertEquals(playerId, state.getKnockedOutPlayerId());
    }

    @Test
    void shouldManageSuddenDeathFlag() {
        assertFalse(state.isSuddenDeath());
        state.setSuddenDeath(true);
        assertTrue(state.isSuddenDeath());
    }

    @Test
    void shouldManagePrizeCountPerPlayer() {
        assertEquals(0, state.getPrizeCountPerPlayer());
        state.setPrizeCountPerPlayer(1);
        assertEquals(1, state.getPrizeCountPerPlayer());
        state.setPrizeCountPerPlayer(6);
        assertEquals(6, state.getPrizeCountPerPlayer());
    }

    @Test
    void shouldManagePlayerDeckIds() {
        UUID deckId = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        UUID deckId2 = UUID.randomUUID();
        assertNull(state.getPlayerDeckIds());

        state.addPlayerDeckId(playerId, deckId);
        state.addPlayerDeckId(p2, deckId2);

        assertEquals(2, state.getPlayerDeckIds().size());
        assertEquals(deckId, state.getPlayerDeckIds().get(playerId));
        assertEquals(deckId2, state.getPlayerDeckIds().get(p2));
    }

    @Test
    void shouldManageFinishReason() {
        assertNull(state.getFinishReason());
        state.setFinishReason(ar.edu.utn.frc.tup.piii.engine.victory.FinishReason.SUDDEN_DEATH);
        assertEquals(ar.edu.utn.frc.tup.piii.engine.victory.FinishReason.SUDDEN_DEATH, state.getFinishReason());
    }

    @Test
    void fromStatus_returnsWaitingMatchState_whenStatusIsWaiting() {
        MatchState matchState = GameState.fromStatus(MatchStatus.WAITING);
        assertInstanceOf(WaitingMatchState.class, matchState);
    }

    @Test
    void fromStatus_returnsSetupMatchState_whenStatusIsSetup() {
        MatchState matchState = GameState.fromStatus(MatchStatus.SETUP);
        assertInstanceOf(SetupMatchState.class, matchState);
    }

    @Test
    void fromStatus_returnsActiveMatchState_whenStatusIsActive() {
        MatchState matchState = GameState.fromStatus(MatchStatus.ACTIVE);
        assertInstanceOf(ActiveMatchState.class, matchState);
    }

    @Test
    void fromStatus_returnsFinishedMatchState_whenStatusIsFinished() {
        MatchState matchState = GameState.fromStatus(MatchStatus.FINISHED);
        assertInstanceOf(FinishedMatchState.class, matchState);
    }

    @Test
    void fromStatus_returnsWaitingMatchState_whenStatusIsNull() {
        MatchState matchState = GameState.fromStatus(null);
        assertInstanceOf(WaitingMatchState.class, matchState);
    }

    @Test
    void fromPhase_returnsDrawTurnState_whenPhaseIsDraw() {
        TurnState turnState = GameState.fromPhase(TurnPhase.DRAW);
        assertInstanceOf(DrawTurnState.class, turnState);
    }

    @Test
    void fromPhase_returnsMainTurnState_whenPhaseIsMain() {
        TurnState turnState = GameState.fromPhase(TurnPhase.MAIN);
        assertInstanceOf(MainTurnState.class, turnState);
    }

    @Test
    void fromPhase_returnsAttackTurnState_whenPhaseIsAttack() {
        TurnState turnState = GameState.fromPhase(TurnPhase.ATTACK);
        assertInstanceOf(AttackTurnState.class, turnState);
    }

    @Test
    void fromPhase_returnsBetweenTurnsTurnState_whenPhaseIsBetweenTurns() {
        TurnState turnState = GameState.fromPhase(TurnPhase.BETWEEN_TURNS);
        assertInstanceOf(BetweenTurnsTurnState.class, turnState);
    }

    @Test
    void fromPhase_returnsDrawTurnState_whenPhaseIsNull() {
        TurnState turnState = GameState.fromPhase(null);
        assertInstanceOf(DrawTurnState.class, turnState);
    }

    @Test
    void hasPlayerCompletedFirstTurn_nullPlayerId_returnsFalse() {
        assertFalse(state.hasPlayerCompletedFirstTurn(null));
    }

    @Test
    void hasPlayerCompletedFirstTurn_nonExistentPlayerId_returnsFalse() {
        assertFalse(state.hasPlayerCompletedFirstTurn(UUID.randomUUID()));
    }

    @Test
    void isMulliganFullyResolved_noPendingDraw_returnsTrue() {
        assertTrue(state.isMulliganFullyResolved());
    }

    @Test
    void isMulliganFullyResolved_pendingDrawNotResolved_returnsTrueWhenAllResolved() {
        state.setMulliganDrawPending(true);
        Map<UUID, Integer> counts = new HashMap<>();
        counts.put(playerId, 1);
        state.setMulliganDrawCounts(counts);
        Set<UUID> resolved = new HashSet<>();
        resolved.add(playerId);
        state.setMulliganDrawResolved(resolved);
        assertTrue(state.isMulliganFullyResolved());
    }

    @Test
    void isMulliganFullyResolved_pendingDrawNotResolved_returnsFalseWhenNotAllResolved() {
        UUID p2 = UUID.randomUUID();
        state.setMulliganDrawPending(true);
        Map<UUID, Integer> counts = new HashMap<>();
        counts.put(playerId, 1);
        counts.put(p2, 1);
        state.setMulliganDrawCounts(counts);
        Set<UUID> resolved = new HashSet<>();
        resolved.add(playerId);
        state.setMulliganDrawResolved(resolved);
        assertFalse(state.isMulliganFullyResolved());
    }

    @Test
    void isMulliganFullyResolved_emptyCounts_returnsTrue() {
        state.setMulliganDrawPending(true);
        state.setMulliganDrawCounts(new HashMap<>());
        assertTrue(state.isMulliganFullyResolved());
    }

    @Test
    void getMatchState_returnsCorrectStateForStatus() {
        state.setStatus(MatchStatus.SETUP);
        assertInstanceOf(SetupMatchState.class, state.getMatchState());
    }

    @Test
    void getTurnState_returnsCorrectStateForPhase() {
        state.setPhase(TurnPhase.ATTACK);
        assertInstanceOf(AttackTurnState.class, state.getTurnState());
    }

    @Test
    void hasPendingInitialMulligan_noPendingPlayers_returnsFalse() {
        assertFalse(state.hasPendingInitialMulligan());
    }

    @Test
    void hasPendingInitialMulligan_withPendingPlayers_returnsTrue() {
        Set<UUID> pending = new HashSet<>();
        pending.add(playerId);
        state.setPendingInitialMulliganPlayers(pending);
        assertTrue(state.hasPendingInitialMulligan());
    }

    @Test
    void hasPendingInitialMulligan_forSpecificPlayer_returnsCorrectResult() {
        Set<UUID> pending = new HashSet<>();
        pending.add(playerId);
        state.setPendingInitialMulliganPlayers(pending);
        assertTrue(state.hasPendingInitialMulligan(playerId));
        assertFalse(state.hasPendingInitialMulligan(UUID.randomUUID()));
    }

    @Test
    void resolveInitialMulligan_removesPlayerFromPending() {
        Set<UUID> pending = new HashSet<>();
        pending.add(playerId);
        state.setPendingInitialMulliganPlayers(pending);
        state.resolveInitialMulligan(playerId);
        assertFalse(state.hasPendingInitialMulligan(playerId));
    }

    @Test
    void hasPendingMulliganDraw_withoutSetup_returnsFalse() {
        assertFalse(state.hasPendingMulliganDraw(playerId));
    }

    @Test
    void hasPendingMulliganDraw_withResolvedPlayer_returnsFalse() {
        state.setMulliganDrawPending(true);
        Map<UUID, Integer> counts = new HashMap<>();
        counts.put(playerId, 1);
        state.setMulliganDrawCounts(counts);
        Set<UUID> resolved = new HashSet<>();
        resolved.add(playerId);
        state.setMulliganDrawResolved(resolved);
        assertFalse(state.hasPendingMulliganDraw(playerId));
    }

    @Test
    void resolveMulliganDraw_withAllResolved_clearsPending() {
        state.setMulliganDrawPending(true);
        Map<UUID, Integer> counts = new HashMap<>();
        counts.put(playerId, 1);
        state.setMulliganDrawCounts(counts);
        state.resolveMulliganDraw(playerId, true);
        assertFalse(state.isMulliganDrawPending());
    }
}
