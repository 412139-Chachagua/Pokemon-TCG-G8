package ar.edu.utn.frc.tup.piii.engine.attack.steps;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AbstractAttackStep;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TargetSelectionStep extends AbstractAttackStep {

    private static final Logger log = LoggerFactory.getLogger(TargetSelectionStep.class);

    @Override
    public AttackStepResult execute(EngineContext ctx, AttackContext attackCtx) {
        if (attackCtx.getDefender() == null) {
            log.warn("[chain] TargetSelectionStep: STOP_CHAIN — defender is null");
            return AttackStepResult.STOP_CHAIN;
        }
        return proceed(ctx, attackCtx);
    }
}
