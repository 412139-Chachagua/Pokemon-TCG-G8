package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

public class RecycleFromDiscardEffect implements PostDamageEffect {

    private static final Logger log = LoggerFactory.getLogger(RecycleFromDiscardEffect.class);

    @Override
    public void apply(EngineContext ctx, AttackContext attackCtx) {
        var attacker = attackCtx.getAttacker();
        PlayerState player = ctx.getPlayer(attacker.getOwnerPlayerId());
        if (player == null || player.getDiscard().isEmpty()) return;
        var card = player.popTopDiscard();
        player.getDeck().addFirst(card);
        log.warn("[recycle] Recycled card from discard to top of deck");
        ctx.addEvent(new GameEvent(
                GameEventType.ABILITY_USED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Card recycled from discard to deck.",
                Map.of()
        ));
    }

    @Override
    public EffectTiming getTiming() { return EffectTiming.AFTER_DAMAGE; }
}
