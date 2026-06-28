package ar.edu.utn.frc.tup.piii.domain.cards;

import java.util.List;

public class EnergyCardDefinition extends CardDefinition {
    private EnergyCardType energyCardType;
    private List<EnergyType> provides;
    private String strategyKey;

    public EnergyCardType getEnergyCardType() { return energyCardType; }
    public void setEnergyCardType(EnergyCardType energyCardType) { this.energyCardType = energyCardType; }
    public List<EnergyType> getProvides() { return provides; }
    public void setProvides(List<EnergyType> provides) { this.provides = provides; }
    public String getStrategyKey() { return strategyKey; }
    public void setStrategyKey(String strategyKey) { this.strategyKey = strategyKey; }
}
