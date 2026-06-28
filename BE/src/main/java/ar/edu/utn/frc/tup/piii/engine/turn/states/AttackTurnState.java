package ar.edu.utn.frc.tup.piii.engine.turn.states;

import ar.edu.utn.frc.tup.piii.engine.turn.TurnPhase;

public class AttackTurnState implements TurnState {

    @Override
    public TurnPhase getPhase() {
        return TurnPhase.ATTACK;
    }

    @Override
    public boolean canDraw() {
        return false;
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
        return false;
    }

    @Override
    public boolean canPlaceBasic() {
        return false;
    }

    @Override
    public TurnState advance() {
        return new BetweenTurnsTurnState();
    }
}
