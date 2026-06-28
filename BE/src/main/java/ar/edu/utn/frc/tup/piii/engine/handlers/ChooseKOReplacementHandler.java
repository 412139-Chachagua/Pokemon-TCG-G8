package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.action.GameError;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.turn.TurnManager;
import ar.edu.utn.frc.tup.piii.engine.turn.TurnPhase;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class ChooseKOReplacementHandler implements GameHandler {

    private final TurnManager turnManager;

    public ChooseKOReplacementHandler(TurnManager turnManager) {
        this.turnManager = turnManager;
    }

    public void handle(EngineContext ctx, GameAction action) {
        GameState state = ctx.getState();

        if (!state.isPendingKOReplacement()) return;
        if (!state.getKnockedOutPlayerId().equals(action.getPlayerId())) return;

        PlayerState player = ctx.getPlayer(action.getPlayerId());

        String benchPkmIdStr = action.getPayloadString("benchPokemonInstanceId");
        if (benchPkmIdStr == null) {
            ctx.setError(new GameError("INVALID_TARGET", "Missing benchPokemonInstanceId."));
            return;
        }
        UUID benchPkmId = UUID.fromString(benchPkmIdStr);

        PokemonInPlay selected = null;
        int index = -1;
        for (int i = 0; i < player.getBench().size(); i++) {
            if (player.getBench().get(i).getInstanceId().equals(benchPkmId)) {
                selected = player.getBench().get(i);
                index = i;
                break;
            }
        }

        if (selected == null) {
            ctx.setError(new GameError("INVALID_TARGET", "Selected Pokemon is not on the bench."));
            return;
        }

        // Move from bench to active
        player.getBench().remove(index);
        player.setActivePokemon(selected);

        // Clear pending replacement flags
        state.setPendingKOReplacement(false);
        state.setKnockedOutPlayerId(null);

        // Publish event
        ctx.addEvent(new GameEvent(
                GameEventType.KO_REPLACEMENT_DONE.name(),
                state.getMatchId(),
                state.getTurnNumber(),
                Instant.now(),
                "KO replacement chosen.",
                Map.of("playerId", action.getPlayerId().toString(),
                        "newActivePokemonInstanceId", selected.getInstanceId().toString())
        ));

        // If prize was already taken and turn hasn't fully started, resume it
        if (state.getPendingPrizeOwnerPlayerId() == null && state.getPhase() != TurnPhase.MAIN) {
            turnManager.startTurn(ctx);
        }
    }
}
