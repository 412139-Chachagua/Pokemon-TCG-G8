package ar.edu.utn.frc.tup.piii.engine.attack;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;

public abstract class AbstractAttackStep implements AttackStep {
    protected AttackStep next;

    @Override
    public void setNext(AttackStep next) {
        this.next = next;
    }

    @Override
    public AttackStep getNext() {
        return next;
    }

    protected AttackStepResult proceed(EngineContext ctx, AttackContext attackCtx) {
        if (next != null) {
            return next.execute(ctx, attackCtx);
        }
        return AttackStepResult.CONTINUE;
    }

    public static AttackStep buildChain(AttackStep... steps) {
        for (int i = 0; i < steps.length - 1; i++) {
            steps[i].setNext(steps[i + 1]);
        }
        return steps[0];
    }
}
