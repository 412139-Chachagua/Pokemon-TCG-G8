package ar.edu.utn.frc.tup.piii.controllers.matches;

import ar.edu.utn.frc.tup.piii.dtos.matches.ChatMessage;
import ar.edu.utn.frc.tup.piii.dtos.matches.CreateMatchRequest;
import ar.edu.utn.frc.tup.piii.dtos.matches.JoinMatchRequest;
import ar.edu.utn.frc.tup.piii.dtos.matches.MatchResponse;
import ar.edu.utn.frc.tup.piii.dtos.matches.MatchStateResponse;
import ar.edu.utn.frc.tup.piii.security.JwtUserDetails;
import ar.edu.utn.frc.tup.piii.services.matches.ChatMessageCacheService;
import ar.edu.utn.frc.tup.piii.services.matches.MatchApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchApplicationService matchApplicationService;
    private final ChatMessageCacheService chatCache;

    public MatchController(MatchApplicationService matchApplicationService,
                           ChatMessageCacheService chatCache) {
        this.matchApplicationService = matchApplicationService;
        this.chatCache = chatCache;
    }

    @GetMapping
    public ResponseEntity<List<MatchResponse>> listMatches(
            @RequestParam(required = false, defaultValue = "WAITING") String status) {
        List<MatchResponse> matches = matchApplicationService.listAvailableMatches(status);
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/active")
    public ResponseEntity<List<MatchResponse>> getActiveMatches(Authentication authentication) {
        UUID playerId = getPlayerId(authentication);
        List<MatchResponse> matches = matchApplicationService.getActiveMatches(playerId);
        return ResponseEntity.ok(matches);
    }

    @PostMapping
    public ResponseEntity<MatchResponse> createMatch(@Valid @RequestBody CreateMatchRequest request) {
        MatchResponse response = matchApplicationService.createMatch(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<MatchResponse> joinMatch(@PathVariable UUID id,
                                                    @Valid @RequestBody JoinMatchRequest request) {
        MatchResponse response = matchApplicationService.joinMatch(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/state")
    public ResponseEntity<MatchStateResponse> getMatchState(@PathVariable UUID id,
                                                             Authentication authentication) {
        UUID playerId = getPlayerId(authentication);
        MatchStateResponse response = matchApplicationService.getMatchState(id, playerId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MatchResponse> cancelMatch(@PathVariable UUID id,
                                                       Authentication authentication) {
        UUID playerId = getPlayerId(authentication);
        MatchResponse response = matchApplicationService.cancelMatch(id, playerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/chat")
    public ResponseEntity<List<ChatMessage>> getChatHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(chatCache.getMessages(id));
    }

    @PostMapping("/{id}/concede")
    public ResponseEntity<MatchResponse> concedeMatch(@PathVariable UUID id,
                                                       Authentication authentication) {
        UUID playerId = getPlayerId(authentication);
        MatchResponse response = matchApplicationService.concedeMatch(id, playerId);
        return ResponseEntity.ok(response);
    }

    private UUID getPlayerId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof JwtUserDetails details) {
            return details.playerId();
        }
        throw new org.springframework.security.access.AccessDeniedException("Player ID not found in token");
    }
}
