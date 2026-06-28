package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardType;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyType;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.energy.EnergyService;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.model.TurnFlags;
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
class AttachEnergyHandlerTest {

    @Mock
    private EngineContext ctx;
    @Mock
    private GameState state;
    @Mock
    private PlayerState player;
    @Mock
    private CardLookupPort cardLookup;
    @Mock
    private EnergyService energyService;

    private AttachEnergyHandler handler;
    private UUID playerId;
    private UUID targetPokemonInstanceId;
    private UUID energyCardInstanceId;
    private CardInstance energyCard;
    private PokemonInPlay targetPokemon;
    private EnergyCardDefinition energyDef;
    private TurnFlags flags;

    @BeforeEach
    void setUp() {
        handler = new AttachEnergyHandler();
        playerId = UUID.randomUUID();
        targetPokemonInstanceId = UUID.randomUUID();
        energyCardInstanceId = UUID.randomUUID();

        energyCard = new CardInstance(energyCardInstanceId, "energy-fire");

        energyDef = new EnergyCardDefinition();
        energyDef.setEnergyCardType(EnergyCardType.BASIC);
        energyDef.setProvides(List.of(EnergyType.FIRE));

        targetPokemon = new PokemonInPlay();
        targetPokemon.setInstanceId(targetPokemonInstanceId);
        targetPokemon.setCardDefinitionId("pkm-charizard");
        targetPokemon.setAttachedEnergies(new ArrayList<>());

        flags = new TurnFlags();
    }

    private GameAction createAction(Integer handIndex, String targetIdStr) {
        GameAction action = new GameAction();
        action.setPlayerId(playerId);
        Map<String, Object> payload = new HashMap<>();
        if (handIndex != null) payload.put("handIndex", handIndex);
        if (targetIdStr != null) payload.put("targetPokemonInstanceId", targetIdStr);
        action.setPayload(payload);
        return action;
    }

    @Test
    void shouldAttachEnergyToPokemon() {
        when(ctx.getState()).thenReturn(state);
        when(state.getTurnFlags()).thenReturn(flags);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(3);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(energyCard)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("energy-fire")).thenReturn(energyDef);

        when(player.getActivePokemon()).thenReturn(targetPokemon);
        when(player.getBench()).thenReturn(new ArrayList<>());
        when(ctx.getEnergyService()).thenReturn(energyService);

        handler.handle(ctx, createAction(0, targetPokemonInstanceId.toString()));

        verify(energyService).attachFromHand(energyCard, targetPokemon, player, ctx);
        assertTrue(flags.hasAttachedEnergy());
        verify(ctx, atLeastOnce()).addEvent(argThat(e ->
                "ENERGY_ATTACHED".equals(e.getType())
        ));
    }

    @Test
    void shouldSetErrorWhenHandIndexIsNull() {
        handler.handle(ctx, createAction(null, targetPokemonInstanceId.toString()));

        verify(ctx).setError(argThat(e ->
                "INVALID_HAND_INDEX".equals(e.getCode())
        ));
    }

    @Test
    void shouldSetErrorWhenTargetIdIsNull() {
        handler.handle(ctx, createAction(0, null));

        verify(ctx).setError(argThat(e ->
                "INVALID_TARGET".equals(e.getCode())
        ));
    }

    @Test
    void shouldSetErrorWhenEnergyAlreadyAttachedThisTurn() {
        flags.setHasAttachedEnergy(true);
        when(ctx.getState()).thenReturn(state);
        when(state.getTurnFlags()).thenReturn(flags);

        handler.handle(ctx, createAction(0, targetPokemonInstanceId.toString()));

        verify(ctx).setError(argThat(e ->
                "ENERGY_ALREADY_ATTACHED".equals(e.getCode())
        ));
    }

    @Test
    void shouldSetErrorWhenHandIndexOutOfBounds() {
        when(ctx.getState()).thenReturn(state);
        when(state.getTurnFlags()).thenReturn(flags);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(energyCard)));

        handler.handle(ctx, createAction(5, targetPokemonInstanceId.toString()));

        verify(ctx).setError(argThat(e ->
                "INVALID_HAND_INDEX".equals(e.getCode())
        ));
    }

    @Test
    void shouldSetErrorWhenCardIsNotEnergy() {
        when(ctx.getState()).thenReturn(state);
        when(state.getTurnFlags()).thenReturn(flags);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(energyCard)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("energy-fire"))
                .thenReturn(new PokemonCardDefinition());

        handler.handle(ctx, createAction(0, targetPokemonInstanceId.toString()));

        verify(ctx).setError(argThat(e ->
                "NOT_AN_ENERGY".equals(e.getCode())
        ));
    }

    @Test
    void shouldSetErrorWhenTargetPokemonNotFound() {
        when(ctx.getState()).thenReturn(state);
        when(state.getTurnFlags()).thenReturn(flags);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(energyCard)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("energy-fire")).thenReturn(energyDef);
        when(player.getActivePokemon()).thenReturn(null);
        when(player.getBench()).thenReturn(new ArrayList<>());

        handler.handle(ctx, createAction(0, targetPokemonInstanceId.toString()));

        verify(ctx).setError(argThat(e ->
                "INVALID_TARGET".equals(e.getCode())
        ));
    }

    @Test
    void shouldFindTargetOnBench() {
        when(ctx.getState()).thenReturn(state);
        when(state.getTurnFlags()).thenReturn(flags);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(3);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(energyCard)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("energy-fire")).thenReturn(energyDef);
        when(player.getActivePokemon()).thenReturn(null);
        when(player.getBench()).thenReturn(new ArrayList<>(List.of(targetPokemon)));
        when(ctx.getEnergyService()).thenReturn(energyService);

        handler.handle(ctx, createAction(0, targetPokemonInstanceId.toString()));

        verify(energyService).attachFromHand(energyCard, targetPokemon, player, ctx);
    }
}
