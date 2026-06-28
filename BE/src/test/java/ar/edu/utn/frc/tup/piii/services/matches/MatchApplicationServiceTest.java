package ar.edu.utn.frc.tup.piii.services.matches;

import ar.edu.utn.frc.tup.piii.dtos.matches.*;
import ar.edu.utn.frc.tup.piii.engine.GameEngine;
import ar.edu.utn.frc.tup.piii.engine.MatchStatus;
import ar.edu.utn.frc.tup.piii.engine.action.ActionResult;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.action.GameActionType;
import ar.edu.utn.frc.tup.piii.engine.action.GameError;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PrivatePlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PublicGameState;
import ar.edu.utn.frc.tup.piii.engine.model.TurnFlags;
import ar.edu.utn.frc.tup.piii.engine.ports.DeckLoadPort;
import ar.edu.utn.frc.tup.piii.engine.ports.StatePersisterPort;
import ar.edu.utn.frc.tup.piii.engine.setup.SetupManager;
import ar.edu.utn.frc.tup.piii.engine.victory.FinishReason;
import ar.edu.utn.frc.tup.piii.exceptions.ConflictException;
import ar.edu.utn.frc.tup.piii.exceptions.NotFoundException;
import ar.edu.utn.frc.tup.piii.repositories.entities.MatchEntity;
import ar.edu.utn.frc.tup.piii.exceptions.ValidationException;
import ar.edu.utn.frc.tup.piii.mappers.matches.MatchMapper;
import ar.edu.utn.frc.tup.piii.repositories.entities.MatchEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.MatchPlayerEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.MatchJpaRepository;
import ar.edu.utn.frc.tup.piii.repositories.jpa.MatchPlayerJpaRepository;
import ar.edu.utn.frc.tup.piii.repositories.jpa.PlayerJpaRepository;
import ar.edu.utn.frc.tup.piii.services.ranking.PlayerStatsService;
import ar.edu.utn.frc.tup.piii.websocket.MatchWebSocketPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchApplicationServiceTest {

    @Mock private GameEngine gameEngine;
    @Mock private SetupManager setupManager;
    @Mock private StatePersisterPort statePersisterPort;
    @Mock private DeckLoadPort deckLoadPort;
    @Mock private MatchMapper matchMapper;
    @Mock private MatchQueryService matchQueryService;
    @Mock private MatchJpaRepository matchJpaRepository;
    @Mock private MatchPlayerJpaRepository matchPlayerJpaRepository;
    @Mock private PlayerJpaRepository playerJpaRepository;
    @Mock private PlayerStatsService playerStatsService;
    @Mock private MatchWebSocketPublisher matchWebSocketPublisher;

    @Captor private ArgumentCaptor<MatchEntity> matchEntityCaptor;

    private MatchApplicationService service;

    private UUID matchId;
    private UUID p1Id;
    private UUID p2Id;
    private UUID deck1Id;
    private UUID deck2Id;

    @BeforeEach
    void setUp() {
        service = new MatchApplicationService(
                gameEngine, setupManager, statePersisterPort, deckLoadPort,
                matchMapper, matchQueryService, matchJpaRepository,
                matchPlayerJpaRepository, playerJpaRepository,
                playerStatsService, matchWebSocketPublisher);

        matchId = UUID.randomUUID();
        p1Id = UUID.randomUUID();
        p2Id = UUID.randomUUID();
        deck1Id = UUID.randomUUID();
        deck2Id = UUID.randomUUID();
    }

    // ----- createMatch -----

    @Test
    void createMatch_nullPlayer1Id_throwsValidation() {
        CreateMatchRequest req = new CreateMatchRequest();
        req.setPlayer1Id(null);

        assertThrows(ValidationException.class, () -> service.createMatch(req));
    }

    @Test
    void createMatch_player2NameWithoutId_throwsValidation() {
        CreateMatchRequest req = new CreateMatchRequest();
        req.setPlayer1Id(p1Id.toString());
        req.setPlayer1Name("Player 1");
        req.setPlayer1DeckId(deck1Id.toString());
        req.setPlayer2Name("Player 2");
        req.setPlayer2Id(null);

        assertThrows(ValidationException.class, () -> service.createMatch(req));
    }

    @Test
    void createMatch_player1NotFound_throwsValidation() {
        when(playerJpaRepository.existsById(p1Id)).thenReturn(false);

        CreateMatchRequest req = new CreateMatchRequest();
        req.setPlayer1Id(p1Id.toString());
        req.setPlayer1Name("Player 1");
        req.setPlayer1DeckId(deck1Id.toString());

        assertThrows(ValidationException.class, () -> service.createMatch(req));
    }

    @Test
    void createMatch_withPlayer1Only_createsWaitingMatch() {
        MatchEntity savedMatch = new MatchEntity();
        savedMatch.setId(matchId);
        savedMatch.setStatus("WAITING");
        savedMatch.setTurnNumber(0);

        when(playerJpaRepository.existsById(p1Id)).thenReturn(true);
        when(matchJpaRepository.save(any(MatchEntity.class))).thenReturn(savedMatch);
        when(matchMapper.toMatchResponse(any(), anyList())).thenReturn(
                new MatchResponse(matchId.toString(), "WAITING", null, 0,
                        null, null, null, null, List.of(),
                        Instant.now(), Instant.now(), null));

        CreateMatchRequest req = new CreateMatchRequest();
        req.setPlayer1Id(p1Id.toString());
        req.setPlayer1Name("Player 1");
        req.setPlayer1DeckId(deck1Id.toString());

        MatchResponse resp = service.createMatch(req);

        assertNotNull(resp);
        verify(matchJpaRepository, times(1)).save(any(MatchEntity.class));
        verify(matchPlayerJpaRepository, times(1)).save(any(MatchPlayerEntity.class));
        verify(setupManager, never()).setup(any(), any(), any(), any(), any(), anyInt(), any(), any(), anyInt());
    }

    @Test
    void createMatch_withQuickMatchFalse_notSetsHandSize30() {
        MatchEntity savedMatch = new MatchEntity();
        savedMatch.setId(matchId);
        savedMatch.setTurnNumber(0);

        when(playerJpaRepository.existsById(p1Id)).thenReturn(true);
        when(matchJpaRepository.save(any(MatchEntity.class))).thenReturn(savedMatch);

        CreateMatchRequest req = new CreateMatchRequest();
        req.setPlayer1Id(p1Id.toString());
        req.setPlayer1Name("Player 1");
        req.setPlayer1DeckId(deck1Id.toString());
        req.setQuickMatch(false);

        service.createMatch(req);

        verify(matchJpaRepository).save(matchEntityCaptor.capture());
        assertNotNull(matchEntityCaptor.getValue().getHandSize());
    }

    @Test
    void createMatch_withQuickMatch_setsHandSize30() {
        MatchEntity savedMatch = new MatchEntity();
        savedMatch.setId(matchId);
        savedMatch.setTurnNumber(0);

        when(playerJpaRepository.existsById(p1Id)).thenReturn(true);
        when(matchJpaRepository.save(any(MatchEntity.class))).thenReturn(savedMatch);

        CreateMatchRequest req = new CreateMatchRequest();
        req.setPlayer1Id(p1Id.toString());
        req.setPlayer1Name("Player 1");
        req.setPlayer1DeckId(deck1Id.toString());
        req.setQuickMatch(true);

        service.createMatch(req);

        verify(matchJpaRepository, atLeastOnce()).save(matchEntityCaptor.capture());
        MatchEntity captured = matchEntityCaptor.getAllValues().stream()
                .filter(e -> e.getHandSize() != null && e.getHandSize() == 30)
                .findFirst().orElse(null);
        assertNotNull(captured);
        assertEquals(30, captured.getHandSize());
    }

    @Test
    void createMatch_withBothPlayers_runsSetup() {
        GameState gameState = new GameState();
        gameState.setStatus(MatchStatus.ACTIVE);
        gameState.setCurrentPlayerId(p1Id);
        gameState.setFirstPlayerId(p1Id);
        gameState.setTurnNumber(1);
        gameState.setPlayers(new PlayerState[]{new PlayerState(), new PlayerState()});

        MatchEntity savedMatch = new MatchEntity();
        savedMatch.setId(matchId);
        savedMatch.setTurnNumber(0);
        savedMatch.setHandSize(7);

        MatchEntity updatedMatch = new MatchEntity();
        updatedMatch.setId(matchId);
        updatedMatch.setStatus("ACTIVE");
        updatedMatch.setCurrentPlayerId(p1Id);
        updatedMatch.setFirstPlayerId(p1Id);
        updatedMatch.setTurnNumber(1);

        when(playerJpaRepository.existsById(p1Id)).thenReturn(true);
        when(playerJpaRepository.existsById(p2Id)).thenReturn(true);
        when(matchJpaRepository.save(any(MatchEntity.class))).thenReturn(savedMatch, updatedMatch);
        when(setupManager.setup(any(), any(), any(), any(), any(), anyInt(), any(), any(), anyInt())).thenReturn(gameState);
        when(matchMapper.toMatchResponse(any(), anyList())).thenReturn(
                new MatchResponse(matchId.toString(), "ACTIVE", null, 1,
                        p1Id.toString(), p1Id.toString(), null, null, List.of(),
                        Instant.now(), Instant.now(), null));

        CreateMatchRequest req = new CreateMatchRequest();
        req.setPlayer1Id(p1Id.toString());
        req.setPlayer1Name("Player 1");
        req.setPlayer1DeckId(deck1Id.toString());
        req.setPlayer2Id(p2Id.toString());
        req.setPlayer2Name("Player 2");
        req.setPlayer2DeckId(deck2Id.toString());

        MatchResponse resp = service.createMatch(req);

        assertNotNull(resp);
        verify(deckLoadPort, times(2)).loadDeck(any());
        verify(statePersisterPort).saveState(eq(matchId), eq(gameState));
    }

    // ----- joinMatch -----

    @Test
    void joinMatch_matchNotFound_throwsNotFound() {
        when(matchJpaRepository.findById(matchId)).thenReturn(Optional.empty());
        JoinMatchRequest req = new JoinMatchRequest("Player 2", deck2Id.toString(), p2Id.toString());

        assertThrows(NotFoundException.class, () -> service.joinMatch(matchId, req));
    }

    @Test
    void joinMatch_notWaiting_throwsValidation() {
        MatchEntity match = new MatchEntity();
        match.setId(matchId);
        match.setStatus("ACTIVE");
        when(matchJpaRepository.findById(matchId)).thenReturn(Optional.of(match));
        JoinMatchRequest req = new JoinMatchRequest("Player 2", deck2Id.toString(), p2Id.toString());

        assertThrows(ValidationException.class, () -> service.joinMatch(matchId, req));
    }

    @Test
    void joinMatch_nullPlayerId_throwsValidation() {
        MatchEntity match = new MatchEntity();
        match.setId(matchId);
        match.setStatus("WAITING");
        when(matchJpaRepository.findById(matchId)).thenReturn(Optional.of(match));
        JoinMatchRequest req = new JoinMatchRequest("Player 2", deck2Id.toString(), null);

        assertThrows(ValidationException.class, () -> service.joinMatch(matchId, req));
    }

    @Test
    void joinMatch_setupFailure_rethrowsException() {
        MatchEntity match = new MatchEntity();
        match.setId(matchId);
        match.setStatus("WAITING");
        match.setHandSize(7);

        MatchPlayerEntity player1 = new MatchPlayerEntity();
        player1.setPlayerId(p1Id);
        player1.setSide("PLAYER_ONE");
        player1.setDeckId(deck1Id);
        player1.setDisplayName("Player 1");

        when(matchJpaRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(playerJpaRepository.existsById(p2Id)).thenReturn(true);
        when(matchPlayerJpaRepository.findByMatch_Id(matchId)).thenReturn(List.of(player1));
        when(setupManager.setup(any(), any(), any(), any(), any(), anyInt(), any(), any(), anyInt()))
                .thenThrow(new RuntimeException("Setup failed"));

        JoinMatchRequest req = new JoinMatchRequest("Player 2", deck2Id.toString(), p2Id.toString());

        assertThrows(RuntimeException.class, () -> service.joinMatch(matchId, req));
    }

    @Test
    void joinMatch_success_setsUpAndPersists() {
        MatchEntity match = new MatchEntity();
        match.setId(matchId);
        match.setStatus("WAITING");
        match.setHandSize(7);

        GameState gameState = new GameState();
        gameState.setStatus(MatchStatus.ACTIVE);
        gameState.setCurrentPlayerId(p1Id);
        gameState.setFirstPlayerId(p1Id);
        gameState.setTurnNumber(1);
        gameState.setPlayers(new PlayerState[]{new PlayerState(), new PlayerState()});

        MatchPlayerEntity player1 = new MatchPlayerEntity();
        player1.setPlayerId(p1Id);
        player1.setSide("PLAYER_ONE");
        player1.setDeckId(deck1Id);
        player1.setDisplayName("Player 1");

        MatchEntity updatedMatch = new MatchEntity();
        updatedMatch.setId(matchId);
        updatedMatch.setStatus("ACTIVE");

        when(matchJpaRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(playerJpaRepository.existsById(p2Id)).thenReturn(true);
        when(matchPlayerJpaRepository.findByMatch_Id(matchId)).thenReturn(List.of(player1));
        when(setupManager.setup(any(), any(), any(), any(), any(), anyInt(), any(), any(), anyInt())).thenReturn(gameState);
        when(matchJpaRepository.save(any(MatchEntity.class))).thenReturn(updatedMatch);
        when(matchMapper.toMatchResponse(any(), anyList())).thenReturn(
                new MatchResponse(matchId.toString(), "ACTIVE", null, 1,
                        p1Id.toString(), p1Id.toString(), null, null, List.of(),
                        Instant.now(), Instant.now(), null));

        JoinMatchRequest req = new JoinMatchRequest("Player 2", deck2Id.toString(), p2Id.toString());
        MatchResponse resp = service.joinMatch(matchId, req);

        assertNotNull(resp);
        verify(statePersisterPort).saveState(eq(matchId), eq(gameState));
        verify(setupManager).setup(eq(matchId), eq(p1Id), eq(p2Id), eq(deck1Id), eq(deck2Id), eq(6), eq("Player 1"), eq("Player 2"), eq(7));
    }

    // ----- executeAction -----

    @Test
    void executeAction_validAction_returnsSuccess() {
        GameState gameState = new GameState();
        gameState.setStatus(MatchStatus.ACTIVE);
        PlayerState p1 = new PlayerState();
        p1.setPlayerId(p1Id);
        PlayerState p2 = new PlayerState();
        p2.setPlayerId(p2Id);
        gameState.setPlayers(new PlayerState[]{p1, p2});
        gameState.setTurnFlags(new TurnFlags());

        ActionResult actionResult = new ActionResult(true, "req-123", null, null, List.of(), null);

        GameActionRequest req = new GameActionRequest(
                "DRAW_CARD", p1Id.toString(), Map.of("key", "value"), "req-123");

        when(gameEngine.applyAction(eq(matchId), eq(p1Id), any(GameAction.class))).thenReturn(actionResult);
        when(gameEngine.loadState(matchId)).thenReturn(gameState);

        PublicGameState publicState = new PublicGameState();
        PublicGameState.PublicPlayerState pp1 = new PublicGameState.PublicPlayerState();
        pp1.setPlayerId(p1Id);
        publicState.setPlayers(new PublicGameState.PublicPlayerState[]{pp1});
        when(matchQueryService.buildPublicState(gameState)).thenReturn(publicState);
        when(matchQueryService.buildPrivateState(gameState, p1Id)).thenReturn(new PrivatePlayerState());

        GameActionResponse resp = service.executeAction(matchId, req);

        assertTrue(resp.success());
        assertNotNull(resp.publicState());
        assertNotNull(resp.privateState());
    }

    @Test
    void executeAction_withEvents_mapsEventDtos() {
        GameState gameState = new GameState();
        gameState.setStatus(MatchStatus.ACTIVE);

        List<GameEvent> events = List.of(
                new GameEvent("CARD_DRAWN", matchId, 1, null, "Drew a card", null));
        ActionResult actionResult = new ActionResult(true, "req-1", null, null, events, null);

        GameActionRequest req = new GameActionRequest(
                "DRAW_CARD", p1Id.toString(), null, "req-1");

        when(gameEngine.applyAction(eq(matchId), eq(p1Id), any(GameAction.class))).thenReturn(actionResult);

        GameActionResponse resp = service.executeAction(matchId, req);

        assertEquals(1, resp.events().size());
        assertEquals("CARD_DRAWN", resp.events().get(0).type());
    }

    @Test
    void executeAction_withoutPayloadMap_doesNotSetPayload() {
        GameState gameState = new GameState();
        gameState.setStatus(MatchStatus.ACTIVE);

        ActionResult actionResult = new ActionResult(true, null, null, null, List.of(), null);

        GameActionRequest req = new GameActionRequest(
                "DRAW_CARD", p1Id.toString(), "not-a-map", null);

        when(gameEngine.applyAction(eq(matchId), eq(p1Id), any(GameAction.class))).thenReturn(actionResult);
        when(gameEngine.loadState(matchId)).thenReturn(null);

        GameActionResponse resp = service.executeAction(matchId, req);

        assertTrue(resp.success());
    }

    @Test
    void executeAction_publicStateAlreadySet_skipsRebuild() {
        Object existingPublic = new Object();
        ActionResult actionResult = new ActionResult(true, null, existingPublic, null, List.of(), null);

        GameActionRequest req = new GameActionRequest(
                "DRAW_CARD", p1Id.toString(), null, null);

        when(gameEngine.applyAction(eq(matchId), eq(p1Id), any(GameAction.class))).thenReturn(actionResult);

        GameActionResponse resp = service.executeAction(matchId, req);

        assertTrue(resp.success());
        verify(gameEngine, never()).loadState(any());
    }

    @Test
    void executeAction_nullEvents_returnsEmptyList() {
        GameState gameState = new GameState();
        gameState.setStatus(MatchStatus.ACTIVE);
        ActionResult actionResult = new ActionResult(true, null, null, null, null, null);

        GameActionRequest req = new GameActionRequest(
                "DRAW_CARD", p1Id.toString(), null, null);

        when(gameEngine.applyAction(eq(matchId), eq(p1Id), any(GameAction.class))).thenReturn(actionResult);

        GameActionResponse resp = service.executeAction(matchId, req);

        assertTrue(resp.success());
        assertTrue(resp.events().isEmpty());
    }

    @Test
    void executeAction_finishedMatch_noOptMatch_finishesGracefully() {
        GameState gameState = new GameState();
        gameState.setStatus(MatchStatus.FINISHED);
        gameState.setWinnerPlayerId(p1Id);
        gameState.setFinishReason(FinishReason.PRIZES);

        PlayerState p1 = new PlayerState();
        p1.setPlayerId(p1Id);
        gameState.setPlayers(new PlayerState[]{p1});

        ActionResult actionResult = new ActionResult(true, null, null, null, List.of(), null);

        when(gameEngine.applyAction(eq(matchId), eq(p1Id), any(GameAction.class))).thenReturn(actionResult);
        when(gameEngine.loadState(matchId)).thenReturn(gameState);
        when(matchJpaRepository.findById(matchId)).thenReturn(Optional.empty());

        PublicGameState publicState = new PublicGameState();
        PublicGameState.PublicPlayerState pp1 = new PublicGameState.PublicPlayerState();
        pp1.setPlayerId(p1Id);
        publicState.setPlayers(new PublicGameState.PublicPlayerState[]{pp1});
        when(matchQueryService.buildPublicState(gameState)).thenReturn(publicState);
        when(matchQueryService.buildPrivateState(gameState, p1Id)).thenReturn(new PrivatePlayerState());

        GameActionRequest req = new GameActionRequest(
                "END_TURN", p1Id.toString(), null, null);

        GameActionResponse resp = service.executeAction(matchId, req);

        assertTrue(resp.success());
        // recordMatchResult is called unconditionally when state is FINISHED
        verify(playerStatsService).recordMatchResult(matchId, p1Id, FinishReason.PRIZES);
        // But matchJpaRepository.save is NOT called since optMatch is empty
        verify(matchJpaRepository, never()).save(any(MatchEntity.class));
    }

    @Test
    void executeAction_errorResult_buildsErrorDto() {
        GameError error = new GameError("NOT_YOUR_TURN", "It is not your turn.");
        ActionResult actionResult = new ActionResult(false, null, null, null, List.of(), error);

        GameActionRequest req = new GameActionRequest(
                "DRAW_CARD", p1Id.toString(), null, null);

        when(gameEngine.applyAction(eq(matchId), eq(p1Id), any(GameAction.class))).thenReturn(actionResult);

        GameActionResponse resp = service.executeAction(matchId, req);

        assertFalse(resp.success());
        assertNotNull(resp.error());
        assertEquals("NOT_YOUR_TURN", resp.error().code());
    }

    @Test
    void executeAction_finishedMatch_updatesEntityAndRecordsStats() {
        GameState gameState = new GameState();
        gameState.setStatus(MatchStatus.FINISHED);
        gameState.setWinnerPlayerId(p1Id);
        gameState.setFinishReason(FinishReason.PRIZES);
        gameState.setTurnNumber(5);
        PlayerState p1 = new PlayerState();
        p1.setPlayerId(p1Id);
        gameState.setPlayers(new PlayerState[]{p1});

        ActionResult actionResult = new ActionResult(true, null, null, null, List.of(), null);

        MatchEntity matchEntity = new MatchEntity();
        matchEntity.setId(matchId);

        when(gameEngine.applyAction(eq(matchId), eq(p1Id), any(GameAction.class))).thenReturn(actionResult);
        when(gameEngine.loadState(matchId)).thenReturn(gameState);
        when(matchJpaRepository.findById(matchId)).thenReturn(Optional.of(matchEntity));
        when(matchJpaRepository.save(any(MatchEntity.class))).thenReturn(matchEntity);

        PublicGameState publicState = new PublicGameState();
        PublicGameState.PublicPlayerState pp1 = new PublicGameState.PublicPlayerState();
        pp1.setPlayerId(p1Id);
        publicState.setPlayers(new PublicGameState.PublicPlayerState[]{pp1});
        when(matchQueryService.buildPublicState(gameState)).thenReturn(publicState);
        when(matchQueryService.buildPrivateState(gameState, p1Id)).thenReturn(new PrivatePlayerState());

        GameActionRequest req = new GameActionRequest(
                "END_TURN", p1Id.toString(), null, null);

        GameActionResponse resp = service.executeAction(matchId, req);

        assertTrue(resp.success());
        verify(playerStatsService).recordMatchResult(matchId, p1Id, FinishReason.PRIZES);
    }

    // ----- getPrivateState -----

    @Test
    void getPrivateState_stateNotFound_returnsNull() {
        when(gameEngine.loadState(matchId)).thenReturn(null);

        PrivatePlayerState result = service.getPrivateState(matchId, p1Id);

        assertNull(result);
    }

    @Test
    void getPrivateState_stateFound_buildsAndReturns() {
        GameState state = new GameState();
        when(gameEngine.loadState(matchId)).thenReturn(state);
        PrivatePlayerState expected = new PrivatePlayerState();
        when(matchQueryService.buildPrivateState(state, p1Id)).thenReturn(expected);

        PrivatePlayerState result = service.getPrivateState(matchId, p1Id);

        assertSame(expected, result);
    }

    // ----- getPlayerIds -----

    @Test
    void getPlayerIds_returnsIds() {
        MatchPlayerEntity mp1 = new MatchPlayerEntity();
        mp1.setPlayerId(p1Id);
        MatchPlayerEntity mp2 = new MatchPlayerEntity();
        mp2.setPlayerId(p2Id);
        when(matchPlayerJpaRepository.findByMatch_Id(matchId)).thenReturn(List.of(mp1, mp2));

        List<UUID> ids = service.getPlayerIds(matchId);

        assertEquals(2, ids.size());
        assertTrue(ids.contains(p1Id));
        assertTrue(ids.contains(p2Id));
    }

    // ----- listAvailableMatches -----

    @Test
    void listAvailableMatches_withResults_mapsResponses() {
        MatchEntity match = new MatchEntity();
        match.setId(matchId);
        when(matchJpaRepository.findByStatus("WAITING")).thenReturn(List.of(match));
        when(matchMapper.toMatchResponse(eq(match), anyList())).thenReturn(
                new MatchResponse(matchId.toString(), "WAITING", null, 0,
                        null, null, null, null, List.of(),
                        Instant.now(), Instant.now(), null));

        List<MatchResponse> result = service.listAvailableMatches(null);

        assertEquals(1, result.size());
        verify(matchMapper).toMatchResponse(eq(match), anyList());
    }

    @Test
    void listAvailableMatches_nullStatus_usesWaiting() {
        when(matchJpaRepository.findByStatus("WAITING")).thenReturn(List.of());

        List<MatchResponse> matches = service.listAvailableMatches(null);

        assertTrue(matches.isEmpty());
        verify(matchJpaRepository).findByStatus("WAITING");
    }

    @Test
    void listAvailableMatches_withStatus_filtersByStatus() {
        when(matchJpaRepository.findByStatus("ACTIVE")).thenReturn(List.of());

        List<MatchResponse> matches = service.listAvailableMatches("ACTIVE");

        assertTrue(matches.isEmpty());
        verify(matchJpaRepository).findByStatus("ACTIVE");
    }

    // ----- getActiveMatches -----

    @Test
    void getActiveMatches_returnsActiveAndSetup() {
        MatchEntity activeMatch = new MatchEntity();
        activeMatch.setId(matchId);
        MatchEntity setupMatch = new MatchEntity();
        setupMatch.setId(UUID.randomUUID());

        when(matchJpaRepository.findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc("ACTIVE", p1Id))
                .thenReturn(List.of(activeMatch));
        when(matchJpaRepository.findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc("SETUP", p1Id))
                .thenReturn(List.of(setupMatch));
        when(matchMapper.toMatchResponse(any(), anyList())).thenReturn(
                new MatchResponse(null, null, null, 0, null, null, null, null, List.of(),
                        null, null, null));

        List<MatchResponse> matches = service.getActiveMatches(p1Id);

        assertEquals(2, matches.size());
    }

    // ----- expireOldWaitingMatches -----

    @Test
    void expireOldWaitingMatches_noOldMatches_doesNothing() {
        when(matchJpaRepository.findByStatusAndCreatedAtBefore(eq("WAITING"), any(Instant.class)))
                .thenReturn(List.of());

        service.expireOldWaitingMatches();

        verify(matchJpaRepository, never()).save(any());
    }

    @Test
    void expireOldWaitingMatches_withOldMatches_expiresThem() {
        MatchEntity oldMatch = new MatchEntity();
        oldMatch.setId(matchId);
        oldMatch.setStatus("WAITING");

        when(matchJpaRepository.findByStatusAndCreatedAtBefore(eq("WAITING"), any(Instant.class)))
                .thenReturn(List.of(oldMatch));

        service.expireOldWaitingMatches();

        verify(matchJpaRepository).save(matchEntityCaptor.capture());
        assertEquals("FINISHED", matchEntityCaptor.getValue().getStatus());
        assertEquals("EXPIRED", matchEntityCaptor.getValue().getFinishReason());
    }

    // ----- cancelMatch -----

    @Test
    void cancelMatch_notFound_throwsNotFound() {
        when(matchJpaRepository.findById(matchId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.cancelMatch(matchId, p1Id));
    }

    @Test
    void cancelMatch_alreadyFinished_throwsValidation() {
        MatchEntity match = new MatchEntity();
        match.setId(matchId);
        match.setStatus("FINISHED");
        when(matchJpaRepository.findById(matchId)).thenReturn(Optional.of(match));

        assertThrows(ValidationException.class, () -> service.cancelMatch(matchId, p1Id));
    }

    @Test
    void cancelMatch_noCreator_throwsIllegalState() {
        MatchEntity match = new MatchEntity();
        match.setId(matchId);
        match.setStatus("WAITING");
        when(matchJpaRepository.findById(matchId)).thenReturn(Optional.of(match));
        when(matchPlayerJpaRepository.findByMatch_Id(matchId)).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> service.cancelMatch(matchId, p1Id));
    }

    @Test
    void cancelMatch_notCreator_throwsValidation() {
        MatchEntity match = new MatchEntity();
        match.setId(matchId);
        match.setStatus("WAITING");
        when(matchJpaRepository.findById(matchId)).thenReturn(Optional.of(match));

        MatchPlayerEntity creator = new MatchPlayerEntity();
        creator.setPlayerId(p1Id);
        creator.setSide("PLAYER_ONE");
        when(matchPlayerJpaRepository.findByMatch_Id(matchId)).thenReturn(List.of(creator));

        assertThrows(ValidationException.class, () -> service.cancelMatch(matchId, p2Id));
    }

    @Test
    void cancelMatch_success() {
        MatchEntity match = new MatchEntity();
        match.setId(matchId);
        match.setStatus("WAITING");
        when(matchJpaRepository.findById(matchId)).thenReturn(Optional.of(match));

        MatchPlayerEntity creator = new MatchPlayerEntity();
        creator.setPlayerId(p1Id);
        creator.setSide("PLAYER_ONE");
        when(matchPlayerJpaRepository.findByMatch_Id(matchId)).thenReturn(List.of(creator));

        MatchEntity savedMatch = new MatchEntity();
        savedMatch.setId(matchId);
        savedMatch.setStatus("FINISHED");
        when(matchJpaRepository.save(any())).thenReturn(savedMatch);
        when(matchMapper.toMatchResponse(any(), anyList())).thenReturn(
                new MatchResponse(matchId.toString(), "FINISHED", null, 0,
                        null, null, null, "CANCELLED", List.of(),
                        Instant.now(), Instant.now(), null));

        MatchResponse resp = service.cancelMatch(matchId, p1Id);

        assertNotNull(resp);
        verify(matchJpaRepository).save(matchEntityCaptor.capture());
        assertEquals("CANCELLED", matchEntityCaptor.getValue().getFinishReason());
    }

    // ----- concedeMatch -----

    @Test
    void concedeMatch_notFound_throwsNotFound() {
        when(matchJpaRepository.findById(matchId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.concedeMatch(matchId, p1Id));
    }

    @Test
    void concedeMatch_alreadyFinished_throwsValidation() {
        MatchEntity match = new MatchEntity();
        match.setId(matchId);
        match.setStatus("FINISHED");
        when(matchJpaRepository.findById(matchId)).thenReturn(Optional.of(match));

        assertThrows(ValidationException.class, () -> service.concedeMatch(matchId, p1Id));
    }

    @Test
    void concedeMatch_notParticipant_throwsValidation() {
        MatchEntity match = new MatchEntity();
        match.setId(matchId);
        match.setStatus("ACTIVE");
        when(matchJpaRepository.findById(matchId)).thenReturn(Optional.of(match));

        MatchPlayerEntity mp1 = new MatchPlayerEntity();
        mp1.setPlayerId(p1Id);
        mp1.setSide("PLAYER_ONE");
        when(matchPlayerJpaRepository.findByMatch_Id(matchId)).thenReturn(List.of(mp1));

        UUID strangerId = UUID.randomUUID();
        assertThrows(ValidationException.class, () -> service.concedeMatch(matchId, strangerId));
    }

    @Test
    void concedeMatch_withWinnerIdAndState_publishesWebSocket() {
        MatchEntity match = new MatchEntity();
        match.setId(matchId);
        match.setStatus("ACTIVE");
        when(matchJpaRepository.findById(matchId)).thenReturn(Optional.of(match));

        MatchPlayerEntity player1 = new MatchPlayerEntity();
        player1.setPlayerId(p1Id);
        player1.setSide("PLAYER_ONE");
        MatchPlayerEntity player2 = new MatchPlayerEntity();
        player2.setPlayerId(p2Id);
        player2.setSide("PLAYER_TWO");
        List<MatchPlayerEntity> players = List.of(player1, player2);
        when(matchPlayerJpaRepository.findByMatch_Id(matchId)).thenReturn(players);

        GameState state = new GameState();
        state.setStatus(MatchStatus.ACTIVE);
        state.setTurnNumber(3);
        when(gameEngine.loadState(matchId)).thenReturn(state);

        PublicGameState publicState = new PublicGameState();
        when(matchQueryService.buildPublicState(state)).thenReturn(publicState);
        PrivatePlayerState privateState = new PrivatePlayerState();
        when(matchQueryService.buildPrivateState(state, p1Id)).thenReturn(privateState);
        when(matchQueryService.buildPrivateState(state, p2Id)).thenReturn(privateState);

        when(matchJpaRepository.save(any())).thenReturn(match);
        when(matchMapper.toMatchResponse(any(), anyList())).thenReturn(
                new MatchResponse(matchId.toString(), "FINISHED", null, 0,
                        null, null, p2Id.toString(), "CONCEDE", List.of(),
                        Instant.now(), Instant.now(), null));

        MatchResponse resp = service.concedeMatch(matchId, p1Id);

        assertNotNull(resp);
        verify(playerStatsService).recordMatchResult(matchId, p2Id, FinishReason.CONCEDE);
        verify(matchWebSocketPublisher, atLeastOnce()).publishPublicState(eq(matchId), any());
        verify(matchWebSocketPublisher, atLeastOnce()).publishPrivateState(eq(matchId), any(), any());
    }

    @Test
    void concedeMatch_noState_skipsWebSocket() {
        MatchEntity match = new MatchEntity();
        match.setId(matchId);
        match.setStatus("ACTIVE");
        when(matchJpaRepository.findById(matchId)).thenReturn(Optional.of(match));

        MatchPlayerEntity player1 = new MatchPlayerEntity();
        player1.setPlayerId(p1Id);
        player1.setSide("PLAYER_ONE");
        MatchPlayerEntity player2 = new MatchPlayerEntity();
        player2.setPlayerId(p2Id);
        player2.setSide("PLAYER_TWO");
        when(matchPlayerJpaRepository.findByMatch_Id(matchId)).thenReturn(List.of(player1, player2));

        when(gameEngine.loadState(matchId)).thenReturn(null);
        when(matchJpaRepository.save(any())).thenReturn(match);
        when(matchMapper.toMatchResponse(any(), anyList())).thenReturn(
                new MatchResponse(matchId.toString(), "FINISHED", null, 0,
                        null, null, null, "CONCEDE", List.of(),
                        Instant.now(), Instant.now(), null));

        MatchResponse resp = service.concedeMatch(matchId, p1Id);

        assertNotNull(resp);
        verify(matchWebSocketPublisher, never()).publishPublicState(any(), any());
    }

    // ----- getMatchState -----

    @Test
    void getMatchState_noState_throwsNotFound() {
        when(gameEngine.loadState(matchId)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> service.getMatchState(matchId, p1Id));
    }

    @Test
    void getMatchState_withState_returnsResponse() {
        GameState state = new GameState();
        when(gameEngine.loadState(matchId)).thenReturn(state);

        PublicGameState publicState = new PublicGameState();
        PrivatePlayerState privateState = new PrivatePlayerState();
        when(matchQueryService.buildPublicState(state)).thenReturn(publicState);
        when(matchQueryService.buildPrivateState(state, p1Id)).thenReturn(privateState);

        MatchStateResponse resp = service.getMatchState(matchId, p1Id);

        assertNotNull(resp);
        assertEquals(matchId.toString(), resp.matchId());
        assertSame(publicState, resp.publicState());
        assertSame(privateState, resp.privateState());
    }

    // ----- forceSuddenDeath -----

    @Test
    void forceSuddenDeath_stateNotFound_throwsNotFound() {
        when(gameEngine.loadState(matchId)).thenReturn(null);

        assertThrows(NotFoundException.class, () -> service.forceSuddenDeath(matchId));
    }

    @Test
    void forceSuddenDeath_withState_emptiesDecks() {
        GameState state = mock(GameState.class);
        PlayerState p1 = new PlayerState();
        PlayerState p2 = new PlayerState();
        p1.setDeck(new ArrayList<>(List.of(new ar.edu.utn.frc.tup.piii.engine.model.CardInstance(
                UUID.randomUUID(), "card-1"))));
        p2.setDeck(new ArrayList<>(List.of(new ar.edu.utn.frc.tup.piii.engine.model.CardInstance(
                UUID.randomUUID(), "card-2"))));

        when(gameEngine.loadState(matchId)).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{p1, p2});

        service.forceSuddenDeath(matchId);

        assertTrue(p1.getDeck().isEmpty());
        assertTrue(p2.getDeck().isEmpty());
        verify(statePersisterPort).saveState(eq(matchId), eq(state));
    }
}
