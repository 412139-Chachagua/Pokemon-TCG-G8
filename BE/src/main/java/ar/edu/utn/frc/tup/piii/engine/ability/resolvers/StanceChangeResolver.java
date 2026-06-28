package ar.edu.utn.frc.tup.piii.engine.ability.resolvers;

import ar.edu.utn.frc.tup.piii.domain.cards.AbilityDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.ability.AbilityResolver;
import ar.edu.utn.frc.tup.piii.engine.action.GameError;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class StanceChangeResolver implements AbilityResolver {

    @Override
    public void resolve(EngineContext ctx, PlayerState player, PokemonInPlay pokemon,
                        AbilityDefinition ability, Map<String, Object> payload) {
        GameState state = ctx.getState();
        CardInstance aegislashInHand = null;
        int handIndex = -1;

        for (int i = 0; i < player.getHand().size(); i++) {
            CardInstance c = player.getHand().get(i);
            CardDefinition def = ctx.getCardLookup().getCardById(c.getCardDefinitionId());
            if (def instanceof PokemonCardDefinition pkmDef && "Aegislash".equals(pkmDef.getName())) {
                aegislashInHand = c;
                handIndex = i;
                break;
            }
        }

        if (aegislashInHand == null) {
            ctx.setError(new GameError("MISSING_TARGET", "No Aegislash found in hand."));
            return;
        }

        player.getHand().remove(handIndex);
        player.pushToDiscard(new CardInstance(pokemon.getInstanceId(), pokemon.getCardDefinitionId()));

        PokemonInPlay newActive = new PokemonInPlay();
        newActive.setInstanceId(aegislashInHand.getInstanceId());
        newActive.setCardDefinitionId(aegislashInHand.getCardDefinitionId());
        newActive.setOwnerPlayerId(player.getPlayerId());
        newActive.setEnteredTurnNumber(state.getTurnNumber());
        newActive.setDamageCounters(0);
        newActive.setSpecialConditions(new java.util.ArrayList<>());
        newActive.setAttachedEnergies(new java.util.ArrayList<>());

        player.setActivePokemon(newActive);

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("pokemonInstanceId", pokemon.getInstanceId().toString());
        eventPayload.put("newActiveInstanceId", aegislashInHand.getInstanceId().toString());
        eventPayload.put("playerId", player.getPlayerId().toString());

        ctx.addEvent(new GameEvent(
                GameEventType.POKEMON_EVOLVED.name(),
                state.getMatchId(),
                state.getTurnNumber(),
                Instant.now(),
                "Stance Change: swapped to Aegislash.",
                eventPayload
        ));
    }
}
