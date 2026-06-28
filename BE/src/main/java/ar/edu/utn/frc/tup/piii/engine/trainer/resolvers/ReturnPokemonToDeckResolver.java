package ar.edu.utn.frc.tup.piii.engine.trainer.resolvers;

import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.trainer.EffectType;
import ar.edu.utn.frc.tup.piii.engine.trainer.TrainerEffectResolver;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ReturnPokemonToDeckResolver implements TrainerEffectResolver {

    @Override
    public void resolve(EngineContext ctx, PlayerState player, TrainerCardDefinition card, Map<String, Object> payload) {
        UUID targetInstanceId = UUID.fromString((String) payload.get("targetPokemonInstanceId"));

        PokemonInPlay target = findPokemonInPlay(player, targetInstanceId);
        if (target == null) return;

        boolean isActive = player.getActivePokemon() != null
                && player.getActivePokemon().getInstanceId().equals(targetInstanceId);
        boolean hasBench = player.getBench() != null && !player.getBench().isEmpty();

        if (isActive && !hasBench) return;

        List<CardInstance> allAttachments = new ArrayList<>();
        if (target.getAttachedEnergies() != null) {
            allAttachments.addAll(target.getAttachedEnergies());
        }
        if (target.getAttachedTool() != null) {
            allAttachments.add(target.getAttachedTool());
        }

        if (isActive) {
            String newActiveIdStr = payload.get("newActiveInstanceId") != null
                    ? payload.get("newActiveInstanceId").toString() : null;
            if (newActiveIdStr != null) {
                UUID newActiveId = UUID.fromString(newActiveIdStr);
                for (int i = 0; i < player.getBench().size(); i++) {
                    if (player.getBench().get(i).getInstanceId().equals(newActiveId)) {
                        PokemonInPlay newActive = player.getBench().remove(i);
                        newActive.setEnteredTurnNumber(ctx.getState().getTurnNumber());
                        player.setActivePokemon(newActive);
                        break;
                    }
                }
            } else {
                player.getBench().get(0).setEnteredTurnNumber(ctx.getState().getTurnNumber());
                player.setActivePokemon(player.getBench().remove(0));
            }
        } else {
            player.getBench().remove(target);
        }

        CardInstance pokemonCard = new CardInstance(target.getInstanceId(), target.getCardDefinitionId());
        player.getDeck().add(pokemonCard);
        player.getDeck().addAll(allAttachments);

        ctx.getRandomizer().shuffle(player.getDeck());

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("pokemonInstanceId", targetInstanceId.toString());
        eventPayload.put("attachedCardsCount", allAttachments.size());

        ctx.addEvent(new GameEvent(
                GameEventType.POKEMON_RETURNED_TO_DECK.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Pokemon returned to deck",
                eventPayload
        ));
    }

    @Override
    public EffectType getType() {
        return EffectType.RETURN_POKEMON_TO_DECK;
    }

    private PokemonInPlay findPokemonInPlay(PlayerState player, UUID instanceId) {
        if (player.getActivePokemon() != null && player.getActivePokemon().getInstanceId().equals(instanceId)) {
            return player.getActivePokemon();
        }
        if (player.getBench() != null) {
            for (PokemonInPlay p : player.getBench()) {
                if (p.getInstanceId().equals(instanceId)) return p;
            }
        }
        return null;
    }
}
