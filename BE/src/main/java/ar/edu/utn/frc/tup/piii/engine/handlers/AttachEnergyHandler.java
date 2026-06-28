package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.ability.hooks.SweetVeilHook;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.action.GameError;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class AttachEnergyHandler implements GameHandler {
    private static final Logger log = LoggerFactory.getLogger(AttachEnergyHandler.class);

    public void handle(EngineContext ctx, GameAction action) {
        Map<String, Object> payload = action.getPayload();
        Integer handIndex = action.getPayloadInt("handIndex");
        if (handIndex == null) {
            ctx.setError(new GameError("INVALID_HAND_INDEX", "handIndex is required"));
            return;
        }
        String targetIdStr = action.getPayloadString("targetPokemonInstanceId");
        if (targetIdStr == null) {
            ctx.setError(new GameError("INVALID_TARGET", "targetPokemonInstanceId is required"));
            return;
        }
        UUID targetPokemonInstanceId = UUID.fromString(targetIdStr);
        var player = ctx.getPlayer(action.getPlayerId());
        log.warn("[DEBUG] AttachEnergyHandler: player={} handIndex={} targetId={}", action.getPlayerId(), handIndex, targetPokemonInstanceId);

        if (ctx.getState().getTurnFlags().hasAttachedEnergy()) {
            ctx.setError(new GameError("ENERGY_ALREADY_ATTACHED", "Ya adjuntaste una energía este turno"));
            return;
        }

        if (handIndex < 0 || handIndex >= player.getHand().size()) {
            ctx.setError(new GameError("INVALID_HAND_INDEX", "handIndex out of bounds"));
            return;
        }
        CardInstance card = player.getHand().get(handIndex);
        log.warn("[DEBUG] AttachEnergyHandler: cardDefinitionId={} handSize={}", card.getCardDefinitionId(), player.getHand().size());

        CardDefinition def = ctx.getCardLookup().getCardById(card.getCardDefinitionId());
        if (!(def instanceof EnergyCardDefinition ed)) {
            ctx.setError(new GameError("NOT_AN_ENERGY", "Card is not an energy card"));
            return;
        }
        log.warn("[DEBUG] AttachEnergyHandler: energyType={} strategyKey={} provides={}", ed.getEnergyCardType(), ed.getStrategyKey(), ed.getProvides());

        PokemonInPlay target = HandlerHelper.findPokemon(player, targetPokemonInstanceId);
        if (target == null) {
            ctx.setError(new GameError("INVALID_TARGET", "Target Pokemon not found"));
            return;
        }
        log.warn("[DEBUG] AttachEnergyHandler: found target pokemon={} cardDefId={}", target.getInstanceId(), target.getCardDefinitionId());

        ctx.getEnergyService().attachFromHand(card, target, player, ctx);
        int afterCount = target.getAttachedEnergies() != null ? target.getAttachedEnergies().size() : 0;
        log.warn("[DEBUG] AttachEnergyHandler: AFTER attachFromHand, attachedEnergies count={}", afterCount);

        SweetVeilHook.syncImmunity(player, ctx.getCardLookup());

        ctx.getState().getTurnFlags().setHasAttachedEnergy(true);

        int newTotal = target.getAttachedEnergies() != null ? target.getAttachedEnergies().size() : 0;
        ctx.addEvent(new GameEvent(
                GameEventType.ENERGY_ATTACHED.name(),
                ctx.getState().getMatchId(),
                ctx.getState().getTurnNumber(),
                Instant.now(),
                "Energy attached",
                Map.of(
                        "pokemonInstanceId", target.getInstanceId().toString(),
                        "energyCardId", card.getCardDefinitionId(),
                        "handIndex", handIndex,
                        "newTotalCount", newTotal
                )
        ));
    }
}
