package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.MatchStatus;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.action.GameError;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.turn.TurnPhase;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class ConfirmSetupHandler implements GameHandler {
    @Override
    public void handle(EngineContext ctx, GameAction action) {
        var player = ctx.getPlayer(action.getPlayerId());
        if (player == null) {
            ctx.setError(new GameError("PLAYER_NOT_FOUND", "Player not found"));
            return;
        }

        if (player.getActivePokemon() == null) {
            ctx.setError(new GameError("NO_ACTIVE_POKEMON", "You must place an active Pokémon before confirming setup"));
            return;
        }

        if (player.isSetupConfirmed()) {
            ctx.setError(new GameError("ALREADY_CONFIRMED", "You have already confirmed your setup"));
            return;
        }

        player.setSetupConfirmed(true);

        ctx.addEvent(new GameEvent(
                GameEventType.SETUP_CONFIRMED.name(),
                ctx.getState().getMatchId(),
                0,
                Instant.now(),
                "Player confirmed setup",
                Map.of("playerId", player.getPlayerId().toString())
        ));

        boolean allConfirmed = true;
        for (PlayerState p : ctx.getState().getPlayers()) {
            if (!p.isSetupConfirmed()) {
                allConfirmed = false;
                break;
            }
        }

        if (allConfirmed) {
            tryTransitionToActive(ctx);
        }
    }

    public static boolean tryTransitionToActive(EngineContext ctx) {
        GameState state = ctx.getState();
        if (state.getStatus() != MatchStatus.SETUP) return false;
        if (state.hasPendingInitialMulligan()) return false;
        if (state.isMulliganDrawPending()) return false;

        for (PlayerState p : state.getPlayers()) {
            if (!p.isSetupConfirmed()) return false;
        }

        int coinFlip = ctx.getRandomizer().nextInt(2);
        UUID firstPlayerId = state.getPlayers()[coinFlip].getPlayerId();

        state.setFirstPlayerId(firstPlayerId);
        state.setCurrentPlayerId(firstPlayerId);
        state.setStatus(MatchStatus.ACTIVE);
        // First player doesn't draw on turn 1 (TCG rule), start in MAIN
        state.setPhase(TurnPhase.MAIN);
        state.setTurnNumber(1);

        String result = coinFlip == 0 ? "HEADS" : "TAILS";

        ctx.addEvent(new GameEvent(
                GameEventType.COIN_FLIP_RESULT.name(),
                state.getMatchId(),
                0,
                Instant.now(),
                result + " - " + firstPlayerId + " goes first",
                Map.of("winner", firstPlayerId.toString(), "result", result)
        ));

        ctx.addEvent(new GameEvent(
                GameEventType.SETUP_COMPLETED.name(),
                state.getMatchId(),
                1,
                Instant.now(),
                "Both players confirmed setup. Match is now active.",
                Map.of("firstPlayerId", firstPlayerId.toString())
        ));

        return true;
    }
}
