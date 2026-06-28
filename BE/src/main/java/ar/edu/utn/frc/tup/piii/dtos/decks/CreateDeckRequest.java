package ar.edu.utn.frc.tup.piii.dtos.decks;

import java.util.List;

public record CreateDeckRequest(
        String name,
        String playerId,
        List<DeckCardRequest> cards
) {
    public record DeckCardRequest(String cardId, int quantity) {}
}