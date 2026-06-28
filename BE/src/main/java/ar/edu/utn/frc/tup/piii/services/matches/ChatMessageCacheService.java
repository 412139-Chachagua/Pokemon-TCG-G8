package ar.edu.utn.frc.tup.piii.services.matches;

import ar.edu.utn.frc.tup.piii.dtos.matches.ChatMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatMessageCacheService {

    private final Map<UUID, List<ChatMessage>> cache = new ConcurrentHashMap<>();

    public void addMessage(UUID matchId, ChatMessage message) {
        cache.computeIfAbsent(matchId, k -> new ArrayList<>()).add(message);
    }

    public List<ChatMessage> getMessages(UUID matchId) {
        return cache.getOrDefault(matchId, List.of());
    }

    public void clearMatch(UUID matchId) {
        cache.remove(matchId);
    }
}
