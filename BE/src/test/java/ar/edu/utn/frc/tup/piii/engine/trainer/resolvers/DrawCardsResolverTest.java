package ar.edu.utn.frc.tup.piii.engine.trainer.resolvers;

import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.trainer.EffectType;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DrawCardsResolverTest {

    @Mock
    private EngineContext ctx;
    @Mock
    private PlayerState player;
    @Mock
    private TrainerCardDefinition card;
    @Mock
    private GameState state;

    @Captor
    private ArgumentCaptor<GameEvent> eventCaptor;

    private DrawCardsResolver resolver;
    private List<CardInstance> deck;
    private List<CardInstance> hand;
    private UUID matchId;
    private int turnNumber;

    @BeforeEach
    void setUp() {
        Map<String, Integer> effectDrawCounts = new HashMap<>();
        effectDrawCounts.put("draw3", 3);
        resolver = new DrawCardsResolver(effectDrawCounts);

        matchId = UUID.randomUUID();
        turnNumber = 5;
        deck = new ArrayList<>();
        hand = new ArrayList<>();

        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(matchId);
        when(state.getTurnNumber()).thenReturn(turnNumber);
        when(player.getDeck()).thenReturn(deck);
        when(player.getHand()).thenReturn(hand);
    }

    @Test
    void resolve_shouldDrawSpecifiedCountFromEffectCode() {
        when(card.getEffectCode()).thenReturn("draw3");
        deck.addAll(createCards(5));

        resolver.resolve(ctx, player, card, new HashMap<>());

        assertEquals(3, hand.size());
        assertEquals(2, deck.size());
    }

    @Test
    void resolve_shouldDrawCountFromPayloadOverridingConfig() {
        when(card.getEffectCode()).thenReturn("draw3");
        deck.addAll(createCards(5));
        Map<String, Object> payload = new HashMap<>();
        payload.put("count", 2);

        resolver.resolve(ctx, player, card, payload);

        assertEquals(2, hand.size());
        assertEquals(3, deck.size());
    }

    @Test
    void resolve_shouldClampToDeckSize() {
        when(card.getEffectCode()).thenReturn("draw3");
        deck.addAll(createCards(2));

        resolver.resolve(ctx, player, card, new HashMap<>());

        assertEquals(2, hand.size());
        assertTrue(deck.isEmpty());
    }

    @Test
    void resolve_withEmptyDeck_shouldDrawNothing() {
        when(card.getEffectCode()).thenReturn("draw3");

        resolver.resolve(ctx, player, card, new HashMap<>());

        assertTrue(hand.isEmpty());
        assertTrue(deck.isEmpty());
    }

    @Test
    void resolve_shouldVerifyCardsMovedFromDeckToHand() {
        when(card.getEffectCode()).thenReturn("draw3");
        deck.addAll(createCards(5));
        CardInstance firstCard = deck.get(0);
        CardInstance secondCard = deck.get(1);

        resolver.resolve(ctx, player, card, new HashMap<>());

        assertSame(firstCard, hand.get(0));
        assertSame(secondCard, hand.get(1));
    }

    @Test
    void resolve_shouldPublishEvent() {
        when(card.getEffectCode()).thenReturn("draw3");
        deck.addAll(createCards(5));

        resolver.resolve(ctx, player, card, new HashMap<>());

        verify(ctx).addEvent(eventCaptor.capture());
        GameEvent event = eventCaptor.getValue();
        assertEquals(GameEventType.CARDS_DRAWN.name(), event.getType());
        assertEquals(matchId, event.getMatchId());
        assertEquals(turnNumber, event.getTurnNumber());
        assertEquals(3, event.getPayload().get("count"));
        assertEquals("draw3", event.getPayload().get("effectCode"));
    }

    @Test
    void resolve_shouldPublishEventWithActualDrawCount() {
        when(card.getEffectCode()).thenReturn("draw3");
        deck.addAll(createCards(1));

        resolver.resolve(ctx, player, card, new HashMap<>());

        verify(ctx).addEvent(eventCaptor.capture());
        GameEvent event = eventCaptor.getValue();
        assertEquals(1, event.getPayload().get("count"));
        assertEquals("draw3", event.getPayload().get("effectCode"));
    }

    @Test
    void resolve_shouldUseDefaultCountWhenEffectCodeNotConfigured() {
        when(card.getEffectCode()).thenReturn("unknown");
        deck.addAll(createCards(5));

        resolver.resolve(ctx, player, card, new HashMap<>());

        assertEquals(1, hand.size());
    }

    @Test
    void getType_shouldReturnDRAW_CARDS() {
        assertEquals(EffectType.DRAW_CARDS, resolver.getType());
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
