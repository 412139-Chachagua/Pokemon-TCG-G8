package ar.edu.utn.frc.tup.piii.engine.trainer.resolvers;

import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.handlers.HandlerHelper;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.trainer.EffectType;
import ar.edu.utn.frc.tup.piii.engine.trainer.TrainerEffectResolver;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ToolAttachResolver implements TrainerEffectResolver {

    @Override
    public void resolve(EngineContext ctx, PlayerState player, TrainerCardDefinition card, Map<String, Object> payload) {
        String targetIdStr = (String) payload.get("targetPokemonInstanceId");
        if (targetIdStr == null) return;

        PokemonInPlay target = HandlerHelper.findPokemon(player, UUID.fromString(targetIdStr));
        if (target == null) return;

        if (target.getToolCardInstanceId() != null) return;

        Integer handIndex = (Integer) payload.get("handIndex");
        if (handIndex == null || handIndex < 0 || handIndex >= player.getHand().size()) return;

        target.setToolCardInstanceId(player.getHand().get(handIndex).getInstanceId());
        target.setAttachedTool(player.getHand().get(handIndex));

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("targetPokemonInstanceId", targetIdStr);
        eventPayload.put("toolCardInstanceId", target.getToolCardInstanceId().toString());

        ctx.addEvent(new GameEvent(
                GameEventType.TOOL_ATTACHED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Pokemon tool attached.",
                eventPayload
        ));
    }

    @Override
    public EffectType getType() {
        return EffectType.TOOL_ATTACH;
    }
}
