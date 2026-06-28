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

public class HealResolver implements TrainerEffectResolver {

    private final Map<String, Integer> effectHealCounts;

    public HealResolver(Map<String, Integer> effectHealCounts) {
        this.effectHealCounts = effectHealCounts;
    }

    public HealResolver() {
        this.effectHealCounts = new HashMap<>();
    }

    @Override
    public void resolve(EngineContext ctx, PlayerState player, TrainerCardDefinition card, Map<String, Object> payload) {
        String effectCode = card.getEffectCode();
        int healCount = effectHealCounts.getOrDefault(effectCode, 2);

        Object payloadCount = payload.get("count");
        if (payloadCount instanceof Number n) {
            healCount = n.intValue();
        }

        String targetIdStr = (String) payload.get("targetPokemonInstanceId");
        if (targetIdStr == null) return;

        PokemonInPlay target = HandlerHelper.findPokemon(player, UUID.fromString(targetIdStr));
        if (target == null) return;

        int countersRemoved = Math.min(healCount, target.getDamageCounters());
        target.setDamageCounters(target.getDamageCounters() - countersRemoved);

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("targetPokemonInstanceId", targetIdStr);
        eventPayload.put("countersRemoved", countersRemoved);
        eventPayload.put("remainingDamageCounters", target.getDamageCounters());

        ctx.addEvent(new GameEvent(
                GameEventType.POKEMON_HEALED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                countersRemoved + " damage counters removed.",
                eventPayload
        ));
    }

    @Override
    public EffectType getType() {
        return EffectType.HEAL;
    }
}
