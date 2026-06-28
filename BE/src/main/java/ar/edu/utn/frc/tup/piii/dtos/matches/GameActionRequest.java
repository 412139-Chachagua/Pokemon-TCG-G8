package ar.edu.utn.frc.tup.piii.dtos.matches;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GameActionRequest(
        @NotBlank String type,
        @NotBlank String playerId,
        @NotNull Object payload,
        String clientRequestId
) {
}
