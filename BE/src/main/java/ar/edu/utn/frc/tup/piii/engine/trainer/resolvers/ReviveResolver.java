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

public class ReviveResolver implements TrainerEffectResolver {

    @Override
    public void resolve(EngineContext ctx, PlayerState player, TrainerCardDefinition card, Map<String, Object> payload) {
        if (player.getBench().size() >= 5) return;

        Integer targetCardIndex = (Integer) payload.get("targetCardIndex");
        if (targetCardIndex == null || targetCardIndex < 0 || targetCardIndex >= player.getDiscard().size()) return;

        // targetCardIndex uses backend natural discard order:
        // index 0 = bottom/oldest card
        // index size-1 = top/most recent card.
        // Any frontend discard viewer using LIFO visual order must convert indices accordingly.
        CardInstance selected = player.getDiscard().get(targetCardIndex);
        CardDefinition def = ctx.getCardLookup().getCardById(selected.getCardDefinitionId());
        if (!(def instanceof PokemonCardDefinition pkmDef)) return;
        if (!"BASIC".equalsIgnoreCase(pkmDef.getStage())) return;

        player.removeFromDiscard(selected.getInstanceId());
        PokemonInPlay newPkm = new PokemonInPlay();
        newPkm.setInstanceId(UUID.randomUUID());
        newPkm.setCardDefinitionId(selected.getCardDefinitionId());
        newPkm.setOwnerPlayerId(player.getPlayerId());
        newPkm.setEnteredTurnNumber(ctx.getState().getTurnNumber());
        newPkm.setDamageCounters(0);
        newPkm.setSpecialConditions(new ArrayList<>());
        newPkm.setAttachedEnergies(new ArrayList<>());
        player.getBench().add(newPkm);

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("pokemonInstanceId", newPkm.getInstanceId().toString());
        eventPayload.put("cardDefinitionId", selected.getCardDefinitionId());

        ctx.addEvent(new GameEvent(
                GameEventType.POKEMON_SEARCHED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Pokemon revived from discard pile.",
                eventPayload
        ));
    }

    @Override
    public EffectType getType() {
        return EffectType.REVIVE;
    }
}
