package ar.edu.utn.frc.tup.piii.engine.turn;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.MatchStatus;
import ar.edu.utn.frc.tup.piii.engine.attack.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.model.TurnFlags;
import ar.edu.utn.frc.tup.piii.engine.ports.RandomizerPort;
import ar.edu.utn.frc.tup.piii.engine.victory.FinishReason;
import ar.edu.utn.frc.tup.piii.engine.victory.VictoryConditionChecker;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TurnManager {
    private final RandomizerPort randomizerPort;

    public TurnManager(RandomizerPort randomizerPort) {
        this.randomizerPort = randomizerPort;
    }

    public void startTurn(EngineContext ctx) {
        GameState state = ctx.getState();
        PlayerState currentPlayer = findPlayerState(state, state.getCurrentPlayerId());

        resetTurnFlags(state.getTurnFlags());
        resetEvolvedThisTurn(currentPlayer);
        resetAbilitiesUsedThisTurn(currentPlayer);
        resetAbilitiesSuppressedNextTurn(state);

        state.setPendingPrizeOwnerPlayerId(null);
        state.setPendingPrizeCount(0);

        // If there's a pending KO replacement for the current player, skip auto-draw
        if (state.isPendingKOReplacement()
                && state.getKnockedOutPlayerId() != null
                && state.getKnockedOutPlayerId().equals(state.getCurrentPlayerId())) {
            return;
        }

        autoResolveDraw(ctx, currentPlayer);
    }

   public void endTurn(EngineContext ctx) {
        GameState state = ctx.getState();

        // Mark current player's first turn as completed if this is their first turn
        state.markPlayerCompletedFirstTurn(state.getCurrentPlayerId());

        state.setPhase(TurnPhase.BETWEEN_TURNS);

        // DC-11.5: Process between-turn status effects (poison, burn, sleep)
        List<GameEvent> betweenTurnEvents = StatusEffectManager.processBetweenTurnStatuses(state, ctx.getRandomizer(), ctx.getCardLookup(), ctx);
        for (GameEvent event : betweenTurnEvents) {
            ctx.addEvent(event);
        }

        // Clear turn-scoped flags for the current player (whose turn just ended).
        // "Next turn" effects expire at the end of the affected player's turn.
        PlayerState endingPlayer = findPlayerState(state, state.getCurrentPlayerId());
        if (endingPlayer != null) {
            endingPlayer.setCannotPlaySupportersNextTurn(false);
            if (endingPlayer.getActivePokemon() != null) {
                endingPlayer.getActivePokemon().setCannotRetreatNextTurn(false);
            }
        }

        if (state.getStatus() == MatchStatus.FINISHED) return;

        UUID nextPlayerId = findOtherPlayerId(state, state.getCurrentPlayerId());

        Map<String, Object> payload = new HashMap<>();
        payload.put("phase", "DRAW");
        payload.put("turnNumber", state.getTurnNumber() + 1);
        payload.put("nextPlayerId", nextPlayerId.toString());

        ctx.addEvent(new GameEvent(
                GameEventType.PHASE_CHANGED.name(),
                state.getMatchId(),
                state.getTurnNumber(),
                Instant.now(),
                "El turno " + state.getTurnNumber() + " del jugador " + state.getCurrentPlayerId() + " ha terminado.",
                payload
        ));

        state.setCurrentPlayerId(nextPlayerId);
        state.setTurnNumber(state.getTurnNumber() + 1);
        state.setPhase(TurnPhase.DRAW);
    }

    public void advancePhase(GameState state) {
        state.setPhase(state.getTurnState().advance().getPhase());
    }

    private void resetEvolvedThisTurn(PlayerState player) {
        if (player.getActivePokemon() != null) {
            player.getActivePokemon().setEvolvedThisTurn(false);
        }
        if (player.getBench() != null) {
            for (PokemonInPlay pkm : player.getBench()) {
                pkm.setEvolvedThisTurn(false);
            }
        }
    }

    private void resetAbilitiesUsedThisTurn(PlayerState player) {
        if (player.getActivePokemon() != null) {
            player.getActivePokemon().getAbilitiesUsedThisTurn().clear();
        }
        if (player.getBench() != null) {
            for (PokemonInPlay pkm : player.getBench()) {
                pkm.getAbilitiesUsedThisTurn().clear();
            }
        }
    }

    private void autoResolveDraw(EngineContext ctx, PlayerState currentPlayer) {
        GameState state = ctx.getState();
        if (state.getTurnFlags().hasDrawnForTurn()) return;

        if (state.getCurrentPlayerId().equals(state.getFirstPlayerId()) && state.getTurnNumber() == 1) {
            return;
        }

        if (currentPlayer.getDeck() == null || currentPlayer.getDeck().isEmpty()) {
            VictoryConditionChecker.VictoryCheckResult victoryResult =
                    VictoryConditionChecker.check(state, currentPlayer.getPlayerId());
            if (victoryResult.finished() && victoryResult.winnerPlayerId() != null) {
                state.setWinnerPlayerId(victoryResult.winnerPlayerId());
                state.setFinishReason(victoryResult.reason());
                state.setStatus(MatchStatus.FINISHED);
                ctx.addEvent(new GameEvent(
                        GameEventType.VICTORY_DECIDED.name(),
                        state.getMatchId(),
                        state.getTurnNumber(),
                        Instant.now(),
                        "Deck is empty. Player cannot draw.",
                        Map.of("winnerPlayerId", victoryResult.winnerPlayerId().toString())
                ));
            } else if (victoryResult.suddenDeath()) {
                state.setSuddenDeath(true);
                state.setStatus(MatchStatus.FINISHED);
                state.setFinishReason(FinishReason.SUDDEN_DEATH);
            }
            return;
        }
    }

    private PlayerState findPlayerState(GameState state, UUID playerId) {
        for (PlayerState player : state.getPlayers()) {
            if (player.getPlayerId().equals(playerId)) {
                return player;
            }
        }
        throw new IllegalStateException("Player not found: " + playerId);
    }

    private UUID findOtherPlayerId(GameState state, UUID currentPlayerId) {
        for (PlayerState player : state.getPlayers()) {
            if (!player.getPlayerId().equals(currentPlayerId)) {
                return player.getPlayerId();
            }
        }
        throw new IllegalStateException("No other player found for currentPlayerId: " + currentPlayerId);
    }

    private void resetTurnFlags(TurnFlags flags) {
        flags.setHasDrawnForTurn(false);
        flags.setHasAttachedEnergy(false);
        flags.setHasRetreated(false);
        flags.setHasPlayedSupporter(false);
        flags.setHasPlayedStadium(false);
        flags.setHasAttacked(false);
        flags.setDamageModifiers(null);
    }

    private void resetAbilitiesSuppressedNextTurn(GameState state) {
        for (PlayerState player : state.getPlayers()) {
            if (player.getActivePokemon() != null) {
                player.getActivePokemon().setAbilitiesSuppressedNextTurn(false);
            }
            if (player.getBench() != null) {
                for (PokemonInPlay pkm : player.getBench()) {
                    pkm.setAbilitiesSuppressedNextTurn(false);
                }
            }
        }
    }

    private void resetPreventAllDamageFlags(GameState state) {
        for (PlayerState player : state.getPlayers()) {
            if (player.getActivePokemon() != null) {
                player.getActivePokemon().setPreventAllDamageNextTurn(false);
                player.getActivePokemon().setCannotAttackNextTurn(false);
                player.getActivePokemon().setNextTurnDamageBonus(0);
                player.getActivePokemon().setCannotRetreatNextTurn(false);
                player.getActivePokemon().setReduceDamageNextTurn(0);
            }
            if (player.getBench() != null) {
                for (PokemonInPlay pkm : player.getBench()) {
                    pkm.setPreventAllDamageNextTurn(false);
                    pkm.setCannotAttackNextTurn(false);
                    pkm.setNextTurnDamageBonus(0);
                    pkm.setCannotRetreatNextTurn(false);
                    pkm.setReduceDamageNextTurn(0);
                }
            }
            player.setCannotPlaySupportersNextTurn(false);
        }
    }
}
