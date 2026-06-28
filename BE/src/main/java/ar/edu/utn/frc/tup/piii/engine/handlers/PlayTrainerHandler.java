package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.TrainerSubtype;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.trainer.EffectType;
import ar.edu.utn.frc.tup.piii.engine.trainer.TrainerEffectRegistry;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class PlayTrainerHandler implements GameHandler {

    private final TrainerEffectRegistry effectRegistry;

    public PlayTrainerHandler(TrainerEffectRegistry effectRegistry) {
        this.effectRegistry = effectRegistry;
    }

    public void handle(EngineContext ctx, GameAction action) {
        GameState state = ctx.getState();
        PlayerState player = ctx.getPlayer(action.getPlayerId());
        Integer handIndex = action.getPayloadInt("handIndex");
        if (handIndex == null) return;

        if (handIndex < 0 || handIndex >= player.getHand().size()) return;
        CardInstance card = player.getHand().get(handIndex);

        CardDefinition def = ctx.getCardLookup().getCardById(card.getCardDefinitionId());
        if (!(def instanceof TrainerCardDefinition trainerDef)) return;

        if (trainerDef.getTrainerSubtype() == TrainerSubtype.SUPPORTER) {
            if (player.isCannotPlaySupportersNextTurn()) return;
            state.getTurnFlags().setHasPlayedSupporter(true);
        }

        if (trainerDef.getTrainerSubtype() == TrainerSubtype.STADIUM) {
            state.getTurnFlags().setHasPlayedStadium(true);
        }

        boolean hasEffect = trainerDef.getEffectCode() != null
                && effectRegistry.isEffectCodeKnown(trainerDef.getEffectCode());
        if (hasEffect) {
            effectRegistry.resolve(ctx, player, trainerDef, action.getPayload());
        }

        if (trainerDef.getTrainerSubtype() != TrainerSubtype.STADIUM) {
            if (player.getHand().contains(card)) {
                player.getHand().remove(card);
            }
            player.pushToDiscard(card);
        }

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("playerId", action.getPlayerId().toString());
        eventPayload.put("cardInstanceId", card.getInstanceId().toString());
        eventPayload.put("trainerSubtype", trainerDef.getTrainerSubtype().name());
        eventPayload.put("effectCode", trainerDef.getEffectCode());
        if (hasEffect) {
            EffectType et = effectRegistry.getEffectType(trainerDef.getEffectCode());
            eventPayload.put("effectType", et != null ? et.name() : null);
        }

        ctx.addEvent(new GameEvent(
                GameEventType.TRAINER_PLAYED.name(),
                state.getMatchId(),
                state.getTurnNumber(),
                Instant.now(),
                hasEffect ? "Trainer card played with effect." : "Trainer card played (no effect).",
                eventPayload
        ));

        if (hasEffect) {
            ctx.addEvent(new GameEvent(
                    GameEventType.TRAINER_EFFECT_RESOLVED.name(),
                    state.getMatchId(),
                    state.getTurnNumber(),
                    Instant.now(),
                    "Trainer effect resolved.",
                    eventPayload
            ));
        }
    }
}
