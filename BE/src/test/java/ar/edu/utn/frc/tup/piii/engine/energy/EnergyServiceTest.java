package ar.edu.utn.frc.tup.piii.engine.energy;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardType;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyType;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import ar.edu.utn.frc.tup.piii.engine.ports.RandomizerPort;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnergyServiceTest {

    // ──────────────────────────────────────────────
    // EnergyService tests
    // ──────────────────────────────────────────────

    @Nested
    class EnergyServiceMethodTest {

        @Mock
        private EnergyMatchingEngine matchingEngine;

        @Mock
        private EnergyStrategyRegistry registry;

        @Mock
        private CardLookupPort cardLookup;

        private EnergyService sut;

        private UUID playerId;
        private PlayerState player;
        private PokemonInPlay pokemon;
        private CardInstance energyCard;
        private CardInstance otherEnergyCard;
        private EnergyCardDefinition energyDef;
        private EnergyCardDefinition otherEnergyDef;
        private EnergySource energySource;
        private EnergySource otherEnergySource;

        void setUpBasic() {
            sut = new EnergyService(matchingEngine, registry);
            playerId = UUID.randomUUID();
            player = new PlayerState();
            player.setPlayerId(playerId);
            player.setHand(new ArrayList<>());
            player.setDeck(new ArrayList<>());
            player.setDiscard(new ArrayList<>());

            pokemon = new PokemonInPlay();
            pokemon.setInstanceId(UUID.randomUUID());
            pokemon.setCardDefinitionId("pkm-1");
            pokemon.setAttachedEnergies(new ArrayList<>());

            energyCard = new CardInstance(UUID.randomUUID(), "energy-1");
            otherEnergyCard = new CardInstance(UUID.randomUUID(), "energy-2");

            energyDef = new EnergyCardDefinition();
            energyDef.setEnergyCardType(EnergyCardType.BASIC);
            energyDef.setProvides(List.of(EnergyType.FIRE));
            energyDef.setStrategyKey("BASIC");

            otherEnergyDef = new EnergyCardDefinition();
            otherEnergyDef.setEnergyCardType(EnergyCardType.BASIC);
            otherEnergyDef.setProvides(List.of(EnergyType.WATER));
            otherEnergyDef.setStrategyKey("BASIC");

            energySource = new EnergySource(
                energyCard.getInstanceId(), energyCard.getCardDefinitionId(),
                1, Set.of(EnergyType.FIRE), MatchBehavior.EXACT);
            otherEnergySource = new EnergySource(
                otherEnergyCard.getInstanceId(), otherEnergyCard.getCardDefinitionId(),
                1, Set.of(EnergyType.WATER), MatchBehavior.EXACT);
        }

        @Test
        void buildPool_withAttachedEnergies_returnsPool() {
            setUpBasic();
            EnergyResolutionStrategy strategy = mock(EnergyResolutionStrategy.class);
            when(strategy.resolve(cardLookup, energyCard, energyDef)).thenReturn(energySource);
            when(strategy.resolve(cardLookup, otherEnergyCard, otherEnergyDef)).thenReturn(otherEnergySource);
            when(registry.getStrategy(energyDef)).thenReturn(strategy);
            when(registry.getStrategy(otherEnergyDef)).thenReturn(strategy);
            when(cardLookup.getCardById("energy-1")).thenReturn(energyDef);
            when(cardLookup.getCardById("energy-2")).thenReturn(otherEnergyDef);
            pokemon.getAttachedEnergies().add(energyCard);
            pokemon.getAttachedEnergies().add(otherEnergyCard);

            List<EnergySource> pool = sut.buildPool(pokemon, cardLookup);

            assertEquals(2, pool.size());
            assertTrue(pool.contains(energySource));
            assertTrue(pool.contains(otherEnergySource));
        }

        @Test
        void buildPool_withFilterIds_returnsFilteredPool() {
            setUpBasic();
            EnergyResolutionStrategy strategy = mock(EnergyResolutionStrategy.class);
            when(strategy.resolve(cardLookup, energyCard, energyDef)).thenReturn(energySource);
            when(registry.getStrategy(energyDef)).thenReturn(strategy);
            when(cardLookup.getCardById("energy-1")).thenReturn(energyDef);
            pokemon.getAttachedEnergies().add(energyCard);
            pokemon.getAttachedEnergies().add(otherEnergyCard);

            List<EnergySource> pool = sut.buildPool(pokemon, cardLookup, List.of(energyCard.getInstanceId()));

            assertEquals(1, pool.size());
            assertEquals(energySource, pool.getFirst());
        }

        @Test
        void buildPool_withNullAttachedEnergies_returnsEmptyList() {
            setUpBasic();
            pokemon.setAttachedEnergies(null);

            List<EnergySource> pool = sut.buildPool(pokemon, cardLookup);

            assertTrue(pool.isEmpty());
        }

        @Test
        void buildPool_withNonEnergyCard_skipsIt() {
            setUpBasic();
            CardDefinition nonEnergy = mock(CardDefinition.class);
            when(cardLookup.getCardById("energy-1")).thenReturn(nonEnergy);
            pokemon.getAttachedEnergies().add(energyCard);

            List<EnergySource> pool = sut.buildPool(pokemon, cardLookup);

            assertTrue(pool.isEmpty());
        }

        @Test
        void checkAttackRequirements_withSufficientEnergy_returnsTrue() {
            setUpBasic();
            PokemonCardDefinition pkmDef = new PokemonCardDefinition();
            PokemonCardDefinition.AttackDefinition atk = new PokemonCardDefinition.AttackDefinition();
            atk.setIndex(0);
            atk.setCost(List.of(EnergyType.FIRE));
            pkmDef.setAttacks(List.of(atk));
            when(cardLookup.getCardById("pkm-1")).thenReturn(pkmDef);
            EnergyResolutionStrategy strategy = mock(EnergyResolutionStrategy.class);
            when(strategy.resolve(cardLookup, energyCard, energyDef)).thenReturn(energySource);
            when(registry.getStrategy(energyDef)).thenReturn(strategy);
            when(cardLookup.getCardById("energy-1")).thenReturn(energyDef);
            pokemon.getAttachedEnergies().add(energyCard);
            EnergyPaymentResult success = new EnergyPaymentResult(true, List.of(), null);
            when(matchingEngine.selectPayment(any(), any())).thenReturn(success);

            boolean result = sut.checkAttackRequirements(pokemon, cardLookup, 0);

            assertTrue(result);
        }

        @Test
        void checkAttackRequirements_withInsufficientEnergy_returnsFalse() {
            setUpBasic();
            PokemonCardDefinition pkmDef = new PokemonCardDefinition();
            PokemonCardDefinition.AttackDefinition atk = new PokemonCardDefinition.AttackDefinition();
            atk.setIndex(0);
            atk.setCost(List.of(EnergyType.FIRE));
            pkmDef.setAttacks(List.of(atk));
            when(cardLookup.getCardById("pkm-1")).thenReturn(pkmDef);
            EnergyPaymentResult failure = new EnergyPaymentResult(false, List.of(), "Not enough energy");
            when(matchingEngine.selectPayment(any(), any())).thenReturn(failure);

            boolean result = sut.checkAttackRequirements(pokemon, cardLookup, 0);

            assertFalse(result);
        }

        @Test
        void checkAttackRequirements_withInvalidAttackIndex_returnsFalse() {
            setUpBasic();
            PokemonCardDefinition pkmDef = new PokemonCardDefinition();
            pkmDef.setAttacks(new ArrayList<>());
            when(cardLookup.getCardById("pkm-1")).thenReturn(pkmDef);

            boolean result = sut.checkAttackRequirements(pokemon, cardLookup, 0);

            assertFalse(result);
        }

        @Test
        void checkAttackRequirements_withNullAttacks_returnsFalse() {
            setUpBasic();
            PokemonCardDefinition pkmDef = new PokemonCardDefinition();
            when(cardLookup.getCardById("pkm-1")).thenReturn(pkmDef);

            boolean result = sut.checkAttackRequirements(pokemon, cardLookup, 0);

            assertFalse(result);
        }

        @Test
        void checkAttackRequirements_withNullCost_returnsTrue() {
            setUpBasic();
            PokemonCardDefinition pkmDef = new PokemonCardDefinition();
            PokemonCardDefinition.AttackDefinition atk = new PokemonCardDefinition.AttackDefinition();
            atk.setIndex(0);
            pkmDef.setAttacks(List.of(atk));
            when(cardLookup.getCardById("pkm-1")).thenReturn(pkmDef);

            boolean result = sut.checkAttackRequirements(pokemon, cardLookup, 0);

            assertTrue(result);
        }

        @Test
        void checkAttackRequirements_withRainbowEnergy_returnsTrue() {
            setUpBasic();
            PokemonCardDefinition pkmDef = new PokemonCardDefinition();
            PokemonCardDefinition.AttackDefinition atk = new PokemonCardDefinition.AttackDefinition();
            atk.setIndex(0);
            atk.setCost(List.of(EnergyType.PSYCHIC));
            pkmDef.setAttacks(List.of(atk));
            when(cardLookup.getCardById("pkm-1")).thenReturn(pkmDef);

            CardInstance rainbowCard = new CardInstance(UUID.randomUUID(), "rainbow-1");
            EnergyCardDefinition rainbowDef = new EnergyCardDefinition();
            rainbowDef.setEnergyCardType(EnergyCardType.SPECIAL);
            rainbowDef.setStrategyKey("RAINBOW");

            EnergySource rainbowSource = new EnergySource(
                rainbowCard.getInstanceId(), rainbowCard.getCardDefinitionId(),
                1, Set.of(EnergyType.values()), MatchBehavior.FLEXIBLE);

            EnergyResolutionStrategy strategy = mock(EnergyResolutionStrategy.class);
            when(strategy.resolve(cardLookup, rainbowCard, rainbowDef)).thenReturn(rainbowSource);
            when(registry.getStrategy(rainbowDef)).thenReturn(strategy);
            when(cardLookup.getCardById("rainbow-1")).thenReturn(rainbowDef);
            pokemon.getAttachedEnergies().add(rainbowCard);

            EnergyPaymentResult success = new EnergyPaymentResult(true, List.of(), null);
            when(matchingEngine.selectPayment(any(), any())).thenReturn(success);

            boolean result = sut.checkAttackRequirements(pokemon, cardLookup, 0);

            assertTrue(result);
        }

        @Test
        void checkAttackRequirements_withDoubleColorlessEnergy_returnsTrue() {
            setUpBasic();
            PokemonCardDefinition pkmDef = new PokemonCardDefinition();
            PokemonCardDefinition.AttackDefinition atk = new PokemonCardDefinition.AttackDefinition();
            atk.setIndex(0);
            atk.setCost(List.of(EnergyType.COLORLESS, EnergyType.COLORLESS));
            pkmDef.setAttacks(List.of(atk));
            when(cardLookup.getCardById("pkm-1")).thenReturn(pkmDef);

            CardInstance dceCard = new CardInstance(UUID.randomUUID(), "dce-1");
            EnergyCardDefinition dceDef = new EnergyCardDefinition();
            dceDef.setEnergyCardType(EnergyCardType.SPECIAL);
            dceDef.setStrategyKey("DOUBLE_COLORLESS");

            EnergySource dceSource = new EnergySource(
                dceCard.getInstanceId(), dceCard.getCardDefinitionId(),
                2, Set.of(EnergyType.COLORLESS), MatchBehavior.EXACT);

            EnergyResolutionStrategy strategy = mock(EnergyResolutionStrategy.class);
            when(strategy.resolve(cardLookup, dceCard, dceDef)).thenReturn(dceSource);
            when(registry.getStrategy(dceDef)).thenReturn(strategy);
            when(cardLookup.getCardById("dce-1")).thenReturn(dceDef);
            pokemon.getAttachedEnergies().add(dceCard);

            EnergyPaymentResult success = new EnergyPaymentResult(true, List.of(), null);
            when(matchingEngine.selectPayment(any(), any())).thenReturn(success);

            boolean result = sut.checkAttackRequirements(pokemon, cardLookup, 0);

            assertTrue(result);
        }

        @Test
        void checkAttackRequirements_withStrongEnergy_returnsTrue() {
            setUpBasic();
            PokemonCardDefinition pkmDef = new PokemonCardDefinition();
            PokemonCardDefinition.AttackDefinition atk = new PokemonCardDefinition.AttackDefinition();
            atk.setIndex(0);
            atk.setCost(List.of(EnergyType.FIGHTING));
            pkmDef.setAttacks(List.of(atk));
            when(cardLookup.getCardById("pkm-1")).thenReturn(pkmDef);

            CardInstance strongCard = new CardInstance(UUID.randomUUID(), "strong-1");
            EnergyCardDefinition strongDef = new EnergyCardDefinition();
            strongDef.setEnergyCardType(EnergyCardType.SPECIAL);
            strongDef.setStrategyKey("STRONG");

            EnergySource strongSource = new EnergySource(
                strongCard.getInstanceId(), strongCard.getCardDefinitionId(),
                1, Set.of(EnergyType.FIGHTING), MatchBehavior.EXACT);

            EnergyResolutionStrategy strategy = mock(EnergyResolutionStrategy.class);
            when(strategy.resolve(cardLookup, strongCard, strongDef)).thenReturn(strongSource);
            when(registry.getStrategy(strongDef)).thenReturn(strategy);
            when(cardLookup.getCardById("strong-1")).thenReturn(strongDef);
            pokemon.getAttachedEnergies().add(strongCard);

            EnergyPaymentResult success = new EnergyPaymentResult(true, List.of(), null);
            when(matchingEngine.selectPayment(any(), any())).thenReturn(success);

            boolean result = sut.checkAttackRequirements(pokemon, cardLookup, 0);

            assertTrue(result);
        }

        @Test
        void checkAttackRequirements_withMixedTypes_returnsTrue() {
            setUpBasic();
            PokemonCardDefinition pkmDef = new PokemonCardDefinition();
            PokemonCardDefinition.AttackDefinition atk = new PokemonCardDefinition.AttackDefinition();
            atk.setIndex(0);
            atk.setCost(List.of(EnergyType.FIRE, EnergyType.COLORLESS));
            pkmDef.setAttacks(List.of(atk));
            when(cardLookup.getCardById("pkm-1")).thenReturn(pkmDef);

            EnergyResolutionStrategy strategy = mock(EnergyResolutionStrategy.class);
            when(strategy.resolve(cardLookup, energyCard, energyDef)).thenReturn(energySource);
            when(strategy.resolve(cardLookup, otherEnergyCard, otherEnergyDef)).thenReturn(otherEnergySource);
            when(registry.getStrategy(energyDef)).thenReturn(strategy);
            when(registry.getStrategy(otherEnergyDef)).thenReturn(strategy);
            when(cardLookup.getCardById("energy-1")).thenReturn(energyDef);
            when(cardLookup.getCardById("energy-2")).thenReturn(otherEnergyDef);
            pokemon.getAttachedEnergies().add(energyCard);
            pokemon.getAttachedEnergies().add(otherEnergyCard);

            EnergyPaymentResult success = new EnergyPaymentResult(true, List.of(), null);
            when(matchingEngine.selectPayment(any(), any())).thenReturn(success);

            boolean result = sut.checkAttackRequirements(pokemon, cardLookup, 0);

            assertTrue(result);
        }

        @Test
        void validateAndPayRetreat_withSufficientEnergy_returnsSuccess() {
            setUpBasic();
            PokemonCardDefinition pkmDef = new PokemonCardDefinition();
            pkmDef.setRetreatCost(List.of(EnergyType.COLORLESS));
            when(cardLookup.getCardById("pkm-1")).thenReturn(pkmDef);
            EnergyResolutionStrategy strategy = mock(EnergyResolutionStrategy.class);
            when(strategy.resolve(cardLookup, energyCard, energyDef)).thenReturn(energySource);
            when(registry.getStrategy(energyDef)).thenReturn(strategy);
            when(cardLookup.getCardById("energy-1")).thenReturn(energyDef);
            pokemon.getAttachedEnergies().add(energyCard);
            EnergyPaymentResult success = new EnergyPaymentResult(true, List.of(), null);
            when(matchingEngine.selectPayment(any(), any())).thenReturn(success);

            EnergyPaymentResult result = sut.validateAndPayRetreat(pokemon, null, cardLookup);

            assertTrue(result.canPay());
        }

        @Test
        void validateAndPayRetreat_withInvalidEnergyId_returnsFailure() {
            setUpBasic();
            UUID fakeId = UUID.randomUUID();

            EnergyPaymentResult result = sut.validateAndPayRetreat(pokemon, List.of(fakeId), cardLookup);

            assertFalse(result.canPay());
            assertNotNull(result.failureReason());
            assertTrue(result.failureReason().contains("Invalid energy instance ID"));
        }

        @Test
        void validateAndPayRetreat_withDuplicateIds_returnsFailure() {
            setUpBasic();
            UUID id = UUID.randomUUID();
            energyCard = new CardInstance(id, "energy-1");
            pokemon.getAttachedEnergies().add(energyCard);

            EnergyPaymentResult result = sut.validateAndPayRetreat(pokemon, List.of(id, id), cardLookup);

            assertFalse(result.canPay());
            assertTrue(result.failureReason().contains("Duplicate"));
        }

        @Test
        void validateAndPayRetreat_withNullRetreatCost_returnsSuccess() {
            setUpBasic();
            PokemonCardDefinition pkmDef = new PokemonCardDefinition();
            when(cardLookup.getCardById("pkm-1")).thenReturn(pkmDef);

            EnergyPaymentResult result = sut.validateAndPayRetreat(pokemon, List.of(), cardLookup);

            assertTrue(result.canPay());
            assertNull(result.failureReason());
        }

        @Test
        void validateAndPayRetreat_withEmptyRetreatCost_returnsSuccess() {
            setUpBasic();
            PokemonCardDefinition pkmDef = new PokemonCardDefinition();
            pkmDef.setRetreatCost(List.of());
            when(cardLookup.getCardById("pkm-1")).thenReturn(pkmDef);

            EnergyPaymentResult result = sut.validateAndPayRetreat(pokemon, List.of(), cardLookup);

            assertTrue(result.canPay());
        }

        @Test
        void validateAndPayRetreat_withNonPokemonDef_returnsFailure() {
            setUpBasic();
            CardDefinition nonPkm = mock(CardDefinition.class);
            when(cardLookup.getCardById("pkm-1")).thenReturn(nonPkm);

            EnergyPaymentResult result = sut.validateAndPayRetreat(pokemon, List.of(), cardLookup);

            assertFalse(result.canPay());
            assertTrue(result.failureReason().contains("Not a Pokemon"));
        }

        @Test
        void validateAndPayRetreat_withInsufficientEnergy_returnsFailure() {
            setUpBasic();
            PokemonCardDefinition pkmDef = new PokemonCardDefinition();
            pkmDef.setRetreatCost(List.of(EnergyType.COLORLESS));
            when(cardLookup.getCardById("pkm-1")).thenReturn(pkmDef);
            // No energies attached -> pool is empty
            EnergyPaymentResult failure = new EnergyPaymentResult(false, List.of(), "Not enough energy");
            when(matchingEngine.selectPayment(any(), any())).thenReturn(failure);

            EnergyPaymentResult result = sut.validateAndPayRetreat(pokemon, null, cardLookup);

            assertFalse(result.canPay());
            assertNotNull(result.failureReason());
        }

        @Test
        void validateAndPayRetreat_withSpecificEnergyDiscard_returnsSuccess() {
            setUpBasic();
            PokemonCardDefinition pkmDef = new PokemonCardDefinition();
            pkmDef.setRetreatCost(List.of(EnergyType.COLORLESS));
            when(cardLookup.getCardById("pkm-1")).thenReturn(pkmDef);
            EnergyResolutionStrategy strategy = mock(EnergyResolutionStrategy.class);
            when(strategy.resolve(cardLookup, energyCard, energyDef)).thenReturn(energySource);
            when(registry.getStrategy(energyDef)).thenReturn(strategy);
            when(cardLookup.getCardById("energy-1")).thenReturn(energyDef);
            pokemon.getAttachedEnergies().add(energyCard);
            EnergyPaymentResult success = new EnergyPaymentResult(true, List.of(), null);
            when(matchingEngine.selectPayment(any(), any())).thenReturn(success);

            EnergyPaymentResult result = sut.validateAndPayRetreat(pokemon, List.of(energyCard.getInstanceId()), cardLookup);

            assertTrue(result.canPay());
        }

        @Test
        void attachFromHand_movesEnergyFromHandToPokemon() {
            setUpBasic();
            player.getHand().add(energyCard);
            GameState state = new GameState();
            state.setTurnNumber(1);
            EngineContext ctx = new EngineContext(state, cardLookup, mock(RandomizerPort.class), null, null);

            sut.attachFromHand(energyCard, pokemon, player, ctx);

            assertTrue(pokemon.getAttachedEnergies().contains(energyCard));
            assertFalse(player.getHand().contains(energyCard));
        }

        @Test
        void attachFromHand_triggersOnAttach() {
            setUpBasic();
            EnergyResolutionStrategy strategy = mock(EnergyResolutionStrategy.class);
            when(registry.getStrategy(energyDef)).thenReturn(strategy);
            when(cardLookup.getCardById("energy-1")).thenReturn(energyDef);
            player.getHand().add(energyCard);
            GameState state = new GameState();
            state.setTurnNumber(1);
            state.setMatchId(UUID.randomUUID());
            EngineContext ctx = new EngineContext(state, cardLookup, mock(RandomizerPort.class), null, null);

            sut.attachFromHand(energyCard, pokemon, player, ctx);

            verify(strategy).onAttach(eq(energyCard), eq(pokemon), eq(ctx), any(AttachmentContext.class));
        }

        @Test
        void attachFromDeck_movesEnergyFromDeckToPokemon() {
            setUpBasic();
            player.getDeck().add(energyCard);
            RandomizerPort randomizer = mock(RandomizerPort.class);
            GameState state = new GameState();
            state.setTurnNumber(1);
            EngineContext ctx = new EngineContext(state, cardLookup, randomizer, null, null);

            sut.attachFromDeck(energyCard, pokemon, player, ctx);

            assertTrue(pokemon.getAttachedEnergies().contains(energyCard));
            assertFalse(player.getDeck().contains(energyCard));
            verify(randomizer).shuffle(player.getDeck());
        }

        @Test
        void detachEnergies_removesSpecificEnergies() {
            setUpBasic();
            pokemon.getAttachedEnergies().add(energyCard);
            pokemon.getAttachedEnergies().add(otherEnergyCard);

            GameState state = new GameState();
            state.setTurnNumber(1);
            EngineContext ctx = new EngineContext(state, cardLookup, null, null, null);

            sut.detachEnergies(pokemon, player, List.of(energyCard), ctx);

            assertFalse(pokemon.getAttachedEnergies().contains(energyCard));
            assertTrue(pokemon.getAttachedEnergies().contains(otherEnergyCard));
            assertTrue(player.getDiscard().contains(energyCard));
        }

        @Test
        void detachEnergies_withNullList_doesNothing() {
            setUpBasic();
            pokemon.getAttachedEnergies().add(energyCard);

            sut.detachEnergies(pokemon, player, null, mock(EngineContext.class));

            assertTrue(pokemon.getAttachedEnergies().contains(energyCard));
        }

        @Test
        void detachEnergies_withEmptyList_doesNothing() {
            setUpBasic();
            pokemon.getAttachedEnergies().add(energyCard);

            sut.detachEnergies(pokemon, player, List.of(), mock(EngineContext.class));

            assertTrue(pokemon.getAttachedEnergies().contains(energyCard));
        }

        @Test
        void detachAllEnergies_removesAll() {
            setUpBasic();
            pokemon.getAttachedEnergies().add(energyCard);
            pokemon.getAttachedEnergies().add(otherEnergyCard);

            GameState state = new GameState();
            state.setTurnNumber(1);
            EngineContext ctx = new EngineContext(state, cardLookup, null, null, null);

            sut.detachAllEnergies(pokemon, player, ctx);

            assertTrue(pokemon.getAttachedEnergies().isEmpty());
            assertTrue(player.getDiscard().contains(energyCard));
            assertTrue(player.getDiscard().contains(otherEnergyCard));
        }

        @Test
        void detachAllEnergies_withNullList_doesNothing() {
            setUpBasic();
            pokemon.setAttachedEnergies(null);

            sut.detachAllEnergies(pokemon, player, mock(EngineContext.class));

            assertNull(pokemon.getAttachedEnergies());
        }

        @Test
        void transferEnergy_movesEnergyBetweenPokemon() {
            setUpBasic();
            PokemonInPlay target = new PokemonInPlay();
            target.setInstanceId(UUID.randomUUID());
            target.setAttachedEnergies(new ArrayList<>());
            pokemon.getAttachedEnergies().add(energyCard);

            GameState state = new GameState();
            state.setTurnNumber(1);
            EngineContext ctx = new EngineContext(state, cardLookup, null, null, null);

            sut.transferEnergy(energyCard, pokemon, target, player, ctx);

            assertFalse(pokemon.getAttachedEnergies().contains(energyCard));
            assertTrue(target.getAttachedEnergies().contains(energyCard));
        }

        @Test
        void calculateDamageBonus_withStrongEnergy_addsBonus() {
            setUpBasic();
            EnergyResolutionStrategy strategy = mock(EnergyResolutionStrategy.class);
            DamageModifier modifier = new DamageModifier(
                "src-1", "Strong Energy", ModifierOperator.ADD, 20,
                (baseDamage, attacker, defender, lookup) -> baseDamage > 0);
            when(strategy.getDamageModifiers(energyCard, pokemon, cardLookup)).thenReturn(List.of(modifier));
            when(registry.getStrategy(energyDef)).thenReturn(strategy);
            when(cardLookup.getCardById("energy-1")).thenReturn(energyDef);
            pokemon.getAttachedEnergies().add(energyCard);

            int bonus = sut.calculateDamageBonus(pokemon, null, cardLookup, 40);

            assertEquals(20, bonus);
        }

        @Test
        void calculateDamageBonus_withNoEnergies_returnsZero() {
            setUpBasic();
            pokemon.setAttachedEnergies(null);

            int bonus = sut.calculateDamageBonus(pokemon, null, cardLookup, 40);

            assertEquals(0, bonus);
        }
    }

    // ──────────────────────────────────────────────
    // EnergyMatchingEngine tests
    // ──────────────────────────────────────────────

    @Nested
    class EnergyMatchingEngineTest {

        private final EnergyMatchingEngine sut = new EnergyMatchingEngine();

        private EnergySource fireSource;
        private EnergySource waterSource;
        private EnergySource colorlessSource;
        private EnergySource rainbowSource;

        void setUp() {
            fireSource = new EnergySource(UUID.randomUUID(), "fire-1", 1,
                Set.of(EnergyType.FIRE), MatchBehavior.EXACT);
            waterSource = new EnergySource(UUID.randomUUID(), "water-1", 1,
                Set.of(EnergyType.WATER), MatchBehavior.EXACT);
            colorlessSource = new EnergySource(UUID.randomUUID(), "dce-1", 2,
                Set.of(EnergyType.COLORLESS), MatchBehavior.EXACT);
            rainbowSource = new EnergySource(UUID.randomUUID(), "rainbow-1", 1,
                Set.of(EnergyType.GRASS, EnergyType.FIRE, EnergyType.WATER, EnergyType.LIGHTNING,
                    EnergyType.PSYCHIC, EnergyType.FIGHTING, EnergyType.DARKNESS, EnergyType.METAL,
                    EnergyType.FAIRY, EnergyType.COLORLESS), MatchBehavior.FLEXIBLE);
        }

        @Test
        void selectPayment_withNullCost_returnsSuccess() {
            setUp();
            EnergyPaymentResult result = sut.selectPayment(List.of(), null);
            assertTrue(result.canPay());
        }

        @Test
        void selectPayment_withEmptyCost_returnsSuccess() {
            setUp();
            EnergyPaymentResult result = sut.selectPayment(List.of(), List.of());
            assertTrue(result.canPay());
        }

        @Test
        void selectPayment_withSufficientExactEnergy_returnsSuccess() {
            setUp();
            EnergyPaymentResult result = sut.selectPayment(List.of(fireSource), List.of(EnergyType.FIRE));
            assertTrue(result.canPay());
            assertEquals(1, result.allocations().size());
        }

        @Test
        void selectPayment_withInsufficientEnergy_returnsFailure() {
            setUp();
            EnergyPaymentResult result = sut.selectPayment(List.of(fireSource), List.of(EnergyType.WATER));
            assertFalse(result.canPay());
            assertNotNull(result.failureReason());
        }

        @Test
        void selectPayment_withColorlessCost_usesAnySource() {
            setUp();
            EnergyPaymentResult result = sut.selectPayment(List.of(fireSource), List.of(EnergyType.COLORLESS));
            assertTrue(result.canPay());
        }

        @Test
        void selectPayment_withDoubleColorless_forTwoColorless() {
            setUp();
            EnergyPaymentResult result = sut.selectPayment(
                List.of(colorlessSource), List.of(EnergyType.COLORLESS, EnergyType.COLORLESS));
            assertTrue(result.canPay());
            assertEquals(2, result.allocations().size());
            assertEquals(1, result.selectedInstanceIds().size());
        }

        @Test
        void selectPayment_withRainbowForAnyType() {
            setUp();
            EnergyPaymentResult result = sut.selectPayment(List.of(rainbowSource), List.of(EnergyType.PSYCHIC));
            assertTrue(result.canPay());
        }

        @Test
        void selectPayment_withMixedCost() {
            setUp();
            EnergyPaymentResult result = sut.selectPayment(
                List.of(fireSource, colorlessSource),
                List.of(EnergyType.FIRE, EnergyType.COLORLESS));
            assertTrue(result.canPay());
            assertEquals(2, result.allocations().size());
        }

        @Test
        void selectPayment_whenExactMatchFails_triesWildcard() {
            setUp();
            EnergySource wildcardSource = new EnergySource(UUID.randomUUID(), "wc-1", 1,
                Set.of(EnergyType.FIRE, EnergyType.WATER), MatchBehavior.FLEXIBLE);
            EnergyPaymentResult result = sut.selectPayment(
                List.of(wildcardSource), List.of(EnergyType.WATER));
            assertTrue(result.canPay());
        }
    }

    // ──────────────────────────────────────────────
    // EnergyStrategyRegistry tests
    // ──────────────────────────────────────────────

    @Nested
    class EnergyStrategyRegistryTest {

        private final EnergyStrategyRegistry sut = new EnergyStrategyRegistry();

        @Test
        void registerAndGetStrategy_forBasic_returnsStrategy() {
            BasicEnergyStrategy strategy = new BasicEnergyStrategy();
            sut.register(strategy);

            EnergyCardDefinition def = new EnergyCardDefinition();
            def.setStrategyKey("BASIC");

            EnergyResolutionStrategy result = sut.getStrategy(def);
            assertInstanceOf(BasicEnergyStrategy.class, result);
        }

        @Test
        void registerAndGetStrategy_forDoubleColorless_returnsStrategy() {
            DoubleColorlessEnergyStrategy strategy = new DoubleColorlessEnergyStrategy();
            sut.register(strategy);

            EnergyCardDefinition def = new EnergyCardDefinition();
            def.setStrategyKey("DOUBLE_COLORLESS");

            EnergyResolutionStrategy result = sut.getStrategy(def);
            assertInstanceOf(DoubleColorlessEnergyStrategy.class, result);
        }

        @Test
        void registerAndGetStrategy_forRainbow_returnsStrategy() {
            RainbowEnergyStrategy strategy = new RainbowEnergyStrategy();
            sut.register(strategy);

            EnergyCardDefinition def = new EnergyCardDefinition();
            def.setStrategyKey("RAINBOW");

            EnergyResolutionStrategy result = sut.getStrategy(def);
            assertInstanceOf(RainbowEnergyStrategy.class, result);
        }

        @Test
        void registerAndGetStrategy_forStrong_returnsStrategy() {
            StrongEnergyStrategy strategy = new StrongEnergyStrategy();
            sut.register(strategy);

            EnergyCardDefinition def = new EnergyCardDefinition();
            def.setStrategyKey("STRONG");

            EnergyResolutionStrategy result = sut.getStrategy(def);
            assertInstanceOf(StrongEnergyStrategy.class, result);
        }

        @Test
        void getStrategy_withNullDef_returnsBasic() {
            BasicEnergyStrategy basic = new BasicEnergyStrategy();
            sut.register(basic);

            EnergyResolutionStrategy result = sut.getStrategy(null);
            assertInstanceOf(BasicEnergyStrategy.class, result);
        }

        @Test
        void getStrategy_withNullKey_returnsBasic() {
            BasicEnergyStrategy basic = new BasicEnergyStrategy();
            sut.register(basic);

            EnergyCardDefinition def = new EnergyCardDefinition();

            EnergyResolutionStrategy result = sut.getStrategy(def);
            assertInstanceOf(BasicEnergyStrategy.class, result);
        }

        @Test
        void getStrategy_withInvalidKey_returnsBasic() {
            BasicEnergyStrategy basic = new BasicEnergyStrategy();
            sut.register(basic);

            EnergyCardDefinition def = new EnergyCardDefinition();
            def.setStrategyKey("UNKNOWN");

            EnergyResolutionStrategy result = sut.getStrategy(def);
            assertInstanceOf(BasicEnergyStrategy.class, result);
        }
    }

    // ──────────────────────────────────────────────
    // BasicEnergyStrategy tests
    // ──────────────────────────────────────────────

    @Nested
    class BasicEnergyStrategyTest {

        private final BasicEnergyStrategy sut = new BasicEnergyStrategy();

        @Mock
        private CardLookupPort cardLookup;

        @Test
        void getKey_returnsBASIC() {
            assertEquals(EnergyStrategyKey.BASIC, sut.getKey());
        }

        @Test
        void resolve_withFireType_returnsFireSource() {
            CardInstance card = new CardInstance(UUID.randomUUID(), "fire-energy");
            EnergyCardDefinition def = new EnergyCardDefinition();
            def.setProvides(List.of(EnergyType.FIRE));

            EnergySource source = sut.resolve(cardLookup, card, def);

            assertEquals(card.getInstanceId(), source.cardInstanceId());
            assertEquals(1, source.totalUnits());
            assertEquals(Set.of(EnergyType.FIRE), source.types());
            assertEquals(MatchBehavior.EXACT, source.behavior());
        }

        @Test
        void resolve_withNullProvides_returnsEmptyTypes() {
            CardInstance card = new CardInstance(UUID.randomUUID(), "empty-energy");
            EnergyCardDefinition def = new EnergyCardDefinition();

            EnergySource source = sut.resolve(cardLookup, card, def);

            assertTrue(source.types().isEmpty());
        }

        @Test
        void onAttach_doesNotModifyPokemon() {
            CardInstance card = new CardInstance(UUID.randomUUID(), "basic-energy");
            PokemonInPlay target = new PokemonInPlay();
            target.setInstanceId(UUID.randomUUID());
            target.setDamageCounters(0);
            GameState state = new GameState();
            state.setMatchId(UUID.randomUUID());
            state.setTurnNumber(1);
            EngineContext ctx = new EngineContext(state, cardLookup, null, null, null);

            AttachmentContext attachCtx = new AttachmentContext(AttachmentOrigin.FROM_HAND, UUID.randomUUID(), 1);
            sut.onAttach(card, target, ctx, attachCtx);

            assertEquals(0, target.getDamageCounters());
            assertTrue(ctx.getPendingEvents().isEmpty());
        }
    }

    // ──────────────────────────────────────────────
    // DoubleColorlessEnergyStrategy tests
    // ──────────────────────────────────────────────

    @Nested
    class DoubleColorlessEnergyStrategyTest {

        private final DoubleColorlessEnergyStrategy sut = new DoubleColorlessEnergyStrategy();

        @Mock
        private CardLookupPort cardLookup;

        @Test
        void getKey_returnsDoubleColorless() {
            assertEquals(EnergyStrategyKey.DOUBLE_COLORLESS, sut.getKey());
        }

        @Test
        void resolve_returnsTwoColorlessUnits() {
            CardInstance card = new CardInstance(UUID.randomUUID(), "dce-1");
            EnergyCardDefinition def = new EnergyCardDefinition();

            EnergySource source = sut.resolve(cardLookup, card, def);

            assertEquals(card.getInstanceId(), source.cardInstanceId());
            assertEquals(2, source.totalUnits());
            assertEquals(Set.of(EnergyType.COLORLESS), source.types());
            assertEquals(MatchBehavior.EXACT, source.behavior());
        }

        @Test
        void getDamageModifiers_returnsEmptyList() {
            CardInstance card = new CardInstance(UUID.randomUUID(), "dce-1");
            PokemonInPlay pokemon = new PokemonInPlay();

            List<DamageModifier> mods = sut.getDamageModifiers(card, pokemon, cardLookup);

            assertTrue(mods.isEmpty());
        }
    }

    // ──────────────────────────────────────────────
    // RainbowEnergyStrategy tests
    // ──────────────────────────────────────────────

    @Nested
    class RainbowEnergyStrategyTest {

        private final RainbowEnergyStrategy sut = new RainbowEnergyStrategy();

        @Mock
        private CardLookupPort cardLookup;

        @Test
        void getKey_returnsRainbow() {
            assertEquals(EnergyStrategyKey.RAINBOW, sut.getKey());
        }

        @Test
        void resolve_returnsAllTypes() {
            CardInstance card = new CardInstance(UUID.randomUUID(), "rainbow-1");
            EnergyCardDefinition def = new EnergyCardDefinition();

            EnergySource source = sut.resolve(cardLookup, card, def);

            assertEquals(card.getInstanceId(), source.cardInstanceId());
            assertEquals(1, source.totalUnits());
            assertEquals(Set.of(EnergyType.values()), source.types());
            assertEquals(MatchBehavior.FLEXIBLE, source.behavior());
        }

        @Test
        void onAttach_fromHand_appliesDamageCounter() {
            CardInstance card = new CardInstance(UUID.randomUUID(), "rainbow-1");
            PokemonInPlay target = new PokemonInPlay();
            target.setInstanceId(UUID.randomUUID());
            target.setDamageCounters(0);
            UUID playerId = UUID.randomUUID();
            GameState state = new GameState();
            state.setMatchId(UUID.randomUUID());
            state.setTurnNumber(1);
            EngineContext ctx = new EngineContext(state, cardLookup, null, null, null);

            AttachmentContext attachCtx = new AttachmentContext(AttachmentOrigin.FROM_HAND, playerId, 1);
            sut.onAttach(card, target, ctx, attachCtx);

            assertEquals(1, target.getDamageCounters());
            assertEquals(1, ctx.getPendingEvents().size());
        }

        @Test
        void onAttach_fromDeck_doesNotApplyDamageCounter() {
            CardInstance card = new CardInstance(UUID.randomUUID(), "rainbow-1");
            PokemonInPlay target = new PokemonInPlay();
            target.setInstanceId(UUID.randomUUID());
            target.setDamageCounters(0);
            UUID playerId = UUID.randomUUID();
            GameState state = new GameState();
            state.setMatchId(UUID.randomUUID());
            state.setTurnNumber(1);
            EngineContext ctx = new EngineContext(state, cardLookup, null, null, null);

            AttachmentContext attachCtx = new AttachmentContext(AttachmentOrigin.FROM_DECK, playerId, 1);
            sut.onAttach(card, target, ctx, attachCtx);

            assertEquals(0, target.getDamageCounters());
            assertTrue(ctx.getPendingEvents().isEmpty());
        }
    }

    // ──────────────────────────────────────────────
    // StrongEnergyStrategy tests
    // ──────────────────────────────────────────────

    @Nested
    class StrongEnergyStrategyTest {

        private final StrongEnergyStrategy sut = new StrongEnergyStrategy();

        @Mock
        private CardLookupPort cardLookup;

        @Test
        void getKey_returnsStrong() {
            assertEquals(EnergyStrategyKey.STRONG, sut.getKey());
        }

        @Test
        void resolve_returnsFightingType() {
            CardInstance card = new CardInstance(UUID.randomUUID(), "strong-1");
            EnergyCardDefinition def = new EnergyCardDefinition();

            EnergySource source = sut.resolve(cardLookup, card, def);

            assertEquals(card.getInstanceId(), source.cardInstanceId());
            assertEquals(1, source.totalUnits());
            assertEquals(Set.of(EnergyType.FIGHTING), source.types());
            assertEquals(MatchBehavior.EXACT, source.behavior());
        }

        @Test
        void getDamageModifiers_withFightingPokemon_returnsAdd20Modifier() {
            CardInstance card = new CardInstance(UUID.randomUUID(), "strong-1");
            PokemonInPlay pokemon = new PokemonInPlay();
            pokemon.setCardDefinitionId("fighting-pkm");
            PokemonCardDefinition pkmDef = new PokemonCardDefinition();
            pkmDef.setTypes(List.of(EnergyType.FIGHTING));
            when(cardLookup.getCardById("fighting-pkm")).thenReturn(pkmDef);

            List<DamageModifier> mods = sut.getDamageModifiers(card, pokemon, cardLookup);

            assertEquals(1, mods.size());
            DamageModifier mod = mods.getFirst();
            assertEquals(ModifierOperator.ADD, mod.operator());
            assertEquals(20, mod.value());

            int applied = mod.applyTo(40, 40, pokemon, null, cardLookup);
            assertEquals(60, applied);
        }

        @Test
        void getDamageModifiers_withNonFightingPokemon_returnsEmpty() {
            CardInstance card = new CardInstance(UUID.randomUUID(), "strong-1");
            PokemonInPlay pokemon = new PokemonInPlay();
            pokemon.setCardDefinitionId("water-pkm");
            PokemonCardDefinition pkmDef = new PokemonCardDefinition();
            pkmDef.setTypes(List.of(EnergyType.WATER));
            when(cardLookup.getCardById("water-pkm")).thenReturn(pkmDef);

            List<DamageModifier> mods = sut.getDamageModifiers(card, pokemon, cardLookup);

            assertTrue(mods.isEmpty());
        }

        @Test
        void getDamageModifiers_withNullTypes_returnsEmpty() {
            CardInstance card = new CardInstance(UUID.randomUUID(), "strong-1");
            PokemonInPlay pokemon = new PokemonInPlay();
            pokemon.setCardDefinitionId("unknown-pkm");
            when(cardLookup.getCardById("unknown-pkm")).thenReturn(new PokemonCardDefinition());

            List<DamageModifier> mods = sut.getDamageModifiers(card, pokemon, cardLookup);

            assertTrue(mods.isEmpty());
        }
    }

    // ──────────────────────────────────────────────
    // EnergyPaymentResult tests
    // ──────────────────────────────────────────────

    @Nested
    class EnergyPaymentResultTest {

        @Test
        void constructor_withSuccess_createsValidResult() {
            EnergyAllocation allocation = new EnergyAllocation(0, EnergyType.FIRE, UUID.randomUUID(), 1, "match");
            EnergyPaymentResult result = new EnergyPaymentResult(true, List.of(allocation), null);

            assertTrue(result.canPay());
            assertEquals(1, result.allocations().size());
            assertNull(result.failureReason());
        }

        @Test
        void constructor_withFailure_createsFailedResult() {
            EnergyPaymentResult result = new EnergyPaymentResult(false, List.of(), "Not enough energy");

            assertFalse(result.canPay());
            assertTrue(result.allocations().isEmpty());
            assertEquals("Not enough energy", result.failureReason());
        }

        @Test
        void selectedInstanceIds_returnsDistinctIds() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            EnergyAllocation a1 = new EnergyAllocation(0, EnergyType.COLORLESS, id1, 1, "first");
            EnergyAllocation a2 = new EnergyAllocation(1, EnergyType.COLORLESS, id1, 1, "second");
            EnergyAllocation a3 = new EnergyAllocation(2, EnergyType.FIRE, id2, 1, "third");
            EnergyPaymentResult result = new EnergyPaymentResult(true, List.of(a1, a2, a3), null);

            List<UUID> ids = result.selectedInstanceIds();

            assertEquals(2, ids.size());
            assertTrue(ids.contains(id1));
            assertTrue(ids.contains(id2));
        }

        @Test
        void unitsPerSource_returnsCorrectCounts() {
            UUID id1 = UUID.randomUUID();
            EnergyAllocation a1 = new EnergyAllocation(0, EnergyType.COLORLESS, id1, 1, "first");
            EnergyAllocation a2 = new EnergyAllocation(1, EnergyType.COLORLESS, id1, 1, "second");
            EnergyPaymentResult result = new EnergyPaymentResult(true, List.of(a1, a2), null);

            Map<UUID, Integer> units = result.unitsPerSource();

            assertEquals(2, units.get(id1));
        }
    }

    // ──────────────────────────────────────────────
    // EnergySource tests
    // ──────────────────────────────────────────────

    @Nested
    class EnergySourceTest {

        @Test
        void constructor_setsFields() {
            UUID id = UUID.randomUUID();
            EnergySource source = new EnergySource(id, "card-1", 2, Set.of(EnergyType.FIRE), MatchBehavior.EXACT);

            assertEquals(id, source.cardInstanceId());
            assertEquals("card-1", source.cardDefinitionId());
            assertEquals(2, source.totalUnits());
            assertEquals(Set.of(EnergyType.FIRE), source.types());
            assertEquals(MatchBehavior.EXACT, source.behavior());
        }

        @Test
        void canPayExact_withMatchingType_returnsTrue() {
            EnergySource source = new EnergySource(UUID.randomUUID(), "c", 1, Set.of(EnergyType.FIRE), MatchBehavior.EXACT);

            assertTrue(source.canPayExact(EnergyType.FIRE));
        }

        @Test
        void canPayExact_withNonMatchingType_returnsFalse() {
            EnergySource source = new EnergySource(UUID.randomUUID(), "c", 1, Set.of(EnergyType.FIRE), MatchBehavior.EXACT);

            assertFalse(source.canPayExact(EnergyType.WATER));
        }
    }

    // ──────────────────────────────────────────────
    // EnergyAllocation tests
    // ──────────────────────────────────────────────

    @Nested
    class EnergyAllocationTest {

        @Test
        void constructor_setsFields() {
            UUID id = UUID.randomUUID();
            EnergyAllocation allocation = new EnergyAllocation(0, EnergyType.FIRE, id, 1, "exact match");

            assertEquals(0, allocation.costIndex());
            assertEquals(EnergyType.FIRE, allocation.requiredType());
            assertEquals(id, allocation.sourceId());
            assertEquals(1, allocation.unitsConsumedFromSource());
            assertEquals("exact match", allocation.matchReason());
        }
    }
}
