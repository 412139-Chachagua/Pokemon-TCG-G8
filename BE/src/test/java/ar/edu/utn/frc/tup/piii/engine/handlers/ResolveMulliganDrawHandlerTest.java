package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResolveMulliganDrawHandlerTest {

    @Mock
    private EngineContext ctx;
    @Mock
    private GameState state;
    @Mock
    private PlayerState player;

    private ResolveMulliganDrawHandler handler;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        handler = new ResolveMulliganDrawHandler();
        playerId = UUID.randomUUID();
    }

    private GameAction createAction(boolean drawCards) {
        GameAction action = new GameAction();
        action.setPlayerId(playerId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("drawCards", drawCards);
        action.setPayload(payload);
        return action;
    }

    @Test
    void shouldDrawCardsWhenChoosingYes() {
        CardInstance card1 = new CardInstance(UUID.randomUUID(), "pkm-pikachu");
        List<CardInstance> deck = new ArrayList<>(List.of(card1));
        List<CardInstance> hand = new ArrayList<>();

        when(ctx.getState()).thenReturn(state);
        when(state.hasPendingMulliganDraw(playerId)).thenReturn(true);
        when(state.getMulliganDrawCounts()).thenReturn(Map.of(playerId, 1));
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getDeck()).thenReturn(deck);
        when(player.getHand()).thenReturn(hand);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());

        handler.handle(ctx, createAction(true));

        assertTrue(deck.isEmpty());
        assertEquals(1, hand.size());
        assertSame(card1, hand.get(0));
        verify(state).resolveMulliganDraw(playerId, true);
    }

    @Test
    void shouldNotDrawCardsWhenChoosingNo() {
        when(ctx.getState()).thenReturn(state);
        when(state.hasPendingMulliganDraw(playerId)).thenReturn(true);
        when(state.getMulliganDrawCounts()).thenReturn(Map.of(playerId, 2));
        when(state.getMatchId()).thenReturn(UUID.randomUUID());

        handler.handle(ctx, createAction(false));

        verify(player, never()).getDeck();
        verify(state).resolveMulliganDraw(playerId, false);
    }

    @Test
    void shouldSetErrorWhenNoPendingMulliganDraw() {
        when(ctx.getState()).thenReturn(state);
        when(state.hasPendingMulliganDraw(playerId)).thenReturn(false);

        handler.handle(ctx, createAction(true));

        verify(ctx).setError(argThat(e ->
                "NO_PENDING_MULLIGAN_DRAW".equals(e.getCode())
        ));
    }

    @Test
    void shouldHandleDrawWhenDrawCardsIsFalse() {
        when(ctx.getState()).thenReturn(state);
        when(state.hasPendingMulliganDraw(playerId)).thenReturn(true);
        when(state.getMulliganDrawCounts()).thenReturn(Map.of(playerId, 2));
        when(state.getMatchId()).thenReturn(UUID.randomUUID());

        GameAction action = new GameAction();
        action.setPlayerId(playerId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("drawCards", false);
        action.setPayload(payload);

        handler.handle(ctx, action);

        verify(state).resolveMulliganDraw(playerId, false);
    }

    @Test
    void shouldHandleDrawWhenDrawCardsNotABoolean() {
        when(ctx.getState()).thenReturn(state);
        when(state.hasPendingMulliganDraw(playerId)).thenReturn(true);
        when(state.getMulliganDrawCounts()).thenReturn(Map.of(playerId, 2));
        when(state.getMatchId()).thenReturn(UUID.randomUUID());

        GameAction action = new GameAction();
        action.setPlayerId(playerId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("drawCards", "not_a_boolean");
        action.setPayload(payload);

        handler.handle(ctx, action);

        verify(state).resolveMulliganDraw(playerId, false);
    }
}
