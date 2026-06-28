package ar.edu.utn.frc.tup.piii.engine.energy;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public record EnergyPaymentResult(
    boolean canPay,
    List<EnergyAllocation> allocations,
    String failureReason
) {
    public List<UUID> selectedInstanceIds() {
        return allocations.stream()
            .map(EnergyAllocation::sourceId)
            .distinct()
            .toList();
    }

    public Map<UUID, Integer> unitsPerSource() {
        return allocations.stream()
            .collect(Collectors.groupingBy(
                EnergyAllocation::sourceId,
                Collectors.summingInt(EnergyAllocation::unitsConsumedFromSource)
            ));
    }
}
