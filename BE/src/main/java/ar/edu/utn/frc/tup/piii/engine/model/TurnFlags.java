package ar.edu.utn.frc.tup.piii.engine.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.HashMap;
import java.util.Map;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class TurnFlags {
    private boolean hasDrawnForTurn;
    private boolean hasAttachedEnergy;
    private boolean hasRetreated;
    private boolean hasPlayedSupporter;
    private boolean hasPlayedStadium;
    private boolean hasAttacked;
    private Map<String, Object> damageModifiers;

    public boolean hasDrawnForTurn() { return hasDrawnForTurn; }
    public void setHasDrawnForTurn(boolean hasDrawnForTurn) { this.hasDrawnForTurn = hasDrawnForTurn; }

    public boolean hasAttachedEnergy() { return hasAttachedEnergy; }
    public void setHasAttachedEnergy(boolean hasAttachedEnergy) { this.hasAttachedEnergy = hasAttachedEnergy; }

    public boolean hasRetreated() { return hasRetreated; }
    public void setHasRetreated(boolean hasRetreated) { this.hasRetreated = hasRetreated; }

    public boolean hasPlayedSupporter() { return hasPlayedSupporter; }
    public void setHasPlayedSupporter(boolean hasPlayedSupporter) { this.hasPlayedSupporter = hasPlayedSupporter; }

    public boolean hasPlayedStadium() { return hasPlayedStadium; }
    public void setHasPlayedStadium(boolean hasPlayedStadium) { this.hasPlayedStadium = hasPlayedStadium; }

    public boolean hasAttacked() { return hasAttacked; }
    public void setHasAttacked(boolean hasAttacked) { this.hasAttacked = hasAttacked; }

    public Map<String, Object> getDamageModifiers() { return damageModifiers; }
    public void setDamageModifiers(Map<String, Object> damageModifiers) { this.damageModifiers = damageModifiers; }
}
