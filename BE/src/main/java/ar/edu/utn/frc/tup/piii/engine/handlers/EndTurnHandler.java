package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.turn.TurnManager;

public class EndTurnHandler implements GameHandler {

    private final TurnManager turnManager;

    public EndTurnHandler(TurnManager turnManager) {
        this.turnManager = turnManager;
    }

    public void handle(EngineContext ctx, GameAction action) {
        turnManager.endTurn(ctx);
        turnManager.startTurn(ctx);
    }
}
