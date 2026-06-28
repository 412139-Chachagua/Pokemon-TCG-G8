package ar.edu.utn.frc.tup.piii.engine.attack;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardType;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyType;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.AbilityDefinition;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DamageCalculatorTest {

    @Mock
    private CardLookupPort cardLookup;

    private PokemonInPlay createPokemon(String cardDefId, int damageCounters) {
        PokemonInPlay p = new PokemonInPlay();
        p.setInstanceId(UUID.randomUUID());
        p.setCardDefinitionId(cardDefId);
        p.setOwnerPlayerId(UUID.randomUUID());
        p.setDamageCounters(damageCounters);
        p.setAttachedEnergies(new ArrayList<>());
        return p;
    }

    private PokemonCardDefinition createAttackerDef(String damage, List<EnergyType> types) {
        PokemonCardDefinition def = new PokemonCardDefinition();
        def.setName("Attacker");
        def.setTypes(types);
        PokemonCardDefinition.AttackDefinition ad = new PokemonCardDefinition.AttackDefinition();
        ad.setIndex(0);
        ad.setDamage(damage);
        ad.setName("Tackle");
        def.setAttacks(List.of(ad));
        return def;
    }

    private PokemonCardDefinition createDefenderDef(
            List<PokemonCardDefinition.WeaknessDefinition> weaknesses,
            List<PokemonCardDefinition.ResistanceDefinition> resistances,
            int hp) {
        PokemonCardDefinition def = new PokemonCardDefinition();
        def.setName("Defender");
        def.setHp(hp);
        def.setWeaknesses(weaknesses);
        def.setResistances(resistances);
        return def;
    }

    @Test
    void calculate_normalDamage_noWeaknessNoResistance_returnsCorrectDamage() {
        PokemonInPlay attacker = createPokemon("att-1", 0);
        PokemonInPlay defender = createPokemon("def-1", 0);
        PokemonCardDefinition attDef = createAttackerDef("40", List.of(EnergyType.COLORLESS));
        PokemonCardDefinition defDef = createDefenderDef(null, null, 100);
        when(cardLookup.getCardById("att-1")).thenReturn(attDef);
        when(cardLookup.getCardById("def-1")).thenReturn(defDef);

        DamageCalculator.DamageCalculatorResult result = DamageCalculator.calculate(attacker, defender, cardLookup, 0);

        assertEquals(40, result.baseDamage());
        assertEquals(1, result.weaknessMultiplier());
        assertEquals(0, result.resistanceValue());
        assertEquals(40, result.finalDamage());
        assertEquals(4, result.damageCountersAdded());
        assertFalse(result.weaknessApplied());
        assertFalse(result.resistanceApplied());
    }

    @Test
    void calculate_withWeakness_appliesMultiplier() {
        PokemonInPlay attacker = createPokemon("att-1", 0);
        PokemonInPlay defender = createPokemon("def-1", 0);
        PokemonCardDefinition attDef = createAttackerDef("40", List.of(EnergyType.FIRE));
        PokemonCardDefinition.WeaknessDefinition w = new PokemonCardDefinition.WeaknessDefinition();
        w.setType(EnergyType.FIRE);
        w.setValue("x2");
        PokemonCardDefinition defDef = createDefenderDef(List.of(w), null, 100);
        when(cardLookup.getCardById("att-1")).thenReturn(attDef);
        when(cardLookup.getCardById("def-1")).thenReturn(defDef);

        DamageCalculator.DamageCalculatorResult result = DamageCalculator.calculate(attacker, defender, cardLookup, 0);

        assertEquals(80, result.finalDamage());
        assertEquals(2, result.weaknessMultiplier());
        assertTrue(result.weaknessApplied());
    }

    @Test
    void calculate_withResistance_subtractsValue() {
        PokemonInPlay attacker = createPokemon("att-1", 0);
        PokemonInPlay defender = createPokemon("def-1", 0);
        PokemonCardDefinition attDef = createAttackerDef("50", List.of(EnergyType.FIRE));
        PokemonCardDefinition.ResistanceDefinition r = new PokemonCardDefinition.ResistanceDefinition();
        r.setType(EnergyType.FIRE);
        r.setValue("-20");
        PokemonCardDefinition defDef = createDefenderDef(null, List.of(r), 100);
        when(cardLookup.getCardById("att-1")).thenReturn(attDef);
        when(cardLookup.getCardById("def-1")).thenReturn(defDef);

        DamageCalculator.DamageCalculatorResult result = DamageCalculator.calculate(attacker, defender, cardLookup, 0);

        assertEquals(30, result.finalDamage());
        assertEquals(-20, result.resistanceValue());
        assertTrue(result.resistanceApplied());
    }

    @Test
    void calculate_damagePreventionNextTurn_returnsZero() {
        PokemonInPlay attacker = createPokemon("att-1", 0);
        PokemonInPlay defender = createPokemon("def-1", 0);
        defender.setPreventAllDamageNextTurn(true);
        when(cardLookup.getCardById("att-1")).thenReturn(createAttackerDef("100", List.of(EnergyType.COLORLESS)));
        when(cardLookup.getCardById("def-1")).thenReturn(createDefenderDef(null, null, 100));

        DamageCalculator.DamageCalculatorResult result = DamageCalculator.calculate(attacker, defender, cardLookup, 0);

        assertEquals(0, result.finalDamage());
        assertEquals(0, result.damageCountersAdded());
        assertFalse(defender.isPreventAllDamageNextTurn());
    }

    @Test
    void calculate_baseDamageOverride_usesOverrideValue() {
        PokemonInPlay attacker = createPokemon("att-1", 0);
        PokemonInPlay defender = createPokemon("def-1", 0);
        when(cardLookup.getCardById("att-1")).thenReturn(createAttackerDef("10", List.of(EnergyType.COLORLESS)));
        when(cardLookup.getCardById("def-1")).thenReturn(createDefenderDef(null, null, 100));

        DamageCalculator.DamageCalculatorResult result = DamageCalculator.calculate(
                attacker, defender, cardLookup, 0, null, 60, false, false);

        assertEquals(60, result.baseDamage());
        assertEquals(60, result.finalDamage());
    }

    @Test
    void calculate_damageModifiers_attackerSide_appliedBeforeWeakness() {
        PokemonInPlay attacker = createPokemon("att-1", 0);
        PokemonInPlay defender = createPokemon("def-1", 0);
        when(cardLookup.getCardById("att-1")).thenReturn(createAttackerDef("40", List.of(EnergyType.COLORLESS)));
        when(cardLookup.getCardById("def-1")).thenReturn(createDefenderDef(null, null, 100));
        Map<String, Object> mods = new HashMap<>();
        mods.put(attacker.getInstanceId().toString(), 10);

        DamageCalculator.DamageCalculatorResult result = DamageCalculator.calculate(
                attacker, defender, cardLookup, 0, mods, null, false, false);

        assertEquals(50, result.finalDamage());
    }

    @Test
    void calculate_damageModifiers_defenderSide_appliedAfterWeakness() {
        PokemonInPlay attacker = createPokemon("att-1", 0);
        PokemonInPlay defender = createPokemon("def-1", 0);
        when(cardLookup.getCardById("att-1")).thenReturn(createAttackerDef("40", List.of(EnergyType.COLORLESS)));
        when(cardLookup.getCardById("def-1")).thenReturn(createDefenderDef(null, null, 100));
        Map<String, Object> mods = new HashMap<>();
        mods.put(defender.getInstanceId().toString(), -10);

        DamageCalculator.DamageCalculatorResult result = DamageCalculator.calculate(
                attacker, defender, cardLookup, 0, mods, null, false, false);

        assertEquals(30, result.finalDamage());
    }

    @Test
    void calculate_negativeModifier_clampsToZero() {
        PokemonInPlay attacker = createPokemon("att-1", 0);
        PokemonInPlay defender = createPokemon("def-1", 0);
        when(cardLookup.getCardById("att-1")).thenReturn(createAttackerDef("10", List.of(EnergyType.COLORLESS)));
        when(cardLookup.getCardById("def-1")).thenReturn(createDefenderDef(null, null, 100));
        Map<String, Object> mods = new HashMap<>();
        mods.put(defender.getInstanceId().toString(), -50);

        DamageCalculator.DamageCalculatorResult result = DamageCalculator.calculate(
                attacker, defender, cardLookup, 0, mods, null, false, false);

        assertEquals(0, result.finalDamage());
    }

    @Test
    void calculate_muscleBandBonus_adds20Damage() {
        PokemonInPlay attacker = createPokemon("att-1", 0);
        PokemonInPlay defender = createPokemon("def-1", 0);
        CardInstance tool = new CardInstance(UUID.randomUUID(), "tool-1");
        attacker.setAttachedTool(tool);
        CardDefinition muscleBand = mock(CardDefinition.class);
        when(muscleBand.getName()).thenReturn("Muscle Band");
        when(cardLookup.getCardById("att-1")).thenReturn(createAttackerDef("40", List.of(EnergyType.COLORLESS)));
        when(cardLookup.getCardById("def-1")).thenReturn(createDefenderDef(null, null, 100));
        when(cardLookup.getCardById("tool-1")).thenReturn(muscleBand);

        DamageCalculator.DamageCalculatorResult result = DamageCalculator.calculate(attacker, defender, cardLookup, 0);

        assertEquals(60, result.finalDamage());
    }

    @Test
    void calculate_hardCharmReduction_reducesBy20() {
        PokemonInPlay attacker = createPokemon("att-1", 0);
        PokemonInPlay defender = createPokemon("def-1", 0);
        CardInstance tool = new CardInstance(UUID.randomUUID(), "tool-1");
        defender.setAttachedTool(tool);
        CardDefinition hardCharm = mock(CardDefinition.class);
        when(hardCharm.getName()).thenReturn("Hard Charm");
        when(cardLookup.getCardById("att-1")).thenReturn(createAttackerDef("50", List.of(EnergyType.COLORLESS)));
        when(cardLookup.getCardById("def-1")).thenReturn(createDefenderDef(null, null, 100));
        when(cardLookup.getCardById("def-1")).thenReturn(createDefenderDef(null, null, 100));
        when(cardLookup.getCardById("tool-1")).thenReturn(hardCharm);
        lenient().when(cardLookup.getCardById("att-1")).thenReturn(createAttackerDef("50", List.of(EnergyType.COLORLESS)));

        DamageCalculator.DamageCalculatorResult result = DamageCalculator.calculate(attacker, defender, cardLookup, 0);

        assertEquals(30, result.finalDamage());
    }

    @Test
    void calculate_furCoatHook_reducesDamage() {
        PokemonInPlay attacker = createPokemon("att-1", 0);
        PokemonInPlay defender = createPokemon("def-1", 0);
        PokemonCardDefinition defDef = createDefenderDef(null, null, 100);
        AbilityDefinition furCoat = new AbilityDefinition("Fur Coat", "Reduces damage", null);
        defDef.setAbilities(List.of(furCoat));
        when(cardLookup.getCardById("att-1")).thenReturn(createAttackerDef("50", List.of(EnergyType.COLORLESS)));
        when(cardLookup.getCardById("def-1")).thenReturn(defDef);

        DamageCalculator.DamageCalculatorResult result = DamageCalculator.calculate(attacker, defender, cardLookup, 0);

        assertEquals(30, result.finalDamage());
    }

    @Test
    void calculate_stadiumShadowCircle_withDarknessEnergy_bypassesWeakness() {
        PokemonInPlay attacker = createPokemon("att-1", 0);
        PokemonInPlay defender = createPokemon("def-1", 0);
        PokemonCardDefinition attDef = createAttackerDef("40", List.of(EnergyType.DARKNESS));
        PokemonCardDefinition.WeaknessDefinition w = new PokemonCardDefinition.WeaknessDefinition();
        w.setType(EnergyType.DARKNESS);
        w.setValue("x2");
        PokemonCardDefinition defDef = createDefenderDef(List.of(w), null, 100);
        CardInstance darkEnergy = new CardInstance(UUID.randomUUID(), "dark-1");
        defender.setAttachedEnergies(new ArrayList<>(List.of(darkEnergy)));
        EnergyCardDefinition darkDef = new EnergyCardDefinition();
        darkDef.setEnergyCardType(EnergyCardType.BASIC);
        darkDef.setProvides(List.of(EnergyType.DARKNESS));
        when(cardLookup.getCardById("att-1")).thenReturn(attDef);
        when(cardLookup.getCardById("def-1")).thenReturn(defDef);
        when(cardLookup.getCardById("dark-1")).thenReturn(darkDef);

        DamageCalculator.DamageCalculatorResult result = DamageCalculator.calculate(
                attacker, defender, cardLookup, 0, null, null, false, false, "SHADOW_CIRCLE");

        assertEquals(40, result.finalDamage());
        assertFalse(result.weaknessApplied());
    }

    @Test
    void calculate_zeroBaseDamage_returnsZero() {
        PokemonInPlay attacker = createPokemon("att-1", 0);
        PokemonInPlay defender = createPokemon("def-1", 0);
        when(cardLookup.getCardById("att-1")).thenReturn(createAttackerDef("0", null));
        when(cardLookup.getCardById("def-1")).thenReturn(createDefenderDef(null, null, 100));

        DamageCalculator.DamageCalculatorResult result = DamageCalculator.calculate(attacker, defender, cardLookup, 0);

        assertEquals(0, result.finalDamage());
        assertEquals(0, result.baseDamage());
    }

    @Test
    void calculate_nextTurnDamageBonus_appliedBeforeWeakness() {
        PokemonInPlay attacker = createPokemon("att-1", 0);
        PokemonInPlay defender = createPokemon("def-1", 0);
        attacker.setNextTurnDamageBonus(20);
        PokemonCardDefinition attDef = createAttackerDef("30", List.of(EnergyType.FIRE));
        PokemonCardDefinition.WeaknessDefinition w = new PokemonCardDefinition.WeaknessDefinition();
        w.setType(EnergyType.FIRE);
        w.setValue("x2");
        PokemonCardDefinition defDef = createDefenderDef(List.of(w), null, 100);
        when(cardLookup.getCardById("att-1")).thenReturn(attDef);
        when(cardLookup.getCardById("def-1")).thenReturn(defDef);

        DamageCalculator.DamageCalculatorResult result = DamageCalculator.calculate(attacker, defender, cardLookup, 0);

        assertEquals(100, result.finalDamage());
        assertEquals(0, attacker.getNextTurnDamageBonus());
    }

    @Test
    void calculate_reduceDamageNextTurn_reducesFinalDamage() {
        PokemonInPlay attacker = createPokemon("att-1", 0);
        PokemonInPlay defender = createPokemon("def-1", 0);
        defender.setReduceDamageNextTurn(30);
        when(cardLookup.getCardById("att-1")).thenReturn(createAttackerDef("50", List.of(EnergyType.COLORLESS)));
        when(cardLookup.getCardById("def-1")).thenReturn(createDefenderDef(null, null, 100));

        DamageCalculator.DamageCalculatorResult result = DamageCalculator.calculate(attacker, defender, cardLookup, 0);

        assertEquals(20, result.finalDamage());
        assertEquals(0, defender.getReduceDamageNextTurn());
    }

    @Test
    void calculate_bypassWeaknessFlag_skipsWeakness() {
        PokemonInPlay attacker = createPokemon("att-1", 0);
        PokemonInPlay defender = createPokemon("def-1", 0);
        PokemonCardDefinition attDef = createAttackerDef("40", List.of(EnergyType.FIRE));
        PokemonCardDefinition.WeaknessDefinition w = new PokemonCardDefinition.WeaknessDefinition();
        w.setType(EnergyType.FIRE);
        w.setValue("x2");
        PokemonCardDefinition defDef = createDefenderDef(List.of(w), null, 100);
        when(cardLookup.getCardById("att-1")).thenReturn(attDef);
        when(cardLookup.getCardById("def-1")).thenReturn(defDef);

        DamageCalculator.DamageCalculatorResult result = DamageCalculator.calculate(
                attacker, defender, cardLookup, 0, null, null, true, false);

        assertEquals(40, result.finalDamage());
        assertFalse(result.weaknessApplied());
    }

    @Test
    void calculate_bypassResistanceFlag_skipsResistance() {
        PokemonInPlay attacker = createPokemon("att-1", 0);
        PokemonInPlay defender = createPokemon("def-1", 0);
        PokemonCardDefinition attDef = createAttackerDef("40", List.of(EnergyType.FIRE));
        PokemonCardDefinition.ResistanceDefinition r = new PokemonCardDefinition.ResistanceDefinition();
        r.setType(EnergyType.FIRE);
        r.setValue("-20");
        PokemonCardDefinition defDef = createDefenderDef(null, List.of(r), 100);
        when(cardLookup.getCardById("att-1")).thenReturn(attDef);
        when(cardLookup.getCardById("def-1")).thenReturn(defDef);

        DamageCalculator.DamageCalculatorResult result = DamageCalculator.calculate(
                attacker, defender, cardLookup, 0, null, null, false, true);

        assertEquals(40, result.finalDamage());
        assertFalse(result.resistanceApplied());
    }

    @Test
    void calculate_attackerNoTypes_noWeaknessOrResistance() {
        PokemonInPlay attacker = createPokemon("att-1", 0);
        PokemonInPlay defender = createPokemon("def-1", 0);
        PokemonCardDefinition attDef = createAttackerDef("40", null);
        PokemonCardDefinition.WeaknessDefinition w = new PokemonCardDefinition.WeaknessDefinition();
        w.setType(EnergyType.COLORLESS);
        w.setValue("x2");
        PokemonCardDefinition defDef = createDefenderDef(List.of(w), null, 100);
        when(cardLookup.getCardById("att-1")).thenReturn(attDef);
        when(cardLookup.getCardById("def-1")).thenReturn(defDef);

        DamageCalculator.DamageCalculatorResult result = DamageCalculator.calculate(attacker, defender, cardLookup, 0);

        assertEquals(40, result.finalDamage());
        assertFalse(result.weaknessApplied());
    }

    @Test
    void calculate_defenderNoWeaknesses_noWeaknessApplied() {
        PokemonInPlay attacker = createPokemon("att-1", 0);
        PokemonInPlay defender = createPokemon("def-1", 0);
        PokemonCardDefinition attDef = createAttackerDef("40", List.of(EnergyType.FIRE));
        PokemonCardDefinition defDef = createDefenderDef(null, null, 100);
        when(cardLookup.getCardById("att-1")).thenReturn(attDef);
        when(cardLookup.getCardById("def-1")).thenReturn(defDef);

        DamageCalculator.DamageCalculatorResult result = DamageCalculator.calculate(attacker, defender, cardLookup, 0);

        assertEquals(40, result.finalDamage());
        assertFalse(result.weaknessApplied());
        assertFalse(result.resistanceApplied());
    }

    @Test
    void parseDamage_validString_returnsCorrectValue() {
        DamageCalculator.DamageParseResult result = DamageCalculator.parseDamage("40");
        assertEquals(40, result.baseValue());
        assertFalse(result.isMultiplier());
    }

    @Test
    void parseDamage_multiplierString_returnsMultiplierFlag() {
        DamageCalculator.DamageParseResult result = DamageCalculator.parseDamage("\u00d710");
        assertEquals(10, result.baseValue());
        assertTrue(result.isMultiplier());
    }

    @Test
    void parseDamage_null_returnsZero() {
        DamageCalculator.DamageParseResult result = DamageCalculator.parseDamage(null);
        assertEquals(0, result.baseValue());
        assertFalse(result.isMultiplier());
    }

    @Test
    void parseDamage_empty_returnsZero() {
        DamageCalculator.DamageParseResult result = DamageCalculator.parseDamage("");
        assertEquals(0, result.baseValue());
        assertFalse(result.isMultiplier());
    }

    @Test
    void parseIntDamage_validString_returnsInt() {
        assertEquals(60, DamageCalculator.parseIntDamage("60"));
    }

    @Test
    void parseIntDamage_null_returnsZero() {
        assertEquals(0, DamageCalculator.parseIntDamage(null));
    }

    @Test
    void calculateMultipliedDamage_multiplier_calculatesProduct() {
        assertEquals(60, DamageCalculator.calculateMultipliedDamage("\u00d710", 6));
    }

    @Test
    void calculateMultipliedDamage_noMultiplier_returnsBaseValue() {
        assertEquals(40, DamageCalculator.calculateMultipliedDamage("40", 5));
    }

    @Test
    void calculateMultipliedDamage_zeroMultiplier_returnsZero() {
        assertEquals(0, DamageCalculator.calculateMultipliedDamage("\u00d710", 0));
    }

    @Test
    void calculateMultipliedDamage_negativeMultiplier_clampsToZero() {
        assertEquals(0, DamageCalculator.calculateMultipliedDamage("\u00d710", -1));
    }

    @Test
    void parseWeaknessMultiplier_x2_returns2() {
        assertEquals(2, DamageCalculator.parseWeaknessMultiplier("x2"));
    }

    @Test
    void parseWeaknessMultiplier_x4_returns4() {
        assertEquals(4, DamageCalculator.parseWeaknessMultiplier("x4"));
    }

    @Test
    void parseWeaknessMultiplier_null_returns1() {
        assertEquals(1, DamageCalculator.parseWeaknessMultiplier(null));
    }

    @Test
    void parseWeaknessMultiplier_blank_returns1() {
        assertEquals(1, DamageCalculator.parseWeaknessMultiplier("  "));
    }

    @Test
    void parseWeaknessMultiplier_plainNumber_returnsNumber() {
        assertEquals(3, DamageCalculator.parseWeaknessMultiplier("3"));
    }

    @Test
    void parseIntResistanceValue_validString_returnsInt() {
        assertEquals(-20, DamageCalculator.parseIntResistanceValue("-20"));
    }

    @Test
    void parseIntResistanceValue_null_returnsZero() {
        assertEquals(0, DamageCalculator.parseIntResistanceValue(null));
    }

    @Test
    void parseIntResistanceValue_blank_returnsZero() {
        assertEquals(0, DamageCalculator.parseIntResistanceValue(""));
    }

    @Test
    void parseIntResistanceValue_invalid_returnsZero() {
        assertEquals(0, DamageCalculator.parseIntResistanceValue("abc"));
    }

    @Test
    void calculate_modifierNull_ignored() {
        PokemonInPlay attacker = createPokemon("att-1", 0);
        PokemonInPlay defender = createPokemon("def-1", 0);
        when(cardLookup.getCardById("att-1")).thenReturn(createAttackerDef("40", List.of(EnergyType.COLORLESS)));
        when(cardLookup.getCardById("def-1")).thenReturn(createDefenderDef(null, null, 100));

        DamageCalculator.DamageCalculatorResult result = DamageCalculator.calculate(
                attacker, defender, cardLookup, 0, null, null, false, false);

        assertEquals(40, result.finalDamage());
    }

    @Test
    void calculate_preventAllDamageAndWeakness_returnsZero() {
        PokemonInPlay attacker = createPokemon("att-1", 0);
        PokemonInPlay defender = createPokemon("def-1", 0);
        defender.setPreventAllDamageNextTurn(true);
        when(cardLookup.getCardById("att-1")).thenReturn(createAttackerDef("100", List.of(EnergyType.FIRE)));
        PokemonCardDefinition.WeaknessDefinition w = new PokemonCardDefinition.WeaknessDefinition();
        w.setType(EnergyType.FIRE);
        w.setValue("x2");
        when(cardLookup.getCardById("def-1")).thenReturn(createDefenderDef(List.of(w), null, 100));

        DamageCalculator.DamageCalculatorResult result = DamageCalculator.calculate(attacker, defender, cardLookup, 0);

        assertEquals(0, result.finalDamage());
        assertFalse(defender.isPreventAllDamageNextTurn());
    }
}
