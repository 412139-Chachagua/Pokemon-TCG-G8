package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import ar.edu.utn.frc.tup.piii.engine.ports.EventPublisherPort;
import ar.edu.utn.frc.tup.piii.engine.ports.RandomizerPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResolveInitialMulliganHandlerTest {

    @Mock
    private EngineContext ctx;
    @Mock
    private GameState state;
    @Mock
    private PlayerState player;
    @Mock
    private CardLookupPort cardLookup;
    @Mock
    private RandomizerPort randomizer;
    @Mock
    private EventPublisherPort eventPublisher;

    private ResolveInitialMulliganHandler handler;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        handler = new ResolveInitialMulliganHandler();
        playerId = UUID.randomUUID();
    }

    private GameAction createAction(String decision) {
        GameAction action = new GameAction();
        action.setPlayerId(playerId);
        Map<String, Object> payload = new HashMap<>();
        if (decision != null) payload.put("decision", decision);
        action.setPayload(payload);
        return action;
    }

    @Test
    void shouldHandleMulliganDecision() {
        CardInstance basicCard = new CardInstance(UUID.randomUUID(), "pkm-pikachu");
        List<CardInstance> hand = new ArrayList<>(List.of(basicCard));
        List<CardInstance> deck = new ArrayList<>();

        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getPlayerId()).thenReturn(playerId);
        when(state.hasPendingInitialMulligan(playerId)).thenReturn(true);
        when(player.isInitialMulliganResolved()).thenReturn(false);
        when(player.getHand()).thenReturn(hand);
        when(player.getDeck()).thenReturn(deck);
        when(ctx.getRandomizer()).thenReturn(randomizer);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-pikachu"))
                .thenReturn(new PokemonCardDefinition());
        when(ctx.getEventPublisher()).thenReturn(eventPublisher);
        when(state.getHandSize()).thenReturn(7);
        when(state.hasPendingInitialMulligan()).thenReturn(true);

        handler.handle(ctx, createAction("MULLIGAN"));

        verify(player).setInitialMulliganResolved(true);
        verify(player).setMulliganCount(1);
        verify(randomizer).shuffle(deck);
    }

    @Test
    void shouldHandleKeepDecision() {
        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getPlayerId()).thenReturn(playerId);
        when(state.hasPendingInitialMulligan(playerId)).thenReturn(true);
        when(player.isInitialMulliganResolved()).thenReturn(false);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());

        when(state.hasPendingInitialMulligan()).thenReturn(true);

        handler.handle(ctx, createAction("KEEP"));

        verify(player).setInitialMulliganResolved(true);
        verify(state).resolveInitialMulligan(playerId);
    }

    @Test
    void shouldSetErrorWhenPlayerNotFound() {
        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(playerId)).thenReturn(null);

        handler.handle(ctx, createAction("MULLIGAN"));

        verify(ctx).setError(argThat(e ->
                "PLAYER_NOT_FOUND".equals(e.getCode())
        ));
    }

    @Test
    void shouldSetErrorWhenNoPendingMulligan() {
        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(state.hasPendingInitialMulligan(playerId)).thenReturn(false);

        handler.handle(ctx, createAction("MULLIGAN"));

        verify(ctx).setError(argThat(e ->
                "NO_PENDING_MULLIGAN".equals(e.getCode())
        ));
    }

    @Test
    void shouldSetErrorWhenAlreadyResolved() {
        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(state.hasPendingInitialMulligan(playerId)).thenReturn(true);
        when(player.isInitialMulliganResolved()).thenReturn(true);

        handler.handle(ctx, createAction("MULLIGAN"));

        verify(ctx).setError(argThat(e ->
                "ALREADY_RESOLVED".equals(e.getCode())
        ));
    }

    @Test
    void shouldSetErrorWhenInvalidDecision() {
        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(state.hasPendingInitialMulligan(playerId)).thenReturn(true);
        when(player.isInitialMulliganResolved()).thenReturn(false);

        handler.handle(ctx, createAction("INVALID"));

        verify(ctx).setError(argThat(e ->
                "INVALID_DECISION".equals(e.getCode())
        ));
    }

    @Test
    void shouldNotResolveWhenMulliganStillNoBasic() {
        CardInstance nonBasic = new CardInstance(UUID.randomUUID(), "trainer-1");
        List<CardInstance> hand = new ArrayList<>(List.of(nonBasic));
        List<CardInstance> deck = new ArrayList<>();

        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getPlayerId()).thenReturn(playerId);
        when(state.hasPendingInitialMulligan(playerId)).thenReturn(true);
        when(player.isInitialMulliganResolved()).thenReturn(false);
        when(player.getHand()).thenReturn(hand);
        when(player.getDeck()).thenReturn(deck);
        when(ctx.getRandomizer()).thenReturn(randomizer);

        when(ctx.getEventPublisher()).thenReturn(eventPublisher);
        when(state.hasPendingInitialMulligan()).thenReturn(true);

        handler.handle(ctx, createAction("MULLIGAN"));

        // Still not resolved because no basic pokemon in new hand
        verify(player, never()).setInitialMulliganResolved(true);
    }
}
