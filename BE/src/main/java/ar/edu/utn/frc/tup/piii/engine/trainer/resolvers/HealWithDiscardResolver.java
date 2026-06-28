package ar.edu.utn.frc.tup.piii.engine.trainer.resolvers;

import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.trainer.EffectType;
import ar.edu.utn.frc.tup.piii.engine.trainer.TrainerEffectResolver;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HealWithDiscardResolver implements TrainerEffectResolver {

    @Override
    public void resolve(EngineContext ctx, PlayerState player, TrainerCardDefinition card, Map<String, Object> payload) {
        UUID targetInstanceId = UUID.fromString((String) payload.get("targetPokemonInstanceId"));
        int energyIndex = payload.containsKey("energyIndex") ? ((Number) payload.get("energyIndex")).intValue() : 0;

        PokemonInPlay target = findPokemonInPlay(player, targetInstanceId);
        if (target == null) return;

        int healed = Math.min(target.getDamageCounters(), 6);
        if (healed <= 0) return;

        target.setDamageCounters(target.getDamageCounters() - healed);

        if (target.getAttachedEnergies() != null && energyIndex >= 0 && energyIndex < target.getAttachedEnergies().size()) {
            CardInstance discardedEnergy = target.getAttachedEnergies().remove(energyIndex);
            player.getDiscard().add(discardedEnergy);
        }

        Map<String, Object> healPayload = new HashMap<>();
        healPayload.put("pokemonInstanceId", targetInstanceId.toString());
        healPayload.put("healed", healed);

        ctx.addEvent(new GameEvent(
                GameEventType.POKEMON_HEALED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Healed " + (healed * 10) + " damage",
                healPayload
        ));

        Map<String, Object> energyPayload = new HashMap<>();
        energyPayload.put("count", 1);
        energyPayload.put("pokemonInstanceId", targetInstanceId.toString());

        ctx.addEvent(new GameEvent(
                GameEventType.ENERGY_DISCARDED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Discarded 1 energy",
                energyPayload
        ));
    }

    @Override
    public EffectType getType() {
        return EffectType.HEAL_WITH_DISCARD;
    }

    private PokemonInPlay findPokemonInPlay(PlayerState player, UUID instanceId) {
        if (player.getActivePokemon() != null && player.getActivePokemon().getInstanceId().equals(instanceId)) {
            return player.getActivePokemon();
        }
        if (player.getBench() != null) {
            for (PokemonInPlay p : player.getBench()) {
                if (p.getInstanceId().equals(instanceId)) return p;
            }
        }
        return null;
    }
}
