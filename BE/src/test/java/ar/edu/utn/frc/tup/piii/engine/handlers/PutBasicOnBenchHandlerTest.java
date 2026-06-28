package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PutBasicOnBenchHandlerTest {

    @Mock
    private EngineContext ctx;
    @Mock
    private GameState state;
    @Mock
    private CardLookupPort cardLookup;
    @Mock
    private PlayerState player;

    private PutBasicOnBenchHandler handler;
    private UUID playerId;
    private UUID cardInstanceId;
    private CardInstance cardInHand;
    private PokemonCardDefinition basicDef;

    @BeforeEach
    void setUp() {
        handler = new PutBasicOnBenchHandler();
        playerId = UUID.randomUUID();
        cardInstanceId = UUID.randomUUID();
        cardInHand = new CardInstance(cardInstanceId, "pkm-bulbasaur");

        basicDef = new PokemonCardDefinition();
        basicDef.setStage("BASIC");
    }

    private GameAction createAction(Integer handIndex) {
        GameAction action = new GameAction();
        action.setPlayerId(playerId);
        Map<String, Object> payload = new HashMap<>();
        if (handIndex != null) {
            payload.put("handIndex", handIndex);
        }
        action.setPayload(payload);
        return action;
    }

    @Test
    void shouldPlaceBasicPokemonOnBench() {
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(2);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getPlayerId()).thenReturn(playerId);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(cardInHand)));
        when(player.getBench()).thenReturn(new ArrayList<>());
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-bulbasaur")).thenReturn(basicDef);

        handler.handle(ctx, createAction(0));

        assertEquals(1, player.getBench().size());
        PokemonInPlay placed = player.getBench().get(0);
        assertEquals(cardInstanceId, placed.getInstanceId());
        assertEquals("pkm-bulbasaur", placed.getCardDefinitionId());
        assertEquals(playerId, placed.getOwnerPlayerId());
        assertEquals(0, placed.getDamageCounters());
        assertEquals(2, placed.getEnteredTurnNumber());
        assertFalse(placed.isEvolvedThisTurn());
        assertTrue(player.getHand().isEmpty());
    }

    @Test
    void shouldSetErrorWhenHandIndexIsNull() {
        handler.handle(ctx, createAction(null));

        verify(ctx).setError(argThat(e ->
                "INVALID_HAND_INDEX".equals(e.getCode())
        ));
    }

    @Test
    void shouldSetErrorWhenHandIndexOutOfBounds() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(cardInHand)));

        handler.handle(ctx, createAction(5));

        verify(ctx).setError(argThat(e ->
                "INVALID_HAND_INDEX".equals(e.getCode())
        ));
    }

    @Test
    void shouldSetErrorWhenHandIndexNegative() {
        when(ctx.getPlayer(playerId)).thenReturn(player);


        handler.handle(ctx, createAction(-1));

        verify(ctx).setError(argThat(e ->
                "INVALID_HAND_INDEX".equals(e.getCode())
        ));
    }

    @Test
    void shouldSetErrorWhenCardIsNotAPokemon() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(cardInHand)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-bulbasaur"))
                .thenReturn(new TrainerCardDefinition());

        handler.handle(ctx, createAction(0));

        verify(ctx).setError(argThat(e ->
                "NOT_A_POKEMON".equals(e.getCode())
        ));
    }

    @Test
    void shouldSetErrorWhenCardIsNotBasic() {
        basicDef.setStage("STAGE_1");

        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(cardInHand)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-bulbasaur")).thenReturn(basicDef);

        handler.handle(ctx, createAction(0));

        verify(ctx).setError(argThat(e ->
                "NOT_BASIC_POKEMON".equals(e.getCode())
        ));
    }

    @Test
    void shouldSetErrorWhenBenchFull() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(cardInHand)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-bulbasaur")).thenReturn(basicDef);
        when(player.getBench()).thenReturn(new ArrayList<>());
        for (int i = 0; i < 5; i++) {
            player.getBench().add(new PokemonInPlay());
        }

        handler.handle(ctx, createAction(0));

        verify(ctx).setError(argThat(e ->
                "BENCH_FULL".equals(e.getCode())
        ));
    }
}
