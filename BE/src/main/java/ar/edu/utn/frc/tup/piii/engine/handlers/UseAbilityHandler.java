package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.domain.cards.AbilityDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.ability.AbilityRegistry;
import ar.edu.utn.frc.tup.piii.engine.ability.AbilityResolver;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.action.GameError;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.SpecialCondition;

import java.time.Instant;
import java.util.*;

public class UseAbilityHandler implements GameHandler {

    private final AbilityRegistry abilityRegistry;

    public UseAbilityHandler(AbilityRegistry abilityRegistry) {
        this.abilityRegistry = abilityRegistry;
    }

    @Override
    public void handle(EngineContext ctx, GameAction action) {
        GameState state = ctx.getState();
        PlayerState player = ctx.getPlayer(action.getPlayerId());

        String pokemonInstanceIdStr = action.getPayloadString("pokemonInstanceId");
        String abilityName = action.getPayloadString("abilityName");
        if (pokemonInstanceIdStr == null || abilityName == null) return;

        UUID pokemonInstanceId = UUID.fromString(pokemonInstanceIdStr);
        PokemonInPlay pokemon = HandlerHelper.findPokemon(player, pokemonInstanceId);
        if (pokemon == null) return;

        CardDefinition cardDef = ctx.getCardLookup().getCardById(pokemon.getCardDefinitionId());
        if (!(cardDef instanceof PokemonCardDefinition pokemonDef)) return;

        AbilityDefinition ability = pokemonDef.getAbilities().stream()
                .filter(a -> a.getName().equals(abilityName))
                .findFirst()
                .orElse(null);
        if (ability == null) {
            ctx.setError(new GameError("ABILITY_NOT_FOUND", "Ability not found: " + abilityName));
            return;
        }

        if (pokemon.isAbilitiesSuppressedNextTurn()) {
            ctx.setError(new GameError("ABILITIES_SUPPRESSED", "Abilities are suppressed for this Pokemon this turn."));
            return;
        }

        if (pokemon.getSpecialConditions() != null
                && (pokemon.getSpecialConditions().contains(SpecialCondition.ASLEEP)
                || pokemon.getSpecialConditions().contains(SpecialCondition.PARALYZED))) {
            ctx.setError(new GameError("POKEMON_CANNOT_USE_ABILITY", "Pokemon cannot use ability due to special condition."));
            return;
        }

        if (pokemon.getAbilitiesUsedThisTurn().contains(abilityName)) {
            ctx.setError(new GameError("ABILITY_ALREADY_USED", "Ability already used this turn: " + abilityName));
            return;
        }

        AbilityResolver resolver = abilityRegistry.get(abilityName);
        if (resolver == null) {
            ctx.setError(new GameError("ABILITY_NOT_FOUND", "No resolver registered for ability: " + abilityName));
            return;
        }

        Map<String, Object> payload = action.getPayload() != null ? new HashMap<>(action.getPayload()) : new HashMap<>();
        resolver.resolve(ctx, player, pokemon, ability, payload);

        if (ctx.getError() == null) {
            pokemon.getAbilitiesUsedThisTurn().add(abilityName);

            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("pokemonInstanceId", pokemonInstanceIdStr);
            eventPayload.put("abilityName", abilityName);
            eventPayload.put("playerId", player.getPlayerId().toString());

            ctx.addEvent(new GameEvent(
                    GameEventType.ABILITY_USED.name(),
                    state.getMatchId(),
                    state.getTurnNumber(),
                    Instant.now(),
                    pokemon.getCardDefinitionId() + " used " + abilityName + ".",
                    eventPayload
            ));
        } else {
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("pokemonInstanceId", pokemonInstanceIdStr);
            eventPayload.put("abilityName", abilityName);
            eventPayload.put("error", ctx.getError().getMessage());
            eventPayload.put("playerId", player.getPlayerId().toString());

            ctx.addEvent(new GameEvent(
                    GameEventType.ABILITY_BLOCKED.name(),
                    state.getMatchId(),
                    state.getTurnNumber(),
                    Instant.now(),
                    abilityName + " was blocked: " + ctx.getError().getMessage(),
                    eventPayload
            ));
        }
    }
}
