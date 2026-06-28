package ar.edu.utn.frc.tup.piii.engine.attack;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;

public class AttackChainBuilder {

    public static AttackStep.AttackStepResult executeChain(AttackStep firstStep, EngineContext ctx, AttackContext attackCtx) {
        return firstStep.execute(ctx, attackCtx);
    }
}
