package ar.edu.utn.frc.tup.piii.engine.trainer.resolvers;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardType;
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

public class DiscardOpponentEnergyResolver implements TrainerEffectResolver {

    @Override
    public void resolve(EngineContext ctx, PlayerState player, TrainerCardDefinition card, Map<String, Object> payload) {
        UUID opponentPlayerId = ctx.getOpponent(player.getPlayerId()).getPlayerId();
        PlayerState opponent = ctx.getPlayer(opponentPlayerId);
        if (opponent == null) return;

        UUID targetInstanceId = UUID.fromString((String) payload.get("targetPokemonInstanceId"));
        int energyIndex = payload.containsKey("energyIndex") ? ((Number) payload.get("energyIndex")).intValue() : 0;

        PokemonInPlay target = findPokemonInPlay(opponent, targetInstanceId);
        if (target == null) return;
        if (target.getAttachedEnergies() == null) return;
        if (energyIndex < 0 || energyIndex >= target.getAttachedEnergies().size()) return;

        CardInstance energyCard = target.getAttachedEnergies().get(energyIndex);
        CardDefinition energyDef = ctx.getCardLookup().getCardById(energyCard.getCardDefinitionId());
        if (!(energyDef instanceof EnergyCardDefinition energyCardDef)
                || energyCardDef.getEnergyCardType() != EnergyCardType.BASIC) return;

        target.getAttachedEnergies().remove(energyIndex);
        opponent.getDiscard().add(energyCard);

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("pokemonInstanceId", targetInstanceId.toString());
        eventPayload.put("energyIndex", energyIndex);
        eventPayload.put("energyCardId", energyCard.getCardDefinitionId());

        ctx.addEvent(new GameEvent(
                GameEventType.OPPONENT_ENERGY_DISCARDED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Discarded opponent's energy",
                eventPayload
        ));
    }

    @Override
    public EffectType getType() {
        return EffectType.DISCARD_OPPONENT_ENERGY;
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
