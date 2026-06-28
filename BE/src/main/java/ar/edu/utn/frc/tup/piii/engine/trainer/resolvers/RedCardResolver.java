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
import java.util.UUID;

public class RedCardResolver implements TrainerEffectResolver {

    @Override
    public void resolve(EngineContext ctx, PlayerState player, TrainerCardDefinition card, Map<String, Object> payload) {
        UUID playerId = player.getPlayerId();
        PlayerState opponent = ctx.getOpponent(playerId);
        if (opponent == null) return;

        opponent.getDeck().addAll(opponent.getHand());
        opponent.getHand().clear();
        ctx.getRandomizer().shuffle(opponent.getDeck());

        int drawCount = Math.min(4, opponent.getDeck().size());
        for (int i = 0; i < drawCount; i++) {
            opponent.getHand().add(opponent.getDeck().remove(0));
        }

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("drawCount", drawCount);

        ctx.addEvent(new GameEvent(
                GameEventType.OPPONENT_HAND_SHUFFLED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Opponent's hand was shuffled into deck, they drew " + drawCount + " cards",
                eventPayload
        ));
    }

    @Override
    public EffectType getType() {
        return EffectType.OPPONENT_SHUFFLE_HAND_DRAW;
    }
}
