package ar.edu.utn.frc.tup.piii.dtos.ranking;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RankingEntryResponse(
        int rank,
        String playerId,
        String displayName,
        int totalWins,
        int totalLosses,
        double winRate,
        int currentWinStreak,
        int maxWinStreak
) {}
