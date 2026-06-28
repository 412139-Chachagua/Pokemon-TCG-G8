package ar.edu.utn.frc.tup.piii.dtos.decks;

import java.util.List;

public record ValidateDeckRequest(
        List<ValidateCardEntry> cards
) {
    public record ValidateCardEntry(String cardId, int quantity) {}
}
