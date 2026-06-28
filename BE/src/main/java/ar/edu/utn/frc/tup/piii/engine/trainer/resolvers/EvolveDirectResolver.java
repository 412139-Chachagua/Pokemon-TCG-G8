package ar.edu.utn.frc.tup.piii.engine.trainer.resolvers;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.ability.hooks.SweetVeilHook;
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

public class EvolveDirectResolver implements TrainerEffectResolver {

    @Override
    public void resolve(EngineContext ctx, PlayerState player, TrainerCardDefinition card, Map<String, Object> payload) {
        UUID targetInstanceId = UUID.fromString((String) payload.get("targetPokemonInstanceId"));

        PokemonInPlay target = findPokemonInPlay(player, targetInstanceId);
        if (target == null) return;

        PokemonCardDefinition currentDef = (PokemonCardDefinition) ctx.getCardLookup().getCardById(target.getCardDefinitionId());
        if (currentDef == null) return;

        int targetCardIndex;
        CardInstance deckCard;

        if (payload.containsKey("targetCardIndex")) {
            targetCardIndex = ((Number) payload.get("targetCardIndex")).intValue();
            if (targetCardIndex < 0 || targetCardIndex >= player.getDeck().size()) return;
            deckCard = player.getDeck().get(targetCardIndex);
        } else {
            targetCardIndex = findEvolutionInDeck(player, ctx, currentDef);
            if (targetCardIndex < 0) {
                Map<String, Object> notFoundPayload = new HashMap<>();
                notFoundPayload.put("foundCount", 0);
                notFoundPayload.put("playerId", player.getPlayerId().toString());
                ctx.addEvent(new GameEvent(
                        GameEventType.POKEMON_SEARCHED.name(),
                        ctx.getState().getMatchId(),
                        ctx.getState().getTurnNumber(),
                        Instant.now(),
                        "No evolution found in deck",
                        notFoundPayload
                ));
                return;
            }
            deckCard = player.getDeck().get(targetCardIndex);
        }

        CardDefinition deckCardDef = ctx.getCardLookup().getCardById(deckCard.getCardDefinitionId());
        if (!(deckCardDef instanceof PokemonCardDefinition pokemonDef)) {
            Map<String, Object> notFoundPayload = new HashMap<>();
            notFoundPayload.put("foundCount", 0);
            notFoundPayload.put("playerId", player.getPlayerId().toString());
            ctx.addEvent(new GameEvent(
                    GameEventType.POKEMON_SEARCHED.name(),
                    ctx.getState().getMatchId(),
                    ctx.getState().getTurnNumber(),
                    Instant.now(),
                    "No valid evolution card in deck",
                    notFoundPayload
            ));
            return;
        }

        var state = ctx.getState();

        // No evolution on the player's first turn
        if (!state.hasPlayerCompletedFirstTurn(player.getPlayerId())) {
            return;
        }

        // No evolution on a Pokémon that entered play this turn
        if (target.getEnteredTurnNumber() == state.getTurnNumber()) {
            return;
        }

        // No evolution on a Pokémon that already evolved this turn
        if (target.isEvolvedThisTurn()) {
            return;
        }

        // Validate stage progression
        String targetStage = currentDef.getStage();
        String evolutionStage = pokemonDef.getStage();
        boolean validProgression =
                ("BASIC".equalsIgnoreCase(targetStage) && "STAGE_1".equalsIgnoreCase(evolutionStage)) ||
                        ("STAGE_1".equalsIgnoreCase(targetStage) && "STAGE_2".equalsIgnoreCase(evolutionStage));
        if (!validProgression) {
            return;
        }

        String evolvesFrom = pokemonDef.getEvolvesFrom();
        if (evolvesFrom == null || !evolvesFrom.equalsIgnoreCase(currentDef.getName())) {
            Map<String, Object> notFoundPayload = new HashMap<>();
            notFoundPayload.put("foundCount", 0);
            notFoundPayload.put("playerId", player.getPlayerId().toString());
            ctx.addEvent(new GameEvent(
                    GameEventType.POKEMON_SEARCHED.name(),
                    ctx.getState().getMatchId(),
                    ctx.getState().getTurnNumber(),
                    Instant.now(),
                    "Evolution doesn't match target Pokemon",
                    notFoundPayload
            ));
            return;
        }

        player.getDeck().remove(targetCardIndex);
        target.setCardDefinitionId(deckCard.getCardDefinitionId());
        target.setEvolvedThisTurn(true);
        target.setSpecialConditions(null);
        ctx.getRandomizer().shuffle(player.getDeck());

        SweetVeilHook.syncImmunity(player, ctx.getCardLookup());

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("pokemonInstanceId", targetInstanceId.toString());
        eventPayload.put("newCardDefinitionId", deckCard.getCardDefinitionId());

        ctx.addEvent(new GameEvent(
                GameEventType.POKEMON_EVOLVED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Pokemon evolved to " + pokemonDef.getName(),
                eventPayload
        ));
    }

    @Override
    public EffectType getType() {
        return EffectType.EVOLVE_DIRECT;
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

    private int findEvolutionInDeck(PlayerState player, EngineContext ctx, PokemonCardDefinition currentDef) {
        for (int i = 0; i < player.getDeck().size(); i++) {
            CardInstance candidate = player.getDeck().get(i);
            CardDefinition candidateDef = ctx.getCardLookup().getCardById(candidate.getCardDefinitionId());
            if (candidateDef instanceof PokemonCardDefinition pokemonDef) {
                String evolvesFrom = pokemonDef.getEvolvesFrom();
                if (evolvesFrom != null && evolvesFrom.equalsIgnoreCase(currentDef.getName())) {
                    return i;
                }
            }
        }
        return -1;
    }
}
