package ar.edu.utn.frc.tup.piii.engine.energy;

import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import java.util.HashSet;
import java.util.Set;

public class BasicEnergyStrategy implements EnergyResolutionStrategy {

    @Override
    public EnergyStrategyKey getKey() {
        return EnergyStrategyKey.BASIC;
    }

    @Override
    public EnergySource resolve(CardLookupPort cardLookup, CardInstance card, EnergyCardDefinition def) {
        Set<EnergyType> types = new HashSet<>();
        if (def.getProvides() != null && !def.getProvides().isEmpty()) {
            types.addAll(def.getProvides());
        }
        return new EnergySource(
            card.getInstanceId(),
            card.getCardDefinitionId(),
            1,
            types,
            MatchBehavior.EXACT
        );
    }
}
