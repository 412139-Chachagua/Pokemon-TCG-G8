package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class DiscardToolEffect implements PostDamageEffect {

    private static final Logger log = LoggerFactory.getLogger(DiscardToolEffect.class);

    @Override
    public void apply(EngineContext ctx, AttackContext attackCtx) {
        var defender = attackCtx.getDefender();
        if (defender == null) return;

        UUID toolId = defender.getToolCardInstanceId();
        if (toolId != null) {
            defender.setToolCardInstanceId(null);
            defender.setAttachedTool(null);
            log.warn("[discardTool] Discarded Pokémon Tool from defender={}", defender.getInstanceId());
            ctx.addEvent(new GameEvent(
                    GameEventType.ENERGY_DISCARDED.name(),
                    ctx.getState().getMatchId(),
                    ctx.getState().getTurnNumber(),
                    Instant.now(),
                    "Pokémon Tool discarded.",
                    Map.of(
                            "targetPokemonInstanceId", defender.getInstanceId().toString(),
                            "toolCardInstanceId", toolId.toString()
                    )
            ));
        }
    }

    @Override
    public EffectTiming getTiming() {
        return EffectTiming.AFTER_DAMAGE;
    }
}
