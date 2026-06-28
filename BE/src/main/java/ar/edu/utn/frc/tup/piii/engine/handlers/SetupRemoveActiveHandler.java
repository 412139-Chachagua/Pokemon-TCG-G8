package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.action.GameError;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;

import java.time.Instant;
import java.util.Map;

public class SetupRemoveActiveHandler implements GameHandler {
    @Override
    public void handle(EngineContext ctx, GameAction action) {
        var player = ctx.getPlayer(action.getPlayerId());
        if (player == null) {
            ctx.setError(new GameError("PLAYER_NOT_FOUND", "Player not found"));
            return;
        }

        PokemonInPlay active = player.getActivePokemon();
        if (active == null) {
            ctx.setError(new GameError("NO_ACTIVE_POKEMON", "No active Pokémon to remove"));
            return;
        }

        CardInstance card = new CardInstance(active.getInstanceId(), active.getCardDefinitionId());
        player.getHand().add(card);
        player.setActivePokemon(null);
        player.setSetupConfirmed(false);

        ctx.addEvent(new GameEvent(
                GameEventType.SETUP_ACTIVE_REMOVED.name(),
                ctx.getState().getMatchId(),
                0,
                Instant.now(),
                "Player removed active Pokémon during setup",
                Map.of("playerId", player.getPlayerId().toString(),
                        "instanceId", active.getInstanceId().toString())
        ));
    }
}
