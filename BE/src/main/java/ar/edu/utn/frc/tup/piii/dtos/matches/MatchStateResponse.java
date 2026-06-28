package ar.edu.utn.frc.tup.piii.dtos.matches;

import ar.edu.utn.frc.tup.piii.engine.model.PrivatePlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PublicGameState;

public record MatchStateResponse(
        String matchId,
        PublicGameState publicState,
        PrivatePlayerState privateState
) {
}
