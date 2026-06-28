package ar.edu.utn.frc.tup.piii.dtos.matches;

import jakarta.validation.constraints.NotBlank;

public record JoinMatchRequest(
        @NotBlank String playerName,
        @NotBlank String deckId,
        @NotBlank String playerId
) {}
