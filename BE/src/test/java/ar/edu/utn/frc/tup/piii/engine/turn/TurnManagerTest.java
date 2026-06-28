package ar.edu.utn.frc.tup.piii.engine.turn;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.model.TurnFlags;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import ar.edu.utn.frc.tup.piii.engine.ports.RandomizerPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TurnManagerTest {

    @Mock
    private RandomizerPort randomizer;
    @Mock
    private CardLookupPort cardLookup;
    @Mock
    private EngineContext ctx;

    private TurnManager turnManager;
    private GameState state;
    private UUID playerId;
    private UUID anotherPlayerId;

    @BeforeEach
    void setUp() {
        turnManager = new TurnManager(randomizer);
        playerId = UUID.randomUUID();
        anotherPlayerId = UUID.randomUUID();

        state = new GameState();
        state.setCurrentPlayerId(playerId);
        state.setFirstPlayerId(playerId);
        state.setTurnFlags(new TurnFlags());

        PlayerState p1 = new PlayerState();
        p1.setPlayerId(playerId);
        p1.setActivePokemon(new PokemonInPlay());
        p1.getActivePokemon().setInstanceId(UUID.randomUUID());
        p1.setDeck(new ArrayList<>(List.of(new CardInstance(UUID.randomUUID(), "card-1"))));

        PlayerState p2 = new PlayerState();
        p2.setPlayerId(anotherPlayerId);
        p2.setActivePokemon(new PokemonInPlay());
        p2.getActivePokemon().setInstanceId(UUID.randomUUID());
        p2.setDeck(new ArrayList<>(List.of(new CardInstance(UUID.randomUUID(), "card-2"))));

        state.setPlayers(new PlayerState[]{p1, p2});
        state.setTurnNumber(1);
        state.setPhase(TurnPhase.DRAW);

        when(ctx.getState()).thenReturn(state);
        lenient().when(ctx.getRandomizer()).thenReturn(randomizer);
        lenient().when(ctx.getCardLookup()).thenReturn(cardLookup);
    }

    @Test
    void shouldResetPendingPrizeOwnerOnStartTurn() {
        state.setPendingPrizeOwnerPlayerId(playerId);
        state.setPendingPrizeCount(2);

        turnManager.startTurn(ctx);

        assertNull(state.getPendingPrizeOwnerPlayerId());
        assertEquals(0, state.getPendingPrizeCount());
    }

    @Test
    void shouldResetTurnFlagsOnStartTurn() {
        TurnFlags flags = state.getTurnFlags();
        flags.setHasDrawnForTurn(true);
        flags.setHasAttachedEnergy(true);
        flags.setHasRetreated(true);
        flags.setHasPlayedSupporter(true);
        flags.setHasPlayedStadium(true);
        flags.setHasAttacked(true);

        turnManager.startTurn(ctx);

        assertFalse(flags.hasDrawnForTurn());
        assertFalse(flags.hasAttachedEnergy());
        assertFalse(flags.hasRetreated());
        assertFalse(flags.hasPlayedSupporter());
        assertFalse(flags.hasPlayedStadium());
        assertFalse(flags.hasAttacked());
    }

    @Test
    void shouldSkipAutoDrawWhenKOPendingForCurrentPlayer() {
        state.setPendingKOReplacement(true);
        state.setKnockedOutPlayerId(playerId);
        state.setFirstPlayerId(anotherPlayerId);
        state.setTurnNumber(3);

        PlayerState current = state.getPlayers()[0];
        current.setDeck(new ArrayList<>(List.of(new CardInstance(UUID.randomUUID(), "deck-card"))));

        turnManager.startTurn(ctx);

        assertEquals(1, current.getDeck().size());
    }

    @Test
    void shouldNotAutoDrawOnFirstTurnOfFirstPlayer() {
        state.setFirstPlayerId(playerId);
        state.setTurnNumber(1);

        PlayerState current = state.getPlayers()[0];
        current.setDeck(new ArrayList<>(List.of(new CardInstance(UUID.randomUUID(), "deck-card"))));

        turnManager.startTurn(ctx);

        assertEquals(1, current.getDeck().size());
    }

    @Test
    void shouldMarkFirstTurnCompletionOnEndTurn() {
        assertFalse(state.hasPlayerCompletedFirstTurn(playerId));

        turnManager.endTurn(ctx);

        assertTrue(state.hasPlayerCompletedFirstTurn(playerId));
    }

    @Test
    void shouldSwitchToOtherPlayerOnEndTurn() {
        state.setTurnNumber(1);

        turnManager.endTurn(ctx);

        assertEquals(anotherPlayerId, state.getCurrentPlayerId());
        assertEquals(2, state.getTurnNumber());
        assertEquals(TurnPhase.DRAW, state.getPhase());
    }

    @Test
    void shouldReturnEarlyFromEndTurnWhenMatchFinished() {
        ar.edu.utn.frc.tup.piii.engine.MatchStatus finished =
                ar.edu.utn.frc.tup.piii.engine.MatchStatus.FINISHED;
        state.setStatus(finished);

        turnManager.endTurn(ctx);

        assertEquals(1, state.getTurnNumber());
        assertEquals(playerId, state.getCurrentPlayerId());
    }
}
