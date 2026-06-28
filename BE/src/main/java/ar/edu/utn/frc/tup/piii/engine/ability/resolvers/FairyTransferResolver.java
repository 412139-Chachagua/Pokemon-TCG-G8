package ar.edu.utn.frc.tup.piii.engine.ability.resolvers;

import ar.edu.utn.frc.tup.piii.domain.cards.AbilityDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyType;
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
import java.util.UUID;

public class FairyTransferResolver implements AbilityResolver {

    @Override
    public void resolve(EngineContext ctx, PlayerState player, PokemonInPlay pokemon,
                        AbilityDefinition ability, Map<String, Object> payload) {
        String sourceEnergyIdStr = (String) payload.get("sourceEnergyInstanceId");
        String targetPokemonIdStr = (String) payload.get("targetPokemonInstanceId");
        if (sourceEnergyIdStr == null || targetPokemonIdStr == null) {
            ctx.setError(new GameError("MISSING_TARGET", "Missing sourceEnergyInstanceId or targetPokemonInstanceId."));
            return;
        }

        UUID sourceEnergyId = UUID.fromString(sourceEnergyIdStr);
        UUID targetPokemonId = UUID.fromString(targetPokemonIdStr);

        PokemonInPlay target = findPlayerPokemon(player, targetPokemonId);
        if (target == null) {
            ctx.setError(new GameError("INVALID_TARGET", "Target Pokemon not found."));
            return;
        }

        CardInstance energyCard = null;
        int energyIndex = -1;
        PokemonInPlay sourcePokemon = null;
        for (PokemonInPlay pkm : getAllPlayerPokemon(player)) {
            for (int i = 0; i < pkm.getAttachedEnergies().size(); i++) {
                if (pkm.getAttachedEnergies().get(i).getInstanceId().equals(sourceEnergyId)) {
                    energyCard = pkm.getAttachedEnergies().get(i);
                    energyIndex = i;
                    sourcePokemon = pkm;
                    break;
                }
            }
            if (energyCard != null) break;
        }

        if (energyCard == null) {
            ctx.setError(new GameError("CARD_NOT_IN_HAND", "Energy card not found attached to your Pokemon."));
            return;
        }

        CardDefinition def = ctx.getCardLookup().getCardById(energyCard.getCardDefinitionId());
        if (!(def instanceof EnergyCardDefinition energyDef)) {
            ctx.setError(new GameError("INVALID_TARGET", "Card is not an energy card."));
            return;
        }
        if (!energyDef.getProvides().contains(EnergyType.FAIRY)) {
            ctx.setError(new GameError("INVALID_TARGET", "Card is not a Fairy Energy."));
            return;
        }

        ctx.getEnergyService().transferEnergy(energyCard, sourcePokemon, target, player, ctx);

        GameState state = ctx.getState();
        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("pokemonInstanceId", pokemon.getInstanceId().toString());
        eventPayload.put("sourcePokemonInstanceId", sourcePokemon.getInstanceId().toString());
        eventPayload.put("targetPokemonInstanceId", targetPokemonIdStr);
        eventPayload.put("energyCardInstanceId", sourceEnergyIdStr);
        eventPayload.put("playerId", player.getPlayerId().toString());

        ctx.addEvent(new GameEvent(
                GameEventType.ENERGY_ATTACHED.name(),
                state.getMatchId(),
                state.getTurnNumber(),
                Instant.now(),
                "Fairy Transfer: moved Fairy Energy.",
                eventPayload
        ));
    }

    private java.util.List<PokemonInPlay> getAllPlayerPokemon(PlayerState player) {
        java.util.List<PokemonInPlay> all = new java.util.ArrayList<>();
        if (player.getActivePokemon() != null) all.add(player.getActivePokemon());
        all.addAll(player.getBench());
        return all;
    }

    private PokemonInPlay findPlayerPokemon(PlayerState player, UUID instanceId) {
        if (player.getActivePokemon() != null
                && player.getActivePokemon().getInstanceId().equals(instanceId)) {
            return player.getActivePokemon();
        }
        for (PokemonInPlay pkm : player.getBench()) {
            if (pkm.getInstanceId().equals(instanceId)) return pkm;
        }
        return null;
    }
}
