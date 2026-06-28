package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;

import java.time.Instant;
import java.util.Map;

public class DrawCardsEffect implements PostDamageEffect {

    private final int count;

    public DrawCardsEffect(int count) {
        this.count = count;
    }

    @Override
    public void apply(EngineContext ctx, AttackContext attackCtx) {
        var attacker = attackCtx.getAttacker();

        PlayerState player = null;
        for (PlayerState ps : ctx.getState().getPlayers()) {
            boolean isAttacker = (ps.getActivePokemon() != null &&
                    ps.getActivePokemon().getInstanceId().equals(attacker.getInstanceId()));
            if (!isAttacker && ps.getBench() != null) {
                isAttacker = ps.getBench().stream()
                        .anyMatch(p -> p.getInstanceId().equals(attacker.getInstanceId()));
            }
            if (isAttacker) {
                player = ps;
                break;
            }
        }
        if (player == null) return;

        int toDraw = Math.min(count, player.getDeck() != null ? player.getDeck().size() : 0);
        int drawn = 0;
        for (int i = 0; i < toDraw; i++) {
            CardInstance card = player.getDeck().remove(0);
            player.getHand().add(card);
            drawn++;
        }

        if (drawn > 0) {
            ctx.addEvent(new GameEvent(
                    GameEventType.CARDS_DRAWN.name(),
                    ctx.getState().getMatchId(),
                    ctx.getState().getTurnNumber(),
                    Instant.now(),
                    "Drew " + drawn + " cards from attack effect.",
                    Map.of("count", drawn, "playerId", player.getPlayerId().toString())
            ));
        }
    }

    @Override
    public EffectTiming getTiming() {
        return EffectTiming.AFTER_DAMAGE;
    }
}
