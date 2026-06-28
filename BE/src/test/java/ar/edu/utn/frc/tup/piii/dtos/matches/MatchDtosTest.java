package ar.edu.utn.frc.tup.piii.dtos.matches;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MatchDtosTest {

    @Test
    void shouldCreateChatMessage() {
        ChatMessage msg = new ChatMessage("p1", "Ash", "Hello!", 1000L);

        assertEquals("p1", msg.senderId());
        assertEquals("Ash", msg.senderName());
        assertEquals("Hello!", msg.content());
        assertEquals(1000L, msg.timestamp());
    }

    @Test
    void shouldCreateCreateMatchRequest() {
        CreateMatchRequest request = new CreateMatchRequest();
        request.setPlayer1Id("p1");
        request.setPlayer1Name("Ash");
        request.setPlayer1DeckId("d1");
        request.setQuickMatch(true);

        assertEquals("p1", request.getPlayer1Id());
        assertEquals("Ash", request.getPlayer1Name());
        assertEquals("d1", request.getPlayer1DeckId());
        assertTrue(request.getQuickMatch());
    }

    @Test
    void shouldCreateCreateMatchRequestWithPlayer2() {
        CreateMatchRequest request = new CreateMatchRequest();
        request.setPlayer1Id("p1");
        request.setPlayer1Name("Ash");
        request.setPlayer1DeckId("d1");
        request.setPlayer2Id("p2");
        request.setPlayer2Name("Misty");
        request.setPlayer2DeckId("d2");

        assertEquals("p2", request.getPlayer2Id());
        assertEquals("Misty", request.getPlayer2Name());
        assertEquals("d2", request.getPlayer2DeckId());
    }

    @Test
    void shouldCreateCreateMatchRequestWithDefaults() {
        CreateMatchRequest request = new CreateMatchRequest();
        assertNull(request.getPlayer1Id());
        assertNull(request.getQuickMatch());
    }

    @Test
    void shouldCreateGameActionRequest() {
        GameActionRequest request = new GameActionRequest("DRAW_CARD", "p1", Map.of("count", 1), "req-1");

        assertEquals("DRAW_CARD", request.type());
        assertEquals("p1", request.playerId());
        assertEquals(1, ((Map) request.payload()).get("count"));
        assertEquals("req-1", request.clientRequestId());
    }

    @Test
    void shouldCreateGameActionResponse() {
        GameEventDto event = new GameEventDto("CARD_DRAWN", "Card drawn", Map.of("count", 1), 1);
        GameActionResponse.ErrorDto error = null;
        GameActionResponse response = new GameActionResponse(true, "req-1", "public", "private", List.of(event), error);

        assertTrue(response.success());
        assertEquals("req-1", response.clientRequestId());
        assertEquals("public", response.publicState());
        assertEquals("private", response.privateState());
        assertEquals(1, response.events().size());
        assertNull(response.error());
    }

    @Test
    void shouldCreateGameActionResponseWithError() {
        GameActionResponse.ErrorDto error = new GameActionResponse.ErrorDto("INVALID_ACTION", "Cannot do that", null);
        GameActionResponse response = new GameActionResponse(false, null, null, null, List.of(), error);

        assertFalse(response.success());
        assertNull(response.clientRequestId());
        assertEquals("INVALID_ACTION", response.error().code());
        assertEquals("Cannot do that", response.error().message());
    }

    @Test
    void shouldCreateGameEventDto() {
        GameEventDto event = new GameEventDto("CARD_DRAWN", "Card drawn", Map.of("count", 1), 1);

        assertEquals("CARD_DRAWN", event.type());
        assertEquals("Card drawn", event.message());
        assertEquals(1, event.payload().get("count"));
        assertEquals(1, event.turnNumber());
    }

    @Test
    void shouldCreateJoinMatchRequest() {
        JoinMatchRequest request = new JoinMatchRequest("Misty", "d2", "p2");

        assertEquals("Misty", request.playerName());
        assertEquals("d2", request.deckId());
        assertEquals("p2", request.playerId());
    }

    @Test
    void shouldCreateMatchResponse() {
        Instant now = Instant.now();
        var player = new MatchResponse.PlayerSummary("p1", "PLAYER_ONE", "Ash");
        MatchResponse response = new MatchResponse(
                "m1", "ACTIVE", "MAIN", 1, "p1", "p1", null, null,
                List.of(player), now, now, null
        );

        assertEquals("m1", response.id());
        assertEquals("ACTIVE", response.status());
        assertEquals("MAIN", response.currentPhase());
        assertEquals(1, response.turnNumber());
        assertEquals("p1", response.currentPlayerId());
        assertEquals(1, response.players().size());
        assertEquals("Ash", response.players().get(0).displayName());
    }

    @Test
    void shouldHandleNullMatchResponseFields() {
        MatchResponse response = new MatchResponse(
                null, null, null, null, null, null, null, null,
                null, null, null, null
        );

        assertNull(response.id());
        assertNull(response.status());
        assertNull(response.players());
    }

    @Test
    void shouldCreateMatchStateResponse() {
        MatchStateResponse response = new MatchStateResponse("m1", null, null);

        assertEquals("m1", response.matchId());
        assertNull(response.publicState());
        assertNull(response.privateState());
    }

    @Test
    void shouldCreateMatchSummaryResponse() {
        Instant now = Instant.now();
        MatchSummaryResponse response = new MatchSummaryResponse("m1", "Ash", "Misty", 10, now, 300L, "KNOCKOUT");

        assertEquals("m1", response.id());
        assertEquals("Ash", response.winnerName());
        assertEquals("Misty", response.loserName());
        assertEquals(10, response.totalTurns());
        assertEquals(300L, response.durationSeconds());
        assertEquals("KNOCKOUT", response.finishReason());
    }

    @Test
    void shouldFormatDuration() {
        MatchSummaryResponse response = new MatchSummaryResponse("m1", null, null, 0, null, 125L, null);

        assertEquals("2m 5s", response.formattedDuration());
    }

    @Test
    void shouldFormatDurationNull() {
        MatchSummaryResponse response = new MatchSummaryResponse("m1", null, null, 0, null, null, null);

        assertEquals("-", response.formattedDuration());
    }

    @Test
    void shouldFormatDate() {
        Instant instant = Instant.parse("2025-06-01T12:00:00Z");
        MatchSummaryResponse response = new MatchSummaryResponse("m1", null, null, 0, instant, null, null);

        assertEquals("01/06/25", response.formattedDate());
        assertEquals("12:00", response.formattedTime());
    }
}
