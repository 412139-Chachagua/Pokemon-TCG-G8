package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OpponentDiscardHandEffect implements PostDamageEffect {

    private static final Logger log = LoggerFactory.getLogger(OpponentDiscardHandEffect.class);
    private final int count;

    public OpponentDiscardHandEffect() {
        this(1);
    }

    public OpponentDiscardHandEffect(int count) {
        this.count = count;
    }

    @Override
    public void apply(EngineContext ctx, AttackContext attackCtx) {
        var attacker = attackCtx.getAttacker();
        PlayerState opponent = ctx.getOpponent(attacker.getOwnerPlayerId());
        if (opponent == null || opponent.getHand() == null || opponent.getHand().isEmpty()) return;

        int toDiscard = Math.min(count, opponent.getHand().size());
        List<CardInstance> discarded = new ArrayList<>();
        for (int i = 0; i < toDiscard; i++) {
            discarded.add(opponent.getHand().remove(0));
        }
        opponent.pushManyToDiscard(discarded);

        log.warn("[opponentDiscardHand] Discarded {} cards from opponent's hand", toDiscard);

        ctx.addEvent(new GameEvent(
                GameEventType.ENERGY_DISCARDED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Opponent discarded " + toDiscard + " cards from hand.",
                Map.of("count", toDiscard)
        ));
    }

    @Override
    public EffectTiming getTiming() {
        return EffectTiming.AFTER_DAMAGE;
    }
}
