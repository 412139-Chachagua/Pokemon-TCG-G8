package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.action.GameError;
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
class SetupPlaceActiveHandlerTest {

    @Mock
    private EngineContext ctx;
    @Mock
    private GameState state;
    @Mock
    private CardLookupPort cardLookup;
    @Mock
    private PlayerState player;

    private SetupPlaceActiveHandler handler;
    private UUID playerId;
    private UUID cardInstanceId;
    private CardInstance cardInHand;
    private PokemonCardDefinition basicPokemonDef;

    @BeforeEach
    void setUp() {
        handler = new SetupPlaceActiveHandler();
        playerId = UUID.randomUUID();
        cardInstanceId = UUID.randomUUID();

        cardInHand = new CardInstance(cardInstanceId, "pkm-bulbasaur");

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
    void shouldPlaceBasicPokemonAsActive() {
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(cardInHand)));
        when(player.getActivePokemon()).thenReturn(null);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-bulbasaur")).thenReturn(basicPokemonDef);
        when(player.getPlayerId()).thenReturn(playerId);
        handler.handle(ctx, createAction(cardInstanceId.toString()));

        verify(player).setActivePokemon(argThat(pkm ->
                pkm.getInstanceId().equals(cardInstanceId)
                        && pkm.getCardDefinitionId().equals("pkm-bulbasaur")
                        && pkm.getOwnerPlayerId().equals(playerId)
                        && pkm.getDamageCounters() == 0
                        && !pkm.isEvolvedThisTurn()
                        && pkm.getEnteredTurnNumber() == 0
        ));
        verify(player).setSetupConfirmed(false);
        assertTrue(player.getHand().isEmpty());
    }

    @Test
    void shouldPlacePokemonWithNullStageAsActive() {
        basicPokemonDef.setStage(null);

        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(cardInHand)));
        when(player.getActivePokemon()).thenReturn(null);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-bulbasaur")).thenReturn(basicPokemonDef);
        when(player.getPlayerId()).thenReturn(playerId);

        handler.handle(ctx, createAction(cardInstanceId.toString()));

        verify(player).setActivePokemon(any());
    }

    @Test
    void shouldSetErrorWhenCardInstanceIdIsNull() {
        handler.handle(ctx, createAction(null));

        verify(ctx).setError(argThat(e ->
                "INVALID_PAYLOAD".equals(e.getCode())
        ));
        verify(player, never()).setActivePokemon(any());
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
    void shouldSetErrorWhenActiveAlreadySet() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getActivePokemon()).thenReturn(new PokemonInPlay());

        handler.handle(ctx, createAction(cardInstanceId.toString()));

        verify(ctx).setError(argThat(e ->
                "ACTIVE_ALREADY_SET".equals(e.getCode())
        ));
    }

    @Test
    void shouldSetErrorWhenCardNotInHand() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getActivePokemon()).thenReturn(null);
        when(player.getHand()).thenReturn(new ArrayList<>());

        handler.handle(ctx, createAction(cardInstanceId.toString()));

        verify(ctx).setError(argThat(e ->
                "CARD_NOT_IN_HAND".equals(e.getCode())
        ));
    }

    @Test
    void shouldSetErrorWhenCardIsNotAPokemon() {
        CardDefinition trainerDef = new TrainerCardDefinition();
        trainerDef.setId("trainer-1");

        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getActivePokemon()).thenReturn(null);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(cardInHand)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-bulbasaur")).thenReturn(trainerDef);

        handler.handle(ctx, createAction(cardInstanceId.toString()));

        verify(ctx).setError(argThat(e ->
                "NOT_BASIC_POKEMON".equals(e.getCode())
        ));
    }

    @Test
    void shouldSetErrorWhenCardIsNotBasicStage() {
        basicPokemonDef.setStage("STAGE_1");

        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getActivePokemon()).thenReturn(null);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(cardInHand)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-bulbasaur")).thenReturn(basicPokemonDef);

        handler.handle(ctx, createAction(cardInstanceId.toString()));

        verify(ctx).setError(argThat(e ->
                "NOT_BASIC_POKEMON".equals(e.getCode())
        ));
    }
}
