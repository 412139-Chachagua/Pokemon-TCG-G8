package ar.edu.utn.frc.tup.piii.engine.attack.steps;

import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AbstractAttackStep;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackEffectType;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

public class PrerequisiteStep extends AbstractAttackStep {

    private static final Logger log = LoggerFactory.getLogger(PrerequisiteStep.class);

    @Override
    public AttackStepResult execute(EngineContext ctx, AttackContext attackCtx) {
        var cardDef = ctx.getCardLookup().getCardById(attackCtx.getAttacker().getCardDefinitionId());
        if (cardDef instanceof PokemonCardDefinition pDef
                && pDef.getAttacks() != null
                && attackCtx.getAttackIndex() >= 0
                && attackCtx.getAttackIndex() < pDef.getAttacks().size()) {

            var attackDef = pDef.getAttacks().get(attackCtx.getAttackIndex());
            if (attackDef.getEffects() == null) return proceed(ctx, attackCtx);

            for (var effect : attackDef.getEffects()) {
                if (effect.getType() == AttackEffectType.COIN_FLIP_BEFORE_DAMAGE) {
                    Map<String, Object> params = effect.getParams() != null ? effect.getParams() : Map.of();
                    String subType = (String) params.get("effectType");
                    if ("CANCEL_ATTACK".equals(subType)) {
                        boolean heads = ctx.getRandomizer().nextInt(2) == 0;
                        log.warn("[prerequisite] Coin flip (cancel attack): {}", heads ? "HEADS" : "TAILS");
                        ctx.addEvent(new GameEvent(
                                GameEventType.COIN_FLIP_RESULT.name(),
                                ctx.getState().getMatchId(),
                                ctx.getState().getTurnNumber(),
                                Instant.now(),
                                heads ? "Cara" : "Cruz",
                                Map.of("result", heads ? "HEADS" : "TAILS", "source", "attack_cancel")
                        ));
                        if (!heads) {
                            log.warn("[prerequisite] Tails! Attack canceled.");
                            attackCtx.setAttackCanceled(true);
                            ctx.addEvent(new GameEvent(
                                    GameEventType.ATTACK_CANCELED.name(),
                                    ctx.getState().getMatchId(),
                                    ctx.getState().getTurnNumber(),
                                    Instant.now(),
                                    "The attack was canceled.",
                                    Map.of("reason", "coin_flip_tails")
                            ));
                            return proceed(ctx, attackCtx);
                        }
                    } else if ("DAMAGE_BONUS".equals(subType)) {
                        boolean heads = ctx.getRandomizer().nextInt(2) == 0;
                        log.warn("[prerequisite] Coin flip (damage bonus): {}", heads ? "HEADS" : "TAILS");
                        ctx.addEvent(new GameEvent(
                                GameEventType.COIN_FLIP_RESULT.name(),
                                ctx.getState().getMatchId(),
                                ctx.getState().getTurnNumber(),
                                Instant.now(),
                                heads ? "Cara" : "Cruz",
                                Map.of("result", heads ? "HEADS" : "TAILS", "source", "attack_effect")
                        ));
                        if (heads) {
                            try {
                                int bonus = Integer.parseInt(params.getOrDefault("effectParam", "0").toString());
                                attackCtx.setCoinFlipDamageBonus(bonus);
                                log.warn("[prerequisite] Heads! Damage bonus +{}", bonus);
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
        }
        return proceed(ctx, attackCtx);
    }
}
