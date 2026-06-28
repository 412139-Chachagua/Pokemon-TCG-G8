package ar.edu.utn.frc.tup.piii.engine.trainer.resolvers;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.handlers.HandlerHelper;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.trainer.EffectType;
import ar.edu.utn.frc.tup.piii.engine.trainer.TrainerEffectResolver;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AttachExtraEnergyResolver implements TrainerEffectResolver {

    @Override
    public void resolve(EngineContext ctx, PlayerState player, TrainerCardDefinition card, Map<String, Object> payload) {
        Integer handIndex = (Integer) payload.get("handIndex");
        if (handIndex == null || handIndex < 0 || handIndex >= player.getHand().size()) return;

        CardInstance energyCard = player.getHand().get(handIndex);
        CardDefinition def = ctx.getCardLookup().getCardById(energyCard.getCardDefinitionId());
        if (!(def instanceof EnergyCardDefinition)) return;

        String targetPkmIdStr = (String) payload.get("targetPokemonInstanceId");
        if (targetPkmIdStr == null) return;

        PokemonInPlay target = HandlerHelper.findPokemon(player, UUID.fromString(targetPkmIdStr));
        if (target == null) return;

        ctx.getEnergyService().attachFromHand(energyCard, target, player, ctx);

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("targetPokemonInstanceId", targetPkmIdStr);
        eventPayload.put("energyCardInstanceId", energyCard.getInstanceId().toString());

        ctx.addEvent(new GameEvent(
                GameEventType.ENERGY_ATTACHED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Extra energy attached by trainer effect.",
                eventPayload
        ));
    }

    @Override
    public EffectType getType() {
        return EffectType.ATTACH_EXTRA_ENERGY;
    }
}
