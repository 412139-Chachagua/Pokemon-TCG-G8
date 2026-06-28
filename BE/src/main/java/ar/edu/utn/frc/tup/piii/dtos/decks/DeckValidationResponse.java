package ar.edu.utn.frc.tup.piii.dtos.decks;

import java.util.List;

public record DeckValidationResponse(
        boolean valid,
        List<DeckValidationError> errors
) {
    public record DeckValidationError(String code, String message, Object details) {}
}
