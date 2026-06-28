package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.MatchStatus;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.model.*;
import ar.edu.utn.frc.tup.piii.engine.turn.TurnManager;
import ar.edu.utn.frc.tup.piii.engine.turn.TurnPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DrawCardHandlerTest {

    @Mock
    private EngineContext ctx;
    @Mock
    private GameState state;
    @Mock
    private PlayerState player;
    @Mock
    private TurnManager turnManager;

    private DrawCardHandler handler;
    private UUID playerId;
    private UUID firstPlayerId;
    private TurnFlags flags;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        firstPlayerId = UUID.randomUUID();
        flags = new TurnFlags();
        handler = new DrawCardHandler(turnManager);
    }

    private GameAction createAction() {
        GameAction action = new GameAction();
        action.setPlayerId(playerId);
        action.setPayload(new HashMap<>());
        return action;
    }

    @Test
    void shouldDrawCardFromDeck() {
        CardInstance card = new CardInstance(UUID.randomUUID(), "pkm-pikachu");
        List<CardInstance> deck = new ArrayList<>(List.of(card));
        List<CardInstance> hand = new ArrayList<>();

        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(state.getCurrentPlayerId()).thenReturn(playerId);
        when(state.getFirstPlayerId()).thenReturn(firstPlayerId);
        when(state.getTurnNumber()).thenReturn(2);
        when(state.getTurnFlags()).thenReturn(flags);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(player.getDeck()).thenReturn(deck);
        when(player.getHand()).thenReturn(hand);

        handler.handle(ctx, createAction());

        assertTrue(deck.isEmpty());
        assertEquals(1, hand.size());
        assertSame(card, hand.get(0));
        assertTrue(flags.hasDrawnForTurn());
        verify(turnManager).advancePhase(state);
    }

    @Test
    void shouldBlockDrawForFirstPlayerOnTurn1() {
        when(ctx.getState()).thenReturn(state);
        when(state.getCurrentPlayerId()).thenReturn(playerId);
        when(state.getFirstPlayerId()).thenReturn(playerId);
        when(state.getTurnNumber()).thenReturn(1);
        when(state.getTurnFlags()).thenReturn(flags);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());

        handler.handle(ctx, createAction());

        assertTrue(flags.hasDrawnForTurn());
        verify(state).setPhase(TurnPhase.MAIN);
        verify(player, never()).getDeck();
    }

    @Test
    void shouldHandleEmptyDeckWithVictory() {
        UUID otherPlayerId = UUID.randomUUID();

        PlayerState p1 = new PlayerState();
        p1.setPlayerId(playerId);
        p1.setDeck(new ArrayList<>());
        p1.setHand(new ArrayList<>());
        p1.setBench(new ArrayList<>());
        p1.setActivePokemon(new PokemonInPlay());
        p1.setPrizes(new ArrayList<>(List.of(
                new CardInstance(UUID.randomUUID(), "prize-1")
        )));

        PlayerState p2 = new PlayerState();
        p2.setPlayerId(otherPlayerId);
        p2.setDeck(new ArrayList<>(List.of(
                new CardInstance(UUID.randomUUID(), "pkm-other")
        )));
        p2.setHand(new ArrayList<>());
        p2.setBench(new ArrayList<>());
        p2.setActivePokemon(new PokemonInPlay());
        p2.setPrizes(new ArrayList<>(List.of(
                new CardInstance(UUID.randomUUID(), "prize-2")
        )));

        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(state.getCurrentPlayerId()).thenReturn(playerId);
        when(state.getFirstPlayerId()).thenReturn(firstPlayerId);
        when(state.getTurnNumber()).thenReturn(2);
        when(state.getTurnFlags()).thenReturn(flags);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(player.getDeck()).thenReturn(new ArrayList<>());
        when(state.getPlayers()).thenReturn(new PlayerState[]{p1, p2});

        handler.handle(ctx, createAction());

        assertTrue(flags.hasDrawnForTurn());
        verify(state).setStatus(MatchStatus.FINISHED);
        verify(state).setWinnerPlayerId(otherPlayerId);
    }

    @Test
    void shouldHandleNullDeck() {
        UUID otherPlayerId = UUID.randomUUID();

        PlayerState p1 = new PlayerState();
        p1.setPlayerId(playerId);
        p1.setDeck(new ArrayList<>());
        p1.setHand(new ArrayList<>());
        p1.setBench(new ArrayList<>());
        p1.setActivePokemon(new PokemonInPlay());
        p1.setPrizes(new ArrayList<>(List.of(
                new CardInstance(UUID.randomUUID(), "prize-1")
        )));

        PlayerState p2 = new PlayerState();
        p2.setPlayerId(otherPlayerId);
        p2.setDeck(new ArrayList<>(List.of(
                new CardInstance(UUID.randomUUID(), "pkm-other")
        )));
        p2.setHand(new ArrayList<>());
        p2.setBench(new ArrayList<>());
        p2.setActivePokemon(new PokemonInPlay());
        p2.setPrizes(new ArrayList<>(List.of(
                new CardInstance(UUID.randomUUID(), "prize-2")
        )));

        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(state.getCurrentPlayerId()).thenReturn(playerId);
        when(state.getFirstPlayerId()).thenReturn(firstPlayerId);
        when(state.getTurnFlags()).thenReturn(flags);
        when(player.getDeck()).thenReturn(null);
        when(state.getPlayers()).thenReturn(new PlayerState[]{p1, p2});

        handler.handle(ctx, createAction());

        assertTrue(flags.hasDrawnForTurn());
        verify(state).setStatus(MatchStatus.FINISHED);
        verify(state).setWinnerPlayerId(otherPlayerId);
    }

    private PlayerState createPlayerState(UUID id) {
        PlayerState ps = new PlayerState();
        ps.setPlayerId(id);
        ps.setDeck(new ArrayList<>());
        ps.setHand(new ArrayList<>());
        ps.setBench(new ArrayList<>());
        ps.setPrizes(new ArrayList<>());
        return ps;
    }
}
