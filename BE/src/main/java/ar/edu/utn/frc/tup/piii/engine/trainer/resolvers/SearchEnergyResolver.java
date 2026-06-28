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

public class SearchEnergyResolver implements TrainerEffectResolver {

    @Override
    public void resolve(EngineContext ctx, PlayerState player, TrainerCardDefinition card, Map<String, Object> payload) {
        Integer targetCardIndex = (Integer) payload.get("targetCardIndex");
        if (targetCardIndex == null || targetCardIndex < 0 || targetCardIndex >= player.getDeck().size()) return;

        CardInstance selected = player.getDeck().get(targetCardIndex);
        CardDefinition def = ctx.getCardLookup().getCardById(selected.getCardDefinitionId());
        if (!(def instanceof EnergyCardDefinition energyDef)) return;
        if (!energyDef.getEnergyCardType().name().equals("BASIC")) return;

        String targetPkmIdStr = (String) payload.get("targetPokemonInstanceId");
        if (targetPkmIdStr == null) return;

        PokemonInPlay target = HandlerHelper.findPokemon(player, UUID.fromString(targetPkmIdStr));
        if (target == null) return;

        ctx.getEnergyService().attachFromDeck(selected, target, player, ctx);

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("targetPokemonInstanceId", targetPkmIdStr);
        eventPayload.put("energyCardInstanceId", selected.getInstanceId().toString());

        ctx.addEvent(new GameEvent(
                GameEventType.POKEMON_SEARCHED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Basic energy searched and attached.",
                eventPayload
        ));
    }

    @Override
    public EffectType getType() {
        return EffectType.SEARCH_ENERGY;
    }
}
