package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.engine.action.ActionResult;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.action.GameActionType;
import ar.edu.utn.frc.tup.piii.engine.action.GameError;
import ar.edu.utn.frc.tup.piii.engine.ability.AbilityRegistry;
import ar.edu.utn.frc.tup.piii.engine.energy.EnergyService;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.handlers.*;
import ar.edu.utn.frc.tup.piii.engine.match.states.MatchState;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import ar.edu.utn.frc.tup.piii.engine.ports.DeckLoadPort;
import ar.edu.utn.frc.tup.piii.engine.ports.EventPublisherPort;
import ar.edu.utn.frc.tup.piii.engine.ports.RandomizerPort;
import ar.edu.utn.frc.tup.piii.engine.ports.StatePersisterPort;
import ar.edu.utn.frc.tup.piii.engine.rules.RuleValidator;
import ar.edu.utn.frc.tup.piii.engine.setup.SetupManager;
import ar.edu.utn.frc.tup.piii.engine.trainer.TrainerEffectRegistry;
import ar.edu.utn.frc.tup.piii.engine.turn.TurnManager;
import ar.edu.utn.frc.tup.piii.engine.turn.TurnPhase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;


public class GameEngine {
    private static final Logger log = LoggerFactory.getLogger(GameEngine.class);

    private final CardLookupPort cardLookup;
    private final RandomizerPort randomizer;
    private final StatePersisterPort persister;
    private final EventPublisherPort eventPublisher;
    private final RuleValidator ruleValidator;
    private final DeckLoadPort deckLoadPort;
    private final SetupManager setupManager;
    private final EnergyService energyService;
    private final Map<GameActionType, GameHandler> handlers;

    public GameEngine(CardLookupPort cardLookup, RandomizerPort randomizer,
                      StatePersisterPort persister, EventPublisherPort eventPublisher,
                      TurnManager turnManager, RuleValidator ruleValidator,
                      TrainerEffectRegistry effectRegistry,
                      AbilityRegistry abilityRegistry,
                      DeckLoadPort deckLoadPort, SetupManager setupManager,
                      EnergyService energyService) {
        this.cardLookup = cardLookup;
        this.randomizer = randomizer;
        this.persister = persister;
        this.eventPublisher = eventPublisher;
        this.ruleValidator = ruleValidator;
        this.deckLoadPort = deckLoadPort;
        this.setupManager = setupManager;
        this.energyService = energyService;
        this.handlers = buildDefaultHandlers(turnManager, effectRegistry, abilityRegistry);
    }

    private Map<GameActionType, GameHandler> buildDefaultHandlers(TurnManager turnManager,
                                                                    TrainerEffectRegistry effectRegistry,
                                                                    AbilityRegistry abilityRegistry) {
        return Map.ofEntries(
                Map.entry(GameActionType.DRAW_CARD, new DrawCardHandler(turnManager)),
                Map.entry(GameActionType.PUT_BASIC_ON_BENCH, new PutBasicOnBenchHandler()),
                Map.entry(GameActionType.ATTACH_ENERGY, new AttachEnergyHandler()),
                Map.entry(GameActionType.EVOLVE_POKEMON, new EvolvePokemonHandler()),
                Map.entry(GameActionType.PLAY_TRAINER, new PlayTrainerHandler(effectRegistry)),
                Map.entry(GameActionType.RETREAT_ACTIVE, new RetreatActiveHandler()),
                Map.entry(GameActionType.DECLARE_ATTACK, new DeclareAttackHandler(turnManager)),
                Map.entry(GameActionType.END_TURN, new EndTurnHandler(turnManager)),
                Map.entry(GameActionType.TAKE_PRIZE_CARD, new TakePrizeCardHandler(turnManager)),
                Map.entry(GameActionType.ATTACH_TOOL, new AttachToolHandler()),
                Map.entry(GameActionType.USE_ABILITY, new UseAbilityHandler(abilityRegistry)),
                Map.entry(GameActionType.CHOOSE_KO_REPLACEMENT, new ChooseKOReplacementHandler(turnManager)),
                Map.entry(GameActionType.SETUP_PLACE_ACTIVE, new SetupPlaceActiveHandler()),
                Map.entry(GameActionType.SETUP_PLACE_BENCH, new SetupPlaceBenchHandler()),
                Map.entry(GameActionType.SETUP_REMOVE_ACTIVE, new SetupRemoveActiveHandler()),
                Map.entry(GameActionType.SETUP_REMOVE_BENCH, new SetupRemoveBenchHandler()),
                Map.entry(GameActionType.CONFIRM_SETUP, new ConfirmSetupHandler()),
                Map.entry(GameActionType.RESOLVE_MULLIGAN_DRAW, new ResolveMulliganDrawHandler()),
                Map.entry(GameActionType.RESOLVE_INITIAL_MULLIGAN, new ResolveInitialMulliganHandler())
        );
    }

    public ActionResult applyAction(UUID matchId, UUID playerId, GameAction action) {
        Instant start = Instant.now();
        try {
            GameState state = persister.loadState(matchId);
            if (state == null) {
                return new ActionResult(false, action.getClientRequestId(), null, null, List.of(),
                        new GameError("MATCH_NOT_FOUND", "Match not found: " + matchId));
            }

            MatchState matchState = state.getMatchState();
            if (!matchState.canAcceptAction(action.getType())) {
                return new ActionResult(false, action.getClientRequestId(), null, null, List.of(),
                        new GameError("ACTION_NOT_ALLOWED", "Action not allowed for current match state: " + action.getType()));
            }

            if (matchState.isActive() && !state.getCurrentPlayerId().equals(playerId)
                    && action.getType() != GameActionType.CHOOSE_KO_REPLACEMENT
                    && action.getType() != GameActionType.TAKE_PRIZE_CARD) {
                return new ActionResult(false, action.getClientRequestId(), null, null, List.of(),
                        new GameError("NOT_YOUR_TURN", "It is not your turn."));
            }

            EngineContext ctx = new EngineContext(state, cardLookup, randomizer, persister, eventPublisher);
            ctx.setEnergyService(energyService);

            autoResolveMulliganDrawIfTimedOut(ctx);

            if (!ruleValidator.validate(ctx, action)) {
                log.warn("[GameEngine] applyAction: ACTION_REJECTED for type={}, playerId={}", action.getType(), action.getPlayerId());
                return new ActionResult(false, action.getClientRequestId(), null, null, List.of(),
                        new GameError("ACTION_REJECTED", "Action rejected by game rules: " + action.getType()));
            }

            GameHandler handler = handlers.get(action.getType());
            if (handler == null) {
                return new ActionResult(false, action.getClientRequestId(), null, null, List.of(),
                        new GameError("UNKNOWN_ACTION", "Unknown action type: " + action.getType()));
            }

            handler.handle(ctx, action);

            if (ctx.getError() != null) {
                return new ActionResult(false, action.getClientRequestId(), null, null, List.of(), ctx.getError());
            }

            handleSuddenDeathIfNeeded(matchId, ctx);

            GameState newState = ctx.getState();
            int previousTurn = state.getTurnNumber();
            if (newState.getTurnNumber() > previousTurn) {
                log.info("[TURN] Match {} entering turn {} (was {})", matchId, newState.getTurnNumber(), previousTurn);
            }

            persister.saveState(matchId, newState);

            long elapsed = Duration.between(start, Instant.now()).toMillis();
            if (elapsed > 200) {
                log.warn("[PERF] applyAction({}) took {}ms (exceeded 200ms)", action.getType(), elapsed);
            } else {
                log.debug("[PERF] applyAction({}) took {}ms", action.getType(), elapsed);
            }

            return new ActionResult(true, action.getClientRequestId(), null, null, ctx.getPendingEvents(), null);

        } catch (Exception e) {
            return new ActionResult(false, action.getClientRequestId(), null, null, List.of(),
                    new GameError("INTERNAL_ERROR", e.getMessage()));
        }
    }

    static final Duration MULLIGAN_TIMEOUT = Duration.ofSeconds(30);

    private boolean resolveTimedOutMulliganDraws(GameState state) {
        if (!state.isMulliganDrawPending()) return false;
        Instant deadline = state.getMulliganDrawDeadline();
        if (deadline == null || Instant.now().isBefore(deadline)) return false;

        boolean any = false;
        for (PlayerState player : state.getPlayers()) {
            UUID pid = player.getPlayerId();
            if (state.hasPendingMulliganDraw(pid)) {
                state.resolveMulliganDraw(pid, false);
                any = true;
            }
        }
        return any;
    }

    private void autoResolveMulliganDrawIfTimedOut(EngineContext ctx) {
        if (resolveTimedOutMulliganDraws(ctx.getState())) {
            GameState state = ctx.getState();
            for (PlayerState player : state.getPlayers()) {
                UUID pid = player.getPlayerId();
                ctx.addEvent(new GameEvent(
                    GameEventType.MULLIGAN_DRAW_RESOLVED.name(),
                    state.getMatchId(), 0, Instant.now(),
                    "Timeout: jugador no robó cartas extra por mulligan",
                    Map.of("playerId", pid.toString(), "drewCards", false, "count", 0)
                ));
            }
        }
    }

    public GameState loadState(UUID matchId) {
        GameState state = persister.loadState(matchId);
        if (state != null && resolveTimedOutMulliganDraws(state)) {
            publishTimeoutEvents(matchId, state);
            persister.saveState(matchId, state);
        }
        return state;
    }

    private void publishTimeoutEvents(UUID matchId, GameState state) {
        for (PlayerState player : state.getPlayers()) {
            UUID pid = player.getPlayerId();
            GameEvent event = new GameEvent(
                GameEventType.MULLIGAN_DRAW_RESOLVED.name(),
                matchId, 0, Instant.now(),
                "Timeout: jugador no robó cartas extra por mulligan",
                Map.of("playerId", pid.toString(), "drewCards", false, "count", 0)
            );
            eventPublisher.publishEvents(matchId, List.of(event));
        }
    }

    public GameState createMemento(UUID matchId) {
        return persister.loadState(matchId);
    }

    public void restoreFromMemento(UUID matchId, GameState memento) {
        persister.saveState(matchId, memento);
    }

    private void handleSuddenDeathIfNeeded(UUID matchId, EngineContext ctx) {
        GameState state = ctx.getState();
        if (!state.isSuddenDeath() || state.getPlayerDeckIds() == null || state.getPlayerDeckIds().size() < 2) {
            return;
        }

        UUID playerOneId = state.getPlayers()[0].getPlayerId();
        UUID playerTwoId = state.getPlayers()[1].getPlayerId();
        UUID deckOneId = state.getPlayerDeckIds().get(playerOneId);
        UUID deckTwoId = state.getPlayerDeckIds().get(playerTwoId);
        if (deckOneId == null || deckTwoId == null) {
            return;
        }

        // Preserve original deck IDs before setup overwrites them
        Map<UUID, UUID> preservedDeckIds = new HashMap<>(state.getPlayerDeckIds());
        String p1Name = state.getPlayers()[0].getDisplayName();
        String p2Name = state.getPlayers()[1].getDisplayName();
        GameState newState = setupManager.setup(matchId, playerOneId, playerTwoId, deckOneId, deckTwoId, 1, p1Name, p2Name);
        newState.setSuddenDeath(false);
        newState.setPrizeCountPerPlayer(1);
        newState.setPlayerDeckIds(preservedDeckIds);

        ctx.addEvent(new GameEvent(
                GameEventType.SUDDEN_DEATH_STARTED.name(), matchId,
                newState.getTurnNumber(), Instant.now(),
                "Sudden death started! Each player has 1 prize card.",
                Map.of()
        ));

        // Replace state in context so it gets persisted instead of the finished state
        ctx.setState(newState);
    }
}
