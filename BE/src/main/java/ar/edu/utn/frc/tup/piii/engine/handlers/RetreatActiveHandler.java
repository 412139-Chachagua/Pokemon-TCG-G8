package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyType;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.attack.StatusEffectManager;
import ar.edu.utn.frc.tup.piii.engine.energy.EnergyPaymentResult;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import java.time.Instant;
import java.util.*;

public class RetreatActiveHandler implements GameHandler {
    public void handle(EngineContext ctx, GameAction action) {
        Map<String, Object> payload = action.getPayload();
        GameState state = ctx.getState();
        var player = ctx.getPlayer(action.getPlayerId());

        if (state.getTurnFlags().hasRetreated()) return;

        PokemonInPlay active = player.getActivePokemon();

        if (active == null) return;

        if (active.isCannotRetreatNextTurn()) return;

        Integer benchIndex = action.getPayloadInt("benchIndex");
        if (benchIndex == null) return;
        if (benchIndex < 0 || benchIndex >= player.getBench().size()) return;
        PokemonInPlay selected = player.getBench().get(benchIndex);

        PokemonCardDefinition activeDef = (PokemonCardDefinition) ctx.getCardLookup().getCardById(active.getCardDefinitionId());
        var retreatCost = activeDef.getRetreatCost();

        if (state.getStadiumCardDefinitionId() != null) {
            CardDefinition stadiumDef = ctx.getCardLookup().getCardById(
                state.getStadiumCardDefinitionId());
            if (stadiumDef instanceof TrainerCardDefinition trainerDef
                && "FAIRY_GARDEN".equals(trainerDef.getEffectCode())) {
                if (hasFairyEnergy(active, ctx.getCardLookup())) {
                    retreatCost = List.of();
                }
            }
        }

        int requiredEnergies = retreatCost != null ? retreatCost.size() : 0;
        var attached = active.getAttachedEnergies();

        // Determine which energy instance IDs to discard
        final List<UUID> toDiscard;
        if (requiredEnergies > 0) {
            @SuppressWarnings("unchecked")
            List<String> rawDiscard = (List<String>) payload.get("energyCardInstanceIdsToDiscard");
            if (rawDiscard != null && !rawDiscard.isEmpty()) {
                toDiscard = rawDiscard.stream().map(UUID::fromString).toList();
            } else if (attached != null) {
                // Auto-select first N attached energies
                toDiscard = attached.stream()
                        .limit(requiredEnergies)
                        .map(CardInstance::getInstanceId)
                        .toList();
            } else {
                toDiscard = List.of();
            }
            EnergyPaymentResult validation = ctx.getEnergyService()
        .validateAndPayRetreat(active, toDiscard, ctx.getCardLookup());
    if (!validation.canPay()) return;
        } else {
            toDiscard = List.of();
        }

        if (attached != null) {
            List<CardInstance> toDiscardInstances = attached.stream()
                    .filter(ci -> toDiscard.contains(ci.getInstanceId()))
                    .toList();
            ctx.getEnergyService().detachEnergies(active, player, toDiscardInstances, ctx);
            for (CardInstance e : toDiscardInstances) {
                ctx.addEvent(new GameEvent(
                        GameEventType.ENERGY_DISCARDED.name(),
                        state.getMatchId(),
                        state.getTurnNumber(),
                        Instant.now(),
                        "Energy discarded for retreat cost.",
                        Map.of(
                                "pokemonInstanceId", active.getInstanceId().toString(),
                                "energyInstanceId", e.getInstanceId().toString(),
                                "reason", "RETREAT_COST"
                        )
                ));
            }
        }

        StatusEffectManager.clearConditionsOnEvolveOrRetreat(active);

        player.getBench().remove(benchIndex.intValue());
        active.setEnteredTurnNumber(state.getTurnNumber());
        player.getBench().add(active);
        player.setActivePokemon(selected);
        state.getTurnFlags().setHasRetreated(true);

        active.setSpecialConditions(new ArrayList<>());

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("effectCode", "RETREAT");
        eventPayload.put("newActivePokemonInstanceId", selected.getInstanceId().toString());
        eventPayload.put("oldActivePokemonInstanceId", active.getInstanceId().toString());

        ctx.addEvent(new GameEvent(
                GameEventType.RETREAT_EXECUTED.name(),
                state.getMatchId(),
                state.getTurnNumber(),
                Instant.now(),
                "Retreat executed.",
                eventPayload
        ));
    }

    private boolean hasFairyEnergy(PokemonInPlay pokemon, CardLookupPort cardLookup) {
        if (pokemon.getAttachedEnergies() == null) return false;
        for (CardInstance ci : pokemon.getAttachedEnergies()) {
            CardDefinition def = cardLookup.getCardById(ci.getCardDefinitionId());
            if (def instanceof EnergyCardDefinition energyDef
                && energyDef.getProvides() != null
                && energyDef.getProvides().contains(EnergyType.FAIRY)) {
                return true;
            }
        }
        return false;
    }
}
