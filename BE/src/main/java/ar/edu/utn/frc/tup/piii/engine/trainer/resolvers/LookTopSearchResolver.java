package ar.edu.utn.frc.tup.piii.engine.trainer.resolvers;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
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

public class LookTopSearchResolver implements TrainerEffectResolver {

    @Override
    public void resolve(EngineContext ctx, PlayerState player, TrainerCardDefinition card, Map<String, Object> payload) {
        Object rawIndex = payload.get("targetCardIndex");
        if (rawIndex == null) return;
        int targetCardIndex = rawIndex instanceof Number n ? n.intValue() : Integer.parseInt(rawIndex.toString());

        int lookCount = Math.min(7, player.getDeck().size());
        List<CardInstance> topCards = new ArrayList<>(player.getDeck().subList(0, lookCount));

        if (targetCardIndex < 0 || targetCardIndex >= topCards.size()) return;

        CardInstance selected = topCards.get(targetCardIndex);
        CardDefinition selectedDef = ctx.getCardLookup().getCardById(selected.getCardDefinitionId());
        if (!(selectedDef instanceof PokemonCardDefinition)) return;

        player.getDeck().remove(selected);
        player.getHand().add(selected);

        List<CardInstance> remaining = new ArrayList<>(topCards);
        remaining.remove(selected);
        player.getDeck().addAll(0, remaining);
        ctx.getRandomizer().shuffle(player.getDeck());

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("cardId", selected.getCardDefinitionId());

        ctx.addEvent(new GameEvent(
                GameEventType.POKEMON_SEARCHED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Searched for a Pokemon",
                eventPayload
        ));
    }

    @Override
    public EffectType getType() {
        return EffectType.LOOK_TOP_SEARCH;
    }
}
