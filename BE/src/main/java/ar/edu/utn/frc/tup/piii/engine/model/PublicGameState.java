package ar.edu.utn.frc.tup.piii.engine.model;

import java.util.List;
import java.util.UUID;

public class PublicGameState {
    private UUID matchId;
    private String status;
    private String phase;
    private int turnNumber;
    private UUID currentPlayerId;
    private UUID firstPlayerId;
    private PublicPlayerState[] players;
    private boolean mulliganDrawPending;
    private String mulliganDrawDeadline;
    private UUID[] pendingInitialMulliganPlayers;
    private boolean pendingKOReplacement;
    private String pendingPrizeOwnerPlayerId;
    private String knockedOutPlayerId;
    private UUID winnerPlayerId;
    private String finishReason;
    private String stadiumCardInstanceId;
    private String stadiumCardDefinitionId;
    private String stadiumOwnerPlayerId;
    private boolean hasPlayedSupporter;
    private boolean hasPlayedStadium;
    private boolean hasAttachedEnergy;
    private boolean hasRetreated;

    public PublicGameState() {}
    public PublicGameState(UUID matchId, String status, String phase, int turnNumber, UUID currentPlayerId, UUID firstPlayerId, PublicPlayerState[] players) {
        this.matchId = matchId;
        this.status = status;
        this.phase = phase;
        this.turnNumber = turnNumber;
        this.currentPlayerId = currentPlayerId;
        this.firstPlayerId = firstPlayerId;
        this.players = players;
    }

    public UUID getMatchId() { return matchId; }
    public String getStatus() { return status; }
    public String getPhase() { return phase; }
    public int getTurnNumber() { return turnNumber; }
    public UUID getCurrentPlayerId() { return currentPlayerId; }
    public UUID getFirstPlayerId() { return firstPlayerId; }
    public PublicPlayerState[] getPlayers() { return players; }

    public void setMatchId(UUID matchId) { this.matchId = matchId; }
    public void setStatus(String status) { this.status = status; }
    public void setPhase(String phase) { this.phase = phase; }
    public void setTurnNumber(int turnNumber) { this.turnNumber = turnNumber; }
    public void setCurrentPlayerId(UUID currentPlayerId) { this.currentPlayerId = currentPlayerId; }
    public void setFirstPlayerId(UUID firstPlayerId) { this.firstPlayerId = firstPlayerId; }
    public void setPlayers(PublicPlayerState[] players) { this.players = players; }

    public boolean isMulliganDrawPending() { return mulliganDrawPending; }
    public void setMulliganDrawPending(boolean mulliganDrawPending) { this.mulliganDrawPending = mulliganDrawPending; }
    public String getMulliganDrawDeadline() { return mulliganDrawDeadline; }
    public void setMulliganDrawDeadline(String mulliganDrawDeadline) { this.mulliganDrawDeadline = mulliganDrawDeadline; }

    public UUID[] getPendingInitialMulliganPlayers() { return pendingInitialMulliganPlayers; }
    public void setPendingInitialMulliganPlayers(UUID[] pendingInitialMulliganPlayers) { this.pendingInitialMulliganPlayers = pendingInitialMulliganPlayers; }

    public boolean isPendingKOReplacement() { return pendingKOReplacement; }
    public void setPendingKOReplacement(boolean pendingKOReplacement) { this.pendingKOReplacement = pendingKOReplacement; }

    public String getPendingPrizeOwnerPlayerId() { return pendingPrizeOwnerPlayerId; }
    public void setPendingPrizeOwnerPlayerId(String pendingPrizeOwnerPlayerId) { this.pendingPrizeOwnerPlayerId = pendingPrizeOwnerPlayerId; }

    public String getKnockedOutPlayerId() { return knockedOutPlayerId; }
    public void setKnockedOutPlayerId(String knockedOutPlayerId) { this.knockedOutPlayerId = knockedOutPlayerId; }

    public UUID getWinnerPlayerId() { return winnerPlayerId; }
    public void setWinnerPlayerId(UUID winnerPlayerId) { this.winnerPlayerId = winnerPlayerId; }

    public String getFinishReason() { return finishReason; }
    public void setFinishReason(String finishReason) { this.finishReason = finishReason; }

    public String getStadiumCardInstanceId() { return stadiumCardInstanceId; }
    public void setStadiumCardInstanceId(String stadiumCardInstanceId) { this.stadiumCardInstanceId = stadiumCardInstanceId; }

    public String getStadiumCardDefinitionId() { return stadiumCardDefinitionId; }
    public void setStadiumCardDefinitionId(String stadiumCardDefinitionId) { this.stadiumCardDefinitionId = stadiumCardDefinitionId; }

    public String getStadiumOwnerPlayerId() { return stadiumOwnerPlayerId; }
    public void setStadiumOwnerPlayerId(String stadiumOwnerPlayerId) { this.stadiumOwnerPlayerId = stadiumOwnerPlayerId; }

    public boolean isHasPlayedSupporter() { return hasPlayedSupporter; }
    public void setHasPlayedSupporter(boolean hasPlayedSupporter) { this.hasPlayedSupporter = hasPlayedSupporter; }

    public boolean isHasPlayedStadium() { return hasPlayedStadium; }
    public void setHasPlayedStadium(boolean hasPlayedStadium) { this.hasPlayedStadium = hasPlayedStadium; }

    public boolean isHasAttachedEnergy() { return hasAttachedEnergy; }
    public void setHasAttachedEnergy(boolean hasAttachedEnergy) { this.hasAttachedEnergy = hasAttachedEnergy; }

    public boolean isHasRetreated() { return hasRetreated; }
    public void setHasRetreated(boolean hasRetreated) { this.hasRetreated = hasRetreated; }

    public static class PublicPlayerState {
        private UUID playerId;
        private String side;
        private PublicPokemonSlot activePokemon;
        private PublicPokemonSlot[] bench;
        private String[] prizes;
        private boolean setupConfirmed;
        private int mulliganCount;
        private int totalPrizeCount;
        private int discardCount;
        private List<PublicDiscardCard> discard;
        private List<List<String>> mulliganRevealedCards;
        private String displayName;
        private boolean firstTurnCompleted;

        public PublicPlayerState() {}
        public PublicPlayerState(UUID playerId, String side, PublicPokemonSlot activePokemon, PublicPokemonSlot[] bench, String[] prizes, boolean setupConfirmed) {
            this.playerId = playerId;
            this.side = side;
            this.activePokemon = activePokemon;
            this.bench = bench;
            this.prizes = prizes;
            this.setupConfirmed = setupConfirmed;
        }

        public UUID getPlayerId() { return playerId; }
        public String getSide() { return side; }
        public PublicPokemonSlot getActivePokemon() { return activePokemon; }
        public PublicPokemonSlot[] getBench() { return bench; }
        public String[] getPrizes() { return prizes; }
        public boolean isSetupConfirmed() { return setupConfirmed; }
        public int getMulliganCount() { return mulliganCount; }
        public int getTotalPrizeCount() { return totalPrizeCount; }
        public int getDiscardCount() { return discardCount; }
        public List<PublicDiscardCard> getDiscard() { return discard; }

        public void setPlayerId(UUID playerId) { this.playerId = playerId; }
        public void setSide(String side) { this.side = side; }
        public void setActivePokemon(PublicPokemonSlot activePokemon) { this.activePokemon = activePokemon; }
        public void setBench(PublicPokemonSlot[] bench) { this.bench = bench; }
        public void setPrizes(String[] prizes) { this.prizes = prizes; }
        public void setSetupConfirmed(boolean setupConfirmed) { this.setupConfirmed = setupConfirmed; }
        public void setMulliganCount(int mulliganCount) { this.mulliganCount = mulliganCount; }
        public void setTotalPrizeCount(int totalPrizeCount) { this.totalPrizeCount = totalPrizeCount; }
        public void setDiscardCount(int discardCount) { this.discardCount = discardCount; }
        public void setDiscard(List<PublicDiscardCard> discard) { this.discard = discard; }
        public boolean isFirstTurnCompleted() { return firstTurnCompleted; }
        public void setFirstTurnCompleted(boolean firstTurnCompleted) { this.firstTurnCompleted = firstTurnCompleted; }

        public List<List<String>> getMulliganRevealedCards() { return mulliganRevealedCards; }
        public void setMulliganRevealedCards(List<List<String>> mulliganRevealedCards) { this.mulliganRevealedCards = mulliganRevealedCards; }

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
    }

    public static class PublicPokemonSlot {
        private String instanceId;
        private String cardId;
        private int damageCounters;
        private String[] specialConditions;
        private String[] attachedCards;
        private boolean evolvedThisTurn;
        private int enteredTurnNumber;
        private String[] attachedEnergyInstanceIds;
        private String attachedToolCardInstanceId;
        private String attachedToolCardDefinitionId;

        public PublicPokemonSlot() {}
        public PublicPokemonSlot(String instanceId, String cardId, int damageCounters, String[] specialConditions, String[] attachedCards, boolean evolvedThisTurn) {
            this(instanceId, cardId, damageCounters, specialConditions, attachedCards, evolvedThisTurn, 0, null, null, null);
        }
        public PublicPokemonSlot(String instanceId, String cardId, int damageCounters, String[] specialConditions, String[] attachedCards, boolean evolvedThisTurn, String[] attachedEnergyInstanceIds) {
            this(instanceId, cardId, damageCounters, specialConditions, attachedCards, evolvedThisTurn, 0, attachedEnergyInstanceIds, null, null);
        }
        public PublicPokemonSlot(String instanceId, String cardId, int damageCounters, String[] specialConditions, String[] attachedCards, boolean evolvedThisTurn, String[] attachedEnergyInstanceIds, String attachedToolCardInstanceId) {
            this(instanceId, cardId, damageCounters, specialConditions, attachedCards, evolvedThisTurn, 0, attachedEnergyInstanceIds, attachedToolCardInstanceId, null);
        }
        public PublicPokemonSlot(String instanceId, String cardId, int damageCounters, String[] specialConditions, String[] attachedCards, boolean evolvedThisTurn, int enteredTurnNumber, String[] attachedEnergyInstanceIds, String attachedToolCardInstanceId, String attachedToolCardDefinitionId) {
            this.instanceId = instanceId;
            this.cardId = cardId;
            this.damageCounters = damageCounters;
            this.specialConditions = specialConditions;
            this.attachedCards = attachedCards;
            this.evolvedThisTurn = evolvedThisTurn;
            this.enteredTurnNumber = enteredTurnNumber;
            this.attachedEnergyInstanceIds = attachedEnergyInstanceIds;
            this.attachedToolCardInstanceId = attachedToolCardInstanceId;
            this.attachedToolCardDefinitionId = attachedToolCardDefinitionId;
        }

        public String getInstanceId() { return instanceId; }
        public String getCardId() { return cardId; }
        public int getDamageCounters() { return damageCounters; }
        public String[] getSpecialConditions() { return specialConditions; }
        public String[] getAttachedCards() { return attachedCards; }
        public boolean isEvolvedThisTurn() { return evolvedThisTurn; }
        public int getEnteredTurnNumber() { return enteredTurnNumber; }
        public String[] getAttachedEnergyInstanceIds() { return attachedEnergyInstanceIds; }
        public String getAttachedToolCardInstanceId() { return attachedToolCardInstanceId; }
        public String getAttachedToolCardDefinitionId() { return attachedToolCardDefinitionId; }

        public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
        public void setCardId(String cardId) { this.cardId = cardId; }
        public void setDamageCounters(int damageCounters) { this.damageCounters = damageCounters; }
        public void setSpecialConditions(String[] specialConditions) { this.specialConditions = specialConditions; }
        public void setAttachedCards(String[] attachedCards) { this.attachedCards = attachedCards; }
        public void setEvolvedThisTurn(boolean evolvedThisTurn) { this.evolvedThisTurn = evolvedThisTurn; }
        public void setEnteredTurnNumber(int enteredTurnNumber) { this.enteredTurnNumber = enteredTurnNumber; }
        public void setAttachedEnergyInstanceIds(String[] attachedEnergyInstanceIds) { this.attachedEnergyInstanceIds = attachedEnergyInstanceIds; }
        public void setAttachedToolCardInstanceId(String attachedToolCardInstanceId) { this.attachedToolCardInstanceId = attachedToolCardInstanceId; }
        public void setAttachedToolCardDefinitionId(String attachedToolCardDefinitionId) { this.attachedToolCardDefinitionId = attachedToolCardDefinitionId; }
    }
}