package ar.edu.utn.frc.tup.piii.engine.attack.steps;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AbstractAttackStep;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.attack.DamageCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DamageStep extends AbstractAttackStep {

    private static final Logger log = LoggerFactory.getLogger(DamageStep.class);

    @Override
    public AttackStepResult execute(EngineContext ctx, AttackContext attackCtx) {
        if (attackCtx.isAttackCanceled()) {
            return proceed(ctx, attackCtx);
        }

        int bonus = attackCtx.getCoinFlipDamageBonus();
        if (bonus > 0) {
            log.warn("[damage] Adding coin flip damage bonus: +{}", bonus);
        }

        String stadiumEffectCode = resolveStadiumEffectCode(ctx);

        var dmgResult = DamageCalculator.calculate(
                attackCtx.getAttacker(),
                attackCtx.getDefender(),
                ctx.getCardLookup(),
                attackCtx.getAttackIndex(),
                attackCtx.getDamageModifiers(),
                attackCtx.getBaseDamageOverride(),
                attackCtx.isBypassWeakness(),
                attackCtx.isBypassResistance(),
                stadiumEffectCode
        );
        attackCtx.setDamageCalc(dmgResult);

        int totalDamageCounters = dmgResult.damageCountersAdded() + bonus / 10;
        int newDamage = attackCtx.getDefender().getDamageCounters() + totalDamageCounters;
        attackCtx.getDefender().setDamageCounters(newDamage);

        return proceed(ctx, attackCtx);
    }

    private String resolveStadiumEffectCode(EngineContext ctx) {
        String stadiumDefId = ctx.getState().getStadiumCardDefinitionId();
        if (stadiumDefId == null) return null;
        CardDefinition stadiumDef = ctx.getCardLookup().getCardById(stadiumDefId);
        if (stadiumDef instanceof TrainerCardDefinition trainerDef) {
            return trainerDef.getEffectCode();
        }
        return null;
    }
}
