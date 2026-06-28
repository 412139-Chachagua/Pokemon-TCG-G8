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

public class WaterShurikenResolver implements AbilityResolver {

    private static final int DAMAGE_COUNTERS = 3;

    @Override
    public void resolve(EngineContext ctx, PlayerState player, PokemonInPlay pokemon,
                        AbilityDefinition ability, Map<String, Object> payload) {
        String energyInstanceIdStr = (String) payload.get("energyCardInstanceId");
        String targetInstanceIdStr = (String) payload.get("targetPokemonInstanceId");
        if (energyInstanceIdStr == null || targetInstanceIdStr == null) {
            ctx.setError(new GameError("MISSING_TARGET", "Missing energyCardInstanceId or targetPokemonInstanceId."));
            return;
        }

        UUID energyInstanceId = UUID.fromString(energyInstanceIdStr);
        UUID targetInstanceId = UUID.fromString(targetInstanceIdStr);

        CardInstance energyCard = null;
        int energyIndex = -1;
        for (int i = 0; i < player.getHand().size(); i++) {
            CardInstance c = player.getHand().get(i);
            if (c.getInstanceId().equals(energyInstanceId)) {
                energyCard = c;
                energyIndex = i;
                break;
            }
        }
        if (energyCard == null) {
            ctx.setError(new GameError("CARD_NOT_IN_HAND", "Energy card not found in hand."));
            return;
        }

        CardDefinition def = ctx.getCardLookup().getCardById(energyCard.getCardDefinitionId());
        if (!(def instanceof EnergyCardDefinition energyDef)) {
            ctx.setError(new GameError("INVALID_TARGET", "Card is not an energy card."));
            return;
        }
        if (!energyDef.getProvides().contains(EnergyType.WATER)) {
            ctx.setError(new GameError("INVALID_TARGET", "Card is not a Water Energy."));
            return;
        }

        GameState state = ctx.getState();
        PlayerState opponent = ctx.getOpponent(player.getPlayerId());
        PokemonInPlay target = findOpponentPokemon(opponent, targetInstanceId);
        if (target == null) {
            ctx.setError(new GameError("INVALID_TARGET", "Target Pokemon not found on opponent's side."));
            return;
        }

        player.getHand().remove(energyIndex);
        player.pushToDiscard(energyCard);

        target.setDamageCounters(target.getDamageCounters() + DAMAGE_COUNTERS);

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("pokemonInstanceId", pokemon.getInstanceId().toString());
        eventPayload.put("targetPokemonInstanceId", targetInstanceIdStr);
        eventPayload.put("energyCardInstanceId", energyInstanceIdStr);
        eventPayload.put("damageCounters", DAMAGE_COUNTERS);
        eventPayload.put("playerId", player.getPlayerId().toString());

        ctx.addEvent(new GameEvent(
                GameEventType.DAMAGE_APPLIED.name(),
                state.getMatchId(),
                state.getTurnNumber(),
                Instant.now(),
                "Water Shuriken: placed " + DAMAGE_COUNTERS + " damage counters.",
                eventPayload
        ));
    }

    private PokemonInPlay findOpponentPokemon(PlayerState opponent, UUID instanceId) {
        if (opponent.getActivePokemon() != null
                && opponent.getActivePokemon().getInstanceId().equals(instanceId)) {
            return opponent.getActivePokemon();
        }
        for (PokemonInPlay pkm : opponent.getBench()) {
            if (pkm.getInstanceId().equals(instanceId)) {
                return pkm;
            }
        }
        return null;
    }
}
