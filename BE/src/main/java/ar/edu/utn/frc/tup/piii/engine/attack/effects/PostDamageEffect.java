package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;

public interface PostDamageEffect {
    void apply(EngineContext ctx, AttackContext attackCtx);
    EffectTiming getTiming();

    enum EffectTiming {
        BEFORE_DAMAGE,
        AFTER_DAMAGE,
        AFTER_KO_CHECK
    }
}
