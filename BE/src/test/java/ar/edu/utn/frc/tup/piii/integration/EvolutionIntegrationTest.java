package ar.edu.utn.frc.tup.piii.integration;

import ar.edu.utn.frc.tup.piii.clients.PokemonTcgApiClient;
import ar.edu.utn.frc.tup.piii.dtos.decks.CreateDeckRequest;
import ar.edu.utn.frc.tup.piii.dtos.matches.*;
import ar.edu.utn.frc.tup.piii.engine.GameEngine;
import ar.edu.utn.frc.tup.piii.engine.MatchStatus;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
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
@Import(EvolutionIntegrationTest.MockConfig.class)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "/seed/clean.sql",
        "/seed/cards-seed-data.sql"
})
class EvolutionIntegrationTest {

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

        // P1: Charmander evolution line + Fire energy
        deck1Id = createTestDeck(player1Id, "Evolution Deck",
                List.of(
                        new CreateDeckRequest.DeckCardRequest("seed-charmander", 4),
                        new CreateDeckRequest.DeckCardRequest("seed-charmeleon", 4),
                        new CreateDeckRequest.DeckCardRequest("seed-charizard", 4),
                        new CreateDeckRequest.DeckCardRequest("seed-fire-energy", 48)
                ));
        // P2: simple Pikachu deck
        deck2Id = createTestDeck(player2Id, "Pika Deck",
                List.of(
                        new CreateDeckRequest.DeckCardRequest("seed-pikachu", 4),
                        new CreateDeckRequest.DeckCardRequest("seed-pikachu-ex", 4),
                        new CreateDeckRequest.DeckCardRequest("seed-lightning-energy", 52)
                ));
    }

    @Test
    void shouldEvolvePokemonDuringGame() {
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

        // Resolve mulligan draws
        resolveInitialMulligansAndDraws(state, matchId);

        // Both place active
        state = gameEngine.loadState(matchId);
        PlayerState p1 = findPlayer(state, player1Id);
        PlayerState p2 = findPlayer(state, player2Id);

        UUID p1BasicId = findFirstBasicInstance(p1);
        assertNotNull(p1BasicId, "P1 should have a basic Pokemon");
        GameActionResponse resp = executeAction(matchId, "SETUP_PLACE_ACTIVE", player1Id,
                Map.of("cardInstanceId", p1BasicId.toString()));
        assertTrue(resp.success(), "P1 place active: " + errorMsg(resp));

        resp = executeAction(matchId, "CONFIRM_SETUP", player1Id, Map.of());
        assertTrue(resp.success(), "P1 confirm: " + errorMsg(resp));

        UUID p2BasicId = findFirstBasicInstance(p2);
        assertNotNull(p2BasicId, "P2 should have a basic Pokemon");
        resp = executeAction(matchId, "SETUP_PLACE_ACTIVE", player2Id,
                Map.of("cardInstanceId", p2BasicId.toString()));
        assertTrue(resp.success(), "P2 place active: " + errorMsg(resp));

        resp = executeAction(matchId, "CONFIRM_SETUP", player2Id, Map.of());
        assertTrue(resp.success(), "P2 confirm: " + errorMsg(resp));

        // Play turns until an eligible player can evolve their Charmander to Charmeleon
        state = gameEngine.loadState(matchId);
        assertEquals(MatchStatus.ACTIVE, state.getStatus());

        boolean evolved = false;
        int safety = 30;
        while (!evolved && safety-- > 0) {
            state = gameEngine.loadState(matchId);
            UUID currentP = state.getCurrentPlayerId();
            PlayerState ps = findPlayer(state, currentP);

            // Draw if in DRAW phase (and not first player's first turn)
            if (state.getPhase() == TurnPhase.DRAW) {
                // Skip auto-no-draw for first player turn 1
                if (state.getFirstPlayerId().equals(currentP) && state.getTurnNumber() == 1
                        && !state.hasPlayerCompletedFirstTurn(currentP)) {
                    // First player's first turn: skip draw, go to MAIN
                } else {
                    resp = executeAction(matchId, "DRAW_CARD", currentP, Map.of());
                    assertTrue(resp.success(), "Draw: " + errorMsg(resp));
                    state = gameEngine.loadState(matchId);
                }
            }

            // If in MAIN phase
            if (state.getPhase() == TurnPhase.MAIN) {
                // Check if current player has Charmander active AND Charmeleon in hand
                PokemonInPlay activePkm = ps.getActivePokemon();
                boolean hasCharmander = activePkm != null && "seed-charmander".equals(activePkm.getCardDefinitionId());
                int evoIdx = hasCharmander ? findFirstEvoHandIndex(state, currentP) : -1;
                if (evoIdx >= 0) {
                    System.err.println("DEBUG - Trying evolve turn=" + state.getTurnNumber()
                            + " player=" + currentP
                            + " firstTurnDone=" + state.hasPlayerCompletedFirstTurn(currentP));
                    resp = executeAction(matchId, "EVOLVE_POKEMON", currentP,
                            Map.of("handIndex", evoIdx,
                                    "targetPokemonInstanceId", activePkm.getInstanceId().toString()));
                    System.err.println("DEBUG - Evolve result: " + resp.success()
                            + " err=" + (resp.error() != null ? resp.error().code() : "none"));
                    if (resp.success()) {
                        evolved = true;
                        break;
                    }
                }

                // Attach energy if possible
                int energyIdx = findFirstEnergyHandIndex(state, currentP);
                if (energyIdx >= 0) {
                    UUID activeId = getActiveInstanceId(state, currentP);
                    executeAction(matchId, "ATTACH_ENERGY", currentP,
                            Map.of("handIndex", energyIdx, "targetPokemonInstanceId", activeId.toString()));
                }

                // End turn
                resp = executeAction(matchId, "END_TURN", currentP, Map.of());
                assertTrue(resp.success(), "End turn: " + errorMsg(resp));
            }
        }

        assertTrue(evolved, "Player with Charmander should have evolved within 30 turns");

        state = gameEngine.loadState(matchId);
        UUID winnerP = findEvolvedPlayer(state);
        PokemonInPlay active = findPlayer(state, winnerP).getActivePokemon();
        assertEquals("seed-charmeleon", active.getCardDefinitionId(),
                "Active Pokemon should be Charmeleon after evolution");
        assertTrue(active.isEvolvedThisTurn(), "Evolved this turn flag should be set");
    }

    private void resolveInitialMulligansAndDraws(GameState state, UUID matchId) {
        for (UUID pid : List.of(player1Id, player2Id)) {
            while (state.hasPendingInitialMulligan(pid)) {
                executeAction(matchId, "RESOLVE_INITIAL_MULLIGAN", pid, Map.of("decision", "MULLIGAN"));
                state = gameEngine.loadState(matchId);
            }
        }
        state = gameEngine.loadState(matchId);
        if (state.isMulliganDrawPending()) {
            for (UUID pid : List.of(player1Id, player2Id)) {
                if (state.hasPendingMulliganDraw(pid)) {
                    executeAction(matchId, "RESOLVE_MULLIGAN_DRAW", pid, Map.of("drawCards", true));
                }
            }
        }
    }

    private UUID findEvolvedPlayer(GameState state) {
        for (PlayerState p : state.getPlayers()) {
            if (p.getActivePokemon() != null && "seed-charmeleon".equals(p.getActivePokemon().getCardDefinitionId())) {
                return p.getPlayerId();
            }
        }
        throw new AssertionError("No evolved player found");
    }

    private int findFirstEvoHandIndex(GameState state, UUID playerId) {
        PlayerState player = findPlayer(state, playerId);
        PokemonInPlay active = player.getActivePokemon();
        if (active == null) return -1;
        String activeCardDefId = active.getCardDefinitionId();
        for (int i = 0; i < player.getHand().size(); i++) {
            String cid = player.getHand().get(i).getCardDefinitionId();
            if ("seed-charmander".equals(activeCardDefId) && "seed-charmeleon".equals(cid)) {
                return i;
            }
            if ("seed-charmeleon".equals(activeCardDefId) && "seed-charizard".equals(cid)) {
                return i;
            }
        }
        return -1;
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

    private UUID findFirstBasicInstance(PlayerState player) {
        var basicCardIds = List.of("seed-pikachu", "seed-pikachu-ex", "seed-charmander", "seed-squirtle");
        return player.getHand().stream()
                .filter(c -> basicCardIds.contains(c.getCardDefinitionId()))
                .findFirst()
                .map(c -> c.getInstanceId())
                .orElse(null);
    }

    private UUID getActiveInstanceId(GameState state, UUID playerId) {
        return findPlayer(state, playerId).getActivePokemon().getInstanceId();
    }

    private int findFirstEnergyHandIndex(GameState state, UUID playerId) {
        PlayerState player = findPlayer(state, playerId);
        for (int i = 0; i < player.getHand().size(); i++) {
            String cid = player.getHand().get(i).getCardDefinitionId();
            if (cid != null && cid.toLowerCase().contains("energy")) return i;
        }
        return -1;
    }
}
