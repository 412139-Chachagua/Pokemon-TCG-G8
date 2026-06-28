package ar.edu.utn.frc.tup.piii.dtos.decks;

import java.util.List;

public record UpdateDeckRequest(
        String name,
        List<CreateDeckRequest.DeckCardRequest> cards
) {
}
