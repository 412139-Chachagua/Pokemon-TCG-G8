package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
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
class SetupPlaceBenchHandlerTest {

    @Mock
    private EngineContext ctx;
    @Mock
    private GameState state;
    @Mock
    private CardLookupPort cardLookup;
    @Mock
    private PlayerState player;

    private SetupPlaceBenchHandler handler;
    private UUID playerId;
    private UUID cardInstanceId;
    private CardInstance cardInHand;
    private PokemonCardDefinition basicPokemonDef;

    @BeforeEach
    void setUp() {
        handler = new SetupPlaceBenchHandler();
        playerId = UUID.randomUUID();
        cardInstanceId = UUID.randomUUID();
        cardInHand = new CardInstance(cardInstanceId, "pkm-squirtle");

        basicPokemonDef = new PokemonCardDefinition();
        basicPokemonDef.setStage("BASIC");
    }

    private GameAction createAction(String cardInstanceIdStr) {
        GameAction action = new GameAction();
        action.setPlayerId(playerId);
        Map<String, Object> payload = new HashMap<>();
        if (cardInstanceIdStr != null) {
            payload.put("cardInstanceId", cardInstanceIdStr);
        }
        action.setPayload(payload);
        return action;
    }

    @Test
    void shouldPlaceBasicPokemonOnBench() {
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());

        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(cardInHand)));
        when(player.getBench()).thenReturn(new ArrayList<>());
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-squirtle")).thenReturn(basicPokemonDef);
        when(player.getPlayerId()).thenReturn(playerId);
        handler.handle(ctx, createAction(cardInstanceId.toString()));

        assertEquals(1, player.getBench().size());
        PokemonInPlay placed = player.getBench().get(0);
        assertEquals(cardInstanceId, placed.getInstanceId());
        assertEquals("pkm-squirtle", placed.getCardDefinitionId());
        assertEquals(playerId, placed.getOwnerPlayerId());
        assertEquals(0, placed.getDamageCounters());
        assertFalse(placed.isEvolvedThisTurn());
        verify(player).setSetupConfirmed(false);
        assertTrue(player.getHand().isEmpty());
    }

    @Test
    void shouldSetErrorWhenCardInstanceIdIsNull() {
        handler.handle(ctx, createAction(null));

        verify(ctx).setError(argThat(e ->
                "INVALID_PAYLOAD".equals(e.getCode())
        ));
    }

    @Test
    void shouldSetErrorWhenPlayerNotFound() {
        when(ctx.getPlayer(playerId)).thenReturn(null);

        handler.handle(ctx, createAction(cardInstanceId.toString()));

        verify(ctx).setError(argThat(e ->
                "PLAYER_NOT_FOUND".equals(e.getCode())
        ));
    }

    @Test
    void shouldSetErrorWhenBenchFull() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getBench()).thenReturn(new ArrayList<>());
        // Fill bench with 5 pokemon
        for (int i = 0; i < 5; i++) {
            PokemonInPlay p = new PokemonInPlay();
            p.setInstanceId(UUID.randomUUID());
            player.getBench().add(p);
        }

        handler.handle(ctx, createAction(cardInstanceId.toString()));

        verify(ctx).setError(argThat(e ->
                "BENCH_FULL".equals(e.getCode())
        ));
    }

    @Test
    void shouldSetErrorWhenCardNotInHand() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getBench()).thenReturn(new ArrayList<>());
        when(player.getHand()).thenReturn(new ArrayList<>());

        handler.handle(ctx, createAction(cardInstanceId.toString()));

        verify(ctx).setError(argThat(e ->
                "CARD_NOT_IN_HAND".equals(e.getCode())
        ));
    }

    @Test
    void shouldSetErrorWhenCardIsNotAPokemon() {
        CardDefinition trainerDef = new TrainerCardDefinition();

        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getBench()).thenReturn(new ArrayList<>());
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(cardInHand)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-squirtle")).thenReturn(trainerDef);

        handler.handle(ctx, createAction(cardInstanceId.toString()));

        verify(ctx).setError(argThat(e ->
                "NOT_BASIC_POKEMON".equals(e.getCode())
        ));
    }

    @Test
    void shouldSetErrorWhenCardIsNotBasicStage() {
        basicPokemonDef.setStage("STAGE_1");

        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getBench()).thenReturn(new ArrayList<>());
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(cardInHand)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-squirtle")).thenReturn(basicPokemonDef);

        handler.handle(ctx, createAction(cardInstanceId.toString()));

        verify(ctx).setError(argThat(e ->
                "NOT_BASIC_POKEMON".equals(e.getCode())
        ));
    }

    @Test
    void shouldPlacePokemonWithNullStageOnBench() {
        basicPokemonDef.setStage(null);

        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        //when(state.getTurnNumber()).thenReturn(0);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(cardInHand)));
        when(player.getBench()).thenReturn(new ArrayList<>());
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-squirtle")).thenReturn(basicPokemonDef);
        lenient().when(player.getPlayerId()).thenReturn(UUID.randomUUID());
        handler.handle(ctx, createAction(cardInstanceId.toString()));

        assertEquals(1, player.getBench().size());
    }
}
