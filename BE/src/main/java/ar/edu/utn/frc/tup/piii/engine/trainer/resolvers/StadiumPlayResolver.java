package ar.edu.utn.frc.tup.piii.engine.trainer.resolvers;

import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.trainer.EffectType;
import ar.edu.utn.frc.tup.piii.engine.trainer.TrainerEffectResolver;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StadiumPlayResolver implements TrainerEffectResolver {

    @Override
    public void resolve(EngineContext ctx, PlayerState player, TrainerCardDefinition card, Map<String, Object> payload) {
        var state = ctx.getState();

        if (state.getStadiumCardInstanceId() != null) {
            UUID oldStadiumId = state.getStadiumCardInstanceId();
            String oldStadiumDefId = state.getStadiumCardDefinitionId();

            if (state.getStadiumOwnerPlayerId() != null) {
                PlayerState previousOwner = ctx.getPlayer(state.getStadiumOwnerPlayerId());
                if (previousOwner != null) {
                    CardInstance oldCard = new CardInstance(oldStadiumId, oldStadiumDefId != null ? oldStadiumDefId : "unknown");
                    previousOwner.pushToDiscard(oldCard);
                }
            }

            Map<String, Object> removePayload = new HashMap<>();
            removePayload.put("stadiumCardInstanceId", oldStadiumId.toString());
            removePayload.put("reason", "replaced");

            ctx.addEvent(new GameEvent(
                    GameEventType.STADIUM_REMOVED.name(),
                    state.getMatchId(),
                    state.getTurnNumber(),
                    Instant.now(),
                    "Stadium replaced.",
                    removePayload
            ));
        }

        Integer handIndex = (Integer) payload.get("handIndex");
        if (handIndex == null || handIndex < 0 || handIndex >= player.getHand().size()) return;

        CardInstance stadiumCard = player.getHand().get(handIndex);
        state.setStadiumCardInstanceId(stadiumCard.getInstanceId());
        state.setStadiumCardDefinitionId(stadiumCard.getCardDefinitionId());
        state.setStadiumOwnerPlayerId(player.getPlayerId());

        player.getHand().remove(handIndex.intValue());

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("stadiumCardInstanceId", stadiumCard.getInstanceId().toString());
        eventPayload.put("playerId", player.getPlayerId().toString());
        eventPayload.put("cardDefinitionId", stadiumCard.getCardDefinitionId());

        ctx.addEvent(new GameEvent(
                GameEventType.STADIUM_PLAYED.name(),
                state.getMatchId(),
                state.getTurnNumber(),
                Instant.now(),
                "Stadium played.",
                eventPayload
        ));
    }

    @Override
    public EffectType getType() {
        return EffectType.STADIUM_PLAY;
    }
}
