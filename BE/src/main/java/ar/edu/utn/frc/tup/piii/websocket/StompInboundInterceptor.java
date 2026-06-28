package ar.edu.utn.frc.tup.piii.websocket;

import ar.edu.utn.frc.tup.piii.security.JwtTokenProvider;
import ar.edu.utn.frc.tup.piii.security.JwtUserDetails;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class StompInboundInterceptor implements ChannelInterceptor {

    private static final Pattern PRIVATE_TOPIC_PATTERN =
            Pattern.compile("^/topic/matches/[^/]+/player/([^/]+)$");

    private final MatchWebSocketEventListener eventListener;
    private final JwtTokenProvider jwtTokenProvider;

    public StompInboundInterceptor(MatchWebSocketEventListener eventListener,
                                    JwtTokenProvider jwtTokenProvider) {
        this.eventListener = eventListener;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        String sessionId = accessor.getSessionId();
        if (sessionId != null) {
            eventListener.updateActivity(sessionId);
        }

        String command = accessor.getCommand() != null ? accessor.getCommand().name() : null;

        if ("CONNECT".equals(command)) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtTokenProvider.validateToken(token)) {
                    JwtUserDetails userDetails = jwtTokenProvider.getUserDetailsFromToken(token);
                    accessor.getSessionAttributes().put("jwtUser", userDetails);
                }
            }
        }

        if ("SUBSCRIBE".equals(command)) {
            String destination = accessor.getDestination();
            if (destination != null) {
                Matcher matcher = PRIVATE_TOPIC_PATTERN.matcher(destination);
                if (matcher.matches()) {
                    String topicPlayerId = matcher.group(1);
                    JwtUserDetails userDetails = (JwtUserDetails) accessor.getSessionAttributes().get("jwtUser");
                    if (userDetails == null || !userDetails.playerId().toString().equals(topicPlayerId)) {
                        throw new IllegalArgumentException("Unauthorized subscription to private topic");
                    }
                }
            }
        }

        return message;
    }
}
