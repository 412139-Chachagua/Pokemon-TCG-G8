package ar.edu.utn.frc.tup.piii.engine.energy;

import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import java.util.EnumMap;
import java.util.Map;

public class EnergyStrategyRegistry {

    private final Map<EnergyStrategyKey, EnergyResolutionStrategy> strategies = new EnumMap<>(EnergyStrategyKey.class);

    public void register(EnergyResolutionStrategy strategy) {
        strategies.put(strategy.getKey(), strategy);
    }

    public EnergyResolutionStrategy getStrategy(EnergyCardDefinition def) {
        if (def == null) return strategies.get(EnergyStrategyKey.BASIC);
        String keyStr = def.getStrategyKey();
        if (keyStr == null) return strategies.get(EnergyStrategyKey.BASIC);
        try {
            EnergyStrategyKey key = EnergyStrategyKey.valueOf(keyStr);
            return strategies.getOrDefault(key, strategies.get(EnergyStrategyKey.BASIC));
        } catch (IllegalArgumentException e) {
            return strategies.get(EnergyStrategyKey.BASIC);
        }
    }
}
