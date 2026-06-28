package ar.edu.utn.frc.tup.piii.engine.match.states;

import ar.edu.utn.frc.tup.piii.engine.MatchStatus;
import ar.edu.utn.frc.tup.piii.engine.action.GameActionType;

import java.util.Set;

public class SetupMatchState implements MatchState {

    private static final Set<GameActionType> SETUP_ACTIONS = Set.of(
            GameActionType.SETUP_PLACE_ACTIVE,
            GameActionType.SETUP_PLACE_BENCH,
            GameActionType.SETUP_REMOVE_ACTIVE,
            GameActionType.SETUP_REMOVE_BENCH,
            GameActionType.CONFIRM_SETUP,
            GameActionType.RESOLVE_MULLIGAN_DRAW,
            GameActionType.RESOLVE_INITIAL_MULLIGAN
    );

    @Override
    public MatchStatus getStatus() {
        return MatchStatus.SETUP;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    public boolean canAcceptAction(GameActionType actionType) {
        return SETUP_ACTIONS.contains(actionType);
    }
}
