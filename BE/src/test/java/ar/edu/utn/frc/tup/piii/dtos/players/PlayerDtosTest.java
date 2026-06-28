package ar.edu.utn.frc.tup.piii.dtos.players;

import ar.edu.utn.frc.tup.piii.dtos.ranking.RankingEntryResponse;
import ar.edu.utn.frc.tup.piii.dtos.ranking.PlayerStatsResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class PlayerDtosTest {

    @Test
    void shouldCreatePlayerResponse() {
        Instant now = Instant.now();
        PlayerResponse response = new PlayerResponse("p1", "Ash", "u1", now, "http://avatar.png");

        assertEquals("p1", response.id());
        assertEquals("Ash", response.displayName());
        assertEquals("u1", response.userId());
        assertEquals(now, response.createdAt());
        assertEquals("http://avatar.png", response.avatarUrl());
    }

    @Test
    void shouldCreatePlayerResponseWithNulls() {
        PlayerResponse response = new PlayerResponse(null, null, null, null, null);

        assertNull(response.id());
        assertNull(response.displayName());
        assertNull(response.avatarUrl());
    }

    @Test
    void shouldCreateUpdatePlayerRequest() {
        UpdatePlayerRequest request = new UpdatePlayerRequest("New Name");

        assertEquals("New Name", request.displayName());
    }

    @Test
    void shouldCreateUpdatePlayerRequestWithNull() {
        UpdatePlayerRequest request = new UpdatePlayerRequest(null);

        assertNull(request.displayName());
    }

    @Test
    void shouldCreatePlayerStatsResponse() {
        PlayerStatsResponse response = new PlayerStatsResponse("p1", "Ash", 10, 2, 5, 8);

        assertEquals("p1", response.playerId());
        assertEquals("Ash", response.displayName());
        assertEquals(10, response.totalWins());
        assertEquals(2, response.totalLosses());
        assertEquals(5, response.currentWinStreak());
        assertEquals(8, response.maxWinStreak());
    }

    @Test
    void shouldHandleZeroStats() {
        PlayerStatsResponse response = new PlayerStatsResponse("p1", "Ash", 0, 0, 0, 0);

        assertEquals(0, response.totalWins());
        assertEquals(0, response.totalLosses());
        assertEquals(0, response.currentWinStreak());
    }

    @Test
    void shouldCreateRankingEntryResponse() {
        RankingEntryResponse response = new RankingEntryResponse(1, "p1", "Ash", 10, 2, 83.3, 5, 8);

        assertEquals(1, response.rank());
        assertEquals("p1", response.playerId());
        assertEquals("Ash", response.displayName());
        assertEquals(10, response.totalWins());
        assertEquals(2, response.totalLosses());
        assertEquals(83.3, response.winRate(), 0.01);
        assertEquals(5, response.currentWinStreak());
        assertEquals(8, response.maxWinStreak());
    }

    @Test
    void shouldHandleRankingEntryWithZeroWins() {
        RankingEntryResponse response = new RankingEntryResponse(10, "p2", "Misty", 0, 5, 0.0, 0, 0);

        assertEquals(10, response.rank());
        assertEquals(0.0, response.winRate(), 0.01);
        assertEquals(0, response.currentWinStreak());
    }
}
