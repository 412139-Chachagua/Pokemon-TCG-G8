package ar.edu.utn.frc.tup.piii.integration;

import ar.edu.utn.frc.tup.piii.clients.PokemonTcgApiClient;
import ar.edu.utn.frc.tup.piii.dtos.decks.CreateDeckRequest;
import ar.edu.utn.frc.tup.piii.dtos.matches.*;
import ar.edu.utn.frc.tup.piii.engine.GameEngine;
import ar.edu.utn.frc.tup.piii.engine.MatchStatus;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.turn.TurnPhase;
import ar.edu.utn.frc.tup.piii.engine.victory.FinishReason;
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
@Import(VictoryIntegrationTest.MockConfig.class)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "/seed/clean.sql",
        "/seed/cards-seed-data.sql"
})
class VictoryIntegrationTest {

    @TestConfiguration
    static class MockConfig {
        @Bean @Primary
        PokemonTcgApiClient pokemonTcgApiClient() { return Mockito.mock(PokemonTcgApiClient.class); }
        @Bean @Primary
        SimpMessagingTemplate simpMessagingTemplate() { return Mockito.mock(SimpMessagingTemplate.class); }
    }

    @Autowired private MatchApplicationService matchService;
    @Autowired private GameEngine gameEngine;
    @Autowired private PlayerJpaRepository playerRepo;
    @Autowired private UserJpaRepository userRepo;
    @Autowired private DeckService deckService;

    private UUID player1Id;
    private UUID player2Id;
    private UUID deck1Id;
    private UUID deck2Id;

    @BeforeEach
    void setUp() {
        userRepo.deleteAll();
        playerRepo.deleteAll();
        player1Id = createTestPlayer("p1@test.com", "Player One");
        player2Id = createTestPlayer("p2@test.com", "Player Two");

        // P1: Pikachu-EX (50 damage, 1 Lightning) can 1-shot anything 50HP or less
        deck1Id = createTestDeck(player1Id, "P1 EX Deck",
                List.of(
                        new CreateDeckRequest.DeckCardRequest("seed-pikachu-ex", 4),
                        new CreateDeckRequest.DeckCardRequest("seed-lightning-energy", 56)
                ));
        // P2: only Charmander (50 HP) so P1 can KO it; energy type irrelevant
        deck2Id = createTestDeck(player2Id, "P2 Fragile Deck",
                List.of(
                        new CreateDeckRequest.DeckCardRequest("seed-charmander", 4),
                        new CreateDeckRequest.DeckCardRequest("seed-water-energy", 56)
                ));
    }

    @Test
    void shouldWinByKnockout() {
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
        PlayerState setupP1 = findPlayer(state, player1Id);
        PlayerState setupP2 = findPlayer(state, player2Id);

        UUID p1BasicId = findFirstBasicInstance(setupP1);
        assertNotNull(p1BasicId, "P1 should have a basic Pokemon");
        GameActionResponse resp = executeAction(matchId, "SETUP_PLACE_ACTIVE", player1Id,
                Map.of("cardInstanceId", p1BasicId.toString()));
        assertTrue(resp.success(), "P1 place active: " + errorMsg(resp));
        resp = executeAction(matchId, "CONFIRM_SETUP", player1Id, Map.of());
        assertTrue(resp.success(), "P1 confirm: " + errorMsg(resp));

        UUID p2BasicId = findFirstBasicInstance(setupP2);
        assertNotNull(p2BasicId, "P2 should have a basic Pokemon");
        resp = executeAction(matchId, "SETUP_PLACE_ACTIVE", player2Id,
                Map.of("cardInstanceId", p2BasicId.toString()));
        assertTrue(resp.success(), "P2 place active: " + errorMsg(resp));
        resp = executeAction(matchId, "CONFIRM_SETUP", player2Id, Map.of());
        assertTrue(resp.success(), "P2 confirm: " + errorMsg(resp));

        state = gameEngine.loadState(matchId);
        assertEquals(MatchStatus.ACTIVE, state.getStatus());

        // Play turns until P1 delivers the knockout attack
        boolean p1HasAttachedEnergy = false;
        int safety = 20;
        boolean attackDelivered = false;

        while (!attackDelivered && safety-- > 0) {
            state = gameEngine.loadState(matchId);
            UUID currentP = state.getCurrentPlayerId();

            if (state.getPhase() == TurnPhase.DRAW) {
                boolean skipDraw = state.getFirstPlayerId().equals(currentP)
                        && state.getTurnNumber() == 1
                        && !state.hasPlayerCompletedFirstTurn(currentP);
                if (!skipDraw) {
                    resp = executeAction(matchId, "DRAW_CARD", currentP, Map.of());
                    assertTrue(resp.success(), "Draw: " + errorMsg(resp));
                    state = gameEngine.loadState(matchId);
                }
            }

            if (state.getPhase() == TurnPhase.MAIN) {
                if (currentP.equals(player1Id)) {
                    if (!p1HasAttachedEnergy) {
                        int energyIdx = findFirstEnergyHandIndex(state, player1Id);
                        if (energyIdx >= 0) {
                            resp = executeAction(matchId, "ATTACH_ENERGY", player1Id,
                                    Map.of("handIndex", energyIdx,
                                            "targetPokemonInstanceId", getActiveInstanceId(state, player1Id).toString()));
                            assertTrue(resp.success(), "P1 attach energy: " + errorMsg(resp));
                            p1HasAttachedEnergy = true;
                        }
                    }
                    if (p1HasAttachedEnergy && state.hasPlayerCompletedFirstTurn(player1Id)) {
                        UUID opponentActiveId = getActiveInstanceId(state, player2Id);
                        resp = executeAction(matchId, "DECLARE_ATTACK", player1Id,
                                Map.of("attackIndex", 0, "targetPokemonInstanceId", opponentActiveId.toString()));
                        if (resp.success()) {
                            attackDelivered = true;
                            break;
                        }
                    }
                }
                resp = executeAction(matchId, "END_TURN", currentP, Map.of());
                assertTrue(resp.success(), "End turn: " + errorMsg(resp));
            }
        }

        assertTrue(attackDelivered, "P1 should have delivered an attack");

        state = gameEngine.loadState(matchId);
        assertEquals(MatchStatus.FINISHED, state.getStatus(), "Match should be finished");
        assertEquals(player1Id, state.getWinnerPlayerId(), "P1 should be the winner");

        boolean validFinish = state.getFinishReason() == FinishReason.KNOCKOUT
                || state.getFinishReason() == FinishReason.PRIZES;
        assertTrue(validFinish, "Finish reason should be KNOCKOUT or PRIZES, got: " + state.getFinishReason());
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
