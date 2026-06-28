package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;

import java.time.Instant;
import java.util.Map;

public class SetHpEffect implements PostDamageEffect {

    private final int targetHp;

    public SetHpEffect(int targetHp) {
        this.targetHp = targetHp;
    }

    @Override
    public void apply(EngineContext ctx, AttackContext attackCtx) {
        PokemonInPlay attacker = attackCtx.getAttacker();
        PokemonInPlay defender = attackCtx.getDefender();

        int attackerDamage = computeDamageToSet(attacker, ctx);
        int defenderDamage = computeDamageToSet(defender, ctx);

        if (attackerDamage >= 0) {
            attacker.setDamageCounters(attackerDamage);
        }
        if (defenderDamage >= 0) {
            defender.setDamageCounters(defenderDamage);
        }

        ctx.addEvent(new GameEvent(
                GameEventType.DAMAGE_APPLIED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Set HP effect: both active Pokemon set to " + targetHp + " HP remaining.",
                Map.of("targetHp", targetHp)
        ));
    }

    private int computeDamageToSet(PokemonInPlay pokemon, EngineContext ctx) {
        var cardDef = ctx.getCardLookup().getCardById(pokemon.getCardDefinitionId());
        if (cardDef instanceof PokemonCardDefinition pDef) {
            int maxHp = pDef.getHp();
            int targetCounters = (maxHp - targetHp) / 10;
            return Math.max(0, targetCounters);
        }
        return -1;
    }

    @Override
    public EffectTiming getTiming() {
        return EffectTiming.AFTER_DAMAGE;
    }
}
