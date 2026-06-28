package ar.edu.utn.frc.tup.piii.engine.energy;

import ar.edu.utn.frc.tup.piii.domain.cards.EnergyType;
import java.util.*;

public class EnergyMatchingEngine {

    public EnergyPaymentResult selectPayment(List<EnergySource> pool, List<EnergyType> cost) {
        if (cost == null || cost.isEmpty()) {
            return new EnergyPaymentResult(true, List.of(), null);
        }

        MatchingState state = new MatchingState(pool);
        List<EnergyAllocation> allocations = new ArrayList<>();

        for (int i = 0; i < cost.size(); i++) {
            EnergyType required = cost.get(i);
            EnergyAllocation allocation;
            if (required == EnergyType.COLORLESS) {
                allocation = consumeAny(state, pool, i);
            } else {
                allocation = consumeExact(state, pool, i, required);
            }
            if (allocation == null) {
                return new EnergyPaymentResult(false, allocations,
                    "Cannot pay cost[" + i + "]=" + required);
            }
            allocations.add(allocation);
        }

        return new EnergyPaymentResult(true, allocations, null);
    }

    private EnergyAllocation consumeAny(MatchingState state, List<EnergySource> pool, int costIndex) {
        for (EnergySource source : pool) {
            if (state.canConsume(source)) {
                state.consume(source);
                return new EnergyAllocation(costIndex, EnergyType.COLORLESS,
                    source.cardInstanceId(), 1, "any source (colorless)");
            }
        }
        return null;
    }

    private EnergyAllocation consumeExact(MatchingState state, List<EnergySource> pool,
                                           int costIndex, EnergyType required) {
        for (EnergySource source : pool) {
            if (state.canConsume(source) && source.canPayExact(required)
                    && source.behavior() == MatchBehavior.EXACT) {
                state.consume(source);
                return new EnergyAllocation(costIndex, required,
                    source.cardInstanceId(), 1, "exact type match");
            }
        }
        for (EnergySource source : pool) {
            if (state.canConsume(source) && source.canPayExact(required)) {
                state.consume(source);
                return new EnergyAllocation(costIndex, required,
                    source.cardInstanceId(), 1, "wildcard match");
            }
        }
        return null;
    }

    private static class MatchingState {
        private final Map<UUID, Integer> remaining;

        MatchingState(List<EnergySource> pool) {
            this.remaining = new HashMap<>();
            for (EnergySource source : pool) {
                remaining.put(source.cardInstanceId(), source.totalUnits());
            }
        }

        boolean canConsume(EnergySource source) {
            return remaining.getOrDefault(source.cardInstanceId(), 0) > 0;
        }

        void consume(EnergySource source) {
            int left = remaining.getOrDefault(source.cardInstanceId(), 0);
            if (left <= 0) {
                throw new IllegalStateException(
                    "Cannot consume from depleted source: " + source.cardInstanceId());
            }
            remaining.put(source.cardInstanceId(), left - 1);
        }
    }
}
