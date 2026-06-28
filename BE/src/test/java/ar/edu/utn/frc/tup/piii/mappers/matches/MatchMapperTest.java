package ar.edu.utn.frc.tup.piii.mappers.matches;

import ar.edu.utn.frc.tup.piii.dtos.matches.MatchResponse;
import ar.edu.utn.frc.tup.piii.repositories.entities.MatchEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.MatchPlayerEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MatchMapperTest {

    private MatchMapper matchMapper;

    @BeforeEach
    void setUp() {
        matchMapper = new MatchMapper();
    }

    @Test
    void toMatchResponse_conDatosValidos_retornaMatchResponse() {
        UUID matchId = UUID.randomUUID();
        UUID currentPlayerId = UUID.randomUUID();
        UUID firstPlayerId = UUID.randomUUID();
        UUID winnerPlayerId = UUID.randomUUID();
        UUID lastResumedPlayerId = UUID.randomUUID();
        Instant now = Instant.now();

        MatchEntity entity = new MatchEntity();
        entity.setId(matchId);
        entity.setStatus("PLAYING");
        entity.setCurrentPhase("MAIN");
        entity.setTurnNumber(5);
        entity.setCurrentPlayerId(currentPlayerId);
        entity.setFirstPlayerId(firstPlayerId);
        entity.setWinnerPlayerId(winnerPlayerId);
        entity.setFinishReason("KNOCKOUT");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setLastResumedPlayerId(lastResumedPlayerId);

        UUID playerId1 = UUID.randomUUID();
        UUID playerId2 = UUID.randomUUID();
        MatchPlayerEntity player1 = createPlayerEntity(playerId1, "ACTIVE", "Ash");
        MatchPlayerEntity player2 = createPlayerEntity(playerId2, "BENCH", "Misty");
        List<MatchPlayerEntity> players = List.of(player1, player2);

        MatchResponse response = matchMapper.toMatchResponse(entity, players);

        assertEquals(matchId.toString(), response.id());
        assertEquals("PLAYING", response.status());
        assertEquals("MAIN", response.currentPhase());
        assertEquals(5, response.turnNumber());
        assertEquals(currentPlayerId.toString(), response.currentPlayerId());
        assertEquals(firstPlayerId.toString(), response.firstPlayerId());
        assertEquals(winnerPlayerId.toString(), response.winnerPlayerId());
        assertEquals("KNOCKOUT", response.finishReason());
        assertEquals(now, response.createdAt());
        assertEquals(now, response.lastSavedAt());
        assertEquals(lastResumedPlayerId.toString(), response.lastResumedPlayerId());
        assertEquals(2, response.players().size());
        assertEquals(playerId1.toString(), response.players().get(0).playerId());
        assertEquals("ACTIVE", response.players().get(0).side());
        assertEquals("Ash", response.players().get(0).displayName());
        assertEquals(playerId2.toString(), response.players().get(1).playerId());
        assertEquals("BENCH", response.players().get(1).side());
        assertEquals("Misty", response.players().get(1).displayName());
    }

    @Test
    void toMatchResponse_conCamposNull_retornaNullsEnResponse() {
        UUID matchId = UUID.randomUUID();
        Instant now = Instant.now();

        MatchEntity entity = new MatchEntity();
        entity.setId(matchId);
        entity.setStatus("WAITING");
        entity.setCurrentPhase(null);
        entity.setTurnNumber(0);
        entity.setCurrentPlayerId(null);
        entity.setFirstPlayerId(null);
        entity.setWinnerPlayerId(null);
        entity.setFinishReason(null);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setLastResumedPlayerId(null);

        MatchResponse response = matchMapper.toMatchResponse(entity, List.of());

        assertEquals(matchId.toString(), response.id());
        assertEquals("WAITING", response.status());
        assertNull(response.currentPhase());
        assertEquals(0, response.turnNumber());
        assertNull(response.currentPlayerId());
        assertNull(response.firstPlayerId());
        assertNull(response.winnerPlayerId());
        assertNull(response.finishReason());
        assertNull(response.lastResumedPlayerId());
        assertTrue(response.players().isEmpty());
    }

    @Test
    void toMatchResponse_conListaVaciaJugadores_retornaListaVacia() {
        MatchEntity entity = new MatchEntity();
        entity.setId(UUID.randomUUID());
        entity.setStatus("FINISHED");
        entity.setTurnNumber(10);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        MatchResponse response = matchMapper.toMatchResponse(entity, new ArrayList<>());

        assertNotNull(response);
        assertTrue(response.players().isEmpty());
    }

    @Test
    void toMatchResponse_unJugador_retornaListaConUnElemento() {
        UUID playerId = UUID.randomUUID();
        MatchPlayerEntity player = createPlayerEntity(playerId, "ACTIVE", "Brock");

        MatchEntity entity = new MatchEntity();
        entity.setId(UUID.randomUUID());
        entity.setStatus("WAITING");
        entity.setTurnNumber(0);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        MatchResponse response = matchMapper.toMatchResponse(entity, List.of(player));

        assertEquals(1, response.players().size());
        assertEquals("Brock", response.players().getFirst().displayName());
    }

    @Test
    void toMatchResponse_idsSeConviertenAString() {
        UUID matchId = UUID.randomUUID();
        UUID currentPlayerId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();

        MatchEntity entity = new MatchEntity();
        entity.setId(matchId);
        entity.setStatus("PLAYING");
        entity.setTurnNumber(1);
        entity.setCurrentPlayerId(currentPlayerId);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        MatchPlayerEntity player = createPlayerEntity(playerId, "ACTIVE", "Gary");

        MatchResponse response = matchMapper.toMatchResponse(entity, List.of(player));

        assertEquals(matchId.toString(), response.id());
        assertEquals(currentPlayerId.toString(), response.currentPlayerId());
        assertEquals(playerId.toString(), response.players().getFirst().playerId());
    }

    private MatchPlayerEntity createPlayerEntity(UUID playerId, String side, String displayName) {
        MatchPlayerEntity player = new MatchPlayerEntity();
        player.setPlayerId(playerId);
        player.setSide(side);
        player.setDisplayName(displayName);
        player.setPlayerKind("HUMAN");
        return player;
    }
}
