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
import ar.edu.utn.frc.tup.piii.engine.model.PublicGameState;
import ar.edu.utn.frc.tup.piii.engine.model.PrivatePlayerState;
import ar.edu.utn.frc.tup.piii.engine.victory.FinishReason;
import ar.edu.utn.frc.tup.piii.websocket.MatchWebSocketPublisher;

import java.time.Instant;
import ar.edu.utn.frc.tup.piii.engine.ports.DeckLoadPort;
import ar.edu.utn.frc.tup.piii.engine.ports.StatePersisterPort;
import ar.edu.utn.frc.tup.piii.engine.setup.SetupManager;
import ar.edu.utn.frc.tup.piii.exceptions.ConflictException;
import ar.edu.utn.frc.tup.piii.exceptions.NotFoundException;
import ar.edu.utn.frc.tup.piii.exceptions.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ar.edu.utn.frc.tup.piii.mappers.matches.MatchMapper;
import ar.edu.utn.frc.tup.piii.repositories.entities.MatchEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.MatchPlayerEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.MatchJpaRepository;
import ar.edu.utn.frc.tup.piii.repositories.jpa.MatchPlayerJpaRepository;
import ar.edu.utn.frc.tup.piii.repositories.jpa.PlayerJpaRepository;
import ar.edu.utn.frc.tup.piii.services.ranking.PlayerStatsService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
public class MatchApplicationService {

    private final GameEngine gameEngine;
    private final SetupManager setupManager;
    private final StatePersisterPort statePersisterPort;
    private final DeckLoadPort deckLoadPort;
    private final MatchMapper matchMapper;
    private final MatchQueryService matchQueryService;
    private final MatchJpaRepository matchJpaRepository;
    private final MatchPlayerJpaRepository matchPlayerJpaRepository;

    private final PlayerJpaRepository playerJpaRepository;
    private final PlayerStatsService playerStatsService;
    private final MatchWebSocketPublisher matchWebSocketPublisher;
    private final ConcurrentHashMap<UUID, ReentrantLock> matchLocks = new ConcurrentHashMap<>();
    private static final long LOCK_TIMEOUT_SECONDS = 10;
    private static final Logger log = LoggerFactory.getLogger(MatchApplicationService.class);

    public MatchApplicationService(GameEngine gameEngine,
                                    SetupManager setupManager,
                                    StatePersisterPort statePersisterPort,
                                    DeckLoadPort deckLoadPort,
                                    MatchMapper matchMapper,
                                    MatchQueryService matchQueryService,
                                    MatchJpaRepository matchJpaRepository,
                                    MatchPlayerJpaRepository matchPlayerJpaRepository,
                                    PlayerJpaRepository playerJpaRepository,
                                    PlayerStatsService playerStatsService,
                                    MatchWebSocketPublisher matchWebSocketPublisher) {
        this.gameEngine = gameEngine;
        this.setupManager = setupManager;
        this.statePersisterPort = statePersisterPort;
        this.deckLoadPort = deckLoadPort;
        this.matchMapper = matchMapper;
        this.matchQueryService = matchQueryService;
        this.matchJpaRepository = matchJpaRepository;
        this.matchPlayerJpaRepository = matchPlayerJpaRepository;
        this.playerJpaRepository = playerJpaRepository;
        this.playerStatsService = playerStatsService;
        this.matchWebSocketPublisher = matchWebSocketPublisher;
    }

    @Transactional
    public MatchResponse createMatch(CreateMatchRequest request) {
        if (request.getPlayer1Id() == null) {
            throw new ValidationException("player1Id is required");
        }
        UUID player1Id = UUID.fromString(request.getPlayer1Id());
        String player1Kind = resolvePlayerKind(player1Id);

        UUID player2Id = null;
        if (request.getPlayer2Id() != null) {
            player2Id = UUID.fromString(request.getPlayer2Id());
            resolvePlayerKind(player2Id);
        } else if (request.getPlayer2Name() != null) {
            throw new ValidationException("player2Id is required when player2Name is provided");
        }

        MatchEntity match = new MatchEntity();
        match.setStatus("WAITING");
        match.setTurnNumber(0);
        if (request.getQuickMatch() != null && request.getQuickMatch()) {
            match.setHandSize(30);
        }
        match = matchJpaRepository.save(match);

        MatchPlayerEntity player1 = new MatchPlayerEntity();
        player1.setMatch(match);
        player1.setPlayerId(player1Id);
        player1.setPlayerKind(player1Kind);
        player1.setSide("PLAYER_ONE");
        player1.setDeckId(UUID.fromString(request.getPlayer1DeckId()));
        player1.setDisplayName(request.getPlayer1Name());
        matchPlayerJpaRepository.save(player1);

        List<MatchPlayerEntity> players = new ArrayList<>(List.of(player1));

        if (player2Id != null) {
            String player2Kind = resolvePlayerKind(player2Id);
            MatchPlayerEntity player2 = new MatchPlayerEntity();
            player2.setMatch(match);
            player2.setPlayerId(player2Id);
            player2.setPlayerKind(player2Kind);
            player2.setSide("PLAYER_TWO");
            player2.setDeckId(UUID.fromString(request.getPlayer2DeckId()));
            player2.setDisplayName(request.getPlayer2Name());
            matchPlayerJpaRepository.save(player2);
            players.add(player2);

            UUID deck1Id = UUID.fromString(request.getPlayer1DeckId());
            UUID deck2Id = UUID.fromString(request.getPlayer2DeckId());

            deckLoadPort.loadDeck(deck1Id);
            deckLoadPort.loadDeck(deck2Id);

            int handSize = match.getHandSize() != null ? match.getHandSize() : 7;
            GameState gameState = setupManager.setup(match.getId(), player1Id, player2Id, deck1Id, deck2Id,
                    6, request.getPlayer1Name(), request.getPlayer2Name(), handSize);
            SetupManager.revealAllPokemon(gameState);
            statePersisterPort.saveState(match.getId(), gameState);

            match.setStatus(gameState.getStatus().name());
            match.setCurrentPlayerId(gameState.getCurrentPlayerId());
            match.setFirstPlayerId(gameState.getFirstPlayerId());
            match.setTurnNumber(gameState.getTurnNumber());
            match = matchJpaRepository.save(match);
        }

        return matchMapper.toMatchResponse(match, players);
    }

    @Transactional
    public MatchResponse joinMatch(UUID matchId, JoinMatchRequest request) {
        MatchEntity match = matchJpaRepository.findById(matchId)
                .orElseThrow(() -> new NotFoundException("Match not found: " + matchId));

        if (!"WAITING".equals(match.getStatus())) {
            throw new ValidationException("Match is not waiting for players");
        }

        if (request.playerId() == null) {
            throw new ValidationException("playerId is required");
        }
        UUID player2Id = UUID.fromString(request.playerId());
        String player2Kind = resolvePlayerKind(player2Id);

        List<MatchPlayerEntity> existingPlayers = matchPlayerJpaRepository.findByMatch_Id(matchId);
        MatchPlayerEntity player1 = existingPlayers.stream()
                .filter(p -> "PLAYER_ONE".equals(p.getSide()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Match has no player 1"));

        MatchPlayerEntity player2 = new MatchPlayerEntity();
        player2.setMatch(match);
        player2.setPlayerId(player2Id);
        player2.setPlayerKind(player2Kind);
        player2.setSide("PLAYER_TWO");
        player2.setDeckId(UUID.fromString(request.deckId()));
        player2.setDisplayName(request.playerName());
        matchPlayerJpaRepository.save(player2);

        UUID deck1Id = player1.getDeckId();
        UUID deck2Id = player2.getDeckId();

        log.warn("[joinMatch] loading decks for match {}", matchId);
        deckLoadPort.loadDeck(deck1Id);
        deckLoadPort.loadDeck(deck2Id);

        log.warn("[joinMatch] setting up match {}", matchId);
        GameState gameState;
        try {
            int handSize = match.getHandSize() != null ? match.getHandSize() : 7;
            gameState = setupManager.setup(matchId, player1.getPlayerId(), player2Id, deck1Id, deck2Id,
                    6, player1.getDisplayName(), request.playerName(), handSize);
            SetupManager.revealAllPokemon(gameState);
        } catch (Exception e) {
            log.error("[joinMatch] setup FAILED for match {}: {}", matchId, e.getMessage(), e);
            throw e;
        }
        log.warn("[joinMatch] setup complete for match {}", matchId);

        statePersisterPort.saveState(matchId, gameState);
        log.warn("[joinMatch] state saved for match {}", matchId);

        match.setStatus(gameState.getStatus().name());
        match.setCurrentPlayerId(gameState.getCurrentPlayerId());
        match.setFirstPlayerId(gameState.getFirstPlayerId());
        match.setTurnNumber(gameState.getTurnNumber());
        match = matchJpaRepository.save(match);
        log.warn("[joinMatch] match {} set to {}", matchId, gameState.getStatus().name());

        List<MatchPlayerEntity> allPlayers = new ArrayList<>(existingPlayers);
        allPlayers.add(player2);

        return matchMapper.toMatchResponse(match, allPlayers);
    }

    @Transactional
    public GameActionResponse executeAction(UUID matchId, GameActionRequest request) {
        ReentrantLock lock = matchLocks.computeIfAbsent(matchId, k -> new ReentrantLock(true));
        try {
            if (!lock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new ConflictException("Match " + matchId + " is busy. Try again.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConflictException("Request interrupted for match " + matchId);
        }

        try {
            GameAction action = new GameAction();
            action.setType(GameActionType.valueOf(request.type()));
            action.setPlayerId(UUID.fromString(request.playerId()));
            action.setClientRequestId(request.clientRequestId());
            if (request.payload() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> payload = (Map<String, Object>) request.payload();
                action.setPayload(payload);
            }

            UUID playerUuid = UUID.fromString(request.playerId());
            ActionResult result = gameEngine.applyAction(matchId, playerUuid, action);

            GameState state = null;
            if (result.getPublicState() == null) {
                state = gameEngine.loadState(matchId);
                if (state != null) {
                    PublicGameState publicState = matchQueryService.buildPublicState(state);
                    log.warn("[DEBUG] executeAction built publicState: hasAttachedEnergy={}", publicState.isHasAttachedEnergy());
                    for (var pp : publicState.getPlayers()) {
                        var active = pp.getActivePokemon();
                        if (active != null) {
                            log.warn("[DEBUG]   player={} active={} attachedCards={}", pp.getPlayerId(), active.getInstanceId(), Arrays.toString(active.getAttachedCards()));
                        }
                    }
                    result.setPublicState(publicState);
                    result.setPrivateState(matchQueryService.buildPrivateState(state, playerUuid));
                }
            }

            if (state != null && state.getStatus() == MatchStatus.FINISHED) {
                var optMatch = matchJpaRepository.findById(matchId);
                if (optMatch.isPresent()) {
                    var match = optMatch.get();
                    match.setWinnerPlayerId(state.getWinnerPlayerId());
                    match.setFinishReason(
                            state.getFinishReason() != null ? state.getFinishReason().name() : null);
                    match.setFinishedAt(Instant.now());
                    match.setTurnNumber(state.getTurnNumber());
                    match.setStatus("FINISHED");
                    matchJpaRepository.save(match);
                }
                playerStatsService.recordMatchResult(
                        matchId, state.getWinnerPlayerId(), state.getFinishReason());
            }

            List<GameEventDto> eventDtos = result.getEvents() != null
                    ? result.getEvents().stream()
                    .map(e -> new GameEventDto(e.getType(), e.getMessage(), e.getPayload(), e.getTurnNumber()))
                    .collect(Collectors.toList())
                    : List.of();

            GameActionResponse.ErrorDto errorDto = null;
            if (result.getError() != null) {
                log.warn("[executeAction] Action FAILED: type={}, playerId={}, errorCode={}, errorMsg={}",
                        request.type(), request.playerId(), result.getError().getCode(), result.getError().getMessage());
                errorDto = new GameActionResponse.ErrorDto(
                        result.getError().getCode(),
                        result.getError().getMessage(),
                        result.getError().getDetails()
                );
            }

            return new GameActionResponse(
                    result.isSuccess(),
                    result.getClientRequestId(),
                    result.getPublicState(),
                    result.getPrivateState(),
                    eventDtos,
                    errorDto
            );
        } finally {
            lock.unlock();
        }
    }

    public PrivatePlayerState getPrivateState(UUID matchId, UUID playerId) {
        GameState state = gameEngine.loadState(matchId);
        if (state == null) return null;
        return matchQueryService.buildPrivateState(state, playerId);
    }

    public List<UUID> getPlayerIds(UUID matchId) {
        return matchPlayerJpaRepository.findByMatch_Id(matchId)
                .stream()
                .map(MatchPlayerEntity::getPlayerId)
                .collect(Collectors.toList());
    }

    private String resolvePlayerKind(UUID playerId) {
        if (playerJpaRepository.existsById(playerId)) {
            return "PLAYER";
        }
        throw new ValidationException("Player not found: " + playerId);
    }

    public List<MatchResponse> listAvailableMatches(String status) {
        String effectiveStatus = status != null ? status : "WAITING";
        List<MatchEntity> matches = matchJpaRepository.findByStatus(effectiveStatus);
        return matches.stream()
                .map(m -> {
                    List<MatchPlayerEntity> players = matchPlayerJpaRepository.findByMatch_Id(m.getId());
                    return matchMapper.toMatchResponse(m, players);
                })
                .toList();
    }

    public List<MatchResponse> getActiveMatches(UUID playerId) {
        List<MatchEntity> active = matchJpaRepository
                .findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc("ACTIVE", playerId);
        List<MatchEntity> setup = matchJpaRepository
                .findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc("SETUP", playerId);
        List<MatchEntity> all = new ArrayList<>();
        all.addAll(active);
        all.addAll(setup);
        return all.stream()
                .map(m -> {
                    List<MatchPlayerEntity> players = matchPlayerJpaRepository.findByMatch_Id(m.getId());
                    return matchMapper.toMatchResponse(m, players);
                })
                .collect(Collectors.toList());
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireOldWaitingMatches() {
        Instant threshold = Instant.now().minus(java.time.Duration.ofMinutes(5));
        List<MatchEntity> oldMatches = matchJpaRepository.findByStatusAndCreatedAtBefore("WAITING", threshold);
        for (MatchEntity match : oldMatches) {
            match.setStatus("FINISHED");
            match.setFinishReason("EXPIRED");
            match.setFinishedAt(Instant.now());
            matchJpaRepository.save(match);
            log.warn("Expired WAITING match {} (created at {})", match.getId(), match.getCreatedAt());
        }
    }

    @Transactional
    public MatchResponse cancelMatch(UUID matchId, UUID playerId) {
        MatchEntity match = matchJpaRepository.findById(matchId)
                .orElseThrow(() -> new NotFoundException("Match not found: " + matchId));
        if ("FINISHED".equals(match.getStatus())) {
            throw new ValidationException("Match already finished");
        }
        List<MatchPlayerEntity> players = matchPlayerJpaRepository.findByMatch_Id(matchId);
        MatchPlayerEntity creator = players.stream()
                .filter(p -> "PLAYER_ONE".equals(p.getSide()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Match has no creator"));
        if (!creator.getPlayerId().equals(playerId)) {
            throw new ValidationException("Only the creator can cancel this match");
        }
        match.setStatus("FINISHED");
        match.setFinishReason("CANCELLED");
        match.setFinishedAt(Instant.now());
        match = matchJpaRepository.save(match);
        return matchMapper.toMatchResponse(match, players);
    }

    @Transactional
    public MatchResponse concedeMatch(UUID matchId, UUID playerId) {
        MatchEntity match = matchJpaRepository.findById(matchId)
                .orElseThrow(() -> new NotFoundException("Match not found: " + matchId));
        if ("FINISHED".equals(match.getStatus())) {
            throw new ValidationException("Match already finished");
        }
        List<MatchPlayerEntity> players = matchPlayerJpaRepository.findByMatch_Id(matchId);
        boolean isParticipant = players.stream().anyMatch(p -> p.getPlayerId().equals(playerId));
        if (!isParticipant) {
            throw new ValidationException("Player is not part of this match");
        }

        UUID winnerId = players.stream()
                .filter(p -> !p.getPlayerId().equals(playerId))
                .findFirst()
                .map(MatchPlayerEntity::getPlayerId)
                .orElse(null);

        match.setStatus("FINISHED");
        match.setFinishReason("CONCEDE");
        match.setWinnerPlayerId(winnerId);
        match.setFinishedAt(Instant.now());
        matchJpaRepository.save(match);

        if (winnerId != null) {
            playerStatsService.recordMatchResult(matchId, winnerId, FinishReason.CONCEDE);
        }

        GameState state = gameEngine.loadState(matchId);
        if (state != null) {
            state.setStatus(MatchStatus.FINISHED);
            state.setWinnerPlayerId(winnerId);
            state.setFinishReason(FinishReason.CONCEDE);
            statePersisterPort.saveState(matchId, state);

            PublicGameState publicState = matchQueryService.buildPublicState(state);
            Map<String, Object> payload = new HashMap<>();
            payload.put("winnerPlayerId", winnerId != null ? winnerId.toString() : null);
            payload.put("finishReason", "CONCEDE");
            List<GameEvent> events = List.of(new GameEvent(
                    "VICTORY_DECIDED", matchId, state.getTurnNumber(), Instant.now(),
                    "El oponente abandon\u00f3 la partida", payload
            ));

            GameActionResponse actionResponse = new GameActionResponse(
                    true, null, publicState, null,
                    events.stream().map(e -> new GameEventDto(e.getType(), e.getMessage(), e.getPayload(), e.getTurnNumber())).collect(Collectors.toList()),
                    null
            );

            matchWebSocketPublisher.publishPublicState(matchId, actionResponse);

            for (MatchPlayerEntity p : players) {
                PrivatePlayerState privateState = matchQueryService.buildPrivateState(state, p.getPlayerId());
                if (privateState != null) {
                    matchWebSocketPublisher.publishPrivateState(matchId, p.getPlayerId(), privateState);
                }
            }
        }

        return matchMapper.toMatchResponse(match, players);
    }

    public MatchStateResponse getMatchState(UUID matchId, UUID playerId) {
        GameState state = gameEngine.loadState(matchId);
        if (state == null) {
            throw new NotFoundException("Match state not found: " + matchId);
        }

        var publicState = matchQueryService.buildPublicState(state);
        var privateState = matchQueryService.buildPrivateState(state, playerId);

        return new MatchStateResponse(
                matchId.toString(),
                publicState,
                privateState
        );
    }

    public void forceSuddenDeath(UUID matchId) {
        GameState state = gameEngine.loadState(matchId);
        if (state == null) {
            throw new NotFoundException("Match not found: " + matchId);
        }
        for (PlayerState player : state.getPlayers()) {
            player.setDeck(new ArrayList<>());
        }
        statePersisterPort.saveState(matchId, state);
        log.warn("[DEBUG] forceSuddenDeath: both decks emptied for match {}", matchId);
    }
}