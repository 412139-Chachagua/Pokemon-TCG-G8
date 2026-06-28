package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.TrainerSubtype;
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
class AttachToolHandlerTest {

    @Mock
    private EngineContext ctx;
    @Mock
    private GameState state;
    @Mock
    private PlayerState player;
    @Mock
    private CardLookupPort cardLookup;

    private AttachToolHandler handler;
    private UUID playerId;
    private UUID toolInstanceId;
    private UUID targetPokemonInstanceId;
    private CardInstance toolCard;
    private TrainerCardDefinition toolDef;
    private PokemonInPlay targetPokemon;

    @BeforeEach
    void setUp() {
        handler = new AttachToolHandler();
        playerId = UUID.randomUUID();
        toolInstanceId = UUID.randomUUID();
        targetPokemonInstanceId = UUID.randomUUID();

        toolCard = new CardInstance(toolInstanceId, "tool-eviolite");
        toolDef = new TrainerCardDefinition();
        toolDef.setTrainerSubtype(TrainerSubtype.POKEMON_TOOL);

        targetPokemon = new PokemonInPlay();
        targetPokemon.setInstanceId(targetPokemonInstanceId);
        targetPokemon.setCardDefinitionId("pkm-pikachu");
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
    void shouldAttachToolToPokemon() {
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(2);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getPlayerId()).thenReturn(playerId);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(toolCard)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("tool-eviolite")).thenReturn(toolDef);
        when(player.getActivePokemon()).thenReturn(targetPokemon);

        handler.handle(ctx, createAction(0, targetPokemonInstanceId.toString()));

        assertTrue(player.getHand().isEmpty());
        assertEquals(toolInstanceId, targetPokemon.getToolCardInstanceId());
        assertSame(toolCard, targetPokemon.getAttachedTool());
        verify(ctx).addEvent(argThat(e ->
                "TOOL_ATTACHED".equals(e.getType())
        ));
    }

    @Test
    void shouldReplaceExistingTool() {
        UUID oldToolInstanceId = UUID.randomUUID();
        CardInstance oldTool = new CardInstance(oldToolInstanceId, "tool-old");
        targetPokemon.setToolCardInstanceId(oldToolInstanceId);
        targetPokemon.setAttachedTool(oldTool);

        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(2);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getPlayerId()).thenReturn(playerId);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(toolCard)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("tool-eviolite")).thenReturn(toolDef);
        when(player.getActivePokemon()).thenReturn(targetPokemon);

        handler.handle(ctx, createAction(0, targetPokemonInstanceId.toString()));

        verify(player).pushToDiscard(oldTool);
        assertEquals(toolInstanceId, targetPokemon.getToolCardInstanceId());
    }

    @Test
    void shouldDoNothingWhenHandIndexIsNull() {
        handler.handle(ctx, createAction(null, targetPokemonInstanceId.toString()));

        verifyNoInteractions(player, cardLookup);
    }

    @Test
    void shouldDoNothingWhenHandIndexOutOfBounds() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(toolCard)));

        handler.handle(ctx, createAction(5, targetPokemonInstanceId.toString()));

        verifyNoInteractions(cardLookup);
    }

    @Test
    void shouldDoNothingWhenCardIsNotTrainer() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(toolCard)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("tool-eviolite"))
                .thenReturn(new PokemonCardDefinition());

        handler.handle(ctx, createAction(0, targetPokemonInstanceId.toString()));

        assertNull(targetPokemon.getToolCardInstanceId());
    }

    @Test
    void shouldDoNothingWhenNotPokemonToolOrItem() {
        toolDef.setTrainerSubtype(TrainerSubtype.SUPPORTER);

        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(toolCard)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("tool-eviolite")).thenReturn(toolDef);

        handler.handle(ctx, createAction(0, targetPokemonInstanceId.toString()));

        assertNull(targetPokemon.getToolCardInstanceId());
    }

    @Test
    void shouldDetectToolViaSubtypesList() {
        toolDef.setTrainerSubtype(TrainerSubtype.ITEM);
        toolDef.setSubtypes(List.of("TOOL"));

        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(2);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getPlayerId()).thenReturn(playerId);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(toolCard)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("tool-eviolite")).thenReturn(toolDef);
        when(player.getActivePokemon()).thenReturn(targetPokemon);

        handler.handle(ctx, createAction(0, targetPokemonInstanceId.toString()));

        assertEquals(toolInstanceId, targetPokemon.getToolCardInstanceId());
    }

    @Test
    void shouldDoNothingWhenTargetIdIsNull() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(toolCard)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("tool-eviolite")).thenReturn(toolDef);

        handler.handle(ctx, createAction(0, null));

        assertNull(targetPokemon.getToolCardInstanceId());
    }

    @Test
    void shouldDoNothingWhenTargetPokemonNotFound() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(toolCard)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("tool-eviolite")).thenReturn(toolDef);
        when(player.getActivePokemon()).thenReturn(null);
        when(player.getBench()).thenReturn(new ArrayList<>());

        handler.handle(ctx, createAction(0, targetPokemonInstanceId.toString()));

        assertNull(targetPokemon.getToolCardInstanceId());
    }
}
