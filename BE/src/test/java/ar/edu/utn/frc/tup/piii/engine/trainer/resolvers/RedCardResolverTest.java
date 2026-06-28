package ar.edu.utn.frc.tup.piii.engine.trainer.resolvers;

import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.trainer.EffectType;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.ports.RandomizerPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedCardResolverTest {

    @Mock
    private EngineContext ctx;
    @Mock
    private PlayerState player;
    @Mock
    private TrainerCardDefinition card;
    @Mock
    private GameState state;
    @Mock
    private PlayerState opponent;
    @Mock
    private RandomizerPort randomizer;

    @Captor
    private ArgumentCaptor<GameEvent> eventCaptor;

    private RedCardResolver resolver;
    private List<CardInstance> opponentHand;
    private List<CardInstance> opponentDeck;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        resolver = new RedCardResolver();

        playerId = UUID.randomUUID();
        opponentHand = new ArrayList<>();
        opponentDeck = new ArrayList<>();

        when(ctx.getState()).thenReturn(state);
        when(ctx.getRandomizer()).thenReturn(randomizer);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(3);
        when(player.getPlayerId()).thenReturn(playerId);
        when(ctx.getOpponent(playerId)).thenReturn(opponent);
        when(opponent.getHand()).thenReturn(opponentHand);
        when(opponent.getDeck()).thenReturn(opponentDeck);
    }

    @Test
    void resolve_shouldShuffleOpponentHandIntoDeckAndDrawFour() {
        opponentHand.addAll(createCards(5));
        opponentDeck.addAll(createCards(10));

        resolver.resolve(ctx, player, card, new HashMap<>());

        assertEquals(4, opponentHand.size());
        assertEquals(11, opponentDeck.size());
        verify(randomizer).shuffle(opponentDeck);
    }

    @Test
    void resolve_shouldShuffleOpponentHandIntoDeckAndDrawLessThanFourIfDeckSmall() {
        opponentHand.addAll(createCards(3));
        opponentDeck.addAll(createCards(1));

        resolver.resolve(ctx, player, card, new HashMap<>());

        assertEquals(4, opponentHand.size());
        assertTrue(opponentDeck.isEmpty());
        verify(randomizer).shuffle(opponentDeck);
    }

    @Test
    void resolve_shouldDrawNothingWhenOpponentDeckEmptyAfterShuffle() {
        opponentHand.addAll(createCards(2));
        opponentDeck.clear();

        resolver.resolve(ctx, player, card, new HashMap<>());

        assertEquals(2, opponentHand.size());
        assertTrue(opponentDeck.isEmpty());
    }

    @Test
    void resolve_shouldDrawAllWhenDeckHasLessThanFour() {
        opponentHand.addAll(createCards(3));
        opponentDeck.addAll(createCards(2));

        resolver.resolve(ctx, player, card, new HashMap<>());

        assertEquals(4, opponentHand.size());
        assertEquals(1, opponentDeck.size());
    }

    @Test
    void resolve_withNullOpponent_shouldReturnEarly() {
        when(ctx.getOpponent(playerId)).thenReturn(null);

        resolver.resolve(ctx, player, card, new HashMap<>());

        verify(ctx, never()).addEvent(any());
    }

    @Test
    void resolve_shouldPublishEvent() {
        opponentHand.addAll(createCards(3));
        opponentDeck.addAll(createCards(10));

        resolver.resolve(ctx, player, card, new HashMap<>());

        verify(ctx).addEvent(eventCaptor.capture());
        GameEvent event = eventCaptor.getValue();
        assertEquals(GameEventType.OPPONENT_HAND_SHUFFLED.name(), event.getType());
        assertEquals(4, event.getPayload().get("drawCount"));
    }

    @Test
    void getType_shouldReturnOPPONENT_SHUFFLE_HAND_DRAW() {
        assertEquals(EffectType.OPPONENT_SHUFFLE_HAND_DRAW, resolver.getType());
    }

    private List<CardInstance> createCards(int count) {
        List<CardInstance> cards = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            CardInstance ci = new CardInstance();
            ci.setInstanceId(UUID.randomUUID());
            ci.setCardDefinitionId("card-" + i);
            cards.add(ci);
        }
        return cards;
    }
}
