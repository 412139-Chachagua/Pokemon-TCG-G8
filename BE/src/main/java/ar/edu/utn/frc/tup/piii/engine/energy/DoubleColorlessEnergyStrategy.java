package ar.edu.utn.frc.tup.piii.engine.energy;

import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import java.util.Set;

public class DoubleColorlessEnergyStrategy implements EnergyResolutionStrategy {

    private static final Set<EnergyType> DCE_TYPES = Set.of(EnergyType.COLORLESS);

    @Override
    public EnergyStrategyKey getKey() {
        return EnergyStrategyKey.DOUBLE_COLORLESS;
    }

    @Override
    public EnergySource resolve(CardLookupPort cardLookup, CardInstance card, EnergyCardDefinition def) {
        return new EnergySource(
            card.getInstanceId(),
            card.getCardDefinitionId(),
            2,
            DCE_TYPES,
            MatchBehavior.EXACT
        );
    }
}
