package ar.edu.utn.frc.tup.piii.dtos.decks;

import java.util.List;

public record ImportDeckRequest(
        String name,
        List<CreateDeckRequest.DeckCardRequest> cards
) {
}
