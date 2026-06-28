package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

public class RetreatLockEffect implements PostDamageEffect {

    private static final Logger log = LoggerFactory.getLogger(RetreatLockEffect.class);

    @Override
    public void apply(EngineContext ctx, AttackContext attackCtx) {
        var defender = attackCtx.getDefender();
        defender.setCannotRetreatNextTurn(true);

        log.warn("[retreatLock] Defender can't retreat next turn");

        ctx.addEvent(new GameEvent(
                GameEventType.ABILITY_USED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Defender cannot retreat next turn.",
                Map.of(
                        "pokemonInstanceId", defender.getInstanceId().toString(),
                        "effectCode", "RETREAT_LOCK"
                )
        ));
    }

    @Override
    public EffectTiming getTiming() {
        return EffectTiming.AFTER_DAMAGE;
    }
}
