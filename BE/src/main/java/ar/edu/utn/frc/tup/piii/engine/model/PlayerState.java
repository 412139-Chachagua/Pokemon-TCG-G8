package ar.edu.utn.frc.tup.piii.engine.model;

import ar.edu.utn.frc.tup.piii.engine.PlayerSide;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class PlayerState {
    private UUID playerId;
    private PlayerSide side;
    private List<CardInstance> deck;
    private List<CardInstance> hand;
    private List<CardInstance> prizes;
    private List<CardInstance> discard = new ArrayList<>();
    private PokemonInPlay activePokemon;
    private List<PokemonInPlay> bench;
    private int mulliganCount;
    private boolean setupConfirmed;
    private boolean initialMulliganResolved;
    private List<List<String>> mulliganRevealedCards;
    private boolean cannotPlaySupportersNextTurn;
    private String displayName;

    public UUID getPlayerId() { return playerId; }
    public void setPlayerId(UUID playerId) { this.playerId = playerId; }

    public PlayerSide getSide() { return side; }
    public void setSide(PlayerSide side) { this.side = side; }

    public List<CardInstance> getDeck() { return deck; }
    public void setDeck(List<CardInstance> deck) { this.deck = deck; }

    public List<CardInstance> getHand() { return hand; }
    public void setHand(List<CardInstance> hand) { this.hand = hand; }

    public List<CardInstance> getPrizes() { return prizes; }
    public void setPrizes(List<CardInstance> prizes) { this.prizes = prizes; }

    /**
     * Do not mutate discard directly. Use discard API methods instead.
     */
    public List<CardInstance> getDiscard() { return discard; }
    public void setDiscard(List<CardInstance> discard) { this.discard = discard != null ? discard : new ArrayList<>(); }

    public void pushToDiscard(CardInstance card) {
        if (card == null) return;
        if (discardContains(card.getInstanceId())) {
            // Ignore duplicate instanceId to keep discard pile consistent.
            return;
        }
        discard.add(card);
    }

    public void pushManyToDiscard(Collection<CardInstance> cards) {
        if (cards == null || cards.isEmpty()) return;
        // Cards are appended in iteration order.
        // Caller is responsible for providing correct discard ordering.
        for (CardInstance card : cards) {
            pushToDiscard(card);
        }
    }

    public CardInstance peekTopDiscard() {
        if (discard.isEmpty()) return null;
        return discard.get(discard.size() - 1);
    }

    public CardInstance popTopDiscard() {
        if (discard.isEmpty()) return null;
        return discard.remove(discard.size() - 1);
    }

    public boolean discardContains(UUID instanceId) {
        if (instanceId == null) return false;
        return discard.stream().anyMatch(c -> instanceId.equals(c.getInstanceId()));
    }

    public Optional<CardInstance> findInDiscard(UUID instanceId) {
        if (instanceId == null) return Optional.empty();
        return discard.stream().filter(c -> instanceId.equals(c.getInstanceId())).findFirst();
    }

    public boolean removeFromDiscard(UUID instanceId) {
        if (instanceId == null) return false;
        Iterator<CardInstance> it = discard.iterator();
        while (it.hasNext()) {
            CardInstance c = it.next();
            if (instanceId.equals(c.getInstanceId())) {
                it.remove();
                // TODO: future zone tracking — removing a card from discard
                // does not update any centralized card-location registry.
                return true;
            }
        }
        return false;
    }

    public PokemonInPlay getActivePokemon() { return activePokemon; }
    public void setActivePokemon(PokemonInPlay activePokemon) { this.activePokemon = activePokemon; }

    public List<PokemonInPlay> getBench() { return bench; }
    public void setBench(List<PokemonInPlay> bench) { this.bench = bench; }

    public int getMulliganCount() { return mulliganCount; }
    public void setMulliganCount(int mulliganCount) { this.mulliganCount = mulliganCount; }

    public boolean isSetupConfirmed() { return setupConfirmed; }
    public void setSetupConfirmed(boolean setupConfirmed) { this.setupConfirmed = setupConfirmed; }

    public boolean isInitialMulliganResolved() { return initialMulliganResolved; }
    public void setInitialMulliganResolved(boolean initialMulliganResolved) { this.initialMulliganResolved = initialMulliganResolved; }

    public List<List<String>> getMulliganRevealedCards() { return mulliganRevealedCards; }
    public void setMulliganRevealedCards(List<List<String>> mulliganRevealedCards) { this.mulliganRevealedCards = mulliganRevealedCards; }

    public boolean isCannotPlaySupportersNextTurn() { return cannotPlaySupportersNextTurn; }
    public void setCannotPlaySupportersNextTurn(boolean cannotPlaySupportersNextTurn) { this.cannotPlaySupportersNextTurn = cannotPlaySupportersNextTurn; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public void addMulliganReveal(List<String> revealedCardIds) {
        if (this.mulliganRevealedCards == null) {
            this.mulliganRevealedCards = new ArrayList<>();
        }
        this.mulliganRevealedCards.add(revealedCardIds);
    }
}
