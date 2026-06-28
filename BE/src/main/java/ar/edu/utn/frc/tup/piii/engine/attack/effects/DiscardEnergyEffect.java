package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DiscardEnergyEffect implements PostDamageEffect {

    private final int count;
    private final String target;
    private final boolean optional;

    public DiscardEnergyEffect(int count) {
        this(count, "defender", false);
    }

    public DiscardEnergyEffect(int count, String target) {
        this(count, target, false);
    }

    public DiscardEnergyEffect(int count, String target, boolean optional) {
        this.count = count;
        this.target = target;
        this.optional = optional;
    }

    @Override
    public void apply(EngineContext ctx, AttackContext attackCtx) {
        var pokemon = "attacker".equals(target) ? attackCtx.getAttacker() : attackCtx.getDefender();
        if (pokemon.getAttachedEnergies() == null || pokemon.getAttachedEnergies().isEmpty()) return;

        // If the discard is optional, only proceed if the player provided specific energy IDs
        if (optional && (attackCtx.getDiscardEnergyInstanceIds() == null
                || attackCtx.getDiscardEnergyInstanceIds().isEmpty())) {
            return;
        }

        PlayerState owner = ctx.getPlayer(pokemon.getOwnerPlayerId());
        List<UUID> specificIds = attackCtx.getDiscardEnergyInstanceIds();

        List<CardInstance> attached = new ArrayList<>(pokemon.getAttachedEnergies());
        List<CardInstance> toRemove;

        if (count >= 99) {
            toRemove = new ArrayList<>(attached);
        } else if (specificIds != null && !specificIds.isEmpty()) {
            toRemove = attached.stream()
                    .filter(ci -> specificIds.contains(ci.getInstanceId()))
                    .limit(count)
                    .toList();
        } else {
            toRemove = attached.stream()
                    .limit(count)
                    .toList();
        }

        ctx.getEnergyService().detachEnergies(pokemon, owner, toRemove, ctx);
        for (var e : toRemove) {
            ctx.addEvent(new GameEvent(
                    GameEventType.ENERGY_DISCARDED.name(),
                    ctx.getState().getMatchId(),
                    ctx.getState().getTurnNumber(),
                    Instant.now(),
                    "Energy discarded from " + target + ".",
                    Map.of(
                            "pokemonInstanceId", pokemon.getInstanceId().toString(),
                            "energyInstanceId", e.getInstanceId().toString(),
                            "reason", "ATTACK_EFFECT"
                    )
            ));
        }
    }

    @Override
    public EffectTiming getTiming() {
        return EffectTiming.AFTER_DAMAGE;
    }
}
