package ar.edu.utn.frc.tup.piii.websocket;

import ar.edu.utn.frc.tup.piii.dtos.matches.GameActionResponse;
import ar.edu.utn.frc.tup.piii.dtos.matches.GameEventDto;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.model.PrivatePlayerState;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchWebSocketPublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private ObjectMapper objectMapper;
    private MatchWebSocketPublisher publisher;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        publisher = new MatchWebSocketPublisher(messagingTemplate, objectMapper);
    }

    @Test
    void publishEvents_sendsToCorrectDestination() {
        UUID matchId = UUID.randomUUID();
        GameEvent event = new GameEvent("TEST_EVENT", matchId, 1, Instant.now(), "test", Map.of());
        List<GameEvent> events = List.of(event);

        publisher.publishEvents(matchId, events);

        verify(messagingTemplate).convertAndSend("/topic/matches/" + matchId + "/events", events);
    }

    @Test
    void publishPublicState_stripsPrivateState() {
        UUID matchId = UUID.randomUUID();
        GameActionResponse original = new GameActionResponse(
                true, "req-1", "publicState", "privateState", List.of(), null
        );

        publisher.publishPublicState(matchId, original);

        ArgumentCaptor<GameActionResponse> captor = ArgumentCaptor.forClass(GameActionResponse.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/matches/" + matchId + "/events"), captor.capture());

        GameActionResponse sent = captor.getValue();
        assertEquals("publicState", sent.publicState());
        assertNull(sent.privateState());
    }

    @Test
    void publishPlayerConnected_sendsPLAYER_RECONNECTEDEvent() {
        UUID matchId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        String playerName = "TestPlayer";

        publisher.publishPlayerConnected(matchId, playerId, playerName);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/matches/" + matchId + "/events"), captor.capture());

        List<GameEvent> events = captor.getValue();
        assertEquals(1, events.size());
        GameEvent event = events.get(0);
        assertEquals("PLAYER_RECONNECTED", event.getType());
        assertEquals(playerId.toString(), event.getPayload().get("playerId"));
    }

    @Test
    void publishPlayerDisconnected_sendsPLAYER_DISCONNECTEDEvent() {
        UUID matchId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();

        publisher.publishPlayerDisconnected(matchId, playerId);

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/matches/" + matchId + "/events"), captor.capture());

        List<GameEvent> events = captor.getValue();
        assertEquals(1, events.size());
        GameEvent event = events.get(0);
        assertEquals("PLAYER_DISCONNECTED", event.getType());
        assertEquals(playerId.toString(), event.getPayload().get("playerId"));
    }

    @Test
    void publishPrivateState_sendsToPlayerSpecificTopic() {
        UUID matchId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        PrivatePlayerState privateState = new PrivatePlayerState();
        privateState.setPlayerId(playerId);

        publisher.publishPrivateState(matchId, playerId, privateState);

        verify(messagingTemplate).convertAndSend(
                "/topic/matches/" + matchId + "/player/" + playerId, privateState
        );
    }
}
