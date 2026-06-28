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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SearchBasicPokemonResolver implements TrainerEffectResolver {

    @Override
    public void resolve(EngineContext ctx, PlayerState player, TrainerCardDefinition card, Map<String, Object> payload) {
        if (player.getBench().size() >= 5) return;

        Integer targetCardIndex = (Integer) payload.get("targetCardIndex");
        if (targetCardIndex == null || targetCardIndex < 0 || targetCardIndex >= player.getDeck().size()) return;

        CardInstance selected = player.getDeck().get(targetCardIndex);
        CardDefinition def = ctx.getCardLookup().getCardById(selected.getCardDefinitionId());
        if (!(def instanceof PokemonCardDefinition pkmDef)) return;
        if (!"BASIC".equalsIgnoreCase(pkmDef.getStage())) return;

        player.getDeck().remove(targetCardIndex.intValue());
        PokemonInPlay newPkm = new PokemonInPlay();
        newPkm.setInstanceId(UUID.randomUUID());
        newPkm.setCardDefinitionId(selected.getCardDefinitionId());
        newPkm.setOwnerPlayerId(player.getPlayerId());
        newPkm.setEnteredTurnNumber(ctx.getState().getTurnNumber());
        newPkm.setDamageCounters(0);
        newPkm.setSpecialConditions(new java.util.ArrayList<>());
        newPkm.setAttachedEnergies(new java.util.ArrayList<>());
        player.getBench().add(newPkm);

        ctx.getRandomizer().shuffle(player.getDeck());

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("pokemonInstanceId", newPkm.getInstanceId().toString());
        eventPayload.put("cardDefinitionId", selected.getCardDefinitionId());

        ctx.addEvent(new GameEvent(
                GameEventType.POKEMON_SEARCHED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Basic Pokemon searched and placed on bench.",
                eventPayload
        ));
    }

    @Override
    public EffectType getType() {
        return EffectType.SEARCH_BASIC_POKEMON;
    }
}
