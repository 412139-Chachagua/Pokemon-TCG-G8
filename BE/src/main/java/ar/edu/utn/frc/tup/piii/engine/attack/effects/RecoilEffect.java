package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

public class RecoilEffect implements PostDamageEffect {

    private static final Logger log = LoggerFactory.getLogger(RecoilEffect.class);
    private final int damageCountersToSelf;

    public RecoilEffect(int damageCountersToSelf) {
        this.damageCountersToSelf = damageCountersToSelf;
    }

    @Override
    public void apply(EngineContext ctx, AttackContext attackCtx) {
        var attacker = attackCtx.getAttacker();
        attacker.setDamageCounters(attacker.getDamageCounters() + damageCountersToSelf);
        int hpDamage = damageCountersToSelf * 10;

        log.warn("[recoil] Attacker {} took {} recoil damage ({} counters)",
                attacker.getInstanceId(), hpDamage, damageCountersToSelf);

        ctx.addEvent(new GameEvent(
                GameEventType.RECOIL_OCCURRED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Recoil: " + hpDamage + " damage to self.",
                Map.of(
                        "attackerPokemonInstanceId", attacker.getInstanceId().toString(),
                        "damage", hpDamage,
                        "damageCounters", damageCountersToSelf
                )
        ));
    }

    @Override
    public EffectTiming getTiming() {
        return EffectTiming.AFTER_DAMAGE;
    }
}
