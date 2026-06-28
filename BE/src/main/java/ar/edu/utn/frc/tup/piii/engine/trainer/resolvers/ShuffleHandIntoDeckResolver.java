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

public class ShuffleHandIntoDeckResolver implements TrainerEffectResolver {

    private final Map<String, Integer> effectDrawCounts;

    public ShuffleHandIntoDeckResolver(Map<String, Integer> effectDrawCounts) {
        this.effectDrawCounts = effectDrawCounts;
    }

    public ShuffleHandIntoDeckResolver() {
        this.effectDrawCounts = new HashMap<>();
    }

    @Override
    public void resolve(EngineContext ctx, PlayerState player, TrainerCardDefinition card, Map<String, Object> payload) {
        String effectCode = card.getEffectCode();
        int drawCount = effectDrawCounts.getOrDefault(effectCode, 7);

        Object payloadCount = payload.get("drawCount");
        if (payloadCount instanceof Number n) {
            drawCount = n.intValue();
        }

        player.getDeck().addAll(player.getHand());
        player.getHand().clear();
        ctx.getRandomizer().shuffle(player.getDeck());

        int actualDraw = Math.min(drawCount, player.getDeck().size());
        for (int i = 0; i < actualDraw; i++) {
            player.getHand().add(player.getDeck().remove(0));
        }

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("drawCount", actualDraw);

        ctx.addEvent(new GameEvent(
                GameEventType.CARDS_DRAWN.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Hand shuffled into deck, " + actualDraw + " cards drawn.",
                eventPayload
        ));
    }

    @Override
    public EffectType getType() {
        return EffectType.SHUFFLE_HAND_INTO_DECK;
    }
}
