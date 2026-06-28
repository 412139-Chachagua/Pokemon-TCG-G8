package ar.edu.utn.frc.tup.piii.engine.match.states;

import ar.edu.utn.frc.tup.piii.engine.MatchStatus;
import ar.edu.utn.frc.tup.piii.engine.action.GameActionType;

public class FinishedMatchState implements MatchState {

    @Override
    public MatchStatus getStatus() {
        return MatchStatus.FINISHED;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public boolean isFinished() {
        return true;
    }

    @Override
    public boolean canAcceptAction(GameActionType actionType) {
        return false;
    }
}
