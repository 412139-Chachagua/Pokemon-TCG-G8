package ar.edu.utn.frc.tup.piii.websocket;

import ar.edu.utn.frc.tup.piii.security.JwtTokenProvider;
import ar.edu.utn.frc.tup.piii.security.JwtUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StompInboundInterceptorTest {

    @Mock
    private MatchWebSocketEventListener eventListener;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private MessageChannel channel;

    private StompInboundInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new StompInboundInterceptor(eventListener, jwtTokenProvider);
    }

    @Test
    void connectWithValidToken_setsJwtUserInSession() {
        UUID userId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        String token = "valid-token";

        StompHeaderAccessor accessor = mock(StompHeaderAccessor.class);
        when(accessor.getSessionId()).thenReturn("session-1");
        when(accessor.getCommand()).thenReturn(StompCommand.CONNECT);
        when(accessor.getFirstNativeHeader("Authorization")).thenReturn("Bearer " + token);

        HashMap<String, Object> sessionAttrs = new HashMap<>();
        when(accessor.getSessionAttributes()).thenReturn(sessionAttrs);

        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserDetailsFromToken(token))
                .thenReturn(new JwtUserDetails(userId, "USER", playerId));

        Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();

        try (var mockedStatic = mockStatic(StompHeaderAccessor.class)) {
            mockedStatic.when(() -> StompHeaderAccessor.wrap(any(Message.class))).thenReturn(accessor);
            Message<?> result = interceptor.preSend(message, channel);

            assertNotNull(result);
            JwtUserDetails stored = (JwtUserDetails) sessionAttrs.get("jwtUser");
            assertNotNull(stored);
            assertEquals(userId, stored.userId());
            assertEquals(playerId, stored.playerId());
        }
    }

    @Test
    void connectWithInvalidToken_noUserSet() {
        String token = "invalid-token";

        StompHeaderAccessor accessor = mock(StompHeaderAccessor.class);
        when(accessor.getSessionId()).thenReturn("session-2");
        when(accessor.getCommand()).thenReturn(StompCommand.CONNECT);
        when(accessor.getFirstNativeHeader("Authorization")).thenReturn("Bearer " + token);

        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();

        try (var mockedStatic = mockStatic(StompHeaderAccessor.class)) {
            mockedStatic.when(() -> StompHeaderAccessor.wrap(any(Message.class))).thenReturn(accessor);
            Message<?> result = interceptor.preSend(message, channel);

            assertNotNull(result);
        }
    }

    @Test
    void subscribeToOwnPrivateTopic_allowed() {
        UUID playerId = UUID.randomUUID();

        StompHeaderAccessor accessor = mock(StompHeaderAccessor.class);
        when(accessor.getSessionId()).thenReturn("session-3");
        when(accessor.getCommand()).thenReturn(StompCommand.SUBSCRIBE);
        when(accessor.getDestination()).thenReturn("/topic/matches/" + UUID.randomUUID() + "/player/" + playerId);

        HashMap<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put("jwtUser", new JwtUserDetails(UUID.randomUUID(), "USER", playerId));
        when(accessor.getSessionAttributes()).thenReturn(sessionAttrs);

        Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();

        try (var mockedStatic = mockStatic(StompHeaderAccessor.class)) {
            mockedStatic.when(() -> StompHeaderAccessor.wrap(any(Message.class))).thenReturn(accessor);
            Message<?> result = interceptor.preSend(message, channel);
            assertNotNull(result);
        }
    }

    @Test
    void subscribeToOtherPrivateTopic_throwsIllegalArgumentException() {
        UUID myPlayerId = UUID.randomUUID();
        UUID otherPlayerId = UUID.randomUUID();

        StompHeaderAccessor accessor = mock(StompHeaderAccessor.class);
        when(accessor.getSessionId()).thenReturn("session-4");
        when(accessor.getCommand()).thenReturn(StompCommand.SUBSCRIBE);
        when(accessor.getDestination()).thenReturn("/topic/matches/" + UUID.randomUUID() + "/player/" + otherPlayerId);

        HashMap<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put("jwtUser", new JwtUserDetails(UUID.randomUUID(), "USER", myPlayerId));
        when(accessor.getSessionAttributes()).thenReturn(sessionAttrs);

        Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();

        try (var mockedStatic = mockStatic(StompHeaderAccessor.class)) {
            mockedStatic.when(() -> StompHeaderAccessor.wrap(any(Message.class))).thenReturn(accessor);
            assertThrows(IllegalArgumentException.class, () -> interceptor.preSend(message, channel));
        }
    }

    @Test
    void updatesActivityOnEveryMessage() {
        StompHeaderAccessor accessor = mock(StompHeaderAccessor.class);
        when(accessor.getSessionId()).thenReturn("session-5");

        Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();

        try (var mockedStatic = mockStatic(StompHeaderAccessor.class)) {
            mockedStatic.when(() -> StompHeaderAccessor.wrap(any(Message.class))).thenReturn(accessor);
            interceptor.preSend(message, channel);
            verify(eventListener).updateActivity("session-5");
        }
    }
}
