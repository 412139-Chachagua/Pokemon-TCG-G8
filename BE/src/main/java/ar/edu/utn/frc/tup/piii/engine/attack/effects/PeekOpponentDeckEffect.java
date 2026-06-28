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

public class PeekOpponentDeckEffect implements PostDamageEffect {

    @Override
    public void apply(EngineContext ctx, AttackContext attackCtx) {
        var attacker = attackCtx.getAttacker();
        PlayerState attackerPlayer = null;
        PlayerState opponent = null;
        for (PlayerState ps : ctx.getState().getPlayers()) {
            boolean isAttacker = ps.getActivePokemon() != null
                    && ps.getActivePokemon().getInstanceId().equals(attacker.getInstanceId());
            if (isAttacker) {
                attackerPlayer = ps;
            } else {
                opponent = ps;
            }
        }
        if (attackerPlayer == null || opponent == null) return;

        // Read the top card of opponent's deck
        String topCardDefId = null;
        String topCardName = null;
        if (opponent.getDeck() != null && !opponent.getDeck().isEmpty()) {
            CardInstance topCard = opponent.getDeck().get(0);
            topCardDefId = topCard.getCardDefinitionId();
            CardDefinition cardDef = ctx.getCardLookup().getCardById(topCardDefId);
            topCardName = cardDef != null ? cardDef.getName() : null;
        }

        ctx.addEvent(new GameEvent(
                GameEventType.DECK_PEEKED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Player looks at opponent's top deck card.",
                Map.of(
                        "playerId", attackerPlayer.getPlayerId().toString(),
                        "opponentId", opponent.getPlayerId().toString(),
                        "cardDefinitionId", topCardDefId != null ? topCardDefId : "",
                        "cardName", topCardName != null ? topCardName : "",
                        "effectCode", "PEEK_OPPONENT_DECK"
                )
        ));
    }

    @Override
    public EffectTiming getTiming() {
        return EffectTiming.AFTER_DAMAGE;
    }
}
