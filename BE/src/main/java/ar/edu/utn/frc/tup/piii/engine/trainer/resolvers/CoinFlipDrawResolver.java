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

public class CoinFlipDrawResolver implements TrainerEffectResolver {

    @Override
    public void resolve(EngineContext ctx, PlayerState player, TrainerCardDefinition card, Map<String, Object> payload) {
        boolean heads = ctx.getRandomizer().nextInt(2) == 0;

        Map<String, Object> coinPayload = new HashMap<>();
        coinPayload.put("result", heads ? "HEADS" : "TAILS");
        coinPayload.put("cardName", card.getName());
        coinPayload.put("source", "multi_coin_flip");
        coinPayload.put("flipIndex", 0);
        coinPayload.put("totalFlips", 1);

        ctx.addEvent(new GameEvent(
                GameEventType.COIN_FLIP_RESULT.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                card.getName() + ": " + (heads ? "Heads" : "Tails"),
                coinPayload
        ));

        if (heads) {
            int drawCount = Math.min(3, player.getDeck().size());
            for (int i = 0; i < drawCount; i++) {
                player.getHand().add(player.getDeck().remove(0));
            }

            Map<String, Object> drawPayload = new HashMap<>();
            drawPayload.put("drawCount", drawCount);
            drawPayload.put("cardName", card.getName());
            drawPayload.put("playerId", player.getPlayerId().toString());

            ctx.addEvent(new GameEvent(
                    GameEventType.CARDS_DRAWN.name(),
                    ctx.getState().getMatchId(),
                    ctx.getState().getTurnNumber(),
                    Instant.now(),
                    card.getName() + ": Drew " + drawCount + " cards",
                    drawPayload
            ));
        }
    }

    @Override
    public EffectType getType() {
        return EffectType.COIN_FLIP_DRAW;
    }
}
