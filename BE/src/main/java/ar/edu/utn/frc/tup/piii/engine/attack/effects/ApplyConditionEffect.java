package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.SpecialCondition;
import ar.edu.utn.frc.tup.piii.engine.ability.hooks.SweetVeilHook;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.attack.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

public class ApplyConditionEffect implements PostDamageEffect {

    private static final Logger log = LoggerFactory.getLogger(ApplyConditionEffect.class);
    private final SpecialCondition condition;
    private final String target; // "defender", "self", "both"

    public ApplyConditionEffect(SpecialCondition condition) {
        this(condition, "defender");
    }

    public ApplyConditionEffect(SpecialCondition condition, String target) {
        this.condition = condition;
        this.target = target != null ? target : "defender";
    }

    @Override
    public void apply(EngineContext ctx, AttackContext attackCtx) {
        if ("both".equals(target)) {
            applyTo(ctx, attackCtx.getAttacker(), attackCtx);
            applyTo(ctx, attackCtx.getDefender(), attackCtx);
        } else if ("self".equals(target)) {
            applyTo(ctx, attackCtx.getAttacker(), attackCtx);
        } else {
            applyTo(ctx, attackCtx.getDefender(), attackCtx);
        }
    }

    private void applyTo(EngineContext ctx, PokemonInPlay target, AttackContext attackCtx) {
        if (target == null) return;

        PlayerState owner = findOwner(ctx, target);
        if (owner != null && SweetVeilHook.isImmune(target, owner, ctx.getCardLookup())) {
            log.warn("[status] Sweet Veil blocked condition {} on target={}", condition, target.getInstanceId());
            return;
        }

        log.warn("[status] ApplyConditionEffect: applying {} to target={}",
                condition, target.getInstanceId());
        StatusEffectManager.applyCondition(target, condition);
        log.warn("[status] ApplyConditionEffect: condition applied, publishing STATUS_APPLIED event");
        ctx.addEvent(new GameEvent(
                GameEventType.STATUS_APPLIED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Special condition applied: " + condition.name(),
                Map.of(
                        "targetPokemonInstanceId", target.getInstanceId().toString(),
                        "condition", condition.name()
                )
        ));
    }

    private PlayerState findOwner(EngineContext ctx, PokemonInPlay target) {
        for (PlayerState player : ctx.getState().getPlayers()) {
            if (player.getActivePokemon() != null
                    && player.getActivePokemon().getInstanceId().equals(target.getInstanceId())) {
                return player;
            }
            if (player.getBench() != null) {
                for (PokemonInPlay pkm : player.getBench()) {
                    if (pkm.getInstanceId().equals(target.getInstanceId())) return player;
                }
            }
        }
        return null;
    }

    @Override
    public EffectTiming getTiming() {
        return EffectTiming.AFTER_DAMAGE;
    }
}
