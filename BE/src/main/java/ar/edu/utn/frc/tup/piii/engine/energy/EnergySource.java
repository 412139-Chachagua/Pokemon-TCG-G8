package ar.edu.utn.frc.tup.piii.engine.energy;

import ar.edu.utn.frc.tup.piii.domain.cards.EnergyType;
import java.util.Set;
import java.util.UUID;

public record EnergySource(
    UUID cardInstanceId,
    String cardDefinitionId,
    int totalUnits,
    Set<EnergyType> types,
    MatchBehavior behavior
) {
    public boolean canPayExact(EnergyType required) {
        return types.contains(required);
    }
}
