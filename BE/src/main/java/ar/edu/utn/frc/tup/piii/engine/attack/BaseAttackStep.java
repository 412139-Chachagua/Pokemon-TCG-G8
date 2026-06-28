package ar.edu.utn.frc.tup.piii.engine.attack;

public abstract class BaseAttackStep implements AttackStep {
    protected AttackStep next;

    @Override
    public void setNext(AttackStep next) {
        this.next = next;
    }

    protected AttackStepResult proceed(AttackStepResult currentResult) {
        if (currentResult != AttackStepResult.CONTINUE) {
            return currentResult;
        }
        if (next != null) {
            return next.execute(null, null);
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
