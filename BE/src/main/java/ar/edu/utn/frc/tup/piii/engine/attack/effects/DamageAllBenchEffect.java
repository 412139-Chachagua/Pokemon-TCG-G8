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

public class DamageAllBenchEffect implements PostDamageEffect {

    private static final Logger log = LoggerFactory.getLogger(DamageAllBenchEffect.class);
    private final int damageCounters;

    public DamageAllBenchEffect() { this(2); }
    public DamageAllBenchEffect(int damageCounters) { this.damageCounters = damageCounters; }

    @Override
    public void apply(EngineContext ctx, AttackContext attackCtx) {
        var attacker = attackCtx.getAttacker();
        PlayerState opponent = ctx.getOpponent(attacker.getOwnerPlayerId());
        if (opponent == null) return;
        // Damage the active Pokémon too (e.g. Eerie Voice: "each of your opponent's Pokémon")
        if (opponent.getActivePokemon() != null) {
            opponent.getActivePokemon().setDamageCounters(
                    opponent.getActivePokemon().getDamageCounters() + damageCounters);
        }
        if (opponent.getBench() != null) {
            for (PokemonInPlay pkm : opponent.getBench()) {
                pkm.setDamageCounters(pkm.getDamageCounters() + damageCounters);
            }
        }
        log.warn("[damageAllBench] Applied {} damage counters to all opponent Pokémon (active + bench)", damageCounters);
        ctx.addEvent(new GameEvent(
                GameEventType.BENCH_DAMAGE.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Damage to all opponent Pokémon.",
                Map.of("damageCounters", damageCounters)
        ));
    }

    @Override
    public EffectTiming getTiming() { return EffectTiming.AFTER_DAMAGE; }
}
