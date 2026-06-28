package ar.edu.utn.frc.tup.piii.engine.trainer.resolvers;

import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.trainer.EffectType;
import ar.edu.utn.frc.tup.piii.engine.trainer.TrainerEffectResolver;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SwitchPokemonResolver implements TrainerEffectResolver {

    @Override
    public void resolve(EngineContext ctx, PlayerState player, TrainerCardDefinition card, Map<String, Object> payload) {
        if (player.getBench().isEmpty()) return;

        String targetIdStr = (String) payload.get("targetPokemonInstanceId");
        if (targetIdStr == null) return;
        UUID targetId = UUID.fromString(targetIdStr);

        PokemonInPlay targetBench = null;
        int targetIndex = -1;
        for (int i = 0; i < player.getBench().size(); i++) {
            if (player.getBench().get(i).getInstanceId().equals(targetId)) {
                targetBench = player.getBench().get(i);
                targetIndex = i;
                break;
            }
        }
        if (targetBench == null) return;

        PokemonInPlay currentActive = player.getActivePokemon();
        player.getBench().set(targetIndex, currentActive);
        player.setActivePokemon(targetBench);

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("previousActiveInstanceId", currentActive != null ? currentActive.getInstanceId().toString() : null);
        eventPayload.put("newActiveInstanceId", targetBench.getInstanceId().toString());

        ctx.addEvent(new GameEvent(
                GameEventType.RETREAT_EXECUTED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Pokemon switched by trainer effect.",
                eventPayload
        ));
    }

    @Override
    public EffectType getType() {
        return EffectType.SWITCH_POKEMON;
    }
}
