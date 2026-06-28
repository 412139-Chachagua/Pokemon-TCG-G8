package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

public class CanNotAttackNextTurnEffect implements PostDamageEffect {

    private static final Logger log = LoggerFactory.getLogger(CanNotAttackNextTurnEffect.class);
    private final String attackName; // null = all attacks restricted

    public CanNotAttackNextTurnEffect() {
        this(null);
    }

    public CanNotAttackNextTurnEffect(String attackName) {
        this.attackName = attackName;
    }

    @Override
    public void apply(EngineContext ctx, AttackContext attackCtx) {
        var attacker = attackCtx.getAttacker();
        attacker.setCannotAttackNextTurn(true);
        if (attackName != null) {
            attacker.setRestrictedAttackName(attackName);
        }

        log.warn("[cannotAttack] Set cannotAttackNextTurn on attacker={}, restrictedAttack={}",
                attacker.getInstanceId(), attackName);

        ctx.addEvent(new GameEvent(
                GameEventType.ABILITY_USED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                attackName != null
                        ? "Cannot use " + attackName + " next turn."
                        : "Cannot attack next turn.",
                Map.of(
                        "pokemonInstanceId", attacker.getInstanceId().toString(),
                        "restrictedAttackName", attackName != null ? attackName : "all"
                )
        ));
    }

    @Override
    public EffectTiming getTiming() {
        return EffectTiming.AFTER_DAMAGE;
    }
}
