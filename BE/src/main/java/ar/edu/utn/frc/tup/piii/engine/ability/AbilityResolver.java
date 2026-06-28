package ar.edu.utn.frc.tup.piii.engine.ability;

import ar.edu.utn.frc.tup.piii.domain.cards.AbilityDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;

import java.util.Map;

@FunctionalInterface
public interface AbilityResolver {
    void resolve(EngineContext ctx, PlayerState player, PokemonInPlay pokemon, AbilityDefinition ability, Map<String, Object> payload);
}
