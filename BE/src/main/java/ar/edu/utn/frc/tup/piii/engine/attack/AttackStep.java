package ar.edu.utn.frc.tup.piii.engine.attack;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;

public interface AttackStep {
    AttackStepResult execute(EngineContext ctx, AttackContext attackCtx);
    void setNext(AttackStep next);
    AttackStep getNext();

    enum AttackStepResult {
        CONTINUE,
        STOP_CHAIN,
        STOP_CHAIN_END_TURN
    }
}
