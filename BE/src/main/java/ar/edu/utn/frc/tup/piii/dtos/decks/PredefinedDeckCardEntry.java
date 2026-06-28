package ar.edu.utn.frc.tup.piii.dtos.decks;

public record PredefinedDeckCardEntry(
        String cardId,
        String name,
        String supertype,
        int quantity
) {
}
