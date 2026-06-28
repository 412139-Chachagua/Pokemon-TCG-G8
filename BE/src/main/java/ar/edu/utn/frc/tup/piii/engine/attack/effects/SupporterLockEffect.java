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

public class SupporterLockEffect implements PostDamageEffect {

    private static final Logger log = LoggerFactory.getLogger(SupporterLockEffect.class);

    @Override
    public void apply(EngineContext ctx, AttackContext attackCtx) {
        var attacker = attackCtx.getAttacker();
        PlayerState opponent = ctx.getOpponent(attacker.getOwnerPlayerId());
        if (opponent == null) return;

        opponent.setCannotPlaySupportersNextTurn(true);

        log.warn("[supporterLock] Opponent cannot play Supporters next turn");

        ctx.addEvent(new GameEvent(
                GameEventType.ABILITY_USED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Opponent cannot play Supporters next turn.",
                Map.of("targetPlayerId", opponent.getPlayerId().toString())
        ));
    }

    @Override
    public EffectTiming getTiming() {
        return EffectTiming.AFTER_DAMAGE;
    }
}
