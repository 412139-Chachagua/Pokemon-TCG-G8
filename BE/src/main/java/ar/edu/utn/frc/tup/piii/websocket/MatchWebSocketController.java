package ar.edu.utn.frc.tup.piii.websocket;

import ar.edu.utn.frc.tup.piii.dtos.matches.ChatMessage;
import ar.edu.utn.frc.tup.piii.dtos.matches.GameActionRequest;
import ar.edu.utn.frc.tup.piii.dtos.matches.GameActionResponse;
import ar.edu.utn.frc.tup.piii.engine.model.PrivatePlayerState;
import ar.edu.utn.frc.tup.piii.services.matches.ChatMessageCacheService;
import ar.edu.utn.frc.tup.piii.services.matches.MatchApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.UUID;

@Controller
public class MatchWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(MatchWebSocketController.class);
    private final MatchApplicationService matchApplicationService;
    private final MatchWebSocketPublisher publisher;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageCacheService chatCache;

    public MatchWebSocketController(MatchApplicationService matchApplicationService,
                                    MatchWebSocketPublisher publisher,
                                    SimpMessagingTemplate messagingTemplate,
                                    ChatMessageCacheService chatCache) {
        this.matchApplicationService = matchApplicationService;
        this.publisher = publisher;
        this.messagingTemplate = messagingTemplate;
        this.chatCache = chatCache;
    }

    @MessageMapping("/matches/{matchId}/actions")
    public void handleMatchAction(
            @DestinationVariable String matchId,
            GameActionRequest request
    ) {
        log.warn("[ws] handleMatchAction: matchId={}, playerId={}, actionType={}",
                matchId, request.playerId(), request.type());
        UUID matchUuid = UUID.fromString(matchId);
        GameActionResponse response = matchApplicationService.executeAction(matchUuid, request);

        UUID actingPlayerId = UUID.fromString(request.playerId());
        publisher.publishPublicState(matchUuid, response);

        if (response.privateState() instanceof PrivatePlayerState actingPrivateState) {
            publisher.publishPrivateState(matchUuid, actingPlayerId, actingPrivateState);
        }

        for (UUID pid : matchApplicationService.getPlayerIds(matchUuid)) {
            if (!pid.equals(actingPlayerId)) {
                PrivatePlayerState otherState = matchApplicationService.getPrivateState(matchUuid, pid);
                if (otherState != null) {
                    publisher.publishPrivateState(matchUuid, pid, otherState);
                }
            }
        }
    }

    @MessageMapping("/matches/{matchId}/chat")
    public void handleChatMessage(
            @DestinationVariable String matchId,
            @Payload ChatMessage message
    ) {
        log.warn("[ws] chat: matchId={}, senderId={}, content={}",
                matchId, message.senderId(), message.content());
        UUID matchUuid = UUID.fromString(matchId);
        ChatMessage enriched = new ChatMessage(
                message.senderId(),
                message.senderName(),
                message.content(),
                Instant.now().toEpochMilli()
        );
        chatCache.addMessage(matchUuid, enriched);
        String destination = "/topic/matches/" + matchId + "/chat";
        messagingTemplate.convertAndSend(destination, enriched);
    }
}
