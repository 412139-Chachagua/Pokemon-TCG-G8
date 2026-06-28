package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.action.GameError;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class ResolveMulliganDrawHandler implements GameHandler {

    @Override
    public void handle(EngineContext ctx, GameAction action) {
        GameState state = ctx.getState();

        if (!state.hasPendingMulliganDraw(action.getPlayerId())) {
            ctx.setError(new GameError("NO_PENDING_MULLIGAN_DRAW",
                "No tienes una decisión de mulligan pendiente"));
            return;
        }

        Object rawDraw = action.getPayload().get("drawCards");
        boolean draw = rawDraw instanceof Boolean && (Boolean) rawDraw;

        int count = state.getMulliganDrawCounts().getOrDefault(action.getPlayerId(), 0);
        if (draw && count > 0) {
            PlayerState player = ctx.getPlayer(action.getPlayerId());
            List<CardInstance> deck = player.getDeck();
            if (deck != null && !deck.isEmpty()) {
                int toDraw = Math.min(count, deck.size());
                for (int i = 0; i < toDraw; i++) {
                    player.getHand().add(deck.remove(0));
                }
            }
        }

        state.resolveMulliganDraw(action.getPlayerId(), draw);

        ctx.addEvent(new GameEvent(
            GameEventType.MULLIGAN_DRAW_RESOLVED.name(),
            state.getMatchId(), 0, Instant.now(),
            draw ? "Jugador robó cartas extra por mulligan"
                 : "Jugador no robó cartas extra por mulligan",
            Map.of(
                "playerId", action.getPlayerId().toString(),
                "drewCards", draw,
                "count", draw ? count : 0
            )
        ));

        ConfirmSetupHandler.tryTransitionToActive(ctx);
    }
}
