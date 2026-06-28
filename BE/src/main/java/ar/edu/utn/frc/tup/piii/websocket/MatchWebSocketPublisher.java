package ar.edu.utn.frc.tup.piii.websocket;

import ar.edu.utn.frc.tup.piii.dtos.matches.GameActionResponse;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.model.PrivatePlayerState;
import ar.edu.utn.frc.tup.piii.engine.ports.EventPublisherPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class MatchWebSocketPublisher implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(MatchWebSocketPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public MatchWebSocketPublisher(SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishEvents(UUID matchId, List<GameEvent> events) {
        String destination = "/topic/matches/" + matchId + "/events";
        logSize("publishEvents", destination, events);
        messagingTemplate.convertAndSend(destination, events);
    }

    public void publishPublicState(UUID matchId, GameActionResponse response) {
        String destination = "/topic/matches/" + matchId + "/events";
        GameActionResponse publicResponse = new GameActionResponse(
                response.success(),
                response.clientRequestId(),
                response.publicState(),
                null,
                response.events(),
                response.error()
        );
        logSize("publishPublicState", destination, publicResponse);
        messagingTemplate.convertAndSend(destination, publicResponse);
    }

    public void publishPlayerConnected(UUID matchId, UUID playerId, String playerName) {
        String destination = "/topic/matches/" + matchId + "/events";
        GameEvent event = new GameEvent(
                "PLAYER_RECONNECTED",
                matchId, 0, java.time.Instant.now(),
                "Jugador reconectado: " + playerName,
                java.util.Map.of("playerId", playerId.toString(), "playerName", playerName)
        );
        messagingTemplate.convertAndSend(destination, List.of(event));
        log.warn("[publishPlayerConnected] published for player {} in match {}", playerId, matchId);
    }

    public void publishPlayerDisconnected(UUID matchId, UUID playerId) {
        String destination = "/topic/matches/" + matchId + "/events";
        GameEvent event = new GameEvent(
                "PLAYER_DISCONNECTED",
                matchId, 0, java.time.Instant.now(),
                "Jugador se desconect\u00f3: " + playerId,
                java.util.Map.of("playerId", playerId.toString())
        );
        messagingTemplate.convertAndSend(destination, List.of(event));
        log.warn("[publishPlayerDisconnected] published for player {} in match {}", playerId, matchId);
    }

    public void publishPrivateState(UUID matchId, UUID playerId, PrivatePlayerState privateState) {
        String destination = "/topic/matches/" + matchId + "/player/" + playerId;
        logSize("publishPrivateState(" + playerId + ")", destination, privateState);
        messagingTemplate.convertAndSend(destination, privateState);
    }

    private void logSize(String method, String destination, Object payload) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(payload);
            log.warn("[{}] -> {} : {} bytes", method, destination, bytes.length);
        } catch (JsonProcessingException e) {
            log.warn("[{}] -> {} : unable to serialize", method, destination, e);
        }
    }
}
