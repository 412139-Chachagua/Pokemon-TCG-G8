package ar.edu.utn.frc.tup.piii.engine.match.states;

import ar.edu.utn.frc.tup.piii.engine.MatchStatus;
import ar.edu.utn.frc.tup.piii.engine.action.GameActionType;

public class ActiveMatchState implements MatchState {

    @Override
    public MatchStatus getStatus() {
        return MatchStatus.ACTIVE;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    public boolean canAcceptAction(GameActionType actionType) {
        return true;
    }
}
