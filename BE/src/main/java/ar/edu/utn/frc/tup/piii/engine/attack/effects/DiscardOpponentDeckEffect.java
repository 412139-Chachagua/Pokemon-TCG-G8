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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;

public class DiscardOpponentDeckEffect implements PostDamageEffect {

    private static final Logger log = LoggerFactory.getLogger(DiscardOpponentDeckEffect.class);
    private final int count;
    private final String target; // "opponent" (default) or "self"
    private final boolean multiplyByDamageCounters;
    private final String attachIfEnergyType; // Dig Out: if top card matches this type, attach instead of discard

    public DiscardOpponentDeckEffect() { this(1, "opponent", false, null); }
    public DiscardOpponentDeckEffect(int count) { this(count, "opponent", false, null); }
    public DiscardOpponentDeckEffect(int count, String target) { this(count, target, false, null); }
    public DiscardOpponentDeckEffect(int count, String target, boolean multiplyByDamageCounters) { this(count, target, multiplyByDamageCounters, null); }
    public DiscardOpponentDeckEffect(int count, String target, boolean multiplyByDamageCounters, String attachIfEnergyType) {
        this.count = count;
        this.target = target != null ? target : "opponent";
        this.multiplyByDamageCounters = multiplyByDamageCounters;
        this.attachIfEnergyType = attachIfEnergyType;
    }

    @Override
    public void apply(EngineContext ctx, AttackContext attackCtx) {
        var attacker = attackCtx.getAttacker();
        PlayerState targetPlayer = "self".equals(target)
                ? ctx.getPlayer(attacker.getOwnerPlayerId())
                : ctx.getOpponent(attacker.getOwnerPlayerId());

        if (targetPlayer == null || targetPlayer.getDeck() == null || targetPlayer.getDeck().isEmpty()) return;
        int actualCount = multiplyByDamageCounters ? Math.max(attackCtx.getAttacker().getDamageCounters(), 0) : count;
        if (actualCount <= 0) return;

        // Dig Out style: check if the top card matches the attachIfEnergyType
        if (attachIfEnergyType != null) {
            CardInstance topCard = targetPlayer.getDeck().get(0);
            CardDefinition def = ctx.getCardLookup().getCardById(topCard.getCardDefinitionId());
            if (def instanceof EnergyCardDefinition ecd && ecd.getProvides() != null) {
                for (EnergyType et : ecd.getProvides()) {
                    if (et.name().equals(attachIfEnergyType)) {
                        targetPlayer.getDeck().remove(0);
                        if (attackCtx.getAttacker().getAttachedEnergies() == null) {
                            attackCtx.getAttacker().setAttachedEnergies(new java.util.ArrayList<>());
                        }
                        attackCtx.getAttacker().getAttachedEnergies().add(topCard);
                        log.warn("[discardDeck] Dig Out: Attached {} (top card) to attacker", topCard.getCardDefinitionId());
                        ctx.addEvent(new GameEvent(
                                GameEventType.ENERGY_SEARCHED.name(),
                                ctx.getState().getMatchId(),
                                ctx.getState().getTurnNumber(),
                                Instant.now(),
                                "Attached top card energy to attacker.",
                                Map.of("count", 1)
                        ));
                        return;
                    }
                }
            }
        }

        int toRemove = Math.min(actualCount, targetPlayer.getDeck().size());
        for (int i = 0; i < toRemove; i++) {
            targetPlayer.pushToDiscard(targetPlayer.getDeck().remove(0));
        }
        log.warn("[discardDeck] Discarded {} cards from {}'s deck", toRemove, target);
        ctx.addEvent(new GameEvent(
                GameEventType.ENERGY_DISCARDED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Discarded top card of " + target + " deck.",
                Map.of("count", toRemove, "target", target)
        ));
    }

    @Override
    public EffectTiming getTiming() { return EffectTiming.AFTER_DAMAGE; }
}
