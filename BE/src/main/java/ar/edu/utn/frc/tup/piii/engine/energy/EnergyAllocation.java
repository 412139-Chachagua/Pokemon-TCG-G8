package ar.edu.utn.frc.tup.piii.engine.energy;

import ar.edu.utn.frc.tup.piii.domain.cards.EnergyType;
import java.util.UUID;

public record EnergyAllocation(
    int costIndex,
    EnergyType requiredType,
    UUID sourceId,
    int unitsConsumedFromSource,
    String matchReason
) {}
