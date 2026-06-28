package ar.edu.utn.frc.tup.piii.engine.attack;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyType;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.ability.hooks.FurCoatHook;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;

import java.util.Map;

public class DamageCalculator {

    public record DamageCalculatorResult(
            int baseDamage,
            int weaknessMultiplier,
            int resistanceValue,
            int finalDamage,
            int damageCountersAdded,
            boolean weaknessApplied,
            boolean resistanceApplied
    ) {}

    public static DamageCalculatorResult calculate(
            PokemonInPlay attacker,
            PokemonInPlay defender,
            CardLookupPort cardLookup,
            int attackIndex) {
        return calculate(attacker, defender, cardLookup, attackIndex, null, null, false, false);
    }

    public static DamageCalculatorResult calculate(
            PokemonInPlay attacker,
            PokemonInPlay defender,
            CardLookupPort cardLookup,
            int attackIndex,
            Map<String, Object> damageModifiers) {
        return calculate(attacker, defender, cardLookup, attackIndex, damageModifiers, null, false, false);
    }

    public static DamageCalculatorResult calculate(
            PokemonInPlay attacker,
            PokemonInPlay defender,
            CardLookupPort cardLookup,
            int attackIndex,
            Map<String, Object> damageModifiers,
            Integer baseDamageOverride) {
        return calculate(attacker, defender, cardLookup, attackIndex, damageModifiers, baseDamageOverride, false, false);
    }

    public static DamageCalculatorResult calculate(
            PokemonInPlay attacker,
            PokemonInPlay defender,
            CardLookupPort cardLookup,
            int attackIndex,
            Map<String, Object> damageModifiers,
            Integer baseDamageOverride,
            boolean bypassWeakness,
            boolean bypassResistance) {

        PokemonCardDefinition attackerDef = (PokemonCardDefinition) cardLookup.getCardById(attacker.getCardDefinitionId());
        PokemonCardDefinition defenderDef = (PokemonCardDefinition) cardLookup.getCardById(defender.getCardDefinitionId());

        if (defender.isPreventAllDamageNextTurn()) {
            defender.setPreventAllDamageNextTurn(false);
            return new DamageCalculatorResult(0, 1, 0, 0, 0, false, false);
        }

        // Step 1: Base damage (use override if provided for multiplier attacks)
        int baseDamage;
        if (baseDamageOverride != null) {
            baseDamage = baseDamageOverride;
        } else {
            String damageStr = attackerDef.getAttacks().get(attackIndex).getDamage();
            baseDamage = parseIntDamage(damageStr);
        }

        if (baseDamage == 0) {
            return new DamageCalculatorResult(0, 1, 0, 0, 0, false, false);
        }

        // Step 2: Apply attacker-side modifiers BEFORE weakness/resistance
        int damageBeforeWeakness = baseDamage;
        damageBeforeWeakness += attacker.getNextTurnDamageBonus();
        attacker.setNextTurnDamageBonus(0);
        damageBeforeWeakness += getMuscleBandBonus(attacker, cardLookup);
        if (damageModifiers != null) {
            Object attackerMod = damageModifiers.get(attacker.getInstanceId().toString());
            if (attackerMod instanceof Number n) {
                damageBeforeWeakness = Math.max(damageBeforeWeakness + n.intValue(), 0);
            }
        }

        // Step 3: Weakness multiplier
        int weaknessMultiplier = 1;
        boolean weaknessApplied = false;
        if (!bypassWeakness && defenderDef.getWeaknesses() != null && attackerDef.getTypes() != null) {
            for (PokemonCardDefinition.WeaknessDefinition w : defenderDef.getWeaknesses()) {
                if (attackerDef.getTypes().contains(w.getType())) {
                    weaknessMultiplier = parseWeaknessMultiplier(w.getValue());
                    weaknessApplied = true;
                    break;
                }
            }
        }

        // Step 4: Resistance subtraction
        int resistanceValue = 0;
        boolean resistanceApplied = false;
        if (!bypassResistance && defenderDef.getResistances() != null && attackerDef.getTypes() != null) {
            for (PokemonCardDefinition.ResistanceDefinition r : defenderDef.getResistances()) {
                if (attackerDef.getTypes().contains(r.getType())) {
                    resistanceValue = parseIntResistanceValue(r.getValue());
                    resistanceApplied = true;
                    break;
                }
            }
        }

        int finalDamage = Math.max(damageBeforeWeakness * weaknessMultiplier + resistanceValue, 0);

        // Step 5: Apply defender-side modifiers AFTER weakness/resistance
        if (damageModifiers != null) {
            Object defenderMod = damageModifiers.get(defender.getInstanceId().toString());
            if (defenderMod instanceof Number n) {
                finalDamage = Math.max(finalDamage + n.intValue(), 0);
            }
        }

        finalDamage = FurCoatHook.reduceDamage(finalDamage, defender, cardLookup);

        finalDamage = Math.max(0, finalDamage - getHardCharmReduction(defender, cardLookup));

        // Moonblast / defensive damage reduction
        int reduceDmg = defender.getReduceDamageNextTurn();
        if (reduceDmg > 0) {
            finalDamage = Math.max(0, finalDamage - reduceDmg);
            defender.setReduceDamageNextTurn(0);
        }

        // Step 6: Convert to damage counters
        int damageCountersAdded = finalDamage / 10;

        return new DamageCalculatorResult(
                baseDamage,
                weaknessMultiplier,
                resistanceValue,
                finalDamage,
                damageCountersAdded,
                weaknessApplied,
                resistanceApplied
        );
    }

    public static DamageCalculatorResult calculate(
            PokemonInPlay attacker,
            PokemonInPlay defender,
            CardLookupPort cardLookup,
            int attackIndex,
            Map<String, Object> damageModifiers,
            Integer baseDamageOverride,
            boolean bypassWeakness,
            boolean bypassResistance,
            String stadiumEffectCode) {
        if ("SHADOW_CIRCLE".equals(stadiumEffectCode)) {
            if (hasDarknessEnergyAttached(defender, cardLookup)) {
                bypassWeakness = true;
            }
        }
        return calculate(attacker, defender, cardLookup, attackIndex, damageModifiers, baseDamageOverride, bypassWeakness, bypassResistance);
    }

    private static boolean hasDarknessEnergyAttached(PokemonInPlay pokemon, CardLookupPort cardLookup) {
        if (pokemon.getAttachedEnergies() == null) return false;
        for (CardInstance ci : pokemon.getAttachedEnergies()) {
            CardDefinition def = cardLookup.getCardById(ci.getCardDefinitionId());
            if (def instanceof EnergyCardDefinition energyDef
                && energyDef.getProvides() != null
                && energyDef.getProvides().contains(EnergyType.DARKNESS)) {
                return true;
            }
        }
        return false;
    }

    public record DamageParseResult(int baseValue, boolean isMultiplier) {}

    public static DamageParseResult parseDamage(String damage) {
        if (damage == null || damage.isBlank()) {
            return new DamageParseResult(0, false);
        }
        boolean isMultiplier = damage.contains("×");
        String numeric = damage.replaceAll("[^0-9-]", "");
        if (numeric.isEmpty() || "-".equals(numeric)) return new DamageParseResult(0, false);
        try {
            return new DamageParseResult(Integer.parseInt(numeric), isMultiplier);
        } catch (NumberFormatException e) {
            return new DamageParseResult(0, false);
        }
    }

    public static int parseIntDamage(String damage) {
        return parseDamage(damage).baseValue();
    }

    public static int calculateMultipliedDamage(String damageStr, int multiplier) {
        DamageParseResult parsed = parseDamage(damageStr);
        if (!parsed.isMultiplier()) return parsed.baseValue();
        return parsed.baseValue() * Math.max(multiplier, 0);
    }

    static int parseWeaknessMultiplier(String value) {
        if (value == null || value.isBlank()) return 1;
        String normalized = value.trim().toLowerCase().replace("×", "x");
        if (normalized.contains("x4")) return 4;
        if (normalized.contains("x2")) return 2;
        try {
            int plain = Integer.parseInt(normalized.replace("x", "").trim());
            if (plain >= 2) return plain;
        } catch (NumberFormatException e) { }
        return 1;
    }

    static int parseIntResistanceValue(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int getMuscleBandBonus(PokemonInPlay pokemon, CardLookupPort cardLookup) {
        if (pokemon.getAttachedTool() == null) return 0;
        CardDefinition toolDef = cardLookup.getCardById(pokemon.getAttachedTool().getCardDefinitionId());
        if (toolDef != null && "Muscle Band".equalsIgnoreCase(toolDef.getName())) {
            return 20;
        }
        return 0;
    }

    private static int getHardCharmReduction(PokemonInPlay pokemon, CardLookupPort cardLookup) {
        if (pokemon.getAttachedTool() == null) return 0;
        CardDefinition toolDef = cardLookup.getCardById(pokemon.getAttachedTool().getCardDefinitionId());
        if (toolDef != null && "Hard Charm".equalsIgnoreCase(toolDef.getName())) {
            return 20;
        }
        return 0;
    }
}
