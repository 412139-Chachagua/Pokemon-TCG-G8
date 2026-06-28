package ar.edu.utn.frc.tup.piii.engine.attack.steps;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.action.GameError;
import ar.edu.utn.frc.tup.piii.engine.attack.AbstractAttackStep;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnergyCheckStep extends AbstractAttackStep {

    private static final Logger log = LoggerFactory.getLogger(EnergyCheckStep.class);

    @Override
    public AttackStepResult execute(EngineContext ctx, AttackContext attackCtx) {
        if (!ctx.getEnergyService().checkAttackRequirements(
                attackCtx.getAttacker(), ctx.getCardLookup(), attackCtx.getAttackIndex())) {
            attackCtx.setEnergyValid(false);
            attackCtx.setErrorMessage("INSUFFICIENT_ENERGY");
            log.warn("[chain] EnergyCheckStep: STOP_CHAIN — insufficient energy for attack index {}",
                    attackCtx.getAttackIndex());
            ctx.setError(new GameError("INSUFFICIENT_ENERGY", "The attacking Pokemon does not have enough energy."));
            return AttackStepResult.STOP_CHAIN;
        }
        attackCtx.setEnergyValid(true);
        return proceed(ctx, attackCtx);
    }
}
