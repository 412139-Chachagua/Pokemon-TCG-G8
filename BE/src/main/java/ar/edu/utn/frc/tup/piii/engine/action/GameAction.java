package ar.edu.utn.frc.tup.piii.engine.action;

import java.util.Map;
import java.util.UUID;

public class GameAction {
    private GameActionType type;
    private UUID playerId;
    private Map<String, Object> payload;
    private String clientRequestId;

    public GameActionType getType() { return type; }
    public void setType(GameActionType type) { this.type = type; }

    public UUID getPlayerId() { return playerId; }
    public void setPlayerId(UUID playerId) { this.playerId = playerId; }

    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }

    public String getClientRequestId() { return clientRequestId; }
    public void setClientRequestId(String clientRequestId) { this.clientRequestId = clientRequestId; }

    public String getPayloadString(String key) {
        if (payload == null) return null;
        Object value = payload.get(key);
        if (value == null) return null;
        return value.toString();
    }

    public Integer getPayloadInt(String key) {
        if (payload == null) return null;
        Object value = payload.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
