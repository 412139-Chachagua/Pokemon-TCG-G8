package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.MatchStatus;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.turn.TurnManager;
import ar.edu.utn.frc.tup.piii.engine.turn.TurnPhase;
import ar.edu.utn.frc.tup.piii.engine.victory.FinishReason;
import ar.edu.utn.frc.tup.piii.engine.victory.VictoryConditionChecker;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TakePrizeCardHandler implements GameHandler {

    private final TurnManager turnManager;

    public TakePrizeCardHandler(TurnManager turnManager) {
        this.turnManager = turnManager;
    }

    @Override
    public void handle(EngineContext ctx, GameAction action) {
        GameState state = ctx.getState();
        UUID ownerPlayerId = state.getPendingPrizeOwnerPlayerId();
        int prizeCount = state.getPendingPrizeCount();
        if (ownerPlayerId == null || prizeCount <= 0) return;
        PlayerState player = ctx.getPlayer(ownerPlayerId);
        takePrizeImmediate(ctx, player, prizeCount);
        state.setPendingPrizeOwnerPlayerId(null);
        state.setPendingPrizeCount(0);
        if (state.getStatus() != MatchStatus.FINISHED && !state.isPendingKOReplacement()
                && state.getPhase() != TurnPhase.MAIN) {
            turnManager.startTurn(ctx);
        }
    }

    public static void takePrizeImmediate(EngineContext ctx, PlayerState player, int prizeCount) {
        int taken = 0;
        while (taken < prizeCount && !player.getPrizes().isEmpty()) {
            CardInstance takenPrize = player.getPrizes().remove(0);
            player.getHand().add(takenPrize);
            taken++;
        }

        VictoryConditionChecker.VictoryCheckResult victoryResult =
                VictoryConditionChecker.check(ctx.getState(), player.getPlayerId());
        if (victoryResult.finished()) {
            if (victoryResult.winnerPlayerId() != null) {
                ctx.getState().setWinnerPlayerId(victoryResult.winnerPlayerId());
                ctx.getState().setFinishReason(victoryResult.reason());
                ctx.getState().setStatus(MatchStatus.FINISHED);
            } else if (victoryResult.suddenDeath()) {
                ctx.getState().setSuddenDeath(true);
                ctx.getState().setStatus(MatchStatus.FINISHED);
                ctx.getState().setFinishReason(FinishReason.SUDDEN_DEATH);
            }
        }

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("playerId", player.getPlayerId().toString());
        eventPayload.put("remainingPrizeCount", player.getPrizes().size());
        eventPayload.put("prizeCount", taken);
        ctx.addEvent(new GameEvent(
                GameEventType.PRIZE_TAKEN.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                taken + " prize card(s) taken.",
                eventPayload
        ));
    }
}