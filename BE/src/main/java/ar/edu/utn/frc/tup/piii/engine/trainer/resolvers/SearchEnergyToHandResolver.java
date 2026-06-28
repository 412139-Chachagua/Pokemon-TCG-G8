package ar.edu.utn.frc.tup.piii.engine.trainer.resolvers;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardType;
import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.trainer.EffectType;
import ar.edu.utn.frc.tup.piii.engine.trainer.TrainerEffectResolver;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchEnergyToHandResolver implements TrainerEffectResolver {

    @Override
    public void resolve(EngineContext ctx, PlayerState player, TrainerCardDefinition card, Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        List<Integer> targetCardIndexes = (List<Integer>) payload.get("targetCardIndexes");
        if (targetCardIndexes == null || targetCardIndexes.isEmpty()) return;

        List<CardInstance> basicEnergies = new ArrayList<>();

        for (int i = 0; i < player.getDeck().size(); i++) {
            CardInstance ci = player.getDeck().get(i);
            CardDefinition def = ctx.getCardLookup().getCardById(ci.getCardDefinitionId());
            if (def instanceof EnergyCardDefinition energyDef && energyDef.getEnergyCardType() == EnergyCardType.BASIC) {
                basicEnergies.add(ci);
            }
        }

        int toTake = Math.min(targetCardIndexes.size(), basicEnergies.size());
        for (int i = 0; i < toTake; i++) {
            int selectedIndex = targetCardIndexes.get(i);
            if (selectedIndex >= 0 && selectedIndex < basicEnergies.size()) {
                CardInstance ci = basicEnergies.get(selectedIndex);
                player.getDeck().remove(ci);
                player.getHand().add(ci);
            }
        }

        ctx.getRandomizer().shuffle(player.getDeck());

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("count", toTake);

        ctx.addEvent(new GameEvent(
                GameEventType.ENERGY_SEARCHED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Searched for " + toTake + " basic Energy cards",
                eventPayload
        ));
    }

    @Override
    public EffectType getType() {
        return EffectType.SEARCH_ENERGY_TO_HAND;
    }
}
