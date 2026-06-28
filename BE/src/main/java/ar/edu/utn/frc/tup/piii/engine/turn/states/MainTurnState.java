package ar.edu.utn.frc.tup.piii.engine.turn.states;

import ar.edu.utn.frc.tup.piii.engine.turn.TurnPhase;

public class MainTurnState implements TurnState {

    @Override
    public TurnPhase getPhase() {
        return TurnPhase.MAIN;
    }

    @Override
    public boolean canDraw() {
        return false;
    }

    @Override
    public boolean canPlay() {
        return true;
    }

    @Override
    public boolean canAttack() {
        return true;
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
        return new AttackTurnState();
    }
}
