package ar.edu.utn.frc.tup.piii.websocket;

import ar.edu.utn.frc.tup.piii.dtos.matches.ChatMessage;
import ar.edu.utn.frc.tup.piii.dtos.matches.GameActionRequest;
import ar.edu.utn.frc.tup.piii.dtos.matches.GameActionResponse;
import ar.edu.utn.frc.tup.piii.dtos.matches.GameEventDto;
import ar.edu.utn.frc.tup.piii.engine.model.PrivatePlayerState;
import ar.edu.utn.frc.tup.piii.services.matches.ChatMessageCacheService;
import ar.edu.utn.frc.tup.piii.services.matches.MatchApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchWebSocketControllerTest {

    @Mock
    private MatchApplicationService matchApplicationService;
    @Mock
    private MatchWebSocketPublisher publisher;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private ChatMessageCacheService chatCache;

    private MatchWebSocketController controller;

    @BeforeEach
    void setUp() {
        controller = new MatchWebSocketController(matchApplicationService, publisher, messagingTemplate, chatCache);
    }

    @Test
    void handleMatchAction_callsServiceAndPublishes() {
        UUID matchId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID opponentId = UUID.randomUUID();
        PrivatePlayerState actingPrivate = new PrivatePlayerState();
        actingPrivate.setPlayerId(playerId);
        PrivatePlayerState opponentPrivate = new PrivatePlayerState();
        opponentPrivate.setPlayerId(opponentId);

        GameActionRequest request = new GameActionRequest("ATTACH_ENERGY", playerId.toString(), Map.of("handIndex", 0), "req-1");
        GameActionResponse response = new GameActionResponse(true, "req-1", "publicState", actingPrivate, List.of(), null);

        when(matchApplicationService.executeAction(eq(matchId), eq(request))).thenReturn(response);
        when(matchApplicationService.getPlayerIds(matchId)).thenReturn(List.of(playerId, opponentId));
        when(matchApplicationService.getPrivateState(matchId, opponentId)).thenReturn(opponentPrivate);

        controller.handleMatchAction(matchId.toString(), request);

        verify(publisher).publishPublicState(matchId, response);
        verify(publisher).publishPrivateState(matchId, playerId, actingPrivate);
        verify(publisher).publishPrivateState(matchId, opponentId, opponentPrivate);
    }

    @Test
    void handleMatchAction_publishesPrivateStateForActingPlayerAndOpponent() {
        UUID matchId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID opponentId = UUID.randomUUID();
        PrivatePlayerState actingPrivate = new PrivatePlayerState();
        actingPrivate.setPlayerId(playerId);
        PrivatePlayerState opponentPrivate = new PrivatePlayerState();
        opponentPrivate.setPlayerId(opponentId);

        GameActionRequest request = new GameActionRequest("ATTACH_ENERGY", playerId.toString(), Map.of("handIndex", 0), "req-2");
        GameActionResponse response = new GameActionResponse(true, "req-2", "publicState", actingPrivate, List.of(), null);

        when(matchApplicationService.executeAction(eq(matchId), eq(request))).thenReturn(response);
        when(matchApplicationService.getPlayerIds(matchId)).thenReturn(List.of(playerId, opponentId));
        when(matchApplicationService.getPrivateState(matchId, opponentId)).thenReturn(opponentPrivate);

        controller.handleMatchAction(matchId.toString(), request);

        verify(publisher).publishPrivateState(matchId, playerId, actingPrivate);
        verify(publisher).publishPrivateState(matchId, opponentId, opponentPrivate);
        verify(publisher, never()).publishPrivateState(matchId, playerId, opponentPrivate);
    }

    @Test
    void handleChatMessage_enrichesAndSendsToBroker() {
        UUID matchId = UUID.randomUUID();
        ChatMessage message = new ChatMessage("player-1", "Player1", "Hello!", 0L);

        controller.handleChatMessage(matchId.toString(), message);

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatCache).addMessage(eq(matchId), captor.capture());

        ChatMessage enriched = captor.getValue();
        assertEquals("player-1", enriched.senderId());
        assertEquals("Player1", enriched.senderName());
        assertEquals("Hello!", enriched.content());
        assertTrue(enriched.timestamp() > 0);

        verify(messagingTemplate).convertAndSend("/topic/matches/" + matchId + "/chat", enriched);
    }
}
