package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.TrainerSubtype;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.TurnFlags;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import ar.edu.utn.frc.tup.piii.engine.trainer.EffectType;
import ar.edu.utn.frc.tup.piii.engine.trainer.TrainerEffectRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayTrainerHandlerTest {

    @Mock
    private EngineContext ctx;
    @Mock
    private GameState state;
    @Mock
    private PlayerState player;
    @Mock
    private CardLookupPort cardLookup;
    @Mock
    private TrainerEffectRegistry effectRegistry;

    private PlayTrainerHandler handler;
    private UUID playerId;
    private UUID trainerInstanceId;
    private CardInstance trainerCard;
    private TrainerCardDefinition trainerDef;
    private TurnFlags flags;

    @BeforeEach
    void setUp() {
        handler = new PlayTrainerHandler(effectRegistry);
        playerId = UUID.randomUUID();
        trainerInstanceId = UUID.randomUUID();

        trainerCard = new CardInstance(trainerInstanceId, "trainer-potion");

        trainerDef = new TrainerCardDefinition();
        trainerDef.setTrainerSubtype(TrainerSubtype.ITEM);

        flags = new TurnFlags();
    }

    private GameAction createAction(Integer handIndex) {
        GameAction action = new GameAction();
        action.setPlayerId(playerId);
        Map<String, Object> payload = new HashMap<>();
        if (handIndex != null) payload.put("handIndex", handIndex);
        action.setPayload(payload);
        return action;
    }

    @Test
    void shouldPlayTrainerAndDiscard() {
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(2);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(trainerCard)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("trainer-potion")).thenReturn(trainerDef);

        handler.handle(ctx, createAction(0));

        assertTrue(player.getHand().isEmpty());
        verify(player).pushToDiscard(trainerCard);
    }

    @Test
    void shouldNotDiscardStadium() {
        trainerDef.setTrainerSubtype(TrainerSubtype.STADIUM);

        when(ctx.getState()).thenReturn(state);
        when(state.getTurnFlags()).thenReturn(flags);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(2);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(trainerCard)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("trainer-potion")).thenReturn(trainerDef);

        handler.handle(ctx, createAction(0));

        assertFalse(player.getHand().isEmpty());
        verify(player, never()).pushToDiscard(any());
    }

    @Test
    void shouldTrackSupporterUsed() {
        trainerDef.setTrainerSubtype(TrainerSubtype.SUPPORTER);

        when(ctx.getState()).thenReturn(state);
        when(state.getTurnFlags()).thenReturn(flags);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(2);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(trainerCard)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("trainer-potion")).thenReturn(trainerDef);

        handler.handle(ctx, createAction(0));

        assertTrue(flags.hasPlayedSupporter());
    }

    @Test
    void shouldBlockSupporterWhenCannotPlay() {
        trainerDef.setTrainerSubtype(TrainerSubtype.SUPPORTER);
        when(player.isCannotPlaySupportersNextTurn()).thenReturn(true);

        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(trainerCard)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("trainer-potion")).thenReturn(trainerDef);

        handler.handle(ctx, createAction(0));

        verify(effectRegistry, never()).resolve(any(), any(), any(), any());
    }

    @Test
    void shouldTrackStadiumPlayed() {
        trainerDef.setTrainerSubtype(TrainerSubtype.STADIUM);

        when(ctx.getState()).thenReturn(state);
        when(state.getTurnFlags()).thenReturn(flags);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(2);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(trainerCard)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("trainer-potion")).thenReturn(trainerDef);

        handler.handle(ctx, createAction(0));

        assertTrue(flags.hasPlayedStadium());
    }

    @Test
    void shouldResolveEffectWhenKnown() {
        trainerDef.setEffectCode("HEAL_30");

        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(2);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(trainerCard)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("trainer-potion")).thenReturn(trainerDef);
        when(effectRegistry.isEffectCodeKnown("HEAL_30")).thenReturn(true);

        handler.handle(ctx, createAction(0));

        
        verify(effectRegistry).resolve(
                eq(ctx),
                eq(player),
                any(TrainerCardDefinition.class),
                anyMap()
        );
    }

    @Test
    void shouldDoNothingWhenHandIndexIsNull() {
        handler.handle(ctx, createAction(null));

        verifyNoInteractions(player, cardLookup, effectRegistry);
    }

    @Test
    void shouldDoNothingWhenHandIndexOutOfBounds() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(trainerCard)));

        handler.handle(ctx, createAction(5));

        verify(cardLookup, never()).getCardById(any());
    }

    @Test
    void shouldDoNothingWhenCardIsNotTrainer() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(new ArrayList<>(List.of(trainerCard)));
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("trainer-potion"))
                .thenReturn(new EnergyCardDefinition());

        handler.handle(ctx, createAction(0));

        verify(effectRegistry, never()).resolve(any(), any(), any(), any());
    }
}
