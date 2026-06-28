package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.engine.ability.AbilityRegistry;
import ar.edu.utn.frc.tup.piii.engine.action.ActionResult;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.action.GameActionType;
import ar.edu.utn.frc.tup.piii.engine.energy.EnergyService;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.TurnFlags;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import ar.edu.utn.frc.tup.piii.engine.ports.DeckLoadPort;
import ar.edu.utn.frc.tup.piii.engine.ports.EventPublisherPort;
import ar.edu.utn.frc.tup.piii.engine.ports.RandomizerPort;
import ar.edu.utn.frc.tup.piii.engine.ports.StatePersisterPort;
import ar.edu.utn.frc.tup.piii.engine.rules.RuleValidator;
import ar.edu.utn.frc.tup.piii.engine.setup.SetupManager;
import ar.edu.utn.frc.tup.piii.engine.trainer.TrainerEffectRegistry;
import ar.edu.utn.frc.tup.piii.engine.turn.TurnManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameEngineTest {

    @Mock private CardLookupPort cardLookup;
    @Mock private RandomizerPort randomizer;
    @Mock private StatePersisterPort persister;
    @Mock private EventPublisherPort eventPublisher;
    @Mock private TurnManager turnManager;
    @Mock private RuleValidator ruleValidator;
    @Mock private TrainerEffectRegistry effectRegistry;
    @Mock private AbilityRegistry abilityRegistry;
    @Mock private DeckLoadPort deckLoadPort;
    @Mock private SetupManager setupManager;
    @Mock private EnergyService energyService;

    private GameEngine gameEngine;
    private UUID matchId;
    private UUID playerId;
    private GameAction action;

    @BeforeEach
    void setUp() {
        gameEngine = new GameEngine(cardLookup, randomizer, persister, eventPublisher,
                turnManager, ruleValidator, effectRegistry, abilityRegistry,
                deckLoadPort, setupManager, energyService);

        matchId = UUID.randomUUID();
        playerId = UUID.randomUUID();
        action = new GameAction();
        action.setType(GameActionType.DRAW_CARD);
        action.setPlayerId(playerId);
        action.setClientRequestId("req-1");
    }

    private GameState createActiveState() {
        GameState state = new GameState();
        state.setStatus(MatchStatus.ACTIVE);
        state.setCurrentPlayerId(playerId);
        state.setTurnNumber(2);
        state.setTurnFlags(new TurnFlags());

        UUID p2Id = UUID.randomUUID();
        PlayerState p1 = new PlayerState();
        p1.setPlayerId(playerId);
        p1.setHand(new ArrayList<>());
        p1.setDeck(new ArrayList<>(List.of(new CardInstance(UUID.randomUUID(), "card-1"))));

        PlayerState p2 = new PlayerState();
        p2.setPlayerId(p2Id);
        p2.setHand(new ArrayList<>());
        p2.setDeck(new ArrayList<>());

        state.setPlayers(new PlayerState[]{p1, p2});
        return state;
    }

    // ----- applyAction -----

    @Test
    void applyAction_stateNotFound_returnsMatchNotFoundError() {
        when(persister.loadState(matchId)).thenReturn(null);
        ActionResult result = gameEngine.applyAction(matchId, playerId, action);

        assertFalse(result.isSuccess());
        assertEquals("MATCH_NOT_FOUND", result.getError().getCode());
    }

    @Test
    void applyAction_finishedMatchState_returnsActionNotAllowedError() {
        GameState state = new GameState();
        state.setStatus(MatchStatus.FINISHED);
        when(persister.loadState(matchId)).thenReturn(state);

        ActionResult result = gameEngine.applyAction(matchId, playerId, action);

        assertFalse(result.isSuccess());
        assertEquals("ACTION_NOT_ALLOWED", result.getError().getCode());
    }

    @Test
    void applyAction_notYourTurn_returnsNotYourTurnError() {
        GameState state = new GameState();
        state.setStatus(MatchStatus.ACTIVE);
        state.setCurrentPlayerId(UUID.randomUUID());
        when(persister.loadState(matchId)).thenReturn(state);

        ActionResult result = gameEngine.applyAction(matchId, playerId, action);

        assertFalse(result.isSuccess());
        assertEquals("NOT_YOUR_TURN", result.getError().getCode());
    }

    @Test
    void applyAction_notYourTurn_butKOReplacementAllowed() {
        GameState state = new GameState();
        state.setStatus(MatchStatus.ACTIVE);
        state.setCurrentPlayerId(UUID.randomUUID());
        action.setType(GameActionType.CHOOSE_KO_REPLACEMENT);
        when(persister.loadState(matchId)).thenReturn(state);
        lenient().when(ruleValidator.validate(any(), any())).thenReturn(true);

        ActionResult result = gameEngine.applyAction(matchId, playerId, action);

        assertTrue(result.isSuccess());
    }

    @Test
    void applyAction_notYourTurn_butTakePrizeCardAllowed() {
        GameState state = new GameState();
        state.setStatus(MatchStatus.ACTIVE);
        state.setCurrentPlayerId(UUID.randomUUID());
        action.setType(GameActionType.TAKE_PRIZE_CARD);
        when(persister.loadState(matchId)).thenReturn(state);
        lenient().when(ruleValidator.validate(any(), any())).thenReturn(true);

        ActionResult result = gameEngine.applyAction(matchId, playerId, action);

        assertTrue(result.isSuccess());
    }

    @Test
    void applyAction_ruleValidationFails_returnsActionRejectedError() {
        GameState state = createActiveState();
        when(persister.loadState(matchId)).thenReturn(state);
        when(ruleValidator.validate(any(), eq(action))).thenReturn(false);

        ActionResult result = gameEngine.applyAction(matchId, playerId, action);

        assertFalse(result.isSuccess());
        assertEquals("ACTION_REJECTED", result.getError().getCode());
    }

    @Test
    void applyAction_nullActionType_throwsInternalError() {
        GameState state = createActiveState();
        action.setType(null);
        when(persister.loadState(matchId)).thenReturn(state);
        when(ruleValidator.validate(any(), any())).thenReturn(true);

        ActionResult result = gameEngine.applyAction(matchId, playerId, action);

        assertFalse(result.isSuccess());
        // Map.ofEntries throws NPE for null key → caught as INTERNAL_ERROR
        assertEquals("INTERNAL_ERROR", result.getError().getCode());
    }

    @Test
    void applyAction_handlerThrows_returnsInternalError() {
        GameState state = createActiveState();
        when(persister.loadState(matchId)).thenReturn(state);
        when(ruleValidator.validate(any(), any())).thenReturn(true);
        doThrow(new RuntimeException("Unexpected error")).when(turnManager).advancePhase(any());

        ActionResult result = gameEngine.applyAction(matchId, playerId, action);

        assertFalse(result.isSuccess());
        assertEquals("INTERNAL_ERROR", result.getError().getCode());
    }

    @Test
    void applyAction_successfulAction_persistsState() {
        GameState state = createActiveState();
        when(persister.loadState(matchId)).thenReturn(state);
        when(ruleValidator.validate(any(), any())).thenReturn(true);

        ActionResult result = gameEngine.applyAction(matchId, playerId, action);

        assertTrue(result.isSuccess());
        verify(persister).saveState(eq(matchId), any(GameState.class));
    }

    @Test
    void applyAction_suddenDeath_rerunsSetup() {
        GameState state = createActiveState();
        state.setSuddenDeath(true);

        UUID p1Id = playerId;
        UUID p2Id = state.getPlayers()[1].getPlayerId();

        Map<UUID, UUID> deckIds = new HashMap<>();
        deckIds.put(p1Id, UUID.randomUUID());
        deckIds.put(p2Id, UUID.randomUUID());
        state.setPlayerDeckIds(deckIds);

        state.getPlayers()[0].setDisplayName("P1");
        state.getPlayers()[1].setDisplayName("P2");

        when(persister.loadState(matchId)).thenReturn(state);
        when(ruleValidator.validate(any(), any())).thenReturn(true);
        when(setupManager.setup(any(), any(), any(), any(), any(), eq(1), any(), any())).thenReturn(new GameState());

        gameEngine.applyAction(matchId, playerId, action);

        verify(setupManager).setup(eq(matchId), any(), any(), any(), any(), eq(1), any(), any());
    }

    @Test
    void applyAction_suddenDeathWithoutDeckIds_skipsSetup() {
        GameState state = createActiveState();
        state.setSuddenDeath(true);
        state.setPlayerDeckIds(null);
        when(persister.loadState(matchId)).thenReturn(state);
        when(ruleValidator.validate(any(), any())).thenReturn(true);

        gameEngine.applyAction(matchId, playerId, action);

        verify(setupManager, never()).setup(any(), any(), any(), any(), any(), anyInt(), any(), any());
    }

    @Test
    void applyAction_suddenDeathWithNullDeckId_skipsSetup() {
        GameState state = createActiveState();
        state.setSuddenDeath(true);
        UUID p2Id = state.getPlayers()[1].getPlayerId();
        Map<UUID, UUID> deckIds = new HashMap<>();
        deckIds.put(playerId, null);
        deckIds.put(p2Id, UUID.randomUUID());
        state.setPlayerDeckIds(deckIds);
        when(persister.loadState(matchId)).thenReturn(state);
        when(ruleValidator.validate(any(), any())).thenReturn(true);

        gameEngine.applyAction(matchId, playerId, action);

        verify(setupManager, never()).setup(any(), any(), any(), any(), any(), anyInt(), any(), any());
    }

    @Test
    void applyAction_suddenDeathWithSingleDeckId_skipsSetup() {
        GameState state = createActiveState();
        state.setSuddenDeath(true);
        Map<UUID, UUID> deckIds = new HashMap<>();
        deckIds.put(playerId, UUID.randomUUID());
        state.setPlayerDeckIds(deckIds);
        when(persister.loadState(matchId)).thenReturn(state);
        when(ruleValidator.validate(any(), any())).thenReturn(true);

        gameEngine.applyAction(matchId, playerId, action);

        verify(setupManager, never()).setup(any(), any(), any(), any(), any(), anyInt(), any(), any());
    }

    // ----- loadState -----

    @Test
    void loadState_stateNotFound_returnsNull() {
        when(persister.loadState(matchId)).thenReturn(null);

        GameState result = gameEngine.loadState(matchId);

        assertNull(result);
    }

    @Test
    void loadState_stateFound_returnsState() {
        GameState state = new GameState();
        state.setMulliganDrawPending(false);
        when(persister.loadState(matchId)).thenReturn(state);

        GameState result = gameEngine.loadState(matchId);

        assertNotNull(result);
    }

    @Test
    void loadState_withMulliganAndZeroPlayers_doesNotPublish() {
        GameState state = mock(GameState.class);
        when(state.isMulliganDrawPending()).thenReturn(true);
        when(state.getMulliganDrawDeadline()).thenReturn(java.time.Instant.now().minusSeconds(10));
        when(state.getPlayers()).thenReturn(new PlayerState[0]);
        when(persister.loadState(matchId)).thenReturn(state);

        gameEngine.loadState(matchId);

        verify(eventPublisher, never()).publishEvents(any(), anyList());
    }

    @Test
    void loadState_withMulliganPendingAndNullDeadline_skipsTimeout() {
        GameState state = new GameState();
        state.setMulliganDrawPending(true);
        state.setMulliganDrawDeadline(null);
        when(persister.loadState(matchId)).thenReturn(state);

        GameState result = gameEngine.loadState(matchId);

        assertNotNull(result);
        verify(eventPublisher, never()).publishEvents(any(), anyList());
    }

    @Test
    void loadState_stateWithTimedOutMulligan_publishesEvents() {
        GameState state = mock(GameState.class);
        when(state.isMulliganDrawPending()).thenReturn(true);
        when(state.getMulliganDrawDeadline()).thenReturn(java.time.Instant.now().minusSeconds(10));
        when(persister.loadState(matchId)).thenReturn(state);

        PlayerState p1 = new PlayerState();
        p1.setPlayerId(UUID.randomUUID());
        when(state.getPlayers()).thenReturn(new PlayerState[]{p1});
        when(state.hasPendingMulliganDraw(p1.getPlayerId())).thenReturn(true);
        doNothing().when(state).resolveMulliganDraw(p1.getPlayerId(), false);

        gameEngine.loadState(matchId);

        verify(eventPublisher).publishEvents(eq(matchId), anyList());
        verify(persister).saveState(eq(matchId), eq(state));
    }

    // ----- createMemento / restoreFromMemento -----

    @Test
    void createMemento_loadsState() {
        GameState state = new GameState();
        when(persister.loadState(matchId)).thenReturn(state);

        GameState result = gameEngine.createMemento(matchId);

        assertSame(state, result);
    }

    @Test
    void restoreFromMemento_savesState() {
        GameState state = new GameState();
        gameEngine.restoreFromMemento(matchId, state);

        verify(persister).saveState(matchId, state);
    }
}
