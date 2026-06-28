package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

public class DamageReductionEffect implements PostDamageEffect {

    private static final Logger log = LoggerFactory.getLogger(DamageReductionEffect.class);
    private final int reduction;

    public DamageReductionEffect(int reduction) {
        this.reduction = reduction;
    }

    @Override
    public void apply(EngineContext ctx, AttackContext attackCtx) {
        var attacker = attackCtx.getAttacker();
        attacker.setReduceDamageNextTurn(reduction);

        log.warn("[damageReduction] Attacker will reduce incoming damage by {} next turn", reduction);

        ctx.addEvent(new GameEvent(
                GameEventType.ABILITY_USED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Damage reduction: -" + reduction + " next turn.",
                Map.of(
                        "pokemonInstanceId", attacker.getInstanceId().toString(),
                        "reduction", reduction
                )
        ));
    }

    @Override
    public EffectTiming getTiming() {
        return EffectTiming.AFTER_DAMAGE;
    }
}
