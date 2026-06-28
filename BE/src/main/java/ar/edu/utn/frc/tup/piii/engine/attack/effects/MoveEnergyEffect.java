package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.handlers.HandlerHelper;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MoveEnergyEffect implements PostDamageEffect {

    private static final Logger log = LoggerFactory.getLogger(MoveEnergyEffect.class);
    private final String sourcePokemon;
    private final String targetPokemonZone;
    private final int count;

    public MoveEnergyEffect(String sourcePokemon, String targetPokemonZone, int count) {
        this.sourcePokemon = sourcePokemon != null ? sourcePokemon : "attacker";
        this.targetPokemonZone = targetPokemonZone != null ? targetPokemonZone : "ownBench";
        this.count = count;
    }

    @Override
    public void apply(EngineContext ctx, AttackContext attackCtx) {
        var attacker = attackCtx.getAttacker();
        var defender = attackCtx.getDefender();

        PokemonInPlay source = "attacker".equals(sourcePokemon) ? attacker : defender;
        PlayerState sourceOwner = ctx.getPlayer(source.getOwnerPlayerId());

        PlayerState targetOwner;
        if ("ownBench".equals(targetPokemonZone)) {
            targetOwner = sourceOwner;
        } else {
            targetOwner = "attacker".equals(sourcePokemon)
                    ? ctx.getPlayer(defender.getOwnerPlayerId())
                    : sourceOwner;
        }

        if (source.getAttachedEnergies() == null || source.getAttachedEnergies().isEmpty()) return;
        if (targetOwner.getBench() == null || targetOwner.getBench().isEmpty()) return;

        PokemonInPlay targetPkm = resolveBenchTarget(attackCtx, targetOwner);
        if (targetPkm == null) return;

        List<CardInstance> toMove;
        List<UUID> specificIds = attackCtx.getMoveEnergyInstanceIds();
        if (specificIds != null && !specificIds.isEmpty()) {
            toMove = source.getAttachedEnergies().stream()
                    .filter(ci -> specificIds.contains(ci.getInstanceId()))
                    .limit(count)
                    .toList();
        } else {
            toMove = new ArrayList<>(source.getAttachedEnergies());
            if (toMove.size() > count) toMove = toMove.subList(0, count);
        }

        for (CardInstance energyCard : toMove) {
            ctx.getEnergyService().transferEnergy(energyCard, source, targetPkm, sourceOwner, ctx);
        }

        log.warn("[moveEnergy] Moved {} energy from {} to bench pokemon {}",
                toMove.size(), sourcePokemon, targetPkm.getInstanceId());

        ctx.addEvent(new GameEvent(
                GameEventType.ENERGY_ATTACHED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Energy moved from " + sourcePokemon + " to bench.",
                Map.of(
                        "sourcePokemonInstanceId", source.getInstanceId().toString(),
                        "targetPokemonInstanceId", targetPkm.getInstanceId().toString(),
                        "count", toMove.size()
                )
        ));
    }

    @Override
    public EffectTiming getTiming() {
        return EffectTiming.AFTER_DAMAGE;
    }

    private PokemonInPlay resolveBenchTarget(AttackContext attackCtx, PlayerState owner) {
        List<Map<String, Object>> benchTargets = attackCtx.getBenchTargets();
        if (benchTargets != null && !benchTargets.isEmpty()) {
            String targetIdStr = (String) benchTargets.get(0).get("instanceId");
            if (targetIdStr != null) {
                UUID targetId = UUID.fromString(targetIdStr);
                return owner.getBench().stream()
                        .filter(p -> p.getInstanceId().equals(targetId))
                        .findFirst()
                        .orElse(null);
            }
        }
        return owner.getBench().get(0);
    }
}
