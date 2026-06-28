package ar.edu.utn.frc.tup.piii.engine.energy;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyType;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import java.util.List;
import java.util.Set;

public class StrongEnergyStrategy implements EnergyResolutionStrategy {

    private static final Set<EnergyType> FIGHTING = Set.of(EnergyType.FIGHTING);

    @Override
    public EnergyStrategyKey getKey() {
        return EnergyStrategyKey.STRONG;
    }

    @Override
    public EnergySource resolve(CardLookupPort cardLookup, CardInstance card, EnergyCardDefinition def) {
        return new EnergySource(
            card.getInstanceId(),
            card.getCardDefinitionId(),
            1,
            FIGHTING,
            MatchBehavior.EXACT
        );
    }

    @Override
    public List<DamageModifier> getDamageModifiers(CardInstance card, PokemonInPlay pokemon, CardLookupPort cardLookup) {
        CardDefinition def = cardLookup.getCardById(pokemon.getCardDefinitionId());
        if (!(def instanceof PokemonCardDefinition pkmDef)) return List.of();
        boolean isFighting = pkmDef.getTypes() != null && pkmDef.getTypes().contains(EnergyType.FIGHTING);
        if (!isFighting) return List.of();
        return List.of(new DamageModifier(
            card.getInstanceId().toString(),
            "Strong Energy",
            ModifierOperator.ADD,
            20,
            (baseDamage, attacker, defender, lookup) -> baseDamage > 0
        ));
    }
}
