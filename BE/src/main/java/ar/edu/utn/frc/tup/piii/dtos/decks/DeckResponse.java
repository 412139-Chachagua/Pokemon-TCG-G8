package ar.edu.utn.frc.tup.piii.dtos.decks;

import java.util.List;

public record DeckResponse(
        String id,
        String name,
        String ownerPlayerId,
    String source,
    int totalCards,
    boolean valid,
    String mainCardId,
    String mainCardImageUrl,
    List<DeckCardResponse> cards,
    DeckValidationResponse validation,
    String createdAt
) {
    public DeckResponse(String id, String name, String ownerPlayerId, String source,
                        int totalCards, boolean valid,
                        List<DeckCardResponse> cards,
                        DeckValidationResponse validation, String createdAt) {
        this(id, name, ownerPlayerId, source, totalCards, valid, null, null,
             cards, validation, createdAt);
    }
}
