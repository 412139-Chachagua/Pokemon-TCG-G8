package ar.edu.utn.frc.tup.piii.engine.model;

import ar.edu.utn.frc.tup.piii.engine.SpecialCondition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PokemonInPlay {
    private UUID instanceId;
    private String cardDefinitionId;
    private UUID ownerPlayerId;
    private int enteredTurnNumber;
    private boolean evolvedThisTurn;
    private boolean faceDown;
    private int damageCounters;
    private List<SpecialCondition> specialConditions = new ArrayList<>();
    private List<CardInstance> attachedEnergies;
    private CardInstance attachedTool;
    private UUID toolCardInstanceId;
    private Set<String> abilitiesUsedThisTurn = new HashSet<>();
    private boolean preventAllDamageNextTurn;
    private boolean cannotAttackNextTurn;
    private int nextTurnDamageBonus;
    private boolean cannotRetreatNextTurn;
    private int reduceDamageNextTurn;
    private Integer preventionDamageThreshold;
    private String restrictedAttackName;
    private boolean abilitiesSuppressedNextTurn;
    private boolean mustFlipToAttackNextTurn;

    public UUID getInstanceId() { return instanceId; }
    public void setInstanceId(UUID instanceId) { this.instanceId = instanceId; }

    public String getCardDefinitionId() { return cardDefinitionId; }
    public void setCardDefinitionId(String cardDefinitionId) { this.cardDefinitionId = cardDefinitionId; }

    public UUID getOwnerPlayerId() { return ownerPlayerId; }
    public void setOwnerPlayerId(UUID ownerPlayerId) { this.ownerPlayerId = ownerPlayerId; }

    public int getEnteredTurnNumber() { return enteredTurnNumber; }
    public void setEnteredTurnNumber(int enteredTurnNumber) { this.enteredTurnNumber = enteredTurnNumber; }

    public boolean isEvolvedThisTurn() { return evolvedThisTurn; }
    public void setEvolvedThisTurn(boolean evolvedThisTurn) { this.evolvedThisTurn = evolvedThisTurn; }

    public boolean isFaceDown() { return faceDown; }
    public void setFaceDown(boolean faceDown) { this.faceDown = faceDown; }

    public int getDamageCounters() { return damageCounters; }
    public void setDamageCounters(int damageCounters) { this.damageCounters = damageCounters; }

    public List<SpecialCondition> getSpecialConditions() { return specialConditions; }
    public void setSpecialConditions(List<SpecialCondition> specialConditions) { this.specialConditions = specialConditions; }

    public List<CardInstance> getAttachedEnergies() { return attachedEnergies; }
    public void setAttachedEnergies(List<CardInstance> attachedEnergies) { this.attachedEnergies = attachedEnergies; }

    public CardInstance getAttachedTool() { return attachedTool; }
    public void setAttachedTool(CardInstance attachedTool) { this.attachedTool = attachedTool; }

    public UUID getToolCardInstanceId() { return toolCardInstanceId; }
    public void setToolCardInstanceId(UUID toolCardInstanceId) { this.toolCardInstanceId = toolCardInstanceId; }

    public Set<String> getAbilitiesUsedThisTurn() { return abilitiesUsedThisTurn; }
    public void setAbilitiesUsedThisTurn(Set<String> abilitiesUsedThisTurn) { this.abilitiesUsedThisTurn = abilitiesUsedThisTurn; }

    public boolean isPreventAllDamageNextTurn() { return preventAllDamageNextTurn; }
    public void setPreventAllDamageNextTurn(boolean preventAllDamageNextTurn) { this.preventAllDamageNextTurn = preventAllDamageNextTurn; }

    public boolean isCannotAttackNextTurn() { return cannotAttackNextTurn; }
    public void setCannotAttackNextTurn(boolean cannotAttackNextTurn) { this.cannotAttackNextTurn = cannotAttackNextTurn; }

    public int getNextTurnDamageBonus() { return nextTurnDamageBonus; }
    public void setNextTurnDamageBonus(int nextTurnDamageBonus) { this.nextTurnDamageBonus = nextTurnDamageBonus; }

    public boolean isCannotRetreatNextTurn() { return cannotRetreatNextTurn; }
    public void setCannotRetreatNextTurn(boolean cannotRetreatNextTurn) { this.cannotRetreatNextTurn = cannotRetreatNextTurn; }

    public int getReduceDamageNextTurn() { return reduceDamageNextTurn; }
    public void setReduceDamageNextTurn(int reduceDamageNextTurn) { this.reduceDamageNextTurn = reduceDamageNextTurn; }

    public Integer getPreventionDamageThreshold() { return preventionDamageThreshold; }
    public void setPreventionDamageThreshold(Integer preventionDamageThreshold) { this.preventionDamageThreshold = preventionDamageThreshold; }

    public String getRestrictedAttackName() { return restrictedAttackName; }
    public void setRestrictedAttackName(String restrictedAttackName) { this.restrictedAttackName = restrictedAttackName; }

    public boolean isAbilitiesSuppressedNextTurn() { return abilitiesSuppressedNextTurn; }
    public void setAbilitiesSuppressedNextTurn(boolean abilitiesSuppressedNextTurn) { this.abilitiesSuppressedNextTurn = abilitiesSuppressedNextTurn; }

    public boolean isMustFlipToAttackNextTurn() { return mustFlipToAttackNextTurn; }
    public void setMustFlipToAttackNextTurn(boolean mustFlipToAttackNextTurn) { this.mustFlipToAttackNextTurn = mustFlipToAttackNextTurn; }
}
