package ar.edu.utn.frc.tup.piii.integration;

import ar.edu.utn.frc.tup.piii.clients.PokemonTcgApiClient;
import ar.edu.utn.frc.tup.piii.dtos.decks.CreateDeckRequest;
import ar.edu.utn.frc.tup.piii.dtos.matches.*;
import ar.edu.utn.frc.tup.piii.engine.GameEngine;
import ar.edu.utn.frc.tup.piii.engine.MatchStatus;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.turn.TurnPhase;
import ar.edu.utn.frc.tup.piii.repositories.entities.*;
import ar.edu.utn.frc.tup.piii.repositories.jpa.*;
import ar.edu.utn.frc.tup.piii.services.decks.DeckService;
import ar.edu.utn.frc.tup.piii.services.matches.MatchApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(FullMatchIntegrationTest.MockConfig.class)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "/seed/clean.sql",
        "/seed/cards-seed-data.sql"
})
class FullMatchIntegrationTest {

    @TestConfiguration
    static class MockConfig {
        @Bean @Primary
        PokemonTcgApiClient pokemonTcgApiClient() {
            return Mockito.mock(PokemonTcgApiClient.class);
        }
        @Bean @Primary
        SimpMessagingTemplate simpMessagingTemplate() {
            return Mockito.mock(SimpMessagingTemplate.class);
        }
    }

    @Autowired
    private MatchApplicationService matchService;
    @Autowired
    private GameEngine gameEngine;
    @Autowired
    private PlayerJpaRepository playerRepo;
    @Autowired
    private UserJpaRepository userRepo;
    @Autowired
    private DeckService deckService;

    private UUID player1Id;
    private UUID player2Id;
    private UUID deck1Id;
    private UUID deck2Id;

    @BeforeEach
    void setUp() {
        userRepo.deleteAll();
        playerRepo.deleteAll();

        player1Id = createTestPlayer("player1@test.com", "Player One");
        player2Id = createTestPlayer("player2@test.com", "Player Two");

        // Max 4 per card to avoid MORE_THAN_4_COPIES validation issue.
        // Use 4 different basic pokemon cards (4 each = 16 total) to ensure high odds of basic in opening hand.
        deck1Id = createTestDeck(player1Id, "P1 Deck",
                List.of(
                        new CreateDeckRequest.DeckCardRequest("seed-pikachu", 4),
                        new CreateDeckRequest.DeckCardRequest("seed-pikachu-ex", 4),
                        new CreateDeckRequest.DeckCardRequest("seed-lightning-energy", 52)
                ));
        // P2 uses only Fire Pokemon (Charmander) + Fire Energy for type consistency
        deck2Id = createTestDeck(player2Id, "P2 Deck",
                List.of(
                        new CreateDeckRequest.DeckCardRequest("seed-charmander", 4),
                        new CreateDeckRequest.DeckCardRequest("seed-fire-energy", 56)
                ));
    }

    @Test
    void shouldProgressThroughFullMatchFlow() {
        CreateMatchRequest createReq = new CreateMatchRequest();
        createReq.setPlayer1Id(player1Id.toString());
        createReq.setPlayer1Name("Player One");
        createReq.setPlayer1DeckId(deck1Id.toString());
        createReq.setPlayer2Id(player2Id.toString());
        createReq.setPlayer2Name("Player Two");
        createReq.setPlayer2DeckId(deck2Id.toString());
        createReq.setQuickMatch(false);
        MatchResponse match = matchService.createMatch(createReq);
        UUID matchId = UUID.fromString(match.id());
        assertEquals("SETUP", match.status());

        GameState state = gameEngine.loadState(matchId);
        assertNotNull(state);
        assertEquals(MatchStatus.SETUP, state.getStatus());

        resolveMulliganIfNeeded(state, player1Id);
        resolveMulliganIfNeeded(state, player2Id);

        state = gameEngine.loadState(matchId);
        if (state.isMulliganDrawPending()) {
            for (UUID pid : List.of(player1Id, player2Id)) {
                if (state.hasPendingMulliganDraw(pid)) {
                    executeAction(matchId, "RESOLVE_MULLIGAN_DRAW", pid, Map.of("drawCards", true));
                }
            }
        }

        state = gameEngine.loadState(matchId);
        PlayerState p1 = findPlayer(state, player1Id);
        PlayerState p2 = findPlayer(state, player2Id);
        assertFalse(p1.getHand().isEmpty());
        assertFalse(p2.getHand().isEmpty());

        UUID p1ActiveInstanceId = findFirstBasicInstance(p1);
        assertNotNull(p1ActiveInstanceId, "P1 should have a basic Pokemon in hand: hand=" + p1.getHand().stream().map(c -> c.getCardDefinitionId()).toList());

        GameActionResponse resp = executeAction(matchId, "SETUP_PLACE_ACTIVE", player1Id,
                Map.of("cardInstanceId", p1ActiveInstanceId.toString()));
        assertTrue(resp.success(), "P1 place active: " + errorMsg(resp));

        resp = executeAction(matchId, "CONFIRM_SETUP", player1Id, Map.of());
        assertTrue(resp.success(), "P1 confirm setup: " + errorMsg(resp));

        UUID p2ActiveInstanceId = findFirstBasicInstance(p2);
        assertNotNull(p2ActiveInstanceId, "P2 should have a basic Pokemon in hand");

        resp = executeAction(matchId, "SETUP_PLACE_ACTIVE", player2Id,
                Map.of("cardInstanceId", p2ActiveInstanceId.toString()));
        assertTrue(resp.success(), "P2 place active: " + errorMsg(resp));

        resp = executeAction(matchId, "CONFIRM_SETUP", player2Id, Map.of());
        assertTrue(resp.success(), "P2 confirm setup: " + errorMsg(resp));

        state = gameEngine.loadState(matchId);
        assertEquals(MatchStatus.ACTIVE, state.getStatus());
        assertEquals(TurnPhase.MAIN, state.getPhase());
        assertEquals(1, state.getTurnNumber());
        UUID firstPlayerId = state.getFirstPlayerId();
        UUID currentP = state.getCurrentPlayerId();
        assertEquals(firstPlayerId, currentP);

        UUID firstPlayerActiveId = getActiveInstanceId(state, firstPlayerId);
        int energyHandIndexP1 = findFirstEnergyHandIndex(state, firstPlayerId);
        if (energyHandIndexP1 >= 0) {
            resp = executeAction(matchId, "ATTACH_ENERGY", firstPlayerId,
                    Map.of("handIndex", energyHandIndexP1,
                            "targetPokemonInstanceId", firstPlayerActiveId.toString()));
            assertTrue(resp.success(), "First player (" + firstPlayerId + ") attach energy: " + errorMsg(resp));
        }

        resp = executeAction(matchId, "END_TURN", firstPlayerId, Map.of());
        assertTrue(resp.success(), "P1 end turn: " + errorMsg(resp));

        state = gameEngine.loadState(matchId);
        assertEquals(2, state.getTurnNumber());
        assertEquals(TurnPhase.DRAW, state.getPhase());
        UUID secondPlayerId = state.getCurrentPlayerId();
        assertNotEquals(firstPlayerId, secondPlayerId);

        resp = executeAction(matchId, "DRAW_CARD", secondPlayerId, Map.of());
        assertTrue(resp.success(), "P2 draw: " + errorMsg(resp));

        state = gameEngine.loadState(matchId);
        assertEquals(TurnPhase.MAIN, state.getPhase());

        UUID secondPlayerActiveId = getActiveInstanceId(state, secondPlayerId);
        int energyHandIndexP2 = findFirstEnergyHandIndex(state, secondPlayerId);
        if (energyHandIndexP2 >= 0) {
            resp = executeAction(matchId, "ATTACH_ENERGY", secondPlayerId,
                    Map.of("handIndex", energyHandIndexP2,
                            "targetPokemonInstanceId", secondPlayerActiveId.toString()));
            assertTrue(resp.success(), "Second player (" + secondPlayerId + ") attach energy: " + errorMsg(resp));
        }

        UUID firstPlayerActiveIdAfter = getActiveInstanceId(state, firstPlayerId);
        resp = executeAction(matchId, "DECLARE_ATTACK", secondPlayerId,
                Map.of("attackIndex", 0, "targetPokemonInstanceId", firstPlayerActiveIdAfter.toString()));
        assertTrue(resp.success(), "Second player attack: " + errorMsg(resp));

        state = gameEngine.loadState(matchId);
        assertTrue(state.getStatus() == MatchStatus.ACTIVE || state.getStatus() == MatchStatus.FINISHED,
                "Match should be ACTIVE or FINISHED after attack, got: " + state.getStatus());
    }

    private UUID createTestPlayer(String email, String displayName) {
        UserEntity user = new UserEntity();
        user.setUsername(email);
        user.setEmail(email);
        user.setPassword("test123");
        user.setRole("PLAYER");
        user.setStatus("ACTIVE");
        user = userRepo.save(user);

        PlayerEntity player = new PlayerEntity();
        player.setUser(user);
        player.setDisplayName(displayName);
        player = playerRepo.save(player);

        user.setPlayer(player);
        userRepo.save(user);
        return player.getId();
    }

    private UUID createTestDeck(UUID playerId, String name, List<CreateDeckRequest.DeckCardRequest> cards) {
        CreateDeckRequest request = new CreateDeckRequest(name, playerId.toString(), cards);
        var response = deckService.createDeck(request);
        return UUID.fromString(response.id());
    }

    private GameActionResponse executeAction(UUID matchId, String type, UUID playerId, Map<String, Object> payload) {
        GameActionRequest req = new GameActionRequest(type, playerId.toString(), payload, UUID.randomUUID().toString());
        return matchService.executeAction(matchId, req);
    }

    private String errorMsg(GameActionResponse resp) {
        return resp.error() != null ? resp.error().code() + ": " + resp.error().message() : "";
    }

    private PlayerState findPlayer(GameState state, UUID playerId) {
        for (PlayerState p : state.getPlayers()) {
            if (p.getPlayerId().equals(playerId)) return p;
        }
        throw new AssertionError("Player not found: " + playerId);
    }

    private UUID getActiveInstanceId(GameState state, UUID playerId) {
        return findPlayer(state, playerId).getActivePokemon().getInstanceId();
    }

    private UUID findFirstBasicInstance(PlayerState player) {
        var basicCardIds = List.of("seed-pikachu", "seed-pikachu-ex", "seed-charmander", "seed-squirtle");
        return player.getHand().stream()
                .filter(c -> basicCardIds.contains(c.getCardDefinitionId()))
                .findFirst()
                .map(c -> c.getInstanceId())
                .orElse(null);
    }

    private void resolveMulliganIfNeeded(GameState state, UUID playerId) {
        UUID matchId = state.getMatchId();
        while (state.hasPendingInitialMulligan(playerId)) {
            GameActionResponse mulliganResp = executeAction(matchId, "RESOLVE_INITIAL_MULLIGAN", playerId,
                    Map.of("decision", "MULLIGAN"));
            assertTrue(mulliganResp.success(), "Mulligan loop for " + playerId + ": " + errorMsg(mulliganResp));
            state = gameEngine.loadState(matchId);
        }
        if (state.isMulliganDrawPending() && state.hasPendingMulliganDraw(playerId)) {
            executeAction(matchId, "RESOLVE_MULLIGAN_DRAW", playerId, Map.of("drawCards", true));
        }
    }

    private int findFirstEnergyHandIndex(GameState state, UUID playerId) {
        PlayerState player = findPlayer(state, playerId);
        for (int i = 0; i < player.getHand().size(); i++) {
            String cid = player.getHand().get(i).getCardDefinitionId();
            if (cid != null && cid.toLowerCase().contains("energy")) {
                return i;
            }
        }
        return -1;
    }
}
