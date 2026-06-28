package ar.edu.utn.frc.tup.piii.engine.ports.impl;

import ar.edu.utn.frc.tup.piii.domain.cards.*;
import ar.edu.utn.frc.tup.piii.repositories.entities.CardAttackEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.CardEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.CardResistanceEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.CardWeaknessEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.CardJpaRepository;
import ar.edu.utn.frc.tup.piii.services.cards.CardCacheSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardLookupAdapterTest {

    @Mock private CardJpaRepository cardJpaRepository;
    @Mock private CardCacheSyncService cardCacheSyncService;

    private CardLookupAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CardLookupAdapter(cardJpaRepository, cardCacheSyncService);
    }

    private CardEntity createPokemonEntity() {
        CardEntity e = new CardEntity();
        e.setId("pokemon-1");
        e.setName("Pikachu");
        e.setSupertype("Pokemon");
        e.setHp(60);
        e.setPokemonStage("BASIC");
        e.setPokemonTypes("LIGHTNING");
        e.setRetreatCost("COLORLESS");
        e.setIsEx(false);
        e.setIsMega(false);
        e.setSubtypes("");
        e.setSetCode("base1");
        e.setNumber("25");
        e.setImageSmallUrl("https://example.com/small.png");
        e.setImageLargeUrl("https://example.com/large.png");
        e.setRulesText("A rule");
        return e;
    }

    // ----- getCardById -----

    @Test
    void getCardById_foundPokemon_mapsCorrectly() {
        CardEntity entity = createPokemonEntity();
        when(cardJpaRepository.findById("pokemon-1")).thenReturn(Optional.of(entity));

        CardDefinition result = adapter.getCardById("pokemon-1");

        assertNotNull(result);
        assertInstanceOf(PokemonCardDefinition.class, result);
        PokemonCardDefinition pkmn = (PokemonCardDefinition) result;
        assertEquals("Pikachu", pkmn.getName());
        assertEquals(60, pkmn.getHp());
        assertEquals("BASIC", pkmn.getStage());
        assertEquals(List.of(EnergyType.LIGHTNING), pkmn.getTypes());
    }

    @Test
    void getCardById_foundTrainer_mapsCorrectly() {
        CardEntity entity = createPokemonEntity();
        entity.setSupertype("Trainer");
        entity.setTrainerSubtype("SUPPORTER");
        entity.setEffectCode("DRAW_3");
        entity.setIsAceSpec(false);
        when(cardJpaRepository.findById("trainer-1")).thenReturn(Optional.of(entity));

        CardDefinition result = adapter.getCardById("trainer-1");

        assertNotNull(result);
        assertInstanceOf(TrainerCardDefinition.class, result);
        TrainerCardDefinition trainer = (TrainerCardDefinition) result;
        assertEquals("DRAW_3", trainer.getEffectCode());
        assertEquals(TrainerSubtype.SUPPORTER, trainer.getTrainerSubtype());
    }

    @Test
    void getCardById_foundEnergy_mapsCorrectly() {
        CardEntity entity = createPokemonEntity();
        entity.setSupertype("Energy");
        entity.setEnergyCardType("BASIC");
        entity.setProvidesEnergyTypes("LIGHTNING");
        entity.setStrategyKey("BASIC_ENERGY");
        when(cardJpaRepository.findById("energy-1")).thenReturn(Optional.of(entity));

        CardDefinition result = adapter.getCardById("energy-1");

        assertNotNull(result);
        assertInstanceOf(EnergyCardDefinition.class, result);
        EnergyCardDefinition energy = (EnergyCardDefinition) result;
        assertEquals(EnergyCardType.BASIC, energy.getEnergyCardType());
        assertEquals(List.of(EnergyType.LIGHTNING), energy.getProvides());
    }

    @Test
    void getCardById_notFound_returnsNull() {
        when(cardJpaRepository.findById("unknown")).thenReturn(Optional.empty());
        when(cardCacheSyncService.syncCardById("unknown")).thenReturn(false);

        CardDefinition result = adapter.getCardById("unknown");

        assertNull(result);
    }

    @Test
    void getCardById_notFoundThenSynced_returnsCard() {
        CardEntity entity = createPokemonEntity();
        when(cardJpaRepository.findById("fallback-card")).thenReturn(Optional.empty(), Optional.of(entity));
        when(cardCacheSyncService.syncCardById("fallback-card")).thenReturn(true);

        CardDefinition result = adapter.getCardById("fallback-card");

        assertNotNull(result);
        verify(cardCacheSyncService).syncCardById("fallback-card");
    }

    // ----- resolveSupertype -----

    @Test
    void resolveSupertype_null_returnsPokemon() {
        CardEntity entity = createPokemonEntity();
        entity.setSupertype(null);
        when(cardJpaRepository.findById("null-super")).thenReturn(Optional.of(entity));

        CardDefinition result = adapter.getCardById("null-super");

        assertInstanceOf(PokemonCardDefinition.class, result);
    }

    @Test
    void resolveSupertype_variantSpellings() {
        CardEntity entity = createPokemonEntity();
        entity.setSupertype("TRAINER");
        entity.setTrainerSubtype("ITEM");
        when(cardJpaRepository.findById("trainer-up")).thenReturn(Optional.of(entity));

        CardDefinition result = adapter.getCardById("trainer-up");

        assertInstanceOf(TrainerCardDefinition.class, result);
    }

    // ----- toPokemon edge cases -----

    @Test
    void toPokemon_nullIsExAndMega_defaultsToFalse() {
        CardEntity entity = createPokemonEntity();
        entity.setIsEx(null);
        entity.setIsMega(null);
        when(cardJpaRepository.findById("null-bools")).thenReturn(Optional.of(entity));

        PokemonCardDefinition result = (PokemonCardDefinition) adapter.getCardById("null-bools");

        assertFalse(result.isEx());
        assertFalse(result.isMega());
    }

    @Test
    void toPokemon_nullHp_defaultsToZero() {
        CardEntity entity = createPokemonEntity();
        entity.setHp(null);
        when(cardJpaRepository.findById("no-hp")).thenReturn(Optional.of(entity));

        PokemonCardDefinition result = (PokemonCardDefinition) adapter.getCardById("no-hp");

        assertEquals(0, result.getHp());
    }

    @Test
    void toPokemon_nullAttacksAndWeaknesses_skipsMapping() {
        CardEntity entity = createPokemonEntity();
        entity.setAttacks(null);
        entity.setWeaknesses(null);
        entity.setResistances(null);
        when(cardJpaRepository.findById("no-attacks")).thenReturn(Optional.of(entity));

        PokemonCardDefinition result = (PokemonCardDefinition) adapter.getCardById("no-attacks");

        assertNull(result.getAttacks());
        assertNull(result.getWeaknesses());
        assertNull(result.getResistances());
    }

    @Test
    void toPokemon_withAttacksWeaknessesResistances_mapsAll() {
        CardEntity entity = createPokemonEntity();

        CardAttackEntity attack = new CardAttackEntity();
        attack.setAttackIndex(0);
        attack.setName("Thunderbolt");
        attack.setPrintedCost("LIGHTNING,COLORLESS");
        attack.setDamageText("50");
        attack.setEffectText("Paralyze");
        attack.setEffectCode(null);
        entity.setAttacks(List.of(attack));

        CardWeaknessEntity weakness = new CardWeaknessEntity();
        weakness.setEnergyType("FIGHTING");
        weakness.setMultiplier(2);
        entity.setWeaknesses(List.of(weakness));

        CardResistanceEntity resistance = new CardResistanceEntity();
        resistance.setEnergyType("FIGHTING");
        resistance.setValue(-20);
        entity.setResistances(List.of(resistance));

        when(cardJpaRepository.findById("full-pokemon")).thenReturn(Optional.of(entity));

        PokemonCardDefinition result = (PokemonCardDefinition) adapter.getCardById("full-pokemon");

        assertEquals(1, result.getAttacks().size());
        assertEquals("Thunderbolt", result.getAttacks().get(0).getName());
        assertEquals("×2", result.getWeaknesses().get(0).getValue());
        assertEquals("-20", result.getResistances().get(0).getValue());
    }

    @Test
    void toPokemon_isEx_isMega() {
        CardEntity entity = createPokemonEntity();
        entity.setIsEx(true);
        entity.setIsMega(true);
        when(cardJpaRepository.findById("ex-mega")).thenReturn(Optional.of(entity));

        PokemonCardDefinition result = (PokemonCardDefinition) adapter.getCardById("ex-mega");

        assertTrue(result.isEx());
        assertTrue(result.isMega());
    }

    // ----- toTrainer edge cases -----

    @Test
    void toTrainer_nullSubtype_fallsBackToSubtypes() {
        CardEntity entity = createPokemonEntity();
        entity.setSupertype("Trainer");
        entity.setTrainerSubtype(null);
        entity.setSubtypes("ITEM");
        when(cardJpaRepository.findById("trainer-fallback")).thenReturn(Optional.of(entity));

        TrainerCardDefinition result = (TrainerCardDefinition) adapter.getCardById("trainer-fallback");

        assertEquals(TrainerSubtype.ITEM, result.getTrainerSubtype());
    }

    @Test
    void toTrainer_isAceSpec() {
        CardEntity entity = createPokemonEntity();
        entity.setSupertype("Trainer");
        entity.setTrainerSubtype("ITEM");
        entity.setIsAceSpec(true);
        when(cardJpaRepository.findById("ace-spec")).thenReturn(Optional.of(entity));

        TrainerCardDefinition result = (TrainerCardDefinition) adapter.getCardById("ace-spec");

        assertTrue(result.isAceSpec());
    }

    // ----- toAttack edge cases -----

    @Test
    void toAttack_nullEffectCode_parsesEffectText() {
        CardEntity entity = createPokemonEntity();
        CardAttackEntity attack = new CardAttackEntity();
        attack.setAttackIndex(0);
        attack.setName("Tackle");
        attack.setEffectText(null);
        attack.setEffectCode(null);
        entity.setAttacks(List.of(attack));
        when(cardJpaRepository.findById("null-effect")).thenReturn(Optional.of(entity));

        PokemonCardDefinition result = (PokemonCardDefinition) adapter.getCardById("null-effect");

        assertTrue(result.getAttacks().get(0).getEffects().isEmpty());
    }

    @Test
    void toAttack_nullAttackIndex_defaultsToZero() {
        CardEntity entity = createPokemonEntity();
        CardAttackEntity attack = new CardAttackEntity();
        attack.setAttackIndex(null);
        attack.setName("Tackle");
        attack.setDamageText("10");
        entity.setAttacks(List.of(attack));
        when(cardJpaRepository.findById("null-index")).thenReturn(Optional.of(entity));

        PokemonCardDefinition result = (PokemonCardDefinition) adapter.getCardById("null-index");

        assertEquals(0, result.getAttacks().get(0).getIndex());
    }

    // ----- parseEnergyType -----

    @Test
    void parseEnergyType_null_returnsNull() {
        CardEntity entity = createPokemonEntity();
        entity.setSupertype("Energy");
        entity.setEnergyCardType(null);
        when(cardJpaRepository.findById("null-energy-type")).thenReturn(Optional.of(entity));

        EnergyCardDefinition result = (EnergyCardDefinition) adapter.getCardById("null-energy-type");

        assertNull(result.getEnergyCardType());
    }

    // ----- splitList -----

    @Test
    void splitList_null_returnsEmpty() {
        CardEntity entity = createPokemonEntity();
        entity.setSubtypes(null);
        when(cardJpaRepository.findById("null-subtypes")).thenReturn(Optional.of(entity));

        PokemonCardDefinition result = (PokemonCardDefinition) adapter.getCardById("null-subtypes");

        assertTrue(result.getSubtypes().isEmpty());
    }

    // ----- hydrateAbilities -----

    @Test
    void hydrateAbilities_nullJson_returnsEmpty() {
        CardEntity entity = createPokemonEntity();
        entity.setAbilities(null);
        when(cardJpaRepository.findById("no-abilities")).thenReturn(Optional.of(entity));

        PokemonCardDefinition result = (PokemonCardDefinition) adapter.getCardById("no-abilities");

        assertTrue(result.getAbilities().isEmpty());
    }

    @Test
    void hydrateAbilities_validJson_parsesAbilities() {
        CardEntity entity = createPokemonEntity();
        entity.setAbilities("[{\"name\":\"Static\",\"text\":\"May cause paralysis\",\"type\":\"Ability\"}]");
        when(cardJpaRepository.findById("with-abilities")).thenReturn(Optional.of(entity));

        PokemonCardDefinition result = (PokemonCardDefinition) adapter.getCardById("with-abilities");

        assertEquals(1, result.getAbilities().size());
        assertEquals("Static", result.getAbilities().get(0).getName());
        assertEquals(AbilityType.ABILITY, result.getAbilities().get(0).getType());
    }

    @Test
    void hydrateAbilities_invalidJson_returnsEmpty() {
        CardEntity entity = createPokemonEntity();
        entity.setAbilities("{invalid json}");
        when(cardJpaRepository.findById("bad-abilities")).thenReturn(Optional.of(entity));

        PokemonCardDefinition result = (PokemonCardDefinition) adapter.getCardById("bad-abilities");

        assertTrue(result.getAbilities().isEmpty());
    }

    // ----- toWeakness / toResistance edge cases -----

    @Test
    void toWeakness_nullMultiplier_returnsNullValue() {
        CardEntity entity = createPokemonEntity();
        CardWeaknessEntity w = new CardWeaknessEntity();
        w.setEnergyType("WATER");
        w.setMultiplier(null);
        entity.setWeaknesses(List.of(w));
        when(cardJpaRepository.findById("null-mult")).thenReturn(Optional.of(entity));

        PokemonCardDefinition result = (PokemonCardDefinition) adapter.getCardById("null-mult");

        assertNull(result.getWeaknesses().get(0).getValue());
    }

    @Test
    void toResistance_nullValue_returnsNull() {
        CardEntity entity = createPokemonEntity();
        CardResistanceEntity r = new CardResistanceEntity();
        r.setEnergyType("FIRE");
        r.setValue(null);
        entity.setResistances(List.of(r));
        when(cardJpaRepository.findById("null-res-val")).thenReturn(Optional.of(entity));

        PokemonCardDefinition result = (PokemonCardDefinition) adapter.getCardById("null-res-val");

        assertNull(result.getResistances().get(0).getValue());
    }
}
