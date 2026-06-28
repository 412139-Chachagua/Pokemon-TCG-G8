package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
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
class EvolvePokemonHandlerTest {

    @Mock
    private EngineContext ctx;
    @Mock
    private GameState state;
    @Mock
    private CardLookupPort cardLookup;
    @Mock
    private PlayerState player;

    private EvolvePokemonHandler handler;
    private UUID playerId;
    private UUID targetInstanceId;
    private UUID evolutionInstanceId;
    private CardInstance evolutionCard;
    private PokemonCardDefinition basicDef;
    private PokemonCardDefinition stage1Def;
    private PokemonInPlay targetPokemon;

    @BeforeEach
    void setUp() {
        handler = new EvolvePokemonHandler();
        playerId = UUID.randomUUID();
        targetInstanceId = UUID.randomUUID();
        evolutionInstanceId = UUID.randomUUID();

        basicDef = new PokemonCardDefinition();
        basicDef.setName("Pikachu");
        basicDef.setStage("BASIC");

        stage1Def = new PokemonCardDefinition();
        stage1Def.setName("Raichu");
        stage1Def.setStage("STAGE_1");
        stage1Def.setEvolvesFrom("Pikachu");

        evolutionCard = new CardInstance(evolutionInstanceId, "pkm-raichu");

        targetPokemon = new PokemonInPlay();
        targetPokemon.setInstanceId(targetInstanceId);
        targetPokemon.setCardDefinitionId("pkm-pikachu");
        targetPokemon.setAttachedEnergies(new ArrayList<>());
        targetPokemon.setDamageCounters(2);
        targetPokemon.setSpecialConditions(new ArrayList<>());
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
    void shouldEvolveBasicToStage1() {
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(3);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(evolutionCard)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-raichu")).thenReturn(stage1Def);
        when(cardLookup.getCardById("pkm-pikachu")).thenReturn(basicDef);
        when(player.getActivePokemon()).thenReturn(targetPokemon);
        when(player.getBench()).thenReturn(new ArrayList<>());

        handler.handle(ctx, createAction(0, targetInstanceId.toString()));

        verify(player).setActivePokemon(argThat(evolved ->
                evolved.getInstanceId().equals(evolutionInstanceId)
                        && evolved.getCardDefinitionId().equals("pkm-raichu")
                        && evolved.getDamageCounters() == 2
                        && evolved.isEvolvedThisTurn()
                        && evolved.getEnteredTurnNumber() == 3
        ));
        assertTrue(player.getHand().isEmpty());
    }

    @Test
    void shouldEvolveOnBench() {
        PokemonInPlay benchTarget = new PokemonInPlay();
        benchTarget.setInstanceId(targetInstanceId);
        benchTarget.setCardDefinitionId("pkm-pikachu");
        benchTarget.setAttachedEnergies(new ArrayList<>());

        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(3);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(evolutionCard)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-raichu")).thenReturn(stage1Def);
        when(cardLookup.getCardById("pkm-pikachu")).thenReturn(basicDef);
        when(player.getActivePokemon()).thenReturn(null);
        when(player.getBench()).thenReturn(new ArrayList<>(List.of(benchTarget)));

        handler.handle(ctx, createAction(0, targetInstanceId.toString()));

        PokemonInPlay evolved = player.getBench().get(0);
        assertEquals(evolutionInstanceId, evolved.getInstanceId());
        assertEquals("pkm-raichu", evolved.getCardDefinitionId());
    }

    @Test
    void shouldDoNothingWhenHandIndexIsNull() {
        handler.handle(ctx, createAction(null, targetInstanceId.toString()));

        verifyNoInteractions(player, cardLookup);
    }

    @Test
    void shouldDoNothingWhenTargetIdIsNull() {
        handler.handle(ctx, createAction(0, null));

        verifyNoInteractions(player, cardLookup);
    }

    @Test
    void shouldDoNothingWhenHandIndexOutOfBounds() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(evolutionCard)));

        handler.handle(ctx, createAction(5, targetInstanceId.toString()));

        verify(player, never()).setActivePokemon(any());
    }

    @Test
    void shouldDoNothingWhenEvolutionDefIsNull() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(evolutionCard)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-raichu")).thenReturn(null);

        handler.handle(ctx, createAction(0, targetInstanceId.toString()));

        verify(player, never()).setActivePokemon(any());
    }

    @Test
    void shouldDoNothingWhenTargetNotFound() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(evolutionCard)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-raichu")).thenReturn(stage1Def);
        when(player.getActivePokemon()).thenReturn(null);
        when(player.getBench()).thenReturn(new ArrayList<>());

        handler.handle(ctx, createAction(0, targetInstanceId.toString()));

        verify(player, never()).setActivePokemon(any());
    }

    @Test
    void shouldDoNothingWhenTargetDefIsNull() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(evolutionCard)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-raichu")).thenReturn(stage1Def);
        when(cardLookup.getCardById("pkm-pikachu")).thenReturn(null);
        when(player.getActivePokemon()).thenReturn(targetPokemon);

        handler.handle(ctx, createAction(0, targetInstanceId.toString()));

        verify(player, never()).setActivePokemon(any());
    }

    @Test
    void shouldDoNothingWhenEvolvesFromDoesNotMatch() {
        stage1Def.setEvolvesFrom("SomethingElse");

        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(evolutionCard)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-raichu")).thenReturn(stage1Def);
        when(cardLookup.getCardById("pkm-pikachu")).thenReturn(basicDef);
        when(player.getActivePokemon()).thenReturn(targetPokemon);

        handler.handle(ctx, createAction(0, targetInstanceId.toString()));

        verify(player, never()).setActivePokemon(any());
    }

    @Test
    void shouldDoNothingWhenTargetAlreadyEvolvedThisTurn() {
        targetPokemon.setEvolvedThisTurn(true);

        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(evolutionCard)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-raichu")).thenReturn(stage1Def);
        when(cardLookup.getCardById("pkm-pikachu")).thenReturn(basicDef);
        when(player.getActivePokemon()).thenReturn(targetPokemon);

        handler.handle(ctx, createAction(0, targetInstanceId.toString()));

        verify(player, never()).setActivePokemon(any());
    }

    @Test
    void shouldDoNothingWhenInvalidStageProgression() {
        // Try evolving a BASIC with a STAGE_2 card
        stage1Def.setStage("STAGE_2");

        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(evolutionCard)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-raichu")).thenReturn(stage1Def);
        when(cardLookup.getCardById("pkm-pikachu")).thenReturn(basicDef);
        when(player.getActivePokemon()).thenReturn(targetPokemon);

        handler.handle(ctx, createAction(0, targetInstanceId.toString()));

        verify(player, never()).setActivePokemon(any());
    }

    @Test
    void shouldEvolveStage1ToStage2() {
        basicDef.setName("Charmander");
        basicDef.setStage("BASIC");
        PokemonCardDefinition stage2Def = new PokemonCardDefinition();
        stage2Def.setName("Charizard");
        stage2Def.setStage("STAGE_2");
        stage2Def.setEvolvesFrom("Charmeleon");

        PokemonCardDefinition stage1DefLocal = new PokemonCardDefinition();
        stage1DefLocal.setName("Charmeleon");
        stage1DefLocal.setStage("STAGE_1");
        stage1DefLocal.setEvolvesFrom("Charmander");

        targetPokemon.setCardDefinitionId("pkm-charmeleon");
        CardInstance stage2Card = new CardInstance(evolutionInstanceId, "pkm-charizard");

        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(5);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(stage2Card)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-charizard")).thenReturn(stage2Def);
        when(cardLookup.getCardById("pkm-charmeleon")).thenReturn(stage1DefLocal);
        when(player.getActivePokemon()).thenReturn(targetPokemon);
        when(player.getBench()).thenReturn(new ArrayList<>());

        handler.handle(ctx, createAction(0, targetInstanceId.toString()));

        verify(player).setActivePokemon(argThat(evolved ->
                evolved.getCardDefinitionId().equals("pkm-charizard")
        ));
    }
}
