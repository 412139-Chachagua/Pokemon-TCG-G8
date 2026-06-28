package ar.edu.utn.frc.tup.piii.engine.turn.states;

import ar.edu.utn.frc.tup.piii.engine.turn.TurnPhase;

public interface TurnState {

    TurnPhase getPhase();

    boolean canDraw();

    boolean canPlay();

    boolean canAttack();

    boolean canEndTurn();

    boolean canPlaceBasic();

    TurnState advance();
}
