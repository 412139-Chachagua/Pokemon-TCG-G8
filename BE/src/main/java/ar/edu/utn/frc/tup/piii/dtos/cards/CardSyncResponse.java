package ar.edu.utn.frc.tup.piii.dtos.cards;

public record CardSyncResponse(
        boolean success,
        String message,
        int newCards,
        int updatedCards
) {
}
