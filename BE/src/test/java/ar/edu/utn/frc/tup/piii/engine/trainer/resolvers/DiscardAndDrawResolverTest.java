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
class DiscardAndDrawResolverTest {

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

    private DiscardAndDrawResolver resolver;
    private List<CardInstance> hand;
    private List<CardInstance> deck;
    private List<CardInstance> discard;
    private Map<String, int[]> effectConfig;

    @BeforeEach
    void setUp() {
        effectConfig = new HashMap<>();
        effectConfig.put("discard1draw1", new int[]{1, 1});
        resolver = new DiscardAndDrawResolver(effectConfig);

        hand = new ArrayList<>();
        deck = new ArrayList<>();
        discard = new ArrayList<>();

        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(3);
        when(player.getHand()).thenReturn(hand);
        when(player.getDeck()).thenReturn(deck);
        when(player.getDiscard()).thenReturn(discard);
        doAnswer(inv -> discard.add(inv.getArgument(0))).when(player).pushToDiscard(any());
        doAnswer(inv -> discard.addAll(inv.getArgument(0))).when(player).pushManyToDiscard(any());
    }

    @Test
    void resolve_shouldDiscardOneAndDrawOneFromConfig() {
        when(card.getEffectCode()).thenReturn("discard1draw1");
        hand.addAll(createCards(3));
        deck.addAll(createCards(5));

        Map<String, Object> payload = new HashMap<>();
        payload.put("targetCardIndex", 0);

        resolver.resolve(ctx, player, card, payload);

        assertEquals(3, hand.size());
        assertEquals(4, deck.size());
        assertEquals(1, discard.size());
    }

    @Test
    void resolve_shouldDiscardFromPayloadTargetCardIndex() {
        when(card.getEffectCode()).thenReturn("discard1draw1");
        hand.addAll(createCards(3));
        deck.addAll(createCards(5));

        Map<String, Object> payload = new HashMap<>();
        payload.put("targetCardIndex", 1);

        resolver.resolve(ctx, player, card, payload);

        assertEquals(3, hand.size());
        assertEquals(1, discard.size());
    }

    @Test
    void resolve_shouldDiscardAllWhenCountIsNegative() {
        when(card.getEffectCode()).thenReturn("discard1draw1");
        hand.addAll(createCards(3));
        deck.addAll(createCards(5));

        Map<String, Object> payload = new HashMap<>();
        payload.put("discardCount", -1);

        resolver.resolve(ctx, player, card, payload);

        assertEquals(3, discard.size());
        assertEquals(1, hand.size());
        assertEquals(4, deck.size());
    }

    @Test
    void resolve_shouldDiscardAllWhenCountExceedsHandSize() {
        when(card.getEffectCode()).thenReturn("discard1draw1");
        hand.addAll(createCards(3));
        deck.addAll(createCards(5));

        Map<String, Object> payload = new HashMap<>();
        payload.put("discardCount", 10);

        resolver.resolve(ctx, player, card, payload);

        assertEquals(3, discard.size());
        assertEquals(1, hand.size());
        assertEquals(4, deck.size());
    }

    @Test
    void resolve_shouldDrawFromPayloadOverride() {
        when(card.getEffectCode()).thenReturn("discard1draw1");
        hand.addAll(createCards(3));
        deck.addAll(createCards(5));

        Map<String, Object> payload = new HashMap<>();
        payload.put("targetCardIndex", 0);
        payload.put("drawCount", 3);

        resolver.resolve(ctx, player, card, payload);

        assertEquals(5, hand.size());
        assertEquals(2, deck.size());
    }

    @Test
    void resolve_shouldDrawNothingWhenDeckEmpty() {
        when(card.getEffectCode()).thenReturn("discard1draw1");
        hand.addAll(createCards(3));

        Map<String, Object> payload = new HashMap<>();
        payload.put("targetCardIndex", 0);

        resolver.resolve(ctx, player, card, payload);

        assertEquals(2, hand.size());
        assertTrue(deck.isEmpty());
        assertEquals(1, discard.size());
    }

    @Test
    void resolve_shouldPublishEvent() {
        when(card.getEffectCode()).thenReturn("discard1draw1");
        hand.addAll(createCards(3));
        deck.addAll(createCards(5));

        Map<String, Object> payload = new HashMap<>();
        payload.put("targetCardIndex", 0);

        resolver.resolve(ctx, player, card, payload);

        verify(ctx).addEvent(eventCaptor.capture());
        GameEvent event = eventCaptor.getValue();
        assertEquals(GameEventType.CARDS_DRAWN.name(), event.getType());
    }

    @Test
    void resolve_shouldUseDefaultConfigWhenEffectCodeNotConfigured() {
        when(card.getEffectCode()).thenReturn("unknown");
        hand.addAll(createCards(3));
        deck.addAll(createCards(5));

        resolver.resolve(ctx, player, card, new HashMap<>());

        assertEquals(3, hand.size());
        assertEquals(5, deck.size());
        assertTrue(discard.isEmpty());
    }

    @Test
    void getType_shouldReturnDISCARD_AND_DRAW() {
        assertEquals(EffectType.DISCARD_AND_DRAW, resolver.getType());
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
