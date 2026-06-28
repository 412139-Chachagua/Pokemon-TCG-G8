package ar.edu.utn.frc.tup.piii.engine.trainer.resolvers;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardType;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.SpecialCondition;
import ar.edu.utn.frc.tup.piii.engine.energy.EnergyService;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.model.TurnFlags;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import ar.edu.utn.frc.tup.piii.engine.ports.RandomizerPort;
import ar.edu.utn.frc.tup.piii.engine.trainer.EffectType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
class AdditionalResolversTest {

    @Mock
    protected EngineContext ctx;
    @Mock
    protected PlayerState player;
    @Mock
    protected TrainerCardDefinition card;
    @Mock
    protected GameState state;
    @Mock
    protected CardLookupPort cardLookup;
    @Mock
    protected RandomizerPort randomizer;
    @Mock
    protected EnergyService energyService;

    @Captor
    protected ArgumentCaptor<GameEvent> eventCaptor;

    protected UUID playerId;
    protected UUID matchId;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        matchId = UUID.randomUUID();
        when(ctx.getState()).thenReturn(state);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(ctx.getRandomizer()).thenReturn(randomizer);
        when(ctx.getEnergyService()).thenReturn(energyService);
        when(state.getMatchId()).thenReturn(matchId);
        when(state.getTurnNumber()).thenReturn(3);
        when(player.getPlayerId()).thenReturn(playerId);
    }

    @Nested
    class AttachExtraEnergyResolverTest {

        @Test
        void resolve_shouldAttachEnergyFromHandToPokemon() {
            AttachExtraEnergyResolver resolver = new AttachExtraEnergyResolver();
            CardInstance energyCard = createCardInstance("energy-1");
            List<CardInstance> hand = new ArrayList<>(List.of(energyCard));
            EnergyCardDefinition energyDef = new EnergyCardDefinition();
            energyDef.setEnergyCardType(EnergyCardType.BASIC);
            PokemonInPlay target = createPokemonInPlay();

            when(player.getHand()).thenReturn(hand);
            when(cardLookup.getCardById("energy-1")).thenReturn(energyDef);
            when(player.getActivePokemon()).thenReturn(target);

            Map<String, Object> payload = new HashMap<>();
            payload.put("handIndex", 0);
            payload.put("targetPokemonInstanceId", target.getInstanceId().toString());

            resolver.resolve(ctx, player, card, payload);

            verify(energyService).attachFromHand(energyCard, target, player, ctx);
            verify(ctx).addEvent(eventCaptor.capture());
            assertEquals(GameEventType.ENERGY_ATTACHED.name(), eventCaptor.getValue().getType());
        }

        @Test
        void resolve_withInvalidHandIndex_shouldReturnEarly() {
            AttachExtraEnergyResolver resolver = new AttachExtraEnergyResolver();
            when(player.getHand()).thenReturn(new ArrayList<>());

            Map<String, Object> payload = new HashMap<>();
            payload.put("handIndex", 0);

            resolver.resolve(ctx, player, card, payload);

            verify(energyService, never()).attachFromHand(any(), any(), any(), any());
        }

        @Test
        void resolve_withNullTargetPokemonId_shouldReturnEarly() {
            AttachExtraEnergyResolver resolver = new AttachExtraEnergyResolver();
            CardInstance energyCard = createCardInstance("energy-1");
            List<CardInstance> hand = new ArrayList<>(List.of(energyCard));
            EnergyCardDefinition energyDef = new EnergyCardDefinition();

            when(player.getHand()).thenReturn(hand);
            when(cardLookup.getCardById("energy-1")).thenReturn(energyDef);

            Map<String, Object> payload = new HashMap<>();
            payload.put("handIndex", 0);

            resolver.resolve(ctx, player, card, payload);

            verify(energyService, never()).attachFromHand(any(), any(), any(), any());
        }

        @Test
        void getType_shouldReturnATTACH_EXTRA_ENERGY() {
            AttachExtraEnergyResolver resolver = new AttachExtraEnergyResolver();
            assertEquals(EffectType.ATTACH_EXTRA_ENERGY, resolver.getType());
        }
    }

    @Nested
    class CoinFlipDrawResolverTest {

        @Test
        void resolve_onHeads_shouldDrawThreeCards() {
            CoinFlipDrawResolver resolver = new CoinFlipDrawResolver();
            List<CardInstance> deck = new ArrayList<>(createCards(5));
            List<CardInstance> hand = new ArrayList<>();

            when(ctx.getRandomizer()).thenReturn(randomizer);
            when(randomizer.nextInt(2)).thenReturn(0);
            when(player.getDeck()).thenReturn(deck);
            when(player.getHand()).thenReturn(hand);
            when(card.getName()).thenReturn("Test Card");

            resolver.resolve(ctx, player, card, new HashMap<>());

            assertEquals(3, hand.size());
            assertEquals(2, deck.size());
        }

        @Test
        void resolve_onTails_shouldDrawNothing() {
            CoinFlipDrawResolver resolver = new CoinFlipDrawResolver();
            List<CardInstance> deck = new ArrayList<>(createCards(5));
            List<CardInstance> hand = new ArrayList<>();

            when(randomizer.nextInt(2)).thenReturn(1);
            when(player.getDeck()).thenReturn(deck);
            when(player.getHand()).thenReturn(hand);
            when(card.getName()).thenReturn("Test Card");

            resolver.resolve(ctx, player, card, new HashMap<>());

            assertTrue(hand.isEmpty());
            assertEquals(5, deck.size());
        }

        @Test
        void resolve_onHeadsWithSmallDeck_shouldDrawAll() {
            CoinFlipDrawResolver resolver = new CoinFlipDrawResolver();
            List<CardInstance> deck = new ArrayList<>(createCards(2));
            List<CardInstance> hand = new ArrayList<>();

            when(randomizer.nextInt(2)).thenReturn(0);
            when(player.getDeck()).thenReturn(deck);
            when(player.getHand()).thenReturn(hand);
            when(card.getName()).thenReturn("Test Card");

            resolver.resolve(ctx, player, card, new HashMap<>());

            assertEquals(2, hand.size());
            assertTrue(deck.isEmpty());
        }

        @Test
        void getType_shouldReturnCOIN_FLIP_DRAW() {
            CoinFlipDrawResolver resolver = new CoinFlipDrawResolver();
            assertEquals(EffectType.COIN_FLIP_DRAW, resolver.getType());
        }
    }

    @Nested
    class ConditionRemoveResolverTest {

        @Test
        void resolve_shouldRemoveAllConditions() {
            ConditionRemoveResolver resolver = new ConditionRemoveResolver();
            PokemonInPlay target = createPokemonInPlay();
            target.setSpecialConditions(new ArrayList<>(List.of(SpecialCondition.ASLEEP, SpecialCondition.PARALYZED)));

            when(player.getActivePokemon()).thenReturn(target);

            Map<String, Object> payload = new HashMap<>();
            payload.put("targetPokemonInstanceId", target.getInstanceId().toString());

            resolver.resolve(ctx, player, card, payload);

            assertTrue(target.getSpecialConditions().isEmpty());
            verify(ctx).addEvent(any());
        }

        @Test
        void resolve_shouldRemoveSpecificCondition() {
            ConditionRemoveResolver resolver = new ConditionRemoveResolver();
            PokemonInPlay target = createPokemonInPlay();
            target.setSpecialConditions(new ArrayList<>(List.of(SpecialCondition.ASLEEP, SpecialCondition.PARALYZED, SpecialCondition.POISONED)));

            when(player.getActivePokemon()).thenReturn(target);

            Map<String, Object> payload = new HashMap<>();
            payload.put("targetPokemonInstanceId", target.getInstanceId().toString());
            payload.put("condition", "PARALYZED");

            resolver.resolve(ctx, player, card, payload);

            assertEquals(2, target.getSpecialConditions().size());
            assertFalse(target.getSpecialConditions().contains(SpecialCondition.PARALYZED));
        }

        @Test
        void resolve_withNullTargetId_shouldReturnEarly() {
            ConditionRemoveResolver resolver = new ConditionRemoveResolver();

            resolver.resolve(ctx, player, card, new HashMap<>());

            verify(ctx, never()).addEvent(any());
        }

        @Test
        void resolve_withTargetNotFound_shouldReturnEarly() {
            ConditionRemoveResolver resolver = new ConditionRemoveResolver();
            when(player.getActivePokemon()).thenReturn(null);
            when(player.getBench()).thenReturn(new ArrayList<>());

            Map<String, Object> payload = new HashMap<>();
            payload.put("targetPokemonInstanceId", UUID.randomUUID().toString());

            resolver.resolve(ctx, player, card, payload);

            verify(ctx, never()).addEvent(any());
        }

        @Test
        void getType_shouldReturnCONDITION_REMOVE() {
            ConditionRemoveResolver resolver = new ConditionRemoveResolver();
            assertEquals(EffectType.CONDITION_REMOVE, resolver.getType());
        }
    }

    @Nested
    class DamageModifyResolverTest {

        @Test
        void resolve_shouldStoreDamageModifier() {
            DamageModifyResolver resolver = new DamageModifyResolver();
            TurnFlags flags = new TurnFlags();

            when(state.getTurnFlags()).thenReturn(flags);

            Map<String, Object> payload = new HashMap<>();
            payload.put("targetPokemonInstanceId", "target-id");
            payload.put("modifierValue", 20);

            resolver.resolve(ctx, player, card, payload);

            assertNotNull(flags.getDamageModifiers());
            assertEquals(20, flags.getDamageModifiers().get("target-id"));
        }

        @Test
        void resolve_withNullTargetId_shouldReturnEarly() {
            DamageModifyResolver resolver = new DamageModifyResolver();

            resolver.resolve(ctx, player, card, new HashMap<>());

            verify(state, never()).getTurnFlags();
        }

        @Test
        void resolve_withExistingModifiers_shouldAppend() {
            DamageModifyResolver resolver = new DamageModifyResolver();
            TurnFlags flags = new TurnFlags();
            Map<String, Object> existing = new HashMap<>();
            existing.put("other-id", 10);
            flags.setDamageModifiers(existing);

            when(state.getTurnFlags()).thenReturn(flags);

            Map<String, Object> payload = new HashMap<>();
            payload.put("targetPokemonInstanceId", "target-id");
            payload.put("modifierValue", -10);

            resolver.resolve(ctx, player, card, payload);

            assertEquals(2, flags.getDamageModifiers().size());
            assertEquals(-10, flags.getDamageModifiers().get("target-id"));
            assertEquals(10, flags.getDamageModifiers().get("other-id"));
        }

        @Test
        void getType_shouldReturnDAMAGE_MODIFY() {
            DamageModifyResolver resolver = new DamageModifyResolver();
            assertEquals(EffectType.DAMAGE_MODIFY, resolver.getType());
        }
    }

    @Nested
    class DiscardOpponentEnergyResolverTest {

        @Test
        void resolve_shouldDiscardOpponentEnergy() {
            DiscardOpponentEnergyResolver resolver = new DiscardOpponentEnergyResolver();
            PlayerState opponent = mock(PlayerState.class);
            UUID opponentId = UUID.randomUUID();
            PokemonInPlay target = createPokemonInPlay();
            CardInstance energyCard = createCardInstance("energy-1");
            target.setAttachedEnergies(new ArrayList<>(List.of(energyCard)));
            EnergyCardDefinition energyDef = new EnergyCardDefinition();
            energyDef.setEnergyCardType(EnergyCardType.BASIC);

            List<CardInstance> opponentDiscard = new ArrayList<>();
            List<PokemonInPlay> opponentBench = new ArrayList<>();

            when(ctx.getOpponent(playerId)).thenReturn(opponent);
            when(opponent.getPlayerId()).thenReturn(opponentId);
            when(ctx.getPlayer(opponentId)).thenReturn(opponent);
            when(opponent.getActivePokemon()).thenReturn(target);
            when(opponent.getBench()).thenReturn(opponentBench);
            when(cardLookup.getCardById("energy-1")).thenReturn(energyDef);
            when(opponent.getDiscard()).thenReturn(opponentDiscard);

            Map<String, Object> payload = new HashMap<>();
            payload.put("targetPokemonInstanceId", target.getInstanceId().toString());

            resolver.resolve(ctx, player, card, payload);

            assertTrue(target.getAttachedEnergies().isEmpty());
            assertEquals(1, opponentDiscard.size());
            verify(ctx).addEvent(eventCaptor.capture());
            assertEquals(GameEventType.OPPONENT_ENERGY_DISCARDED.name(), eventCaptor.getValue().getType());
        }

        @Test
        void resolve_withNoOpponent_shouldReturnEarly() {
            DiscardOpponentEnergyResolver resolver = new DiscardOpponentEnergyResolver();
            when(ctx.getOpponent(playerId)).thenReturn(null);

            Map<String, Object> payload = new HashMap<>();
            payload.put("targetPokemonInstanceId", UUID.randomUUID().toString());

            assertThrows(NullPointerException.class, () -> resolver.resolve(ctx, player, card, payload));
        }

        @Test
        void getType_shouldReturnDISCARD_OPPONENT_ENERGY() {
            DiscardOpponentEnergyResolver resolver = new DiscardOpponentEnergyResolver();
            assertEquals(EffectType.DISCARD_OPPONENT_ENERGY, resolver.getType());
        }
    }

    @Nested
    class EvolveDirectResolverTest {

        @Test
        void resolve_withNullCurrentDef_shouldReturnEarly() {
            EvolveDirectResolver resolver = new EvolveDirectResolver();
            PokemonInPlay target = createPokemonInPlay();
            target.setCardDefinitionId("pkm-1");

            when(player.getActivePokemon()).thenReturn(target);
            when(cardLookup.getCardById("pkm-1")).thenReturn(null);

            Map<String, Object> payload = new HashMap<>();
            payload.put("targetPokemonInstanceId", target.getInstanceId().toString());

            resolver.resolve(ctx, player, card, payload);

            verify(ctx, never()).addEvent(any());
        }

        @Test
        void getType_shouldReturnEVOLVE_DIRECT() {
            EvolveDirectResolver resolver = new EvolveDirectResolver();
            assertEquals(EffectType.EVOLVE_DIRECT, resolver.getType());
        }
    }

    @Nested
    class EvolveSearchResolverTest {

        @Test
        void resolve_shouldSearchAndAddToHand() {
            EvolveSearchResolver resolver = new EvolveSearchResolver();
            List<CardInstance> deck = new ArrayList<>();
            CardInstance pokemonCard = createCardInstance("pkm-1");
            deck.add(pokemonCard);
            PokemonCardDefinition pkmDef = createPokemonDef();

            List<CardInstance> hand = new ArrayList<>();

            when(player.getDeck()).thenReturn(deck);
            when(player.getHand()).thenReturn(hand);
            when(cardLookup.getCardById("pkm-1")).thenReturn(pkmDef);

            Map<String, Object> payload = new HashMap<>();
            payload.put("targetCardIndex", 0);

            resolver.resolve(ctx, player, card, payload);

            assertTrue(deck.isEmpty());
            assertEquals(1, hand.size());
            assertSame(pokemonCard, hand.get(0));
            verify(randomizer).shuffle(deck);
        }

        @Test
        void resolve_withInvalidIndex_shouldReturnEarly() {
            EvolveSearchResolver resolver = new EvolveSearchResolver();
            Map<String, Object> payload = new HashMap<>();

            resolver.resolve(ctx, player, card, payload);

            verify(cardLookup, never()).getCardById(any());
        }

        @Test
        void resolve_withNonPokemonCard_shouldSkip() {
            EvolveSearchResolver resolver = new EvolveSearchResolver();
            List<CardInstance> deck = new ArrayList<>();
            deck.add(createCardInstance("energy-1"));

            when(player.getDeck()).thenReturn(deck);
            when(cardLookup.getCardById("energy-1")).thenReturn(mock(CardDefinition.class));

            Map<String, Object> payload = new HashMap<>();
            payload.put("targetCardIndex", 0);

            resolver.resolve(ctx, player, card, payload);

            assertEquals(1, deck.size());
        }

        @Test
        void getType_shouldReturnEVOLVE_SEARCH() {
            EvolveSearchResolver resolver = new EvolveSearchResolver();
            assertEquals(EffectType.EVOLVE_SEARCH, resolver.getType());
        }
    }

    @Nested
    class LookTopSearchResolverTest {

        @Test
        void resolve_shouldSearchTopCardsAndAddToHand() {
            LookTopSearchResolver resolver = new LookTopSearchResolver();
            List<CardInstance> deck = new ArrayList<>(createCards(7));
            List<CardInstance> hand = new ArrayList<>();
            PokemonCardDefinition pkmDef = createPokemonDef();

            when(player.getDeck()).thenReturn(deck);
            when(player.getHand()).thenReturn(hand);
            when(cardLookup.getCardById(anyString())).thenReturn(pkmDef);

            Map<String, Object> payload = new HashMap<>();
            payload.put("targetCardIndex", 2);

            resolver.resolve(ctx, player, card, payload);

            assertEquals(1, hand.size());
            verify(randomizer).shuffle(deck);
        }

        @Test
        void resolve_withNullIndex_shouldReturnEarly() {
            LookTopSearchResolver resolver = new LookTopSearchResolver();
            when(player.getDeck()).thenReturn(new ArrayList<>());

            resolver.resolve(ctx, player, card, new HashMap<>());

            verify(cardLookup, never()).getCardById(any());
        }

        @Test
        void getType_shouldReturnLOOK_TOP_SEARCH() {
            LookTopSearchResolver resolver = new LookTopSearchResolver();
            assertEquals(EffectType.LOOK_TOP_SEARCH, resolver.getType());
        }
    }

    @Nested
    class ReturnPokemonToDeckResolverTest {

        @Test
        void resolve_shouldReturnBenchPokemonToDeck() {
            ReturnPokemonToDeckResolver resolver = new ReturnPokemonToDeckResolver();
            List<PokemonInPlay> bench = new ArrayList<>();
            PokemonInPlay target = createPokemonInPlay();
            bench.add(target);
            List<CardInstance> deck = new ArrayList<>();

            when(player.getActivePokemon()).thenReturn(null);
            when(player.getBench()).thenReturn(bench);
            when(player.getDeck()).thenReturn(deck);

            Map<String, Object> payload = new HashMap<>();
            payload.put("targetPokemonInstanceId", target.getInstanceId().toString());

            resolver.resolve(ctx, player, card, payload);

            assertTrue(bench.isEmpty());
            assertEquals(1, deck.size());
            assertEquals(target.getInstanceId(), deck.get(0).getInstanceId());
            assertEquals(target.getCardDefinitionId(), deck.get(0).getCardDefinitionId());
            verify(randomizer).shuffle(deck);
        }

        @Test
        void resolve_withTargetNotFound_shouldReturnEarly() {
            ReturnPokemonToDeckResolver resolver = new ReturnPokemonToDeckResolver();
            when(player.getActivePokemon()).thenReturn(null);
            when(player.getBench()).thenReturn(new ArrayList<>());

            Map<String, Object> payload = new HashMap<>();
            payload.put("targetPokemonInstanceId", UUID.randomUUID().toString());

            resolver.resolve(ctx, player, card, payload);

            verify(randomizer, never()).shuffle(any());
        }

        @Test
        void getType_shouldReturnRETURN_POKEMON_TO_DECK() {
            ReturnPokemonToDeckResolver resolver = new ReturnPokemonToDeckResolver();
            assertEquals(EffectType.RETURN_POKEMON_TO_DECK, resolver.getType());
        }
    }

    @Nested
    class SearchEnergyResolverTest {

        @Test
        void resolve_shouldSearchAndAttachBasicEnergy() {
            SearchEnergyResolver resolver = new SearchEnergyResolver();
            List<CardInstance> deck = new ArrayList<>();
            CardInstance energyCard = createCardInstance("energy-1");
            deck.add(energyCard);
            EnergyCardDefinition energyDef = new EnergyCardDefinition();
            energyDef.setEnergyCardType(EnergyCardType.BASIC);
            PokemonInPlay target = createPokemonInPlay();

            when(player.getDeck()).thenReturn(deck);
            when(cardLookup.getCardById("energy-1")).thenReturn(energyDef);
            when(player.getActivePokemon()).thenReturn(target);

            Map<String, Object> payload = new HashMap<>();
            payload.put("targetCardIndex", 0);
            payload.put("targetPokemonInstanceId", target.getInstanceId().toString());

            resolver.resolve(ctx, player, card, payload);

            verify(energyService).attachFromDeck(energyCard, target, player, ctx);
        }

        @Test
        void resolve_withInvalidIndex_shouldReturnEarly() {
            SearchEnergyResolver resolver = new SearchEnergyResolver();

            resolver.resolve(ctx, player, card, new HashMap<>());

            verify(energyService, never()).attachFromDeck(any(), any(), any(), any());
        }

        @Test
        void getType_shouldReturnSEARCH_ENERGY() {
            SearchEnergyResolver resolver = new SearchEnergyResolver();
            assertEquals(EffectType.SEARCH_ENERGY, resolver.getType());
        }
    }

    @Nested
    class SearchEnergyToHandResolverTest {

        @Test
        void resolve_shouldSearchBasicEnergiesToHand() {
            SearchEnergyToHandResolver resolver = new SearchEnergyToHandResolver();
            List<CardInstance> deck = new ArrayList<>();
            CardInstance energy1 = createCardInstance("energy-1");
            CardInstance energy2 = createCardInstance("energy-2");
            CardInstance nonEnergy = createCardInstance("pkm-1");
            deck.add(nonEnergy);
            deck.add(energy1);
            deck.add(energy2);
            List<CardInstance> hand = new ArrayList<>();

            EnergyCardDefinition energyDef = new EnergyCardDefinition();
            energyDef.setEnergyCardType(EnergyCardType.BASIC);

            when(player.getDeck()).thenReturn(deck);
            when(player.getHand()).thenReturn(hand);
            when(cardLookup.getCardById("energy-1")).thenReturn(energyDef);
            when(cardLookup.getCardById("energy-2")).thenReturn(energyDef);
            when(cardLookup.getCardById("pkm-1")).thenReturn(mock(CardDefinition.class));

            Map<String, Object> payload = new HashMap<>();
            payload.put("targetCardIndexes", List.of(0, 1));

            resolver.resolve(ctx, player, card, payload);

            assertEquals(2, hand.size());
            assertEquals(1, deck.size());
            verify(randomizer).shuffle(deck);
        }

        @Test
        void resolve_withNullIndexes_shouldReturnEarly() {
            SearchEnergyToHandResolver resolver = new SearchEnergyToHandResolver();

            resolver.resolve(ctx, player, card, new HashMap<>());

            verify(randomizer, never()).shuffle(any());
        }

        @Test
        void getType_shouldReturnSEARCH_ENERGY_TO_HAND() {
            SearchEnergyToHandResolver resolver = new SearchEnergyToHandResolver();
            assertEquals(EffectType.SEARCH_ENERGY_TO_HAND, resolver.getType());
        }
    }

    @Nested
    class StadiumPlayResolverTest {

        @Test
        void resolve_shouldPlayStadiumFromHand() {
            StadiumPlayResolver resolver = new StadiumPlayResolver();
            List<CardInstance> hand = new ArrayList<>();
            CardInstance stadiumCard = createCardInstance("stadium-1");
            hand.add(stadiumCard);

            when(player.getHand()).thenReturn(hand);

            Map<String, Object> payload = new HashMap<>();
            payload.put("handIndex", 0);

            resolver.resolve(ctx, player, card, payload);

            verify(state).setStadiumCardInstanceId(stadiumCard.getInstanceId());
            verify(state).setStadiumCardDefinitionId("stadium-1");
            verify(state).setStadiumOwnerPlayerId(playerId);
            assertTrue(hand.isEmpty());
            verify(ctx).addEvent(eventCaptor.capture());
            assertEquals(GameEventType.STADIUM_PLAYED.name(), eventCaptor.getValue().getType());
        }

        @Test
        void resolve_withInvalidHandIndex_shouldReturnEarly() {
            StadiumPlayResolver resolver = new StadiumPlayResolver();

            Map<String, Object> payload = new HashMap<>();
            payload.put("handIndex", -1);

            resolver.resolve(ctx, player, card, payload);

            verify(state, never()).setStadiumCardInstanceId(any());
        }

        @Test
        void getType_shouldReturnSTADIUM_PLAY() {
            StadiumPlayResolver resolver = new StadiumPlayResolver();
            assertEquals(EffectType.STADIUM_PLAY, resolver.getType());
        }
    }

    @Nested
    class ToolAttachResolverTest {

        @Test
        void resolve_shouldAttachToolToPokemon() {
            ToolAttachResolver resolver = new ToolAttachResolver();
            PokemonInPlay target = createPokemonInPlay();
            List<CardInstance> hand = new ArrayList<>();
            CardInstance toolCard = createCardInstance("tool-1");
            hand.add(toolCard);

            when(player.getActivePokemon()).thenReturn(target);
            when(player.getHand()).thenReturn(hand);

            Map<String, Object> payload = new HashMap<>();
            payload.put("targetPokemonInstanceId", target.getInstanceId().toString());
            payload.put("handIndex", 0);

            resolver.resolve(ctx, player, card, payload);

            assertEquals(toolCard.getInstanceId(), target.getToolCardInstanceId());
            assertSame(toolCard, target.getAttachedTool());
            verify(ctx).addEvent(eventCaptor.capture());
            assertEquals(GameEventType.TOOL_ATTACHED.name(), eventCaptor.getValue().getType());
        }

        @Test
        void resolve_withNullTargetId_shouldReturnEarly() {
            ToolAttachResolver resolver = new ToolAttachResolver();

            Map<String, Object> payload = new HashMap<>();
            payload.put("handIndex", 0);

            resolver.resolve(ctx, player, card, payload);

            verify(ctx, never()).addEvent(any());
        }

        @Test
        void resolve_whenToolAlreadyEquipped_shouldReturnEarly() {
            ToolAttachResolver resolver = new ToolAttachResolver();
            PokemonInPlay target = createPokemonInPlay();
            target.setToolCardInstanceId(UUID.randomUUID());

            when(player.getActivePokemon()).thenReturn(target);

            Map<String, Object> payload = new HashMap<>();
            payload.put("targetPokemonInstanceId", target.getInstanceId().toString());

            resolver.resolve(ctx, player, card, payload);

            verify(ctx, never()).addEvent(any());
        }

        @Test
        void getType_shouldReturnTOOL_ATTACH() {
            ToolAttachResolver resolver = new ToolAttachResolver();
            assertEquals(EffectType.TOOL_ATTACH, resolver.getType());
        }
    }

    private CardInstance createCardInstance(String cardDefId) {
        CardInstance ci = new CardInstance();
        ci.setInstanceId(UUID.randomUUID());
        ci.setCardDefinitionId(cardDefId);
        return ci;
    }

    private PokemonInPlay createPokemonInPlay() {
        PokemonInPlay pkm = new PokemonInPlay();
        pkm.setInstanceId(UUID.randomUUID());
        pkm.setCardDefinitionId("pkm-id");
        pkm.setOwnerPlayerId(playerId);
        pkm.setDamageCounters(0);
        pkm.setAttachedEnergies(new ArrayList<>());
        pkm.setSpecialConditions(new ArrayList<>());
        return pkm;
    }

    private PokemonCardDefinition createPokemonDef() {
        PokemonCardDefinition def = new PokemonCardDefinition();
        def.setStage("BASIC");
        return def;
    }

    private List<CardInstance> createCards(int count) {
        List<CardInstance> cards = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            cards.add(createCardInstance("card-" + i));
        }
        return cards;
    }
}
