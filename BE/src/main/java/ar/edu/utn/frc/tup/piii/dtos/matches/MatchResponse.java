package ar.edu.utn.frc.tup.piii.dtos.matches;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MatchResponse(
        String id,
        String status,
        String currentPhase,
        Integer turnNumber,
        String currentPlayerId,
        String firstPlayerId,
        String winnerPlayerId,
        String finishReason,
        List<PlayerSummary> players,
        Instant createdAt,
        Instant lastSavedAt,
        String lastResumedPlayerId
) {
    public record PlayerSummary(String playerId, String side, String displayName) {}
}

