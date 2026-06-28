package ar.edu.utn.frc.tup.piii.engine.trainer.resolvers;

import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.TurnFlags;
import ar.edu.utn.frc.tup.piii.engine.trainer.EffectType;
import ar.edu.utn.frc.tup.piii.engine.trainer.TrainerEffectResolver;

import java.util.HashMap;
import java.util.Map;

public class DamageModifyResolver implements TrainerEffectResolver {

    @Override
    public void resolve(EngineContext ctx, PlayerState player, TrainerCardDefinition card, Map<String, Object> payload) {
        String targetIdStr = (String) payload.get("targetPokemonInstanceId");
        if (targetIdStr == null) return;

        int modifierValue = 0;
        Object value = payload.get("modifierValue");
        if (value instanceof Number n) {
            modifierValue = n.intValue();
        }

        TurnFlags flags = ctx.getState().getTurnFlags();
        Map<String, Object> damageModifiers = flags.getDamageModifiers();
        if (damageModifiers == null) {
            damageModifiers = new HashMap<>();
            flags.setDamageModifiers(damageModifiers);
        }
        damageModifiers.put(targetIdStr, modifierValue);
    }

    @Override
    public EffectType getType() {
        return EffectType.DAMAGE_MODIFY;
    }
}
