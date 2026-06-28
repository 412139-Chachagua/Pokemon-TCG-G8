package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;

import java.time.Instant;
import java.util.Map;

public class OpponentRandomDiscardEffect implements PostDamageEffect {

    @Override
    public void apply(EngineContext ctx, AttackContext attackCtx) {
        var attacker = attackCtx.getAttacker();
        PlayerState opponent = null;
        for (PlayerState ps : ctx.getState().getPlayers()) {
            boolean isAttacker = ps.getActivePokemon() != null
                    && ps.getActivePokemon().getInstanceId().equals(attacker.getInstanceId());
            if (isAttacker) continue;
            opponent = ps;
        }
        if (opponent == null) return;
        if (opponent.getHand() == null || opponent.getHand().isEmpty()) return;

        // Pick a random card from opponent's hand
        int randomIndex = ctx.getRandomizer().nextInt(opponent.getHand().size());
        CardInstance picked = opponent.getHand().remove(randomIndex);

        // Reveal it and shuffle into deck
        CardDefinition cardDef = ctx.getCardLookup().getCardById(picked.getCardDefinitionId());
        String cardName = cardDef != null ? cardDef.getName() : "Unknown";

        if (opponent.getDeck() == null) {
            opponent.setDeck(new java.util.ArrayList<>());
        }
        opponent.getDeck().add(picked);
        ctx.getRandomizer().shuffle(opponent.getDeck());

        ctx.addEvent(new GameEvent(
                GameEventType.OPPONENT_RANDOM_DISCARD.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Random card from opponent's hand shuffled into deck.",
                Map.of(
                        "opponentId", opponent.getPlayerId().toString(),
                        "cardDefinitionId", picked.getCardDefinitionId(),
                        "cardName", cardName
                )
        ));
    }

    @Override
    public EffectTiming getTiming() {
        return EffectTiming.AFTER_DAMAGE;
    }
}
