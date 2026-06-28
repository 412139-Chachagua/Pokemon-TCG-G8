package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

public class OpponentShuffleDrawEffect implements PostDamageEffect {

    private static final Logger log = LoggerFactory.getLogger(OpponentShuffleDrawEffect.class);
    private final int drawCount;

    public OpponentShuffleDrawEffect() { this(4); }
    public OpponentShuffleDrawEffect(int drawCount) { this.drawCount = drawCount; }

    @Override
    public void apply(EngineContext ctx, AttackContext attackCtx) {
        var attacker = attackCtx.getAttacker();
        PlayerState opponent = ctx.getOpponent(attacker.getOwnerPlayerId());
        if (opponent == null) return;
        if (opponent.getHand() != null) {
            opponent.getDeck().addAll(opponent.getHand());
            opponent.getHand().clear();
        }
        ctx.getRandomizer().shuffle(opponent.getDeck());
        int toDraw = Math.min(drawCount, opponent.getDeck().size());
        for (int i = 0; i < toDraw; i++) {
            opponent.getHand().add(opponent.getDeck().remove(0));
        }
        log.warn("[oppShuffleDraw] Opponent shuffled hand into deck and drew {} cards", toDraw);
        ctx.addEvent(new GameEvent(
                GameEventType.CARDS_DRAWN.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Opponent shuffled hand and drew cards.",
                Map.of("playerId", opponent.getPlayerId().toString(), "count", toDraw)
        ));
    }

    @Override
    public EffectTiming getTiming() { return EffectTiming.AFTER_DAMAGE; }
}
