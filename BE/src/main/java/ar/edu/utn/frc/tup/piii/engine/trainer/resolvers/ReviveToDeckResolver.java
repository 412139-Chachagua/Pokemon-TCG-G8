package ar.edu.utn.frc.tup.piii.engine.trainer.resolvers;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
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
import java.util.Map;
import java.util.UUID;

public class ReviveToDeckResolver implements TrainerEffectResolver {

    @Override
    public void resolve(EngineContext ctx, PlayerState player, TrainerCardDefinition card, Map<String, Object> payload) {
        if (player.getBench().size() >= 5) return;

        Object rawIndex = payload.get("targetCardIndex");
        if (rawIndex == null) return;
        int targetCardIndex = rawIndex instanceof Number n ? n.intValue() : Integer.parseInt(rawIndex.toString());

        if (targetCardIndex < 0 || targetCardIndex >= player.getDiscard().size()) return;

        CardInstance target = player.getDiscard().get(targetCardIndex);
        CardDefinition targetDef = ctx.getCardLookup().getCardById(target.getCardDefinitionId());
        if (!(targetDef instanceof PokemonCardDefinition)) return;

        player.getDiscard().remove(targetCardIndex);
        PokemonInPlay newPkm = new PokemonInPlay();
        newPkm.setInstanceId(UUID.randomUUID());
        newPkm.setCardDefinitionId(target.getCardDefinitionId());
        newPkm.setOwnerPlayerId(player.getPlayerId());
        newPkm.setEnteredTurnNumber(ctx.getState().getTurnNumber());
        newPkm.setDamageCounters(0);
        newPkm.setSpecialConditions(new ArrayList<>());
        newPkm.setAttachedEnergies(new ArrayList<>());
        player.getBench().add(newPkm);

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("pokemonInstanceId", newPkm.getInstanceId().toString());
        eventPayload.put("cardDefinitionId", target.getCardDefinitionId());

        ctx.addEvent(new GameEvent(
                GameEventType.POKEMON_REVIVED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Revived " + targetDef.getName() + " to bench",
                eventPayload
        ));
    }

    @Override
    public EffectType getType() {
        return EffectType.REVIVE_TO_DECK;
    }
}
