package ar.edu.utn.frc.tup.piii.engine.model;

import java.util.List;
import java.util.UUID;

public class PrivatePlayerState {
    private UUID playerId;
    private List<PrivateHandCard> hand;
    private int deckCount;
    private int discardCount;
    private List<PrizeSlot> prizes;
    private int pendingMulliganDrawCount;
    private List<PrivateHandCard> deck;

    public PrivatePlayerState() {}
    public PrivatePlayerState(UUID playerId, List<PrivateHandCard> hand, int deckCount, int discardCount, List<PrizeSlot> prizes) {
        this.playerId = playerId;
        this.hand = hand;
        this.deckCount = deckCount;
        this.discardCount = discardCount;
        this.prizes = prizes;
    }

    public UUID getPlayerId() { return playerId; }
    public List<PrivateHandCard> getHand() { return hand; }
    public int getDeckCount() { return deckCount; }
    public int getDiscardCount() { return discardCount; }
    public List<PrizeSlot> getPrizes() { return prizes; }

    public void setPlayerId(UUID playerId) { this.playerId = playerId; }
    public void setHand(List<PrivateHandCard> hand) { this.hand = hand; }
    public void setDeckCount(int deckCount) { this.deckCount = deckCount; }
    public void setDiscardCount(int discardCount) { this.discardCount = discardCount; }
    public void setPrizes(List<PrizeSlot> prizes) { this.prizes = prizes; }

    public int getPendingMulliganDrawCount() { return pendingMulliganDrawCount; }
    public void setPendingMulliganDrawCount(int pendingMulliganDrawCount) { this.pendingMulliganDrawCount = pendingMulliganDrawCount; }

    public List<PrivateHandCard> getDeck() { return deck; }
    public void setDeck(List<PrivateHandCard> deck) { this.deck = deck; }

    public static class PrivateHandCard {
        private String instanceId;
        private String cardId;
        private String name;
        private String supertype;
        private String effectCode;

        public PrivateHandCard() {}
        public PrivateHandCard(String instanceId, String cardId, String name, String supertype) {
            this(instanceId, cardId, name, supertype, null);
        }
        public PrivateHandCard(String instanceId, String cardId, String name, String supertype, String effectCode) {
            this.instanceId = instanceId;
            this.cardId = cardId;
            this.name = name;
            this.supertype = supertype;
            this.effectCode = effectCode;
        }

        public String getInstanceId() { return instanceId; }
        public String getCardId() { return cardId; }
        public String getName() { return name; }
        public String getSupertype() { return supertype; }
        public String getEffectCode() { return effectCode; }

        public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
        public void setCardId(String cardId) { this.cardId = cardId; }
        public void setName(String name) { this.name = name; }
        public void setSupertype(String supertype) { this.supertype = supertype; }
        public void setEffectCode(String effectCode) { this.effectCode = effectCode; }
    }



    public static class PrizeSlot {
        private int slot;
        private boolean known;
        private String cardId;

        public PrizeSlot() {}
        public PrizeSlot(int slot, boolean known, String cardId) {
            this.slot = slot;
            this.known = known;
            this.cardId = cardId;

        }
        public int getSlot() { return slot; }
        public boolean isKnown() { return known; }
        public String getCardId() { return cardId; }

        public void setSlot(int slot) { this.slot = slot; }
        public void setKnown(boolean known) { this.known = known; }
        public void setCardId(String cardId) { this.cardId = cardId; }
    }
}
