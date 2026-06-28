package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

public class DamagePreventionEffect implements PostDamageEffect {

    private static final Logger log = LoggerFactory.getLogger(DamagePreventionEffect.class);
    private final boolean preventEffects;
    private final Integer threshold; // null = no threshold (prevent all)

    public DamagePreventionEffect() {
        this(true, null);
    }

    public DamagePreventionEffect(boolean preventEffects) {
        this(preventEffects, null);
    }

    public DamagePreventionEffect(boolean preventEffects, Integer threshold) {
        this.preventEffects = preventEffects;
        this.threshold = threshold;
    }

    @Override
    public void apply(EngineContext ctx, AttackContext attackCtx) {
        var attacker = attackCtx.getAttacker();
        attacker.setPreventAllDamageNextTurn(true);
        if (threshold != null) {
            attacker.setPreventionDamageThreshold(threshold);
        }

        log.warn("[damagePrevention] Set preventAllDamageNextTurn on attacker={} with threshold={}",
                attacker.getInstanceId(), threshold);

        ctx.addEvent(new GameEvent(
                GameEventType.ABILITY_USED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                threshold != null
                        ? "Damage prevention activated for next turn (up to " + threshold + " damage)."
                        : "Damage prevention activated for next turn.",
                Map.of(
                        "pokemonInstanceId", attacker.getInstanceId().toString(),
                        "preventEffects", preventEffects,
                        "threshold", threshold != null ? String.valueOf(threshold) : "all"
                )
        ));
    }

    @Override
    public EffectTiming getTiming() {
        return EffectTiming.AFTER_DAMAGE;
    }
}
