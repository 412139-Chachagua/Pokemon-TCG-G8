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
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SwitchDefenderEffect implements PostDamageEffect {

    private static final Logger log = LoggerFactory.getLogger(SwitchDefenderEffect.class);
    private final boolean switchAttacker;
    private final boolean conditionalOnEnergy;

    public SwitchDefenderEffect() {
        this(false, false);
    }

    public SwitchDefenderEffect(boolean switchAttacker) {
        this(switchAttacker, false);
    }

    public SwitchDefenderEffect(boolean switchAttacker, boolean conditionalOnEnergy) {
        this.switchAttacker = switchAttacker;
        this.conditionalOnEnergy = conditionalOnEnergy;
    }

    @Override
    public void apply(EngineContext ctx, AttackContext attackCtx) {
        // If conditional on energy attach, only switch if energy was attached this attack
        if (conditionalOnEnergy && !attackCtx.isEnergyAttachedThisAttack()) {
            log.warn("[switch] Conditional switch skipped - no energy was attached this attack");
            return;
        }

        PokemonInPlay target = switchAttacker ? attackCtx.getAttacker() : attackCtx.getDefender();
        PlayerState owner = ctx.getPlayer(target.getOwnerPlayerId());
        if (owner == null || owner.getBench() == null || owner.getBench().isEmpty()) {
            log.warn("[switch] No bench available for swap");
            return;
        }

        PokemonInPlay replacement = null;
        List<Map<String, Object>> benchTargets = attackCtx.getBenchTargets();
        if (benchTargets != null && !benchTargets.isEmpty()) {
            String targetIdStr = (String) benchTargets.get(0).get("instanceId");
            if (targetIdStr != null) {
                UUID targetId = UUID.fromString(targetIdStr);
                replacement = owner.getBench().stream()
                        .filter(p -> p.getInstanceId().equals(targetId))
                        .findFirst()
                        .orElse(null);
            }
        }
        if (replacement == null) {
            replacement = owner.getBench().get(0);
        }

        owner.getBench().remove(replacement);
        owner.getBench().add(target);
        owner.setActivePokemon(replacement);

        log.warn("[switch] Switched {} pokemon: {} -> {}",
                switchAttacker ? "attacker" : "defender",
                target.getInstanceId(), replacement.getInstanceId());

        ctx.addEvent(new GameEvent(
                GameEventType.SWITCH_EXECUTED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                (switchAttacker ? "Attacker" : "Defender") + " switched with bench Pokemon.",
                Map.of(
                        "oldActiveInstanceId", target.getInstanceId().toString(),
                        "newActiveInstanceId", replacement.getInstanceId().toString()
                )
        ));
    }

    @Override
    public EffectTiming getTiming() {
        return EffectTiming.AFTER_DAMAGE;
    }
}
