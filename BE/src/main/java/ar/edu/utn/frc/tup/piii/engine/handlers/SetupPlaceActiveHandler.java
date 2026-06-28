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
import java.util.*;

public class SetupPlaceActiveHandler implements GameHandler {
    @Override
    public void handle(EngineContext ctx, GameAction action) {
        String cardInstanceIdStr = action.getPayloadString("cardInstanceId");
        if (cardInstanceIdStr == null) {
            ctx.setError(new GameError("INVALID_PAYLOAD", "cardInstanceId is required"));
            return;
        }
        UUID cardInstanceId = UUID.fromString(cardInstanceIdStr);

        var player = ctx.getPlayer(action.getPlayerId());
        if (player == null) {
            ctx.setError(new GameError("PLAYER_NOT_FOUND", "Player not found"));
            return;
        }

        if (player.getActivePokemon() != null) {
            ctx.setError(new GameError("ACTIVE_ALREADY_SET", "Active Pokémon already selected"));
            return;
        }

        Optional<CardInstance> cardOpt = player.getHand().stream()
                .filter(c -> c.getInstanceId().equals(cardInstanceId))
                .findFirst();
        if (cardOpt.isEmpty()) {
            ctx.setError(new GameError("CARD_NOT_IN_HAND", "Card not found in hand"));
            return;
        }
        CardInstance card = cardOpt.get();

        CardDefinition def = ctx.getCardLookup().getCardById(card.getCardDefinitionId());
        if (!(def instanceof PokemonCardDefinition pkmnDef) || !("BASIC".equalsIgnoreCase(pkmnDef.getStage()) || pkmnDef.getStage() == null)) {
            ctx.setError(new GameError("NOT_BASIC_POKEMON", "Only Basic Pokémon can be placed as active during setup"));
            return;
        }

        player.getHand().remove(card);

        PokemonInPlay active = new PokemonInPlay();
        active.setInstanceId(card.getInstanceId());
        active.setCardDefinitionId(card.getCardDefinitionId());
        active.setOwnerPlayerId(player.getPlayerId());
        active.setEnteredTurnNumber(0);
        active.setEvolvedThisTurn(false);
        active.setDamageCounters(0);
        active.setSpecialConditions(new ArrayList<>());
        active.setAttachedEnergies(new ArrayList<>());
        player.setActivePokemon(active);
        player.setSetupConfirmed(false);

        ctx.addEvent(new GameEvent(
                GameEventType.SETUP_ACTIVE_PLACED.name(),
                ctx.getState().getMatchId(),
                0,
                Instant.now(),
                "Player placed active Pokémon during setup",
                Map.of("playerId", player.getPlayerId().toString(),
                        "cardDefinitionId", card.getCardDefinitionId(),
                        "instanceId", card.getInstanceId().toString())
        ));
    }
}
