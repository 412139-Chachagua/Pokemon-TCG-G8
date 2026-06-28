package ar.edu.utn.frc.tup.piii.engine.energy;

import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;

public record DamageModifier(
    String sourceId,
    String sourceName,
    ModifierOperator operator,
    int value,
    ModifierCondition condition
) {
    public int applyTo(int currentDamage, int baseDamage, PokemonInPlay attacker,
                       PokemonInPlay defender, CardLookupPort cardLookup) {
        if (condition != null && !condition.test(baseDamage, attacker, defender, cardLookup))
            return currentDamage;
        return switch (operator) {
            case ADD -> currentDamage + value;
            case SUBTRACT -> Math.max(currentDamage - value, 0);
            case MULTIPLY -> currentDamage * value;
            case OVERRIDE -> value;
        };
    }
}
