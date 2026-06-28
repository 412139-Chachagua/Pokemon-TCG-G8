package ar.edu.utn.frc.tup.piii.engine.trainer.resolvers;

import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.ports.RandomizerPort;
import ar.edu.utn.frc.tup.piii.engine.trainer.EffectType;
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
class ShuffleHandIntoDeckResolverTest {

    @Mock
    private EngineContext ctx;
    @Mock
    private PlayerState player;
    @Mock
    private TrainerCardDefinition card;
    @Mock
    private GameState state;
    @Mock
    private RandomizerPort randomizer;

    @Captor
    private ArgumentCaptor<GameEvent> eventCaptor;

    private ShuffleHandIntoDeckResolver resolver;
    private List<CardInstance> hand;
    private List<CardInstance> deck;
    private Map<String, Integer> effectDrawCounts;

    @BeforeEach
    void setUp() {
        effectDrawCounts = new HashMap<>();
        effectDrawCounts.put("shuffleDraw7", 7);
        resolver = new ShuffleHandIntoDeckResolver(effectDrawCounts);

        hand = new ArrayList<>();
        deck = new ArrayList<>();

        when(ctx.getState()).thenReturn(state);
        when(ctx.getRandomizer()).thenReturn(randomizer);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(3);
        when(player.getHand()).thenReturn(hand);
        when(player.getDeck()).thenReturn(deck);
    }

    @Test
    void resolve_shouldShuffleHandIntoDeckAndDrawSeven() {
        when(card.getEffectCode()).thenReturn("shuffleDraw7");
        hand.addAll(createCards(3));
        deck.addAll(createCards(10));

        resolver.resolve(ctx, player, card, new HashMap<>());

        assertEquals(7, hand.size());
        assertEquals(6, deck.size());
        verify(randomizer).shuffle(deck);
    }

    @Test
    void resolve_shouldDrawSpecifiedCountFromConfig() {
        when(card.getEffectCode()).thenReturn("shuffleDraw7");
        hand.addAll(createCards(5));
        deck.addAll(createCards(20));

        resolver.resolve(ctx, player, card, new HashMap<>());

        assertEquals(7, hand.size());
        assertEquals(18, deck.size());
    }

    @Test
    void resolve_shouldDrawFromPayloadOverride() {
        when(card.getEffectCode()).thenReturn("shuffleDraw7");
        hand.addAll(createCards(3));
        deck.addAll(createCards(10));

        Map<String, Object> payload = new HashMap<>();
        payload.put("drawCount", 4);

        resolver.resolve(ctx, player, card, payload);

        assertEquals(4, hand.size());
        assertEquals(9, deck.size());
    }

    @Test
    void resolve_shouldDrawLessWhenDeckIsSmall() {
        when(card.getEffectCode()).thenReturn("shuffleDraw7");
        hand.addAll(createCards(2));
        deck.addAll(createCards(3));

        resolver.resolve(ctx, player, card, new HashMap<>());

        assertEquals(5, hand.size());
        assertTrue(deck.isEmpty());
        verify(randomizer).shuffle(deck);
    }

    @Test
    void resolve_withEmptyHand_shouldJustShuffleAndDraw() {
        when(card.getEffectCode()).thenReturn("shuffleDraw7");
        deck.addAll(createCards(10));

        resolver.resolve(ctx, player, card, new HashMap<>());

        assertEquals(7, hand.size());
        assertEquals(3, deck.size());
        verify(randomizer).shuffle(deck);
    }

    @Test
    void resolve_shouldUseDefaultCountWhenEffectCodeNotConfigured() {
        when(card.getEffectCode()).thenReturn("unknown");
        hand.addAll(createCards(2));
        deck.addAll(createCards(10));

        resolver.resolve(ctx, player, card, new HashMap<>());

        assertEquals(7, hand.size());
        assertEquals(5, deck.size());
    }

    @Test
    void resolve_shouldPublishEvent() {
        when(card.getEffectCode()).thenReturn("shuffleDraw7");
        hand.addAll(createCards(3));
        deck.addAll(createCards(10));

        resolver.resolve(ctx, player, card, new HashMap<>());

        verify(ctx).addEvent(eventCaptor.capture());
        GameEvent event = eventCaptor.getValue();
        assertEquals(GameEventType.CARDS_DRAWN.name(), event.getType());
        assertEquals(7, event.getPayload().get("drawCount"));
    }

    @Test
    void getType_shouldReturnSHUFFLE_HAND_INTO_DECK() {
        assertEquals(EffectType.SHUFFLE_HAND_INTO_DECK, resolver.getType());
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
