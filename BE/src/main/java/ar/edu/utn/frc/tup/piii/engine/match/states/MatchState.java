package ar.edu.utn.frc.tup.piii.engine.match.states;

import ar.edu.utn.frc.tup.piii.engine.MatchStatus;
import ar.edu.utn.frc.tup.piii.engine.action.GameActionType;

public interface MatchState {

    MatchStatus getStatus();

    boolean isActive();

    boolean isFinished();

    boolean canAcceptAction(GameActionType actionType);
}
