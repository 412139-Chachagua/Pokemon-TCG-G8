package ar.edu.utn.frc.tup.piii.engine.model;

import ar.edu.utn.frc.tup.piii.engine.MatchStatus;
import ar.edu.utn.frc.tup.piii.engine.match.states.ActiveMatchState;
import ar.edu.utn.frc.tup.piii.engine.match.states.FinishedMatchState;
import ar.edu.utn.frc.tup.piii.engine.match.states.MatchState;
import ar.edu.utn.frc.tup.piii.engine.match.states.SetupMatchState;
import ar.edu.utn.frc.tup.piii.engine.match.states.WaitingMatchState;
import ar.edu.utn.frc.tup.piii.engine.turn.TurnPhase;
import ar.edu.utn.frc.tup.piii.engine.turn.states.AttackTurnState;
import ar.edu.utn.frc.tup.piii.engine.turn.states.BetweenTurnsTurnState;
import ar.edu.utn.frc.tup.piii.engine.turn.states.DrawTurnState;
import ar.edu.utn.frc.tup.piii.engine.turn.states.MainTurnState;
import ar.edu.utn.frc.tup.piii.engine.turn.states.TurnState;
import ar.edu.utn.frc.tup.piii.engine.victory.FinishReason;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class GameState {
    private UUID matchId;
    private MatchStatus status;
    private TurnPhase phase;
    private int turnNumber;
    private UUID currentPlayerId;
    private UUID firstPlayerId;
    private PlayerState[] players;
    private UUID stadiumCardInstanceId;
    private String stadiumCardDefinitionId;
    private UUID stadiumOwnerPlayerId;
    private TurnFlags turnFlags;
    private Set<UUID> playersWhoCompletedFirstTurn;
    private boolean pendingKOReplacement;
    private UUID knockedOutPlayerId;
    private boolean suddenDeath;
    private int prizeCountPerPlayer;
    private int handSize = 7;
    private Object pendingDecision;
    private UUID pendingPrizeOwnerPlayerId;
    private int pendingPrizeCount;
    private UUID winnerPlayerId;
    private FinishReason finishReason;
    private Map<UUID, UUID> playerDeckIds;
    private Instant createdAt;
    private Instant updatedAt;

    // Initial mulligan fields
    private Set<UUID> pendingInitialMulliganPlayers;

    // Mulligan draw fields
    private boolean mulliganDrawPending;
    private Map<UUID, Integer> mulliganDrawCounts;
    private Set<UUID> mulliganDrawResolved;
    private Instant mulliganDrawDeadline;

    public UUID getMatchId() { return matchId; }
    public void setMatchId(UUID matchId) { this.matchId = matchId; }

    public MatchStatus getStatus() { return status; }
    public void setStatus(MatchStatus status) { this.status = status; }

    @JsonIgnore
    public MatchState getMatchState() {
        return fromStatus(status);
    }

    public static MatchState fromStatus(MatchStatus status) {
        if (status == null) return new WaitingMatchState();
        return switch (status) {
            case WAITING -> new WaitingMatchState();
            case SETUP -> new SetupMatchState();
            case ACTIVE -> new ActiveMatchState();
            case FINISHED -> new FinishedMatchState();
        };
    }

    public TurnPhase getPhase() { return phase; }
    public void setPhase(TurnPhase phase) { this.phase = phase; }

    @JsonIgnore
    public TurnState getTurnState() {
        return fromPhase(phase);
    }

    public static TurnState fromPhase(TurnPhase phase) {
        if (phase == null) return new DrawTurnState();
        return switch (phase) {
            case DRAW -> new DrawTurnState();
            case MAIN -> new MainTurnState();
            case ATTACK -> new AttackTurnState();
            case BETWEEN_TURNS -> new BetweenTurnsTurnState();
        };
    }

    public int getTurnNumber() { return turnNumber; }
    public void setTurnNumber(int turnNumber) { this.turnNumber = turnNumber; }

    public UUID getCurrentPlayerId() { return currentPlayerId; }
    public void setCurrentPlayerId(UUID currentPlayerId) { this.currentPlayerId = currentPlayerId; }

    public UUID getFirstPlayerId() { return firstPlayerId; }
    public void setFirstPlayerId(UUID firstPlayerId) { this.firstPlayerId = firstPlayerId; }

    public PlayerState[] getPlayers() { return players; }
    public void setPlayers(PlayerState[] players) { this.players = players; }

    public UUID getStadiumCardInstanceId() { return stadiumCardInstanceId; }
    public void setStadiumCardInstanceId(UUID stadiumCardInstanceId) { this.stadiumCardInstanceId = stadiumCardInstanceId; }

    public String getStadiumCardDefinitionId() { return stadiumCardDefinitionId; }
    public void setStadiumCardDefinitionId(String stadiumCardDefinitionId) { this.stadiumCardDefinitionId = stadiumCardDefinitionId; }

    public UUID getStadiumOwnerPlayerId() { return stadiumOwnerPlayerId; }
    public void setStadiumOwnerPlayerId(UUID stadiumOwnerPlayerId) { this.stadiumOwnerPlayerId = stadiumOwnerPlayerId; }

    public TurnFlags getTurnFlags() { return turnFlags; }
    public void setTurnFlags(TurnFlags turnFlags) { this.turnFlags = turnFlags; }

    public Set<UUID> getPlayersWhoCompletedFirstTurn() { return playersWhoCompletedFirstTurn; }
    public void setPlayersWhoCompletedFirstTurn(Set<UUID> playersWhoCompletedFirstTurn) { this.playersWhoCompletedFirstTurn = playersWhoCompletedFirstTurn; }

    public void markPlayerCompletedFirstTurn(UUID playerId) {
        if (playersWhoCompletedFirstTurn == null) {
            playersWhoCompletedFirstTurn = new HashSet<>();
        }
        playersWhoCompletedFirstTurn.add(playerId);
    }

    public boolean hasPlayerCompletedFirstTurn(UUID playerId) {
        return playersWhoCompletedFirstTurn != null && playersWhoCompletedFirstTurn.contains(playerId);
    }

    public boolean isPendingKOReplacement() { return pendingKOReplacement; }
    public void setPendingKOReplacement(boolean pendingKOReplacement) { this.pendingKOReplacement = pendingKOReplacement; }

    public UUID getKnockedOutPlayerId() { return knockedOutPlayerId; }
    public void setKnockedOutPlayerId(UUID knockedOutPlayerId) { this.knockedOutPlayerId = knockedOutPlayerId; }

    public boolean isSuddenDeath() { return suddenDeath; }
    public void setSuddenDeath(boolean suddenDeath) { this.suddenDeath = suddenDeath; }

    public int getPrizeCountPerPlayer() { return prizeCountPerPlayer; }
    public void setPrizeCountPerPlayer(int prizeCountPerPlayer) { this.prizeCountPerPlayer = prizeCountPerPlayer; }

    public int getHandSize() { return handSize; }
    public void setHandSize(int handSize) { this.handSize = handSize; }

    public Object getPendingDecision() { return pendingDecision; }
    public void setPendingDecision(Object pendingDecision) { this.pendingDecision = pendingDecision; }

    public UUID getPendingPrizeOwnerPlayerId() { return pendingPrizeOwnerPlayerId; }
    public void setPendingPrizeOwnerPlayerId(UUID pendingPrizeOwnerPlayerId) { this.pendingPrizeOwnerPlayerId = pendingPrizeOwnerPlayerId; }

    public int getPendingPrizeCount() { return pendingPrizeCount; }
    public void setPendingPrizeCount(int pendingPrizeCount) { this.pendingPrizeCount = pendingPrizeCount; }

    public UUID getWinnerPlayerId() { return winnerPlayerId; }
    public void setWinnerPlayerId(UUID winnerPlayerId) { this.winnerPlayerId = winnerPlayerId; }

    public FinishReason getFinishReason() { return finishReason; }
    public void setFinishReason(FinishReason finishReason) { this.finishReason = finishReason; }

    public Map<UUID, UUID> getPlayerDeckIds() { return playerDeckIds; }
    public void setPlayerDeckIds(Map<UUID, UUID> playerDeckIds) { this.playerDeckIds = playerDeckIds; }
    public void addPlayerDeckId(UUID playerId, UUID deckId) {
        if (playerDeckIds == null) {
            playerDeckIds = new HashMap<>();
        }
        playerDeckIds.put(playerId, deckId);
    }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Set<UUID> getPendingInitialMulliganPlayers() { return pendingInitialMulliganPlayers; }
    public void setPendingInitialMulliganPlayers(Set<UUID> pendingInitialMulliganPlayers) { this.pendingInitialMulliganPlayers = pendingInitialMulliganPlayers; }

    public boolean hasPendingInitialMulligan() {
        return pendingInitialMulliganPlayers != null && !pendingInitialMulliganPlayers.isEmpty();
    }

    public boolean hasPendingInitialMulligan(UUID playerId) {
        return pendingInitialMulliganPlayers != null && pendingInitialMulliganPlayers.contains(playerId);
    }

    public void resolveInitialMulligan(UUID playerId) {
        if (pendingInitialMulliganPlayers != null) {
            pendingInitialMulliganPlayers.remove(playerId);
        }
    }

    public boolean isMulliganDrawPending() { return mulliganDrawPending; }
    public void setMulliganDrawPending(boolean mulliganDrawPending) { this.mulliganDrawPending = mulliganDrawPending; }

    public Map<UUID, Integer> getMulliganDrawCounts() { return mulliganDrawCounts; }
    public void setMulliganDrawCounts(Map<UUID, Integer> mulliganDrawCounts) { this.mulliganDrawCounts = mulliganDrawCounts; }

    public Set<UUID> getMulliganDrawResolved() { return mulliganDrawResolved; }
    public void setMulliganDrawResolved(Set<UUID> mulliganDrawResolved) { this.mulliganDrawResolved = mulliganDrawResolved; }

    public Instant getMulliganDrawDeadline() { return mulliganDrawDeadline; }
    public void setMulliganDrawDeadline(Instant mulliganDrawDeadline) { this.mulliganDrawDeadline = mulliganDrawDeadline; }

    public boolean hasPendingMulliganDraw(UUID playerId) {
        return mulliganDrawPending
            && mulliganDrawCounts != null
            && mulliganDrawCounts.containsKey(playerId)
            && (mulliganDrawResolved == null || !mulliganDrawResolved.contains(playerId));
    }

    public void resolveMulliganDraw(UUID playerId, boolean drew) {
        if (mulliganDrawResolved == null) mulliganDrawResolved = new HashSet<>();
        mulliganDrawResolved.add(playerId);
        if (mulliganDrawResolved.containsAll(mulliganDrawCounts.keySet())) {
            mulliganDrawPending = false;
        }
    }

    @JsonIgnore
    public boolean isMulliganFullyResolved() {
        return !mulliganDrawPending
            || mulliganDrawCounts == null
            || mulliganDrawCounts.isEmpty()
            || (mulliganDrawResolved != null && mulliganDrawResolved.containsAll(mulliganDrawCounts.keySet()));
    }
}