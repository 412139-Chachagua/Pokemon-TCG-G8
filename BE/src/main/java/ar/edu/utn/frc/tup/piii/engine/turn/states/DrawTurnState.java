package ar.edu.utn.frc.tup.piii.engine.turn.states;

import ar.edu.utn.frc.tup.piii.engine.turn.TurnPhase;

public class DrawTurnState implements TurnState {

    @Override
    public TurnPhase getPhase() {
        return TurnPhase.DRAW;
    }

    @Override
    public boolean canDraw() {
        return true;
    }

    @Override
    public boolean canPlay() {
        return false;
    }

    @Override
    public boolean canAttack() {
        return false;
    }

    @Override
    public boolean canEndTurn() {
        return true;
    }

    @Override
    public boolean canPlaceBasic() {
        return true;
    }

    @Override
    public TurnState advance() {
        return new MainTurnState();
    }
}
