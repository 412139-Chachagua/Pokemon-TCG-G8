package ar.edu.utn.frc.tup.piii.engine.attack;

import ar.edu.utn.frc.tup.piii.engine.attack.DamageCalculator;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AttackContext {
    private final PokemonInPlay attacker;
    private final PokemonInPlay defender;
    private final int attackIndex;
    private final Map<String, Object> damageModifiers;
    private final UUID targetPokemonInstanceId;
    private DamageCalculator.DamageCalculatorResult damageCalc;
    private boolean confusedSelfHit;
    private int selfDamageCounters;
    private boolean energyValid;
    private String errorMessage;
    private boolean knockoutOccurred;
    private List<UUID> discardEnergyInstanceIds;
    private List<Map<String, Object>> benchTargets;
    private boolean attackCanceled;
    private int coinFlipDamageBonus;
    private UUID healTargetId;
    private Integer baseDamageOverride;
    private List<UUID> moveEnergyInstanceIds;
    private boolean bypassWeakness;
    private boolean bypassResistance;
    private boolean optionalBonusApplied;
    private int totalCoinFlips;
    private int headsCoinFlips;
    private boolean energyAttachedThisAttack;
    private String restrictedAttackName;
    private boolean flipHandledByInline;
    private String chosenCondition;

    public boolean isFlipHandledByInline() { return flipHandledByInline; }
    public void setFlipHandledByInline(boolean v) { flipHandledByInline = v; }

    public String getChosenCondition() { return chosenCondition; }
    public void setChosenCondition(String chosenCondition) { this.chosenCondition = chosenCondition; }

    public void addCoinFlipResult(boolean heads) {
        totalCoinFlips++;
        if (heads) headsCoinFlips++;
    }

    public int getTotalCoinFlips() { return totalCoinFlips; }

    public boolean allCoinFlipsHeads() {
        return totalCoinFlips > 0 && headsCoinFlips == totalCoinFlips;
    }

    public AttackContext(PokemonInPlay attacker, PokemonInPlay defender, int attackIndex,
                         Map<String, Object> damageModifiers, UUID targetPokemonInstanceId) {
        this.attacker = attacker;
        this.defender = defender;
        this.attackIndex = attackIndex;
        this.damageModifiers = damageModifiers;
        this.targetPokemonInstanceId = targetPokemonInstanceId;
    }

    public PokemonInPlay getAttacker() { return attacker; }
    public PokemonInPlay getDefender() { return defender; }
    public int getAttackIndex() { return attackIndex; }
    public Map<String, Object> getDamageModifiers() { return damageModifiers; }
    public UUID getTargetPokemonInstanceId() { return targetPokemonInstanceId; }

    public DamageCalculator.DamageCalculatorResult getDamageCalc() { return damageCalc; }
    public void setDamageCalc(DamageCalculator.DamageCalculatorResult damageCalc) { this.damageCalc = damageCalc; }

    public boolean isConfusedSelfHit() { return confusedSelfHit; }
    public void setConfusedSelfHit(boolean confusedSelfHit) { this.confusedSelfHit = confusedSelfHit; }

    public int getSelfDamageCounters() { return selfDamageCounters; }
    public void setSelfDamageCounters(int selfDamageCounters) { this.selfDamageCounters = selfDamageCounters; }

    public boolean isEnergyValid() { return energyValid; }
    public void setEnergyValid(boolean energyValid) { this.energyValid = energyValid; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public boolean isKnockoutOccurred() { return knockoutOccurred; }
    public void setKnockoutOccurred(boolean knockoutOccurred) { this.knockoutOccurred = knockoutOccurred; }

    public List<UUID> getDiscardEnergyInstanceIds() { return discardEnergyInstanceIds; }
    public void setDiscardEnergyInstanceIds(List<UUID> discardEnergyInstanceIds) { this.discardEnergyInstanceIds = discardEnergyInstanceIds; }

    public List<Map<String, Object>> getBenchTargets() { return benchTargets; }
    public void setBenchTargets(List<Map<String, Object>> benchTargets) { this.benchTargets = benchTargets; }

    public boolean isAttackCanceled() { return attackCanceled; }
    public void setAttackCanceled(boolean attackCanceled) { this.attackCanceled = attackCanceled; }

    public int getCoinFlipDamageBonus() { return coinFlipDamageBonus; }
    public void setCoinFlipDamageBonus(int coinFlipDamageBonus) { this.coinFlipDamageBonus = coinFlipDamageBonus; }

    public UUID getHealTargetId() { return healTargetId; }
    public void setHealTargetId(UUID healTargetId) { this.healTargetId = healTargetId; }

    public Integer getBaseDamageOverride() { return baseDamageOverride; }
    public void setBaseDamageOverride(Integer baseDamageOverride) { this.baseDamageOverride = baseDamageOverride; }

    public List<UUID> getMoveEnergyInstanceIds() { return moveEnergyInstanceIds; }
    public void setMoveEnergyInstanceIds(List<UUID> moveEnergyInstanceIds) { this.moveEnergyInstanceIds = moveEnergyInstanceIds; }

    public boolean isBypassWeakness() { return bypassWeakness; }
    public void setBypassWeakness(boolean bypassWeakness) { this.bypassWeakness = bypassWeakness; }

    public boolean isBypassResistance() { return bypassResistance; }
    public void setBypassResistance(boolean bypassResistance) { this.bypassResistance = bypassResistance; }

    public boolean isOptionalBonusApplied() { return optionalBonusApplied; }
    public void setOptionalBonusApplied(boolean optionalBonusApplied) { this.optionalBonusApplied = optionalBonusApplied; }

    public boolean isEnergyAttachedThisAttack() { return energyAttachedThisAttack; }
    public void setEnergyAttachedThisAttack(boolean energyAttachedThisAttack) { this.energyAttachedThisAttack = energyAttachedThisAttack; }

    public String getRestrictedAttackName() { return restrictedAttackName; }
    public void setRestrictedAttackName(String restrictedAttackName) { this.restrictedAttackName = restrictedAttackName; }
}
