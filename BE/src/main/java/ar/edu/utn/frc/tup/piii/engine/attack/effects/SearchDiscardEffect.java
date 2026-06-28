package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SearchDiscardEffect implements PostDamageEffect {

    private static final Logger log = LoggerFactory.getLogger(SearchDiscardEffect.class);
    private final int count;
    private final String cardType;

    public SearchDiscardEffect() { this(2); }
    public SearchDiscardEffect(int count) { this(count, null); }
    public SearchDiscardEffect(int count, String cardType) {
        this.count = count;
        this.cardType = cardType;
    }

    @Override
    public void apply(EngineContext ctx, AttackContext attackCtx) {
        var attacker = attackCtx.getAttacker();
        PlayerState player = ctx.getPlayer(attacker.getOwnerPlayerId());
        if (player == null || player.getDiscard().isEmpty()) return;

        List<CardInstance> discardPool = new ArrayList<>(player.getDiscard());
        List<CardInstance> taken = new ArrayList<>();

        for (CardInstance ci : discardPool) {
            if (taken.size() >= count) break;
            if (cardType != null) {
                CardDefinition def = ctx.getCardLookup().getCardById(ci.getCardDefinitionId());
                if (def == null) continue;
                if (!matchesType(def)) continue;
            }
            player.getDiscard().remove(ci);
            taken.add(ci);
        }
        player.getHand().addAll(taken);
        log.warn("[searchDiscard] Retrieved {} cards from discard to hand", taken.size());
        ctx.addEvent(new GameEvent(
                GameEventType.POKEMON_SEARCHED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Retrieved cards from discard.",
                Map.of("count", taken.size())
        ));
    }

    private boolean matchesType(CardDefinition def) {
        if ("ITEM".equals(cardType)) {
            return "TRAINER".equalsIgnoreCase(def.getSupertype())
                    && def.getSubtypes() != null && def.getSubtypes().stream().anyMatch(s -> s.equalsIgnoreCase("ITEM"));
        }
        return true;
    }

    @Override
    public EffectTiming getTiming() { return EffectTiming.AFTER_DAMAGE; }
}
