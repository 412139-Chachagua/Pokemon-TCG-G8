package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;

import java.time.Instant;
import java.util.*;

public class DamageBenchEffect implements PostDamageEffect {

    private final int damage;
    private final boolean ownBench;

    public DamageBenchEffect(int damage) {
        this(damage, false);
    }

    public DamageBenchEffect(int damage, boolean ownBench) {
        this.damage = damage;
        this.ownBench = ownBench;
    }

    @Override
    public void apply(EngineContext ctx, AttackContext attackCtx) {
        List<Map<String, Object>> benchTargets = attackCtx.getBenchTargets();
        var attacker = attackCtx.getAttacker();
        PlayerState targetPlayer = resolveTargetPlayer(ctx, attacker);

        if (targetPlayer == null || targetPlayer.getBench() == null || targetPlayer.getBench().isEmpty()) return;

        int damageCounters = damage / 10;
        List<Map<String, Object>> targetsResult = new ArrayList<>();

        if (ownBench) {
            for (PokemonInPlay pkm : targetPlayer.getBench()) {
                pkm.setDamageCounters(pkm.getDamageCounters() + damageCounters);
                targetsResult.add(Map.of(
                        "instanceId", pkm.getInstanceId().toString(),
                        "damageCounters", damageCounters
                ));
            }
        } else if (benchTargets != null) {
            for (Map<String, Object> target : benchTargets) {
                String instanceIdStr = (String) target.get("instanceId");
                if (instanceIdStr == null) continue;

                UUID instanceId = UUID.fromString(instanceIdStr);
                PokemonInPlay benched = targetPlayer.getBench().stream()
                        .filter(p -> p.getInstanceId().equals(instanceId))
                        .findFirst()
                        .orElse(null);
                if (benched == null) continue;

                benched.setDamageCounters(benched.getDamageCounters() + damageCounters);
                targetsResult.add(Map.of(
                        "instanceId", instanceIdStr,
                        "damageCounters", damageCounters
                ));
            }
        }

        if (!targetsResult.isEmpty()) {
            ctx.addEvent(new GameEvent(
                    GameEventType.BENCH_DAMAGE.name(),
                    ctx.getState().getMatchId(),
                    ctx.getState().getTurnNumber(),
                    Instant.now(),
                    "Damage applied to benched Pokemon.",
                    Map.of("targets", targetsResult)
            ));
        }
    }

    private PlayerState resolveTargetPlayer(EngineContext ctx, PokemonInPlay attacker) {
        for (PlayerState ps : ctx.getState().getPlayers()) {
            boolean isAttackerPlayer = (ps.getActivePokemon() != null &&
                    ps.getActivePokemon().getInstanceId().equals(attacker.getInstanceId()));
            if (!isAttackerPlayer && ps.getBench() != null) {
                isAttackerPlayer = ps.getBench().stream()
                        .anyMatch(p -> p.getInstanceId().equals(attacker.getInstanceId()));
            }
            if (ownBench && isAttackerPlayer) return ps;
            if (!ownBench && !isAttackerPlayer) return ps;
        }
        return null;
    }

    @Override
    public EffectTiming getTiming() {
        return EffectTiming.AFTER_DAMAGE;
    }
}
