package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.action.GameError;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PutBasicOnBenchHandler implements GameHandler {
    public void handle(EngineContext ctx, GameAction action) {
        Integer handIndex = action.getPayloadInt("handIndex");
        if (handIndex == null) {
            ctx.setError(new GameError("INVALID_HAND_INDEX", "handIndex is required"));
            return;
        }
        var player = ctx.getPlayer(action.getPlayerId());

        if (handIndex < 0 || handIndex >= player.getHand().size()) {
            ctx.setError(new GameError("INVALID_HAND_INDEX", "handIndex out of bounds"));
            return;
        }
        CardInstance card = player.getHand().get(handIndex);

        CardDefinition def = ctx.getCardLookup().getCardById(card.getCardDefinitionId());
        if (!(def instanceof PokemonCardDefinition pokemonCardDef)) {
            ctx.setError(new GameError("NOT_A_POKEMON", "Card is not a Pokemon"));
            return;
        }

        if (!"BASIC".equals(pokemonCardDef.getStage())) {
            ctx.setError(new GameError("NOT_BASIC_POKEMON", "Only Basic Pokemon can be placed on bench"));
            return;
        }

        if (player.getBench().size() >= 5) {
            ctx.setError(new GameError("BENCH_FULL", "Bench is full"));
            return;
        }

        player.getHand().remove(handIndex.intValue());

        PokemonInPlay pkm = new PokemonInPlay();
        pkm.setInstanceId(card.getInstanceId());
        pkm.setCardDefinitionId(card.getCardDefinitionId());
        pkm.setOwnerPlayerId(player.getPlayerId());
        pkm.setDamageCounters(0);
        pkm.setSpecialConditions(new ArrayList<>());
        pkm.setAttachedEnergies(new ArrayList<>());
        pkm.setEvolvedThisTurn(false);
        pkm.setEnteredTurnNumber(ctx.getState().getTurnNumber());
        player.getBench().add(pkm);

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("cardDefinitionId", card.getCardDefinitionId());
        eventPayload.put("handIndex", handIndex);
        eventPayload.put("instanceId", card.getInstanceId().toString());
        ctx.addEvent(new GameEvent(GameEventType.POKEMON_PLACED_ON_BENCH.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Placed on bench",
                eventPayload
        ));
    }
}
