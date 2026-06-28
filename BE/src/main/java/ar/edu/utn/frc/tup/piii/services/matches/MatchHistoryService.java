package ar.edu.utn.frc.tup.piii.services.matches;

import ar.edu.utn.frc.tup.piii.dtos.matches.MatchSummaryResponse;
import ar.edu.utn.frc.tup.piii.repositories.entities.MatchEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.MatchPlayerEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.MatchJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatchHistoryService {

    private final MatchJpaRepository matchJpaRepository;

    public MatchHistoryService(MatchJpaRepository matchJpaRepository) {
        this.matchJpaRepository = matchJpaRepository;
    }

    @Transactional(readOnly = true)
    public List<MatchSummaryResponse> getHistoryByPlayer(UUID playerId) {
        List<MatchEntity> matches = matchJpaRepository
                .findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc("FINISHED", playerId);

        return matches.stream()
                .filter(m -> !"EXPIRED".equals(m.getFinishReason()))
                .map(m -> buildSummary(m, playerId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<MatchSummaryResponse> getHistoryDetail(UUID matchId) {
        return matchJpaRepository.findById(matchId)
                .filter(m -> "FINISHED".equals(m.getStatus()) && !"EXPIRED".equals(m.getFinishReason()))
                .map(m -> buildSummary(m, null));
    }

    private MatchSummaryResponse buildSummary(MatchEntity match, UUID requestingPlayerId) {
        List<MatchPlayerEntity> players = match.getPlayers() != null
                ? match.getPlayers().stream()
                        .sorted(Comparator.comparing(MatchPlayerEntity::getSide))
                        .toList()
                : List.of();

        UUID winnerId = match.getWinnerPlayerId();

        String p1Name = players.size() > 0 ? players.get(0).getDisplayName() : "Unknown";
        String p2Name = players.size() > 1 ? players.get(1).getDisplayName() : "Unknown";

        String winnerName, loserName;
        if (winnerId != null) {
            boolean p1Wins = players.size() > 0 && players.get(0).getPlayerId().equals(winnerId);
            winnerName = p1Wins ? p1Name : p2Name;
            loserName  = p1Wins ? p2Name : p1Name;
        } else {
            winnerName = p1Name;
            loserName  = p2Name;
        }

        Long durationSeconds = (match.getCreatedAt() != null && match.getFinishedAt() != null)
                ? Duration.between(match.getCreatedAt(), match.getFinishedAt()).getSeconds()
                : null;

        return new MatchSummaryResponse(
                match.getId().toString(),
                winnerName,
                loserName,
                match.getTurnNumber() != null ? match.getTurnNumber() : 0,
                match.getCreatedAt(),
                durationSeconds,
                match.getFinishReason()
        );
    }
}
