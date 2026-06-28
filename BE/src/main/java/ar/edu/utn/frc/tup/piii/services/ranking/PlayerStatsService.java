package ar.edu.utn.frc.tup.piii.services.ranking;

import ar.edu.utn.frc.tup.piii.dtos.ranking.PlayerStatsResponse;
import ar.edu.utn.frc.tup.piii.dtos.ranking.RankingEntryResponse;
import ar.edu.utn.frc.tup.piii.engine.victory.FinishReason;
import ar.edu.utn.frc.tup.piii.repositories.entities.MatchPlayerEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.PlayerStatsEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.MatchPlayerJpaRepository;
import ar.edu.utn.frc.tup.piii.repositories.jpa.PlayerJpaRepository;
import ar.edu.utn.frc.tup.piii.repositories.jpa.PlayerStatsJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PlayerStatsService {

    private static final Logger log = LoggerFactory.getLogger(PlayerStatsService.class);

    private final PlayerStatsJpaRepository playerStatsJpaRepository;
    private final PlayerJpaRepository playerJpaRepository;
    private final MatchPlayerJpaRepository matchPlayerJpaRepository;

    public PlayerStatsService(PlayerStatsJpaRepository playerStatsJpaRepository,
                              PlayerJpaRepository playerJpaRepository,
                              MatchPlayerJpaRepository matchPlayerJpaRepository) {
        this.playerStatsJpaRepository = playerStatsJpaRepository;
        this.playerJpaRepository = playerJpaRepository;
        this.matchPlayerJpaRepository = matchPlayerJpaRepository;
    }

    @Transactional
    public void recordMatchResult(UUID matchId, UUID winnerPlayerId, FinishReason finishReason) {
        if (winnerPlayerId == null || finishReason == FinishReason.SUDDEN_DEATH) {
            return;
        }

        List<MatchPlayerEntity> matchPlayers = matchPlayerJpaRepository.findByMatch_Id(matchId);
        if (matchPlayers.size() < 2) {
            log.warn("Cannot record stats for match {}: less than 2 players", matchId);
            return;
        }

        UUID loserPlayerId = matchPlayers.stream()
                .map(MatchPlayerEntity::getPlayerId)
                .filter(pid -> !pid.equals(winnerPlayerId))
                .findFirst()
                .orElse(null);

        if (loserPlayerId == null) {
            log.warn("Cannot find loser for match {}", matchId);
            return;
        }

        updatePlayerStats(winnerPlayerId, true);
        updatePlayerStats(loserPlayerId, false);
    }

    private void updatePlayerStats(UUID playerId, boolean isWinner) {
        PlayerStatsEntity stats = playerStatsJpaRepository.findByPlayerId(playerId)
                .orElseGet(() -> {
                    PlayerStatsEntity newStats = new PlayerStatsEntity();
                    newStats.setPlayerId(playerId);
                    return newStats;
                });

        if (isWinner) {
            stats.setTotalWins(stats.getTotalWins() + 1);
            stats.setCurrentWinStreak(stats.getCurrentWinStreak() + 1);
            if (stats.getCurrentWinStreak() > stats.getMaxWinStreak()) {
                stats.setMaxWinStreak(stats.getCurrentWinStreak());
            }
        } else {
            stats.setTotalLosses(stats.getTotalLosses() + 1);
            stats.setCurrentWinStreak(0);
        }

        playerStatsJpaRepository.save(stats);
    }

    @Transactional(readOnly = true)
    public List<RankingEntryResponse> getRanking() {
        List<PlayerStatsEntity> statsList = playerStatsJpaRepository
                .findAllByOrderByTotalWinsDescMaxWinStreakDesc();

        List<RankingEntryResponse> result = new ArrayList<>();
        int rank = 1;
        for (PlayerStatsEntity stats : statsList) {
            String displayName = playerJpaRepository.findById(stats.getPlayerId())
                    .map(p -> p.getDisplayName())
                    .orElse("Unknown");

            int totalGames = stats.getTotalWins() + stats.getTotalLosses();
            double winRate = totalGames > 0
                    ? (double) stats.getTotalWins() / totalGames * 100.0
                    : 0.0;

            result.add(new RankingEntryResponse(
                    rank++,
                    stats.getPlayerId().toString(),
                    displayName,
                    stats.getTotalWins(),
                    stats.getTotalLosses(),
                    Math.round(winRate * 100.0) / 100.0,
                    stats.getCurrentWinStreak(),
                    stats.getMaxWinStreak()
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public PlayerStatsResponse getPlayerStats(UUID playerId) {
        String displayName = playerJpaRepository.findById(playerId)
                .map(p -> p.getDisplayName())
                .orElse("Unknown");

        PlayerStatsEntity stats = playerStatsJpaRepository.findByPlayerId(playerId)
                .orElse(new PlayerStatsEntity());

        return new PlayerStatsResponse(
                playerId.toString(),
                displayName,
                stats.getTotalWins(),
                stats.getTotalLosses(),
                stats.getCurrentWinStreak(),
                stats.getMaxWinStreak()
        );
    }
}
