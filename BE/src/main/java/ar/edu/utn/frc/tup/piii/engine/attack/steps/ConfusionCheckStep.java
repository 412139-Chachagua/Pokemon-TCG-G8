package ar.edu.utn.frc.tup.piii.engine.attack.steps;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AbstractAttackStep;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.attack.StatusEffectManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfusionCheckStep extends AbstractAttackStep {

    private static final Logger log = LoggerFactory.getLogger(ConfusionCheckStep.class);

    @Override
    public AttackStepResult execute(EngineContext ctx, AttackContext attackCtx) {
        var attacker = attackCtx.getAttacker();

        // CONFUSED → 50% self-hit (Paso 2 RF-01c)
        if (StatusEffectManager.isConfused(attacker) && ctx.getRandomizer().nextInt(2) == 0) {
            int selfDmgCounters = 3;
            attackCtx.setConfusedSelfHit(true);
            attackCtx.setSelfDamageCounters(selfDmgCounters);
            return AttackStepResult.STOP_CHAIN_END_TURN;
        }
        return proceed(ctx, attackCtx);
    }
}
