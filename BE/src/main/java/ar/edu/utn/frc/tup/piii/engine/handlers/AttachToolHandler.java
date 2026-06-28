package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.TrainerSubtype;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import java.util.List;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AttachToolHandler implements GameHandler {

    public void handle(EngineContext ctx, GameAction action) {
        GameState state = ctx.getState();
        PlayerState player = ctx.getPlayer(action.getPlayerId());

        Integer handIndex = action.getPayloadInt("handIndex");
        if (handIndex == null) return;
        if (handIndex < 0 || handIndex >= player.getHand().size()) return;

        CardInstance card = player.getHand().get(handIndex);
        CardDefinition def = ctx.getCardLookup().getCardById(card.getCardDefinitionId());
        if (!(def instanceof TrainerCardDefinition trainerDef)) return;
        if (trainerDef.getTrainerSubtype() != TrainerSubtype.POKEMON_TOOL
                && trainerDef.getTrainerSubtype() != TrainerSubtype.ITEM) {
            List<String> subtypes = trainerDef.getSubtypes();
            boolean found = false;
            if (subtypes != null) {
                for (String s : subtypes) {
                    String upper = s.toUpperCase()
                            .replace("É", "E")
                            .replace(" ", "_");
                    if ("TOOL".equals(upper) || "POKEMON_TOOL".equals(upper)) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) return;
        }

        String targetIdStr = action.getPayloadString("targetPokemonInstanceId");
        if (targetIdStr == null) return;

        PokemonInPlay target = HandlerHelper.findPokemon(player, UUID.fromString(targetIdStr));
        if (target == null) return;

        if (target.getToolCardInstanceId() != null) {
            player.pushToDiscard(target.getAttachedTool());
        }

        player.getHand().remove(handIndex.intValue());
        target.setToolCardInstanceId(card.getInstanceId());
        target.setAttachedTool(card);

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("playerId", player.getPlayerId().toString());
        eventPayload.put("targetPokemonInstanceId", targetIdStr);
        eventPayload.put("toolCardInstanceId", card.getInstanceId().toString());
        eventPayload.put("cardDefinitionId", card.getCardDefinitionId());

        ctx.addEvent(new GameEvent(
                GameEventType.TOOL_ATTACHED.name(),
                state.getMatchId(),
                state.getTurnNumber(),
                Instant.now(),
                "Pokemon tool attached.",
                eventPayload
        ));
    }
}
