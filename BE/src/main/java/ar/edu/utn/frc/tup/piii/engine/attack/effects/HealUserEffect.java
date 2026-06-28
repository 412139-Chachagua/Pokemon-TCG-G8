package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class HealUserEffect implements PostDamageEffect {

    private static final Logger log = LoggerFactory.getLogger(HealUserEffect.class);
    private final int damageCountersHealed;
    private final boolean targetBench;
    private final boolean healAll;
    private final boolean clearConditions;
    private final boolean healFull;
    private final boolean clearDefenderConditions;

    public HealUserEffect(int damageCountersHealed) {
        this(damageCountersHealed, false, false, false, false, false);
    }

    public HealUserEffect(int damageCountersHealed, boolean targetBench) {
        this(damageCountersHealed, targetBench, false, false, false, false);
    }

    public HealUserEffect(int damageCountersHealed, boolean targetBench, boolean healAll, boolean clearConditions, boolean healFull) {
        this(damageCountersHealed, targetBench, healAll, clearConditions, healFull, false);
    }

    public HealUserEffect(int damageCountersHealed, boolean targetBench, boolean healAll, boolean clearConditions, boolean healFull, boolean clearDefenderConditions) {
        this.damageCountersHealed = damageCountersHealed;
        this.targetBench = targetBench;
        this.healAll = healAll;
        this.clearConditions = clearConditions;
        this.healFull = healFull;
        this.clearDefenderConditions = clearDefenderConditions;
    }

    @Override
    public void apply(EngineContext ctx, AttackContext attackCtx) {
        var attacker = attackCtx.getAttacker();
        PlayerState owner = ctx.getPlayer(attacker.getOwnerPlayerId());
        if (owner == null) return;

        if (healAll) {
            healTarget(ctx, owner.getActivePokemon(), attacker);
            if (owner.getBench() != null) {
                for (PokemonInPlay pkm : owner.getBench()) {
                    healTarget(ctx, pkm, attacker);
                }
            }
            return;
        }

        // Handle clearDefenderConditions - clear conditions from defender
        if (clearDefenderConditions) {
            PokemonInPlay defender = attackCtx.getDefender();
            if (defender != null && defender.getSpecialConditions() != null) {
                defender.getSpecialConditions().clear();
                log.warn("[heal] Removed special conditions from defender {}", defender.getInstanceId());
                ctx.addEvent(new GameEvent(
                        GameEventType.STATUS_REMOVED.name(),
                        ctx.getState().getMatchId(),
                        ctx.getState().getTurnNumber(),
                        Instant.now(),
                        "Removed all special conditions.",
                        Map.of("targetPokemonInstanceId", defender.getInstanceId().toString())
                ));
            }
        }

        // Regular target selection for healing
        if (clearDefenderConditions && damageCountersHealed <= 0) {
            return; // Only cleared conditions, no HP heal
        }

        UUID healTargetId = attackCtx.getHealTargetId();
        PokemonInPlay target;

        if (healTargetId != null) {
            if (owner.getActivePokemon() != null && owner.getActivePokemon().getInstanceId().equals(healTargetId)) {
                target = owner.getActivePokemon();
            } else if (owner.getBench() != null) {
                target = owner.getBench().stream()
                        .filter(p -> p.getInstanceId().equals(healTargetId))
                        .findFirst().orElse(attacker);
            } else {
                target = attacker;
            }
        } else if (targetBench) {
            if (owner.getBench() != null && !owner.getBench().isEmpty()) {
                target = owner.getBench().get(0);
            } else {
                target = attacker;
            }
        } else {
            target = attacker;
        }

        healTarget(ctx, target, attacker);
    }

    private void healTarget(EngineContext ctx, PokemonInPlay target, PokemonInPlay attacker) {
        if (target == null) return;
        int currentDamage = target.getDamageCounters();
        int actualHeal = healFull ? currentDamage : damageCountersHealed;
        int newDamage = Math.max(0, currentDamage - actualHeal);
        target.setDamageCounters(newDamage);

        if (clearConditions && !clearDefenderConditions && target.getSpecialConditions() != null) {
            target.getSpecialConditions().clear();
            log.warn("[heal] Removed special conditions from {}", target.getInstanceId());
        }

        log.warn("[heal] Healed {} counters from {} (id={})", actualHeal,
                target == attacker ? "attacker" : "other", target.getInstanceId());

        ctx.addEvent(new GameEvent(
                GameEventType.POKEMON_HEALED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Healed " + (actualHeal * 10) + " damage.",
                Map.of(
                        "targetPokemonInstanceId", target.getInstanceId().toString(),
                        "healedCounters", actualHeal,
                        "playerId", attacker.getOwnerPlayerId().toString()
                )
        ));
    }

    @Override
    public EffectTiming getTiming() {
        return EffectTiming.AFTER_DAMAGE;
    }
}
