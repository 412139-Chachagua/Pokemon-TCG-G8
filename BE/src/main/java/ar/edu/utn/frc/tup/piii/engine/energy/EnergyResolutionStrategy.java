package ar.edu.utn.frc.tup.piii.engine.energy;

import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import java.util.List;

public interface EnergyResolutionStrategy {
    EnergyStrategyKey getKey();
    EnergySource resolve(CardLookupPort cardLookup, CardInstance card, EnergyCardDefinition def);

    default List<DamageModifier> getDamageModifiers(CardInstance card, PokemonInPlay pokemon, CardLookupPort cardLookup) {
        return List.of();
    }

    default void onAttach(CardInstance card, PokemonInPlay target, EngineContext ctx, AttachmentContext attachmentContext) {}

    default void onDetach(CardInstance card, PokemonInPlay source, EngineContext ctx) {}
}
