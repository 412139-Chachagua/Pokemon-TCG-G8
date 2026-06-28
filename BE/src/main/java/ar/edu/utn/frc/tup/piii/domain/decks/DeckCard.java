package ar.edu.utn.frc.tup.piii.domain.decks;

import java.util.UUID;

public class DeckCard {
    private UUID id;
    private UUID deckId;
    private String cardId;
    private int quantity;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getDeckId() { return deckId; }
    public void setDeckId(UUID deckId) { this.deckId = deckId; }
    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
