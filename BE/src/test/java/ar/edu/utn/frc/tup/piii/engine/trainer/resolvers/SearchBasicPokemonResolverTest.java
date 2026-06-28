package ar.edu.utn.frc.tup.piii.engine.trainer.resolvers;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
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
class SearchBasicPokemonResolverTest {

    @Mock
    private EngineContext ctx;
    @Mock
    private PlayerState player;
    @Mock
    private TrainerCardDefinition card;
    @Mock
    private GameState state;
    @Mock
    private CardLookupPort cardLookup;
    @Mock
    private RandomizerPort randomizer;

    @Captor
    private ArgumentCaptor<GameEvent> eventCaptor;

    private SearchBasicPokemonResolver resolver;
    private List<CardInstance> deck;
    private List<PokemonInPlay> bench;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        resolver = new SearchBasicPokemonResolver();
        playerId = UUID.randomUUID();
        deck = new ArrayList<>();
        bench = new ArrayList<>();

        when(ctx.getState()).thenReturn(state);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(ctx.getRandomizer()).thenReturn(randomizer);
        when(player.getDeck()).thenReturn(deck);
        when(player.getBench()).thenReturn(bench);
        when(player.getPlayerId()).thenReturn(playerId);
        when(state.getTurnNumber()).thenReturn(3);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
    }

    @Test
    void resolve_shouldSearchAndPlaceBasicPokemonOnBench() {
        CardInstance pokemonCard = createCardInstance("pkm-1");
        deck.add(pokemonCard);
        PokemonCardDefinition pkmDef = createPokemonDef("BASIC");

        when(cardLookup.getCardById("pkm-1")).thenReturn(pkmDef);

        Map<String, Object> payload = new HashMap<>();
        payload.put("targetCardIndex", 0);

        resolver.resolve(ctx, player, card, payload);

        assertEquals(1, bench.size());
        assertEquals("pkm-1", bench.get(0).getCardDefinitionId());
        assertEquals(playerId, bench.get(0).getOwnerPlayerId());
        assertEquals(3, bench.get(0).getEnteredTurnNumber());
        assertEquals(0, bench.get(0).getDamageCounters());
        assertTrue(deck.isEmpty());
        verify(randomizer).shuffle(deck);
    }

    @Test
    void resolve_withBenchFull_shouldSkip() {
        for (int i = 0; i < 5; i++) {
            bench.add(new PokemonInPlay());
        }
        deck.add(createCardInstance("pkm-1"));

        resolver.resolve(ctx, player, card, new HashMap<>());

        assertEquals(5, bench.size());
        verify(cardLookup, never()).getCardById(any());
    }

    @Test
    void resolve_withNullTargetCardIndex_shouldReturnEarly() {
        deck.add(createCardInstance("pkm-1"));
        Map<String, Object> payload = new HashMap<>();

        resolver.resolve(ctx, player, card, payload);

        assertTrue(bench.isEmpty());
        verify(cardLookup, never()).getCardById(any());
    }

    @Test
    void resolve_withNegativeTargetCardIndex_shouldReturnEarly() {
        deck.add(createCardInstance("pkm-1"));
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetCardIndex", -1);

        resolver.resolve(ctx, player, card, payload);

        assertTrue(bench.isEmpty());
        verify(cardLookup, never()).getCardById(any());
    }

    @Test
    void resolve_withOutOfBoundsTargetCardIndex_shouldReturnEarly() {
        deck.add(createCardInstance("pkm-1"));
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetCardIndex", 5);

        resolver.resolve(ctx, player, card, payload);

        assertTrue(bench.isEmpty());
        verify(cardLookup, never()).getCardById(any());
    }

    @Test
    void resolve_withNonPokemonCard_shouldSkip() {
        CardInstance energyCard = createCardInstance("energy-1");
        deck.add(energyCard);
        CardDefinition energyDef = mock(CardDefinition.class);

        when(cardLookup.getCardById("energy-1")).thenReturn(energyDef);

        Map<String, Object> payload = new HashMap<>();
        payload.put("targetCardIndex", 0);

        resolver.resolve(ctx, player, card, payload);

        assertTrue(bench.isEmpty());
        assertEquals(1, deck.size());
    }

    @Test
    void resolve_withNonBasicStagePokemon_shouldSkip() {
        CardInstance pokemonCard = createCardInstance("pkm-1");
        deck.add(pokemonCard);
        PokemonCardDefinition pkmDef = createPokemonDef("STAGE_1");

        when(cardLookup.getCardById("pkm-1")).thenReturn(pkmDef);

        Map<String, Object> payload = new HashMap<>();
        payload.put("targetCardIndex", 0);

        resolver.resolve(ctx, player, card, payload);

        assertTrue(bench.isEmpty());
        assertEquals(1, deck.size());
    }

    @Test
    void resolve_shouldShuffleDeckAfter() {
        CardInstance pokemonCard = createCardInstance("pkm-1");
        deck.add(createCardInstance("other-1"));
        deck.add(pokemonCard);
        deck.add(createCardInstance("other-2"));
        PokemonCardDefinition pkmDef = createPokemonDef("BASIC");

        when(cardLookup.getCardById("pkm-1")).thenReturn(pkmDef);

        Map<String, Object> payload = new HashMap<>();
        payload.put("targetCardIndex", 1);

        resolver.resolve(ctx, player, card, payload);

        assertEquals(2, deck.size());
        verify(randomizer).shuffle(deck);
    }

    @Test
    void resolve_shouldPublishEvent() {
        CardInstance pokemonCard = createCardInstance("pkm-1");
        deck.add(pokemonCard);
        PokemonCardDefinition pkmDef = createPokemonDef("BASIC");

        when(cardLookup.getCardById("pkm-1")).thenReturn(pkmDef);

        Map<String, Object> payload = new HashMap<>();
        payload.put("targetCardIndex", 0);

        resolver.resolve(ctx, player, card, payload);

        verify(ctx).addEvent(eventCaptor.capture());
        GameEvent event = eventCaptor.getValue();
        assertEquals(GameEventType.POKEMON_SEARCHED.name(), event.getType());
        assertEquals("pkm-1", event.getPayload().get("cardDefinitionId"));
        assertNotNull(event.getPayload().get("pokemonInstanceId"));
    }

    @Test
    void getType_shouldReturnSEARCH_BASIC_POKEMON() {
        assertEquals(EffectType.SEARCH_BASIC_POKEMON, resolver.getType());
    }

    private CardInstance createCardInstance(String cardDefId) {
        CardInstance ci = new CardInstance();
        ci.setInstanceId(UUID.randomUUID());
        ci.setCardDefinitionId(cardDefId);
        return ci;
    }

    private PokemonCardDefinition createPokemonDef(String stage) {
        PokemonCardDefinition def = new PokemonCardDefinition();
        def.setStage(stage);
        return def;
    }
}
