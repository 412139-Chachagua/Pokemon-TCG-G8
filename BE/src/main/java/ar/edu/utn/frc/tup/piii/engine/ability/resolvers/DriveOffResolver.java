package ar.edu.utn.frc.tup.piii.engine.ability.resolvers;

import ar.edu.utn.frc.tup.piii.domain.cards.AbilityDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.ability.AbilityResolver;
import ar.edu.utn.frc.tup.piii.engine.action.GameError;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DriveOffResolver implements AbilityResolver {

    @Override
    public void resolve(EngineContext ctx, PlayerState player, PokemonInPlay pokemon,
                        AbilityDefinition ability, Map<String, Object> payload) {
        String targetInstanceIdStr = (String) payload.get("targetPokemonInstanceId");
        if (targetInstanceIdStr == null) {
            ctx.setError(new GameError("MISSING_TARGET", "Missing targetPokemonInstanceId."));
            return;
        }

        UUID targetInstanceId = UUID.fromString(targetInstanceIdStr);
        GameState state = ctx.getState();
        PlayerState opponent = ctx.getOpponent(player.getPlayerId());

        PokemonInPlay benchTarget = null;
        for (PokemonInPlay pkm : opponent.getBench()) {
            if (pkm.getInstanceId().equals(targetInstanceId)) {
                benchTarget = pkm;
                break;
            }
        }
        if (benchTarget == null) {
            ctx.setError(new GameError("INVALID_TARGET", "Target Pokemon not found on opponent's bench."));
            return;
        }

        PokemonInPlay currentActive = opponent.getActivePokemon();
        int benchIndex = opponent.getBench().indexOf(benchTarget);
        opponent.getBench().set(benchIndex, currentActive);
        opponent.setActivePokemon(benchTarget);

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("pokemonInstanceId", pokemon.getInstanceId().toString());
        eventPayload.put("targetPokemonInstanceId", targetInstanceIdStr);
        eventPayload.put("previousActiveInstanceId", currentActive.getInstanceId().toString());
        eventPayload.put("playerId", player.getPlayerId().toString());

        ctx.addEvent(new GameEvent(
                GameEventType.RETREAT_EXECUTED.name(),
                state.getMatchId(),
                state.getTurnNumber(),
                Instant.now(),
                "Drive Off: " + ctx.getCardLookup().getCardById(pokemon.getCardDefinitionId()).getName()
                        + " us\u00f3 Drive Off e intercambi\u00f3 el Activo del rival por un Pok\u00e9mon de su Banca.",
                eventPayload
        ));
    }
}
