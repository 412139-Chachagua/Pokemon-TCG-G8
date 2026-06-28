package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.MatchStatus;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.turn.TurnManager;
import ar.edu.utn.frc.tup.piii.engine.turn.TurnPhase;
import ar.edu.utn.frc.tup.piii.engine.victory.FinishReason;
import ar.edu.utn.frc.tup.piii.engine.victory.VictoryConditionChecker;

import java.time.Instant;
import java.util.*;

public class DrawCardHandler implements GameHandler {

    private final TurnManager turnManager;

    public DrawCardHandler(TurnManager turnManager) {
        this.turnManager = turnManager;
    }

    public void handle(EngineContext ctx, GameAction action) {
        var state = ctx.getState();

        if (state.getCurrentPlayerId().equals(state.getFirstPlayerId()) && state.getTurnNumber() == 1) {
            ctx.addEvent(new GameEvent(
                    GameEventType.STATE_UPDATED.name(),
                    state.getMatchId(),
                    state.getTurnNumber(),
                    Instant.now(),
                    "El primer jugador no puede robar en su primer turno.",
                    Map.of("playerId", action.getPlayerId().toString())
            ));
            state.getTurnFlags().setHasDrawnForTurn(true);
            state.setPhase(TurnPhase.MAIN);
            return;
        }

        var player = ctx.getPlayer(action.getPlayerId());

        if (player.getDeck() == null || player.getDeck().isEmpty()) {
            VictoryConditionChecker.VictoryCheckResult victoryResult =
                    VictoryConditionChecker.check(state, action.getPlayerId());
            if (victoryResult.finished()) {
                if (victoryResult.winnerPlayerId() != null) {
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
            }

            state.getTurnFlags().setHasDrawnForTurn(true);
            if (state.getStatus() != MatchStatus.FINISHED) {
                turnManager.advancePhase(state);
            }
            return;
        }

        CardInstance card = player.getDeck().remove(0);
        player.getHand().add(card);

        state.getTurnFlags().setHasDrawnForTurn(true);
        turnManager.advancePhase(state);

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("count", 1);
        eventPayload.put("playerId", action.getPlayerId().toString());

        ctx.addEvent(new GameEvent(
                GameEventType.CARD_DRAWN.name(),
                state.getMatchId(),
                state.getTurnNumber(),
                Instant.now(),
                "Player drew 1 card.",
                eventPayload
        ));
    }
}
