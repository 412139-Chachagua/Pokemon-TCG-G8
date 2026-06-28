package ar.edu.utn.frc.tup.piii.engine.energy;

import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;

@FunctionalInterface
public interface ModifierCondition {
    boolean test(int baseDamage, PokemonInPlay attacker, PokemonInPlay defender, CardLookupPort cardLookup);
}
