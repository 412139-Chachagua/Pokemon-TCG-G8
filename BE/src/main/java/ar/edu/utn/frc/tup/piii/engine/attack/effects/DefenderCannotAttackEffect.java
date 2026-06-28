package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

public class DefenderCannotAttackEffect implements PostDamageEffect {

    private static final Logger log = LoggerFactory.getLogger(DefenderCannotAttackEffect.class);

    @Override
    public void apply(EngineContext ctx, AttackContext attackCtx) {
        var defender = attackCtx.getDefender();
        defender.setCannotAttackNextTurn(true);

        String restrictedAttack = attackCtx.getRestrictedAttackName();
        if (restrictedAttack != null) {
            defender.setRestrictedAttackName(restrictedAttack);
        }

        log.warn("[defenderLock] Defender cannot attack next turn{}",
                restrictedAttack != null ? " (restricted: " + restrictedAttack + ")" : "");

        ctx.addEvent(new GameEvent(
                GameEventType.ABILITY_USED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                restrictedAttack != null
                        ? "Cannot use " + restrictedAttack + " next turn."
                        : "Defender cannot attack next turn.",
                Map.of("pokemonInstanceId", defender.getInstanceId().toString(),
                        "restrictedAttackName", restrictedAttack != null ? restrictedAttack : "all")
        ));
    }

    @Override
    public EffectTiming getTiming() {
        return EffectTiming.AFTER_DAMAGE;
    }
}
