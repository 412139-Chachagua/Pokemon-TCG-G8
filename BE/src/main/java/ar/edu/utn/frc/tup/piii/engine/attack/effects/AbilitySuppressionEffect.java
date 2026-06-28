package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

public class AbilitySuppressionEffect implements PostDamageEffect {

    private static final Logger log = LoggerFactory.getLogger(AbilitySuppressionEffect.class);

    @Override
    public void apply(EngineContext ctx, AttackContext attackCtx) {
        var defender = attackCtx.getDefender();
        if (defender == null) return;

        defender.setAbilitiesSuppressedNextTurn(true);

        log.warn("[abilitySuppression] Suppressed abilities on defender={}", defender.getInstanceId());

        ctx.addEvent(new GameEvent(
                GameEventType.ABILITY_USED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Abilities suppressed for next turn.",
                Map.of("targetPokemonInstanceId", defender.getInstanceId().toString())
        ));
    }

    @Override
    public EffectTiming getTiming() {
        return EffectTiming.AFTER_DAMAGE;
    }
}
