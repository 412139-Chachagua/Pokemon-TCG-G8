package ar.edu.utn.frc.tup.piii.domain.decks;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Deck {
    private UUID id;
    private String name;
    private UUID ownerPlayerId;
    private String source;
    private Instant createdAt;
    private Instant updatedAt;
    private List<DeckCard> cards;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getOwnerPlayerId() { return ownerPlayerId; }
    public void setOwnerPlayerId(UUID ownerPlayerId) { this.ownerPlayerId = ownerPlayerId; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public List<DeckCard> getCards() { return cards; }
    public void setCards(List<DeckCard> cards) { this.cards = cards; }
}
