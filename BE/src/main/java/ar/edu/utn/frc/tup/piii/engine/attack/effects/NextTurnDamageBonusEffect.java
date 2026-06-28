package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

public class NextTurnDamageBonusEffect implements PostDamageEffect {

    private static final Logger log = LoggerFactory.getLogger(NextTurnDamageBonusEffect.class);
    private final int bonus;

    public NextTurnDamageBonusEffect(int bonus) {
        this.bonus = bonus;
    }

    @Override
    public void apply(EngineContext ctx, AttackContext attackCtx) {
        var attacker = attackCtx.getAttacker();
        attacker.setNextTurnDamageBonus(attacker.getNextTurnDamageBonus() + bonus);

        log.warn("[nextTurnDamageBonus] Set +{} damage bonus for next turn on attacker={}",
                bonus, attacker.getInstanceId());

        ctx.addEvent(new GameEvent(
                GameEventType.ABILITY_USED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Next turn damage bonus +" + bonus + ".",
                Map.of(
                        "pokemonInstanceId", attacker.getInstanceId().toString(),
                        "bonus", bonus
                )
        ));
    }

    @Override
    public EffectTiming getTiming() {
        return EffectTiming.AFTER_DAMAGE;
    }
}
