package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyType;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
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

public class AttachEnergyEffect implements PostDamageEffect {

    private static final Logger log = LoggerFactory.getLogger(AttachEnergyEffect.class);
    private final String source;
    private final String energyType;
    private final int count;
    private final String target;

    public AttachEnergyEffect(String source, String energyType, int count, String target) {
        this.source = source != null ? source : "deck";
        this.energyType = energyType;
        this.count = count;
        this.target = target != null ? target : "attacker";
    }

    @Override
    public void apply(EngineContext ctx, AttackContext attackCtx) {
        var attacker = attackCtx.getAttacker();
        PlayerState player = ctx.getPlayer(attacker.getOwnerPlayerId());
        if (player == null) return;

        PokemonInPlay targetPkm = resolveTargetPokemon(ctx, attackCtx, player);
        if (targetPkm == null) return;

        List<CardInstance> energyCards = findEnergyCards(player, ctx);
        int attached = 0;

        for (CardInstance energyCard : energyCards) {
            if (attached >= count) break;

            if ("deck".equals(source)) {
                player.getDeck().remove(energyCard);
                ctx.getEnergyService().attachFromDeck(energyCard, targetPkm, player, ctx);
            } else if ("discard".equals(source)) {
                if (!player.removeFromDiscard(energyCard.getInstanceId())) {
                    continue;
                }
                player.getHand().add(energyCard);
                ctx.getEnergyService().attachFromHand(energyCard, targetPkm, player, ctx);
            }
            attached++;
        }

        if (attached > 0) {
            attackCtx.setEnergyAttachedThisAttack(true);
            if ("deck".equals(source)) {
                ctx.getRandomizer().shuffle(player.getDeck());
            }
        }

        log.warn("[attachEnergy] Attached {} energy cards from {} to {}", attached, source,
                targetPkm.getInstanceId());

        ctx.addEvent(new GameEvent(
                GameEventType.ENERGY_ATTACHED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                attached + " energy attached from " + source + ".",
                Map.of(
                        "targetPokemonInstanceId", targetPkm.getInstanceId().toString(),
                        "source", source,
                        "energyType", energyType != null ? energyType : "ANY",
                        "count", attached
                )
        ));
    }

    private PokemonInPlay resolveTargetPokemon(EngineContext ctx, AttackContext attackCtx, PlayerState player) {
        if ("bench".equals(target)) {
            List<Map<String, Object>> benchTargets = attackCtx.getBenchTargets();
            if (benchTargets != null && !benchTargets.isEmpty()) {
                String targetIdStr = (String) benchTargets.get(0).get("instanceId");
                if (targetIdStr != null) {
                    UUID targetId = UUID.fromString(targetIdStr);
                    for (PokemonInPlay pkm : player.getBench()) {
                        if (pkm.getInstanceId().equals(targetId)) {
                            return pkm;
                        }
                    }
                }
            }
            if (player.getBench() != null && !player.getBench().isEmpty()) {
                return player.getBench().get(0);
            }
            return null;
        }
        PokemonInPlay active = player.getActivePokemon();
        if (active != null && active.getInstanceId().equals(attackCtx.getAttacker().getInstanceId())) {
            return active;
        }
        return null;
    }

    private List<CardInstance> findEnergyCards(PlayerState player, EngineContext ctx) {
        List<CardInstance> pool;
        if ("deck".equals(source)) {
            pool = player.getDeck() != null ? new ArrayList<>(player.getDeck()) : new ArrayList<>();
            // If no energy found in deck, also check hand (for search-then-attach sequences
            // like Energy Glide, where SearchDeckEffect already moved the card to hand)
            if (!hasMatchingEnergy(pool, ctx)) {
                pool = player.getHand() != null ? new ArrayList<>(player.getHand()) : pool;
            }
        } else if ("discard".equals(source)) {
            pool = new ArrayList<>(player.getDiscard());
        } else {
            return List.of();
        }

        List<CardInstance> result = new ArrayList<>();
        for (CardInstance ci : pool) {
            if (result.size() >= count) break;
            CardDefinition def = ctx.getCardLookup().getCardById(ci.getCardDefinitionId());
            if (def instanceof EnergyCardDefinition ecd) {
                if (energyType == null) {
                    result.add(ci);
                } else {
                    try {
                        EnergyType et = EnergyType.valueOf(energyType);
                        if (ecd.getProvides() != null && ecd.getProvides().contains(et)) {
                            result.add(ci);
                        }
                    } catch (IllegalArgumentException e) {
                    }
                }
            }
        }
        return result;
    }

    private boolean hasMatchingEnergy(List<CardInstance> pool, EngineContext ctx) {
        for (CardInstance ci : pool) {
            CardDefinition def = ctx.getCardLookup().getCardById(ci.getCardDefinitionId());
            if (def instanceof EnergyCardDefinition ecd) {
                if (energyType == null) return true;
                try {
                    EnergyType et = EnergyType.valueOf(energyType);
                    if (ecd.getProvides() != null && ecd.getProvides().contains(et)) return true;
                } catch (IllegalArgumentException e) {}
            }
        }
        return false;
    }

    @Override
    public EffectTiming getTiming() {
        return EffectTiming.AFTER_DAMAGE;
    }
}
