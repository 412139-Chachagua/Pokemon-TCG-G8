package ar.edu.utn.frc.tup.piii.engine.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

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
}
