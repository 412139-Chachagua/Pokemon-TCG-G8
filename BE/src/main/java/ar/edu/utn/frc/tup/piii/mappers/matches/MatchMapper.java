package ar.edu.utn.frc.tup.piii.mappers.matches;

import ar.edu.utn.frc.tup.piii.dtos.matches.MatchResponse;
import ar.edu.utn.frc.tup.piii.repositories.entities.MatchEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.MatchPlayerEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MatchMapper {

    public MatchResponse toMatchResponse(MatchEntity entity, List<MatchPlayerEntity> players) {
        List<MatchResponse.PlayerSummary> playerSummaries = players.stream()
                .map(p -> new MatchResponse.PlayerSummary(
                        p.getPlayerId().toString(),
                        p.getSide(),
                        p.getDisplayName()))
                .collect(Collectors.toList());

        return new MatchResponse(
                entity.getId().toString(),
                entity.getStatus(),
                entity.getCurrentPhase(),
                entity.getTurnNumber(),
                entity.getCurrentPlayerId() != null ? entity.getCurrentPlayerId().toString() : null,
                entity.getFirstPlayerId() != null ? entity.getFirstPlayerId().toString() : null,
                entity.getWinnerPlayerId() != null ? entity.getWinnerPlayerId().toString() : null,
                entity.getFinishReason(),
                playerSummaries,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getLastResumedPlayerId() != null ? entity.getLastResumedPlayerId().toString() : null
        );
    }
}
