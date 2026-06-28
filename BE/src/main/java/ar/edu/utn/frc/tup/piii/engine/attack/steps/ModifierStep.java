package ar.edu.utn.frc.tup.piii.engine.attack.steps;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AbstractAttackStep;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;

import java.util.Map;

public class ModifierStep extends AbstractAttackStep {

    @Override
    public AttackStepResult execute(EngineContext ctx, AttackContext attackCtx) {
        var state = ctx.getState();
        if (state.getTurnFlags().getDamageModifiers() != null) {
            Map<String, Object> turnMods = state.getTurnFlags().getDamageModifiers();
            for (var entry : turnMods.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (attackCtx.getDamageModifiers().containsKey(key)) {
                    Object existing = attackCtx.getDamageModifiers().get(key);
                    if (existing instanceof Number n && value instanceof Number v) {
                        attackCtx.getDamageModifiers().put(key, n.intValue() + v.intValue());
                    }
                } else {
                    attackCtx.getDamageModifiers().put(key, value);
                }
            }
            state.getTurnFlags().setDamageModifiers(null);
        }
        return proceed(ctx, attackCtx);
    }
}
