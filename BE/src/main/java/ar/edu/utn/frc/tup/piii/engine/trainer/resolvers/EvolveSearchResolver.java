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
import java.util.HashMap;
import java.util.Map;

public class EvolveSearchResolver implements TrainerEffectResolver {

    @Override
    public void resolve(EngineContext ctx, PlayerState player, TrainerCardDefinition card, Map<String, Object> payload) {
        Integer targetCardIndex = (Integer) payload.get("targetCardIndex");
        if (targetCardIndex == null || targetCardIndex < 0 || targetCardIndex >= player.getDeck().size()) return;

        CardInstance selected = player.getDeck().get(targetCardIndex);
        CardDefinition def = ctx.getCardLookup().getCardById(selected.getCardDefinitionId());
        if (!(def instanceof PokemonCardDefinition)) return;

        player.getDeck().remove(targetCardIndex.intValue());
        player.getHand().add(selected);

        ctx.getRandomizer().shuffle(player.getDeck());

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("cardDefinitionId", selected.getCardDefinitionId());

        ctx.addEvent(new GameEvent(
                GameEventType.POKEMON_SEARCHED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Evolution card searched and added to hand.",
                eventPayload
        ));
    }

    @Override
    public EffectType getType() {
        return EffectType.EVOLVE_SEARCH;
    }
}
