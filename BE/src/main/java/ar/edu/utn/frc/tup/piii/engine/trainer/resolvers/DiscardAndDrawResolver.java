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

public class DiscardAndDrawResolver implements TrainerEffectResolver {

    private final Map<String, int[]> effectConfig;

    public DiscardAndDrawResolver(Map<String, int[]> effectConfig) {
        this.effectConfig = effectConfig;
    }

    public DiscardAndDrawResolver() {
        this.effectConfig = new HashMap<>();
    }

    @Override
    public void resolve(EngineContext ctx, PlayerState player, TrainerCardDefinition card, Map<String, Object> payload) {
        String effectCode = card.getEffectCode();
        int[] config = effectConfig.getOrDefault(effectCode, new int[]{0, 0});
        int discardCount = config.length > 0 ? config[0] : 0;
        int drawCount = config.length > 1 ? config[1] : 0;

        Integer discardPayload = (Integer) payload.get("discardCount");
        if (discardPayload != null) discardCount = discardPayload;

        Integer drawPayload = (Integer) payload.get("drawCount");
        if (drawPayload != null) drawCount = drawPayload;

        if (discardCount > 0 && payload.get("targetCardIndex") instanceof Integer idx
                && idx >= 0 && idx < player.getHand().size()) {
            player.pushToDiscard(player.getHand().remove(idx.intValue()));
        } else if (discardCount == -1 || discardCount >= player.getHand().size()) {
            player.pushManyToDiscard(player.getHand());
            player.getHand().clear();
        }

        int actualDraw = Math.min(drawCount, player.getDeck().size());
        for (int i = 0; i < actualDraw; i++) {
            player.getHand().add(player.getDeck().remove(0));
        }

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("discardCount", discardCount);
        eventPayload.put("drawCount", actualDraw);

        ctx.addEvent(new GameEvent(
                GameEventType.CARDS_DRAWN.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                discardCount + " discarded, " + actualDraw + " drawn.",
                eventPayload
        ));
    }

    @Override
    public EffectType getType() {
        return EffectType.DISCARD_AND_DRAW;
    }
}
