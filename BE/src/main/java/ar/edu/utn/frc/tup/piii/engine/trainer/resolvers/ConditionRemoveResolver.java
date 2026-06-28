package ar.edu.utn.frc.tup.piii.engine.trainer.resolvers;

import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.SpecialCondition;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.handlers.HandlerHelper;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.trainer.EffectType;
import ar.edu.utn.frc.tup.piii.engine.trainer.TrainerEffectResolver;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConditionRemoveResolver implements TrainerEffectResolver {

    @Override
    public void resolve(EngineContext ctx, PlayerState player, TrainerCardDefinition card, Map<String, Object> payload) {
        String targetIdStr = (String) payload.get("targetPokemonInstanceId");
        if (targetIdStr == null) return;

        PokemonInPlay target = HandlerHelper.findPokemon(player, UUID.fromString(targetIdStr));
        if (target == null) return;

        String conditionStr = (String) payload.get("condition");
        if (conditionStr != null && !conditionStr.isBlank()) {
            try {
                SpecialCondition condition = SpecialCondition.valueOf(conditionStr.toUpperCase());
                target.getSpecialConditions().remove(condition);
            } catch (IllegalArgumentException ignored) {}
        } else {
            target.setSpecialConditions(new ArrayList<>());
        }

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("targetPokemonInstanceId", targetIdStr);
        eventPayload.put("removedCondition", conditionStr != null ? conditionStr : "ALL");

        ctx.addEvent(new GameEvent(
                GameEventType.POKEMON_HEALED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Special conditions removed.",
                eventPayload
        ));
    }

    @Override
    public EffectType getType() {
        return EffectType.CONDITION_REMOVE;
    }
}
