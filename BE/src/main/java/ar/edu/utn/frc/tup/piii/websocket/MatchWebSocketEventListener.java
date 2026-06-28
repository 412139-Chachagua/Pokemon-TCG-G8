package ar.edu.utn.frc.tup.piii.websocket;

import ar.edu.utn.frc.tup.piii.repositories.entities.MatchEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.MatchJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MatchWebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(MatchWebSocketEventListener.class);
    private static final Pattern PRIVATE_TOPIC_PATTERN =
            Pattern.compile("/topic/matches/([0-9a-f-]+)/player/([0-9a-f-]+)");
    private static final Duration STALE_THRESHOLD = Duration.ofSeconds(30);

    private final MatchWebSocketPublisher publisher;
    private final MatchJpaRepository matchJpaRepository;

    private final Map<String, MatchPlayerSession> activeSessions = new ConcurrentHashMap<>();

    public MatchWebSocketEventListener(MatchWebSocketPublisher publisher, MatchJpaRepository matchJpaRepository) {
        this.publisher = publisher;
        this.matchJpaRepository = matchJpaRepository;
    }

    public void updateActivity(String sessionId) {
        MatchPlayerSession session = activeSessions.get(sessionId);
        if (session != null) {
            activeSessions.put(sessionId, new MatchPlayerSession(session.matchId(), session.playerId(), Instant.now()));
        }
    }

    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String destination = accessor.getDestination();

        if (destination == null || sessionId == null) return;

        Matcher matcher = PRIVATE_TOPIC_PATTERN.matcher(destination);
        if (matcher.matches()) {
            UUID matchId = UUID.fromString(matcher.group(1));
            UUID playerId = UUID.fromString(matcher.group(2));
            log.warn("[handleSessionSubscribe] Player {} subscribed to match {} private topic", playerId, matchId);

            activeSessions.put(sessionId, new MatchPlayerSession(matchId, playerId, Instant.now()));
            publisher.publishPlayerConnected(matchId, playerId, playerId.toString());

            Optional<MatchEntity> optMatch = matchJpaRepository.findById(matchId);
            optMatch.ifPresent(match -> {
                match.setLastResumedPlayerId(playerId);
                matchJpaRepository.save(match);
                log.warn("[handleSessionSubscribe] Updated lastResumedPlayerId={} for match {}", playerId, matchId);
            });
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        if (sessionId == null) return;

        MatchPlayerSession session = activeSessions.remove(sessionId);
        if (session != null) {
            handleDisconnect(session);
        }
    }

    @Scheduled(fixedRate = 15000)
    public void cleanStaleSessions() {
        Instant threshold = Instant.now().minus(STALE_THRESHOLD);
        activeSessions.entrySet().removeIf(entry -> {
            if (entry.getValue().lastActivity().isBefore(threshold)) {
                log.warn("[cleanStaleSessions] Removing stale session {} for player {} match {}",
                        entry.getKey(), entry.getValue().playerId(), entry.getValue().matchId());
                handleDisconnect(entry.getValue());
                return true;
            }
            return false;
        });
    }

    private void handleDisconnect(MatchPlayerSession session) {
        publisher.publishPlayerDisconnected(session.matchId(), session.playerId());

        Optional<MatchEntity> optMatch = matchJpaRepository.findById(session.matchId());
        optMatch.ifPresent(match -> {
            if (session.playerId().equals(match.getLastResumedPlayerId())) {
                match.setLastResumedPlayerId(null);
                matchJpaRepository.save(match);
                log.warn("[handleDisconnect] Cleared lastResumedPlayerId for match {}", session.matchId());
            }
        });
    }

    record MatchPlayerSession(UUID matchId, UUID playerId, Instant lastActivity) {}
}
