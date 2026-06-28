package ar.edu.utn.frc.tup.piii.engine.model;

import java.util.UUID;

public class CardInstance {
    private UUID instanceId;
    private String cardDefinitionId;

    public CardInstance() {}

    public CardInstance(UUID instanceId, String cardDefinitionId) {
        this.instanceId = instanceId;
        this.cardDefinitionId = cardDefinitionId;
    }

    public UUID getInstanceId() { return instanceId; }
    public void setInstanceId(UUID instanceId) { this.instanceId = instanceId; }

    public String getCardDefinitionId() { return cardDefinitionId; }
    public void setCardDefinitionId(String cardDefinitionId) { this.cardDefinitionId = cardDefinitionId; }
}
