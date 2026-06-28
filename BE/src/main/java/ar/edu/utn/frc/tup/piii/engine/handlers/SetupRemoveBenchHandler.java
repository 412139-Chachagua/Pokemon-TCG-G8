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
import java.util.UUID;

public class SetupRemoveBenchHandler implements GameHandler {
    @Override
    public void handle(EngineContext ctx, GameAction action) {
        String cardInstanceIdStr = action.getPayloadString("cardInstanceId");
        if (cardInstanceIdStr == null) {
            ctx.setError(new GameError("INVALID_PAYLOAD", "cardInstanceId is required"));
            return;
        }
        UUID cardInstanceId = UUID.fromString(cardInstanceIdStr);

        var player = ctx.getPlayer(action.getPlayerId());
        if (player == null) {
            ctx.setError(new GameError("PLAYER_NOT_FOUND", "Player not found"));
            return;
        }

        PokemonInPlay toRemove = null;
        for (PokemonInPlay pkm : player.getBench()) {
            if (pkm.getInstanceId().equals(cardInstanceId)) {
                toRemove = pkm;
                break;
            }
        }
        if (toRemove == null) {
            ctx.setError(new GameError("POKEMON_NOT_ON_BENCH", "Pokémon not found on bench"));
            return;
        }

        player.getBench().remove(toRemove);

        CardInstance card = new CardInstance(toRemove.getInstanceId(), toRemove.getCardDefinitionId());
        player.getHand().add(card);
        player.setSetupConfirmed(false);

        ctx.addEvent(new GameEvent(
                GameEventType.SETUP_BENCH_REMOVED.name(),
                ctx.getState().getMatchId(),
                0,
                Instant.now(),
                "Player removed Pokémon from bench during setup",
                Map.of("playerId", player.getPlayerId().toString(),
                        "instanceId", toRemove.getInstanceId().toString())
        ));
    }
}
