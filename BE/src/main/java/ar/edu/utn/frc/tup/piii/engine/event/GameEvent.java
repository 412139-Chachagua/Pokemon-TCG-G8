package ar.edu.utn.frc.tup.piii.engine.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class GameEvent {
    private String type;
    private UUID matchId;
    private int turnNumber;
    private Instant createdAt;
    private String message;
    private Map<String, Object> payload;

    public GameEvent(String type,
                     UUID matchId,
                     int turnNumber,
                     Instant createdAt,
                     String message,
                     Map<String, Object> payload) {
        this.type = type;
        this.matchId = matchId;
        this.turnNumber = turnNumber;
        this.createdAt = createdAt;
        this.message = message;
        this.payload = payload;
    }


    public String getType() { return type; }
    public UUID getMatchId() { return matchId; }
    public int getTurnNumber() { return turnNumber; }
    public Instant getCreatedAt() { return createdAt; }
    public String getMessage() { return message; }
    public Map<String, Object> getPayload() { return payload; }
}
