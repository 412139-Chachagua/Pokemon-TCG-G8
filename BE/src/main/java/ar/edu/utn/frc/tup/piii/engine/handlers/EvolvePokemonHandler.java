package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.ability.hooks.SweetVeilHook;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.attack.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EvolvePokemonHandler implements GameHandler {
    public void handle(EngineContext ctx, GameAction action) {
        GameState state = ctx.getState();
        var player = ctx.getPlayer(action.getPlayerId());
        Integer handIndex = action.getPayloadInt("handIndex");
        if (handIndex == null) return;
        String targetIdStr = action.getPayloadString("targetPokemonInstanceId");
        if (targetIdStr == null) return;
        UUID targetPokemonInstanceId = UUID.fromString(targetIdStr);

        if (handIndex < 0 || handIndex >= player.getHand().size()) return;
        CardInstance evolutionCard = player.getHand().get(handIndex);

        PokemonCardDefinition evolutionDef = (PokemonCardDefinition)
                ctx.getCardLookup().getCardById(evolutionCard.getCardDefinitionId());

        if (evolutionDef == null) return;

        PokemonInPlay target = HandlerHelper.findPokemon(player, targetPokemonInstanceId);
        if (target == null) return;

        PokemonCardDefinition targetDef = (PokemonCardDefinition)
                ctx.getCardLookup().getCardById(target.getCardDefinitionId());
        if (targetDef == null) return;

        if (!evolutionDef.getEvolvesFrom().equalsIgnoreCase(targetDef.getName())) return;

        if (target.isEvolvedThisTurn()) return;

        String targetStage = targetDef.getStage();
        String evolutionStage = evolutionDef.getStage();
        if (!("BASIC".equalsIgnoreCase(targetStage) && "STAGE_1".equalsIgnoreCase(evolutionStage)) &&
                !("STAGE_1".equalsIgnoreCase(targetStage) && "STAGE_2".equalsIgnoreCase(evolutionStage))) {
            return;
        }

        player.getHand().remove(handIndex.intValue());
        PokemonInPlay evolved = new PokemonInPlay();
        evolved.setInstanceId(evolutionCard.getInstanceId());
        evolved.setCardDefinitionId(evolutionCard.getCardDefinitionId());
        evolved.setOwnerPlayerId(player.getPlayerId());
        evolved.setDamageCounters(target.getDamageCounters());
        evolved.setAttachedEnergies(target.getAttachedEnergies());
        evolved.setEvolvedThisTurn(true);
        evolved.setEnteredTurnNumber(state.getTurnNumber());
        evolved.setSpecialConditions(new ArrayList<>());
        StatusEffectManager.clearConditionsOnEvolveOrRetreat(evolved);

        if (player.getActivePokemon() != null &&
                player.getActivePokemon().getInstanceId().equals(targetPokemonInstanceId)) {
            player.setActivePokemon(evolved);
        } else {
            int index = player.getBench().indexOf(target);
            if (index != -1) {
                player.getBench().set(index, evolved);
            }
        }

        SweetVeilHook.syncImmunity(player, ctx.getCardLookup());

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("fromCardDefinitionId", target.getCardDefinitionId());
        eventPayload.put("toCardDefinitionId", evolutionCard.getCardDefinitionId());
        eventPayload.put("targetPokemonInstanceId", targetPokemonInstanceId.toString());

        ctx.addEvent(new GameEvent(
                GameEventType.POKEMON_EVOLVED.name(),
                state.getMatchId(),
                state.getTurnNumber(),
                Instant.now(),
                "Pokemon evolved.",
                eventPayload
        ));
    }
}
