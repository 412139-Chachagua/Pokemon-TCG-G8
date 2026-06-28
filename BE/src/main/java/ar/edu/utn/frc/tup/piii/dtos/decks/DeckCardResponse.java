package ar.edu.utn.frc.tup.piii.dtos.decks;

import java.util.List;

public record DeckCardResponse(
        String cardId,
        String name,
        int quantity,
        String supertype,
        boolean isBasicEnergy,
        List<String> subtypes,
        String stage
) {
}
