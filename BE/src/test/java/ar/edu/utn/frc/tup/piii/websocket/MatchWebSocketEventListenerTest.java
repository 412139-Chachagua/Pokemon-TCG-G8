package ar.edu.utn.frc.tup.piii.websocket;

import ar.edu.utn.frc.tup.piii.repositories.entities.MatchEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.MatchJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchWebSocketEventListenerTest {

    @Mock
    private MatchWebSocketPublisher publisher;
    @Mock
    private MatchJpaRepository matchJpaRepository;

    private MatchWebSocketEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new MatchWebSocketEventListener(publisher, matchJpaRepository);
    }

    @Test
    void handleSessionSubscribe_registersSessionAndPublishesPlayerConnected() {
        UUID matchId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        String sessionId = "session-1";

        StompHeaderAccessor accessor = mock(StompHeaderAccessor.class);
        when(accessor.getSessionId()).thenReturn(sessionId);
        when(accessor.getDestination()).thenReturn("/topic/matches/" + matchId + "/player/" + playerId);

        SessionSubscribeEvent event = mock(SessionSubscribeEvent.class);
        when(event.getMessage()).thenReturn(mock(Message.class));

        when(matchJpaRepository.findById(matchId)).thenReturn(Optional.of(new MatchEntity()));

        try (var mockedStatic = mockStatic(StompHeaderAccessor.class)) {
            mockedStatic.when(() -> StompHeaderAccessor.wrap(any(Message.class))).thenReturn(accessor);
            listener.handleSessionSubscribe(event);
            verify(publisher).publishPlayerConnected(matchId, playerId, playerId.toString());
        }
    }

    @Test
    void handleSessionSubscribe_updatesLastResumedPlayerId() {
        UUID matchId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        String sessionId = "session-2";

        StompHeaderAccessor accessor = mock(StompHeaderAccessor.class);
        when(accessor.getSessionId()).thenReturn(sessionId);
        when(accessor.getDestination()).thenReturn("/topic/matches/" + matchId + "/player/" + playerId);

        SessionSubscribeEvent event = mock(SessionSubscribeEvent.class);
        when(event.getMessage()).thenReturn(mock(Message.class));

        MatchEntity matchEntity = new MatchEntity();
        when(matchJpaRepository.findById(matchId)).thenReturn(Optional.of(matchEntity));

        try (var mockedStatic = mockStatic(StompHeaderAccessor.class)) {
            mockedStatic.when(() -> StompHeaderAccessor.wrap(any(Message.class))).thenReturn(accessor);
            listener.handleSessionSubscribe(event);
            verify(matchJpaRepository).save(argThat(m -> m.getLastResumedPlayerId().equals(playerId)));
        }
    }

    @Test
    void handleSessionDisconnect_removesSessionAndPublishesDisconnect() {
        UUID matchId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        String sessionId = "session-3";

        MatchEntity matchEntity = new MatchEntity();
        matchEntity.setLastResumedPlayerId(playerId);

        StompHeaderAccessor subscribeAccessor = mock(StompHeaderAccessor.class);
        when(subscribeAccessor.getSessionId()).thenReturn(sessionId);
        when(subscribeAccessor.getDestination()).thenReturn("/topic/matches/" + matchId + "/player/" + playerId);

        SessionSubscribeEvent subscribeEvent = mock(SessionSubscribeEvent.class);
        when(subscribeEvent.getMessage()).thenReturn(mock(Message.class));
        when(matchJpaRepository.findById(matchId)).thenReturn(Optional.of(matchEntity));

        try (var mockedStatic = mockStatic(StompHeaderAccessor.class)) {
            mockedStatic.when(() -> StompHeaderAccessor.wrap(any(Message.class))).thenReturn(subscribeAccessor);
            listener.handleSessionSubscribe(subscribeEvent);
        }

        StompHeaderAccessor disconnectAccessor = mock(StompHeaderAccessor.class);
        when(disconnectAccessor.getSessionId()).thenReturn(sessionId);

        SessionDisconnectEvent disconnectEvent = mock(SessionDisconnectEvent.class);
        when(disconnectEvent.getMessage()).thenReturn(mock(Message.class));

        try (var mockedStatic = mockStatic(StompHeaderAccessor.class)) {
            mockedStatic.when(() -> StompHeaderAccessor.wrap(any(Message.class))).thenReturn(disconnectAccessor);
            listener.handleSessionDisconnect(disconnectEvent);
            verify(publisher).publishPlayerDisconnected(matchId, playerId);
        }
    }

    @Test
    void cleanStaleSessions_noSessions() {
        listener.cleanStaleSessions();
        verify(publisher, never()).publishPlayerDisconnected(any(), any());
    }

    @Test
    void updateActivity_notRegistered_doesNothing() {
        listener.updateActivity("unknown-session");
        listener.cleanStaleSessions();
        verify(publisher, never()).publishPlayerDisconnected(any(), any());
    }
}
