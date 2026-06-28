package ar.edu.utn.frc.tup.piii.dtos.ranking;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PlayerStatsResponse(
        String playerId,
        String displayName,
        int totalWins,
        int totalLosses,
        int currentWinStreak,
        int maxWinStreak
) {}
