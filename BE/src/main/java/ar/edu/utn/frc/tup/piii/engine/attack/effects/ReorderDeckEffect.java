package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;

import java.time.Instant;
import java.util.Map;

public class ReorderDeckEffect implements PostDamageEffect {

    private final int count;

    public ReorderDeckEffect(int count) {
        this.count = count;
    }

    @Override
    public void apply(EngineContext ctx, AttackContext attackCtx) {
        var attacker = attackCtx.getAttacker();
        PlayerState player = null;
        for (PlayerState ps : ctx.getState().getPlayers()) {
            if (ps.getActivePokemon() != null
                    && ps.getActivePokemon().getInstanceId().equals(attacker.getInstanceId())) {
                player = ps;
                break;
            }
        }
        if (player == null) return;

        int topCount = Math.min(count, player.getDeck() != null ? player.getDeck().size() : 0);

        ctx.addEvent(new GameEvent(
                GameEventType.DECK_ORDERED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Player looks at top " + topCount + " cards and reorders them.",
                Map.of("count", topCount, "playerId", player.getPlayerId().toString())
        ));
    }

    @Override
    public EffectTiming getTiming() {
        return EffectTiming.AFTER_DAMAGE;
    }
}
