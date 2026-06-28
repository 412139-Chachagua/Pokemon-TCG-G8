package ar.edu.utn.frc.tup.piii.engine.trainer.resolvers;

import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.trainer.EffectType;
import ar.edu.utn.frc.tup.piii.engine.trainer.TrainerEffectResolver;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class DrawCardsResolver implements TrainerEffectResolver {

    private final Map<String, Integer> effectDrawCounts;

    public DrawCardsResolver(Map<String, Integer> effectDrawCounts) {
        this.effectDrawCounts = effectDrawCounts;
    }

    public DrawCardsResolver() {
        this.effectDrawCounts = new HashMap<>();
    }

    @Override
    public void resolve(EngineContext ctx, PlayerState player, TrainerCardDefinition card, Map<String, Object> payload) {
        String effectCode = card.getEffectCode();
        int count = effectDrawCounts.getOrDefault(effectCode, 1);

        Object payloadCount = payload.get("count");
        if (payloadCount instanceof Number n) {
            count = n.intValue();
        }

        int actualDraw = Math.min(count, player.getDeck().size());
        for (int i = 0; i < actualDraw; i++) {
            player.getHand().add(player.getDeck().remove(0));
        }

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("count", actualDraw);
        eventPayload.put("effectCode", effectCode);

        ctx.addEvent(new GameEvent(
                GameEventType.CARDS_DRAWN.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                actualDraw + " cards drawn by trainer effect.",
                eventPayload
        ));
    }

    @Override
    public EffectType getType() {
        return EffectType.DRAW_CARDS;
    }
}
