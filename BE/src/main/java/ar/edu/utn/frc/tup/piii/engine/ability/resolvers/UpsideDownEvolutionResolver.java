package ar.edu.utn.frc.tup.piii.engine.ability.resolvers;

import ar.edu.utn.frc.tup.piii.domain.cards.AbilityDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.ability.hooks.SweetVeilHook;
import ar.edu.utn.frc.tup.piii.engine.ability.AbilityResolver;
import ar.edu.utn.frc.tup.piii.engine.action.GameError;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.SpecialCondition;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UpsideDownEvolutionResolver implements AbilityResolver {

    @Override
    public void resolve(EngineContext ctx, PlayerState player, PokemonInPlay pokemon,
                        AbilityDefinition ability, Map<String, Object> payload) {
        if (!pokemon.getSpecialConditions().contains(SpecialCondition.CONFUSED)) {
            ctx.setError(new GameError("POKEMON_CANNOT_USE_ABILITY", "Inkay must be CONFUSED to use Upside-Down Evolution."));
            return;
        }

        GameState state = ctx.getState();
        CardInstance evolutionCard = null;
        int deckIndex = -1;

        for (int i = 0; i < player.getDeck().size(); i++) {
            CardInstance c = player.getDeck().get(i);
            CardDefinition def = ctx.getCardLookup().getCardById(c.getCardDefinitionId());
            if (def instanceof PokemonCardDefinition pkmDef
                    && "Inkay".equalsIgnoreCase(pkmDef.getEvolvesFrom())) {
                evolutionCard = c;
                deckIndex = i;
                break;
            }
        }

        if (evolutionCard == null) {
            ctx.setError(new GameError("MISSING_TARGET", "No evolution for Inkay found in deck."));
            return;
        }

        PokemonCardDefinition evolutionDef = (PokemonCardDefinition)
                ctx.getCardLookup().getCardById(evolutionCard.getCardDefinitionId());

        player.getDeck().remove(deckIndex);

        PokemonInPlay evolved = new PokemonInPlay();
        evolved.setInstanceId(evolutionCard.getInstanceId());
        evolved.setCardDefinitionId(evolutionCard.getCardDefinitionId());
        evolved.setOwnerPlayerId(player.getPlayerId());
        evolved.setDamageCounters(pokemon.getDamageCounters());
        evolved.setAttachedEnergies(new ArrayList<>(pokemon.getAttachedEnergies()));
        evolved.setEvolvedThisTurn(true);
        evolved.setEnteredTurnNumber(state.getTurnNumber());
        evolved.setSpecialConditions(new ArrayList<>());

        if (player.getActivePokemon() != null
                && player.getActivePokemon().getInstanceId().equals(pokemon.getInstanceId())) {
            player.setActivePokemon(evolved);
        } else {
            int index = player.getBench().indexOf(pokemon);
            if (index != -1) {
                player.getBench().set(index, evolved);
            }
        }

        SweetVeilHook.syncImmunity(player, ctx.getCardLookup());

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("pokemonInstanceId", pokemon.getInstanceId().toString());
        eventPayload.put("toCardDefinitionId", evolutionCard.getCardDefinitionId());
        eventPayload.put("playerId", player.getPlayerId().toString());

        ctx.addEvent(new GameEvent(
                GameEventType.POKEMON_EVOLVED.name(),
                state.getMatchId(),
                state.getTurnNumber(),
                Instant.now(),
                "Upside-Down Evolution: Inkay evolved into " + evolutionDef.getName() + ".",
                eventPayload
        ));
    }
}
