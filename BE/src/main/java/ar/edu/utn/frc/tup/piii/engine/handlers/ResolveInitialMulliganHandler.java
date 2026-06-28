package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.action.GameError;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ResolveInitialMulliganHandler implements GameHandler {

    @Override
    public void handle(EngineContext ctx, GameAction action) {
        GameState state = ctx.getState();
        PlayerState player = ctx.getPlayer(action.getPlayerId());

        if (player == null) {
            ctx.setError(new GameError("PLAYER_NOT_FOUND", "Player not found"));
            return;
        }

        if (!state.hasPendingInitialMulligan(action.getPlayerId())) {
            ctx.setError(new GameError("NO_PENDING_MULLIGAN", "No tienes un mulligan pendiente"));
            return;
        }

        if (player.isInitialMulliganResolved()) {
            ctx.setError(new GameError("ALREADY_RESOLVED", "Ya resolviste tu mulligan inicial"));
            return;
        }

        Object rawDecision = action.getPayload().get("decision");
        String decision = rawDecision instanceof String ? (String) rawDecision : "";

        if ("MULLIGAN".equals(decision)) {
            handleMulligan(ctx, player, state, action);
        } else if ("KEEP".equals(decision)) {
            handleKeep(ctx, player, state, action);
        } else {
            ctx.setError(new GameError("INVALID_DECISION", "Decisión inválida. Usa MULLIGAN o KEEP."));
        }
    }

    private void handleMulligan(EngineContext ctx, PlayerState player, GameState state, GameAction action) {
        List<CardInstance> deck = player.getDeck();
        List<CardInstance> hand = player.getHand();

        // Capture revealed card definition IDs before shuffling back
        List<String> revealedCardIds = hand.stream()
                .map(CardInstance::getCardDefinitionId)
                .toList();
        player.addMulliganReveal(revealedCardIds);

        // Publish real-time event so opponent sees the revealed cards immediately
        ctx.getEventPublisher().publishEvents(state.getMatchId(), List.of(
            new GameEvent(
                GameEventType.MULLIGAN_REVEALED.name(),
                state.getMatchId(), 0, Instant.now(),
                "Mano revelada por mulligan",
                Map.of(
                    "playerId", player.getPlayerId().toString(),
                    "revealedCardIds", revealedCardIds
                )
            )
        ));

        deck.addAll(hand);
        hand.clear();

        ctx.getRandomizer().shuffle(deck);

        int handSize = state.getHandSize();
        int toDraw = Math.min(handSize, deck.size());
        for (int i = 0; i < toDraw; i++) {
            hand.add(deck.remove(0));
        }

        player.setMulliganCount(player.getMulliganCount() + 1);

        boolean hasBasic = hasBasicPokemon(ctx, player.getHand());
        if (hasBasic) {
            player.setInitialMulliganResolved(true);
            state.resolveInitialMulligan(player.getPlayerId());
        }

        ctx.addEvent(new GameEvent(
            GameEventType.INITIAL_MULLIGAN_RESOLVED.name(),
            state.getMatchId(), 0, Instant.now(),
            hasBasic
                ? "Jugador hizo mulligan y obtuvo un Pokemon Basic"
                : "Jugador hizo mulligan pero aún no tiene Pokemon Basic",
            Map.of(
                "playerId", player.getPlayerId().toString(),
                "action", "MULLIGAN",
                "mulliganCount", player.getMulliganCount(),
                "hasBasic", hasBasic
            )
        ));

        if (!state.hasPendingInitialMulligan()) {
            finalizeInitialMulligan(ctx, state);
            ConfirmSetupHandler.tryTransitionToActive(ctx);
        }
    }

    private void handleKeep(EngineContext ctx, PlayerState player, GameState state, GameAction action) {
        player.setInitialMulliganResolved(true);
        state.resolveInitialMulligan(player.getPlayerId());

        ctx.addEvent(new GameEvent(
            GameEventType.INITIAL_MULLIGAN_RESOLVED.name(),
            state.getMatchId(), 0, Instant.now(),
            "Jugador decidió quedarse su mano sin Pokemon Basic",
            Map.of(
                "playerId", player.getPlayerId().toString(),
                "action", "KEEP",
                "mulliganCount", player.getMulliganCount()
            )
        ));

        if (!state.hasPendingInitialMulligan()) {
            finalizeInitialMulligan(ctx, state);
            ConfirmSetupHandler.tryTransitionToActive(ctx);
        }
    }

    private void finalizeInitialMulligan(EngineContext ctx, GameState state) {
        int prizeCount = state.getPrizeCountPerPlayer();
        for (PlayerState p : state.getPlayers()) {
            if (p.getPrizes() == null || p.getPrizes().isEmpty()) {
                List<CardInstance> deck = p.getDeck();
                List<CardInstance> prizes = new ArrayList<>(deck.subList(0, prizeCount));
                deck.subList(0, prizeCount).clear();
                p.setPrizes(prizes);
            }
        }

        Map<UUID, Integer> draws = new HashMap<>();
        PlayerState[] players = state.getPlayers();
        if (players.length >= 2) {
            if (players[1].getMulliganCount() > 0) draws.put(players[0].getPlayerId(), players[1].getMulliganCount());
            if (players[0].getMulliganCount() > 0) draws.put(players[1].getPlayerId(), players[0].getMulliganCount());
        }

        if (!draws.isEmpty()) {
            state.setMulliganDrawPending(true);
            state.setMulliganDrawCounts(draws);
            state.setMulliganDrawResolved(new HashSet<>());
            state.setMulliganDrawDeadline(Instant.now().plusSeconds(30));

            for (var entry : draws.entrySet()) {
                ctx.addEvent(new GameEvent(
                    GameEventType.MULLIGAN_DRAW_OPPORTUNITY.name(),
                    state.getMatchId(), 0, Instant.now(),
                    "Puedes robar " + entry.getValue() + " carta(s) por mulligan del oponente",
                    Map.of("playerId", entry.getKey().toString(), "count", entry.getValue())
                ));
            }
        }
    }

    private boolean hasBasicPokemon(EngineContext ctx, List<CardInstance> hand) {
        for (CardInstance card : hand) {
            CardDefinition def = ctx.getCardLookup().getCardById(card.getCardDefinitionId());
            if (def instanceof PokemonCardDefinition pkmn && (pkmn.getStage() == null || "BASIC".equalsIgnoreCase(pkmn.getStage()))) {
                return true;
            }
        }
        return false;
    }
}
