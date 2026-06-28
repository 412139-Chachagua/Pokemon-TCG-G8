package ar.edu.utn.frc.tup.piii.controllers.matches;

import ar.edu.utn.frc.tup.piii.advice.GlobalExceptionHandler;
import ar.edu.utn.frc.tup.piii.dtos.matches.*;
import ar.edu.utn.frc.tup.piii.exceptions.ConflictException;
import ar.edu.utn.frc.tup.piii.exceptions.NotFoundException;
import ar.edu.utn.frc.tup.piii.security.JwtUserDetails;
import ar.edu.utn.frc.tup.piii.services.matches.ChatMessageCacheService;
import ar.edu.utn.frc.tup.piii.services.matches.MatchApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MatchControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MatchApplicationService matchApplicationService;

    @Mock
    private ChatMessageCacheService chatCache;

    @Mock
    private Authentication authentication;

    private ObjectMapper objectMapper;
    private UUID playerId;
    private UUID matchId;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        playerId = UUID.randomUUID();
        matchId = UUID.randomUUID();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new MatchController(matchApplicationService, chatCache))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @SuppressWarnings("unchecked")
    private void mockAuth(UUID pid) {
        JwtUserDetails details = new JwtUserDetails(UUID.randomUUID(), "PLAYER", pid);
        doReturn(details).when(authentication).getPrincipal();
        doReturn((Collection) List.of(new SimpleGrantedAuthority("ROLE_PLAYER"))).when(authentication).getAuthorities();
    }

    // === listMatches ===

    @Test
    void listMatches_default_retorna200() throws Exception {
        MatchResponse match = new MatchResponse(
                matchId.toString(), "WAITING", null, null, null, null,
                null, null, List.of(), null, null, null);

        when(matchApplicationService.listAvailableMatches("WAITING")).thenReturn(List.of(match));

        mockMvc.perform(get("/api/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("WAITING"));
    }

    @Test
    void listMatches_conFiltroStatus_retorna200() throws Exception {
        when(matchApplicationService.listAvailableMatches("PLAYING")).thenReturn(List.of());

        mockMvc.perform(get("/api/matches")
                        .param("status", "PLAYING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void listMatches_sinResultados_retorna200Vacio() throws Exception {
        when(matchApplicationService.listAvailableMatches("WAITING")).thenReturn(List.of());

        mockMvc.perform(get("/api/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // === createMatch ===

    @Test
    void createMatch_dataValida_retorna201() throws Exception {
        CreateMatchRequest request = new CreateMatchRequest();
        request.setPlayer1Id("player-1");
        request.setPlayer1Name("Alice");
        request.setPlayer1DeckId("deck-1");

        MatchResponse response = new MatchResponse(
                matchId.toString(), "WAITING", null, null, null, null,
                null, null, List.of(), null, null, null);

        when(matchApplicationService.createMatch(any(CreateMatchRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(matchId.toString()));
    }

    @Test
    void createMatch_bodyInvalido_retorna400() throws Exception {
        String invalidJson = "{}";

        mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createMatch_player1NameVacio_retorna400() throws Exception {
        String invalidJson = "{\"player1Id\":\"p1\",\"player1Name\":\"\",\"player1DeckId\":\"d1\"}";

        mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    // === joinMatch ===

    @Test
    void joinMatch_dataValida_retorna200() throws Exception {
        JoinMatchRequest request = new JoinMatchRequest("Bob", "deck-2", "player-2");
        MatchResponse response = new MatchResponse(
                matchId.toString(), "PLAYING", null, null, null, null,
                null, null, List.of(), null, null, null);

        when(matchApplicationService.joinMatch(eq(matchId), any(JoinMatchRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/matches/{id}/join", matchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLAYING"));
    }

    @Test
    void joinMatch_noExiste_retorna404() throws Exception {
        JoinMatchRequest request = new JoinMatchRequest("Bob", "deck-2", "player-2");

        when(matchApplicationService.joinMatch(eq(matchId), any(JoinMatchRequest.class)))
                .thenThrow(new NotFoundException("Match not found"));

        mockMvc.perform(post("/api/matches/{id}/join", matchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void joinMatch_matchLleno_retorna409() throws Exception {
        JoinMatchRequest request = new JoinMatchRequest("Bob", "deck-2", "player-2");

        when(matchApplicationService.joinMatch(eq(matchId), any(JoinMatchRequest.class)))
                .thenThrow(new ConflictException("Match is full"));

        mockMvc.perform(post("/api/matches/{id}/join", matchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    // === getMatchState ===

    @Test
    void getMatchState_existe_retorna200() throws Exception {
        mockAuth(playerId);

        MatchStateResponse response = new MatchStateResponse(
                matchId.toString(), null, null);

        when(matchApplicationService.getMatchState(matchId, playerId)).thenReturn(response);

        mockMvc.perform(get("/api/matches/{id}/state", matchId)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value(matchId.toString()));
    }

    @Test
    void getMatchState_noExiste_retorna404() throws Exception {
        mockAuth(playerId);

        when(matchApplicationService.getMatchState(matchId, playerId))
                .thenThrow(new NotFoundException("Match not found"));

        mockMvc.perform(get("/api/matches/{id}/state", matchId)
                        .principal(authentication))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // === cancelMatch ===

    @Test
    void cancelMatch_existe_retorna200() throws Exception {
        mockAuth(playerId);

        MatchResponse response = new MatchResponse(
                matchId.toString(), "CANCELLED", null, null, null, null,
                null, null, List.of(), null, null, null);

        when(matchApplicationService.cancelMatch(matchId, playerId)).thenReturn(response);

        mockMvc.perform(delete("/api/matches/{id}", matchId)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void cancelMatch_noExiste_retorna404() throws Exception {
        mockAuth(playerId);

        when(matchApplicationService.cancelMatch(matchId, playerId))
                .thenThrow(new NotFoundException("Match not found"));

        mockMvc.perform(delete("/api/matches/{id}", matchId)
                        .principal(authentication))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // === getChatHistory ===

    @Test
    void getChatHistory_conMensajes_retorna200() throws Exception {
        ChatMessage msg = new ChatMessage("sender-1", "Alice", "Hello!", System.currentTimeMillis());

        when(chatCache.getMessages(matchId)).thenReturn(List.of(msg));

        mockMvc.perform(get("/api/matches/{id}/chat", matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Hello!"))
                .andExpect(jsonPath("$[0].senderName").value("Alice"));
    }

    @Test
    void getChatHistory_vacio_retorna200Vacio() throws Exception {
        when(chatCache.getMessages(matchId)).thenReturn(List.of());

        mockMvc.perform(get("/api/matches/{id}/chat", matchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // === concedeMatch ===

    @Test
    void concedeMatch_existe_retorna200() throws Exception {
        mockAuth(playerId);

        MatchResponse response = new MatchResponse(
                matchId.toString(), "FINISHED", null, null, null, null,
                null, "SURRENDER", List.of(), null, null, null);

        when(matchApplicationService.concedeMatch(matchId, playerId)).thenReturn(response);

        mockMvc.perform(post("/api/matches/{id}/concede", matchId)
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finishReason").value("SURRENDER"));
    }

    @Test
    void concedeMatch_noExiste_retorna404() throws Exception {
        mockAuth(playerId);

        when(matchApplicationService.concedeMatch(matchId, playerId))
                .thenThrow(new NotFoundException("Match not found"));

        mockMvc.perform(post("/api/matches/{id}/concede", matchId)
                        .principal(authentication))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // === getActiveMatches ===

    @Test
    void getActiveMatches_conAuth_retorna200() throws Exception {
        mockAuth(playerId);

        MatchResponse match = new MatchResponse(
                matchId.toString(), "PLAYING", null, null, null, null,
                null, null, List.of(), null, null, null);

        when(matchApplicationService.getActiveMatches(playerId)).thenReturn(List.of(match));

        mockMvc.perform(get("/api/matches/active")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PLAYING"));
    }

    @Test
    void getActiveMatches_sinAuth_retorna500() throws Exception {
        when(authentication.getPrincipal()).thenReturn("anonymousUser");

        mockMvc.perform(get("/api/matches/active")
                        .principal(authentication))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    @Test
    void createMatch_errorServidor_retorna500() throws Exception {
        CreateMatchRequest request = new CreateMatchRequest();
        request.setPlayer1Id("player-1");
        request.setPlayer1Name("Alice");
        request.setPlayer1DeckId("deck-1");

        when(matchApplicationService.createMatch(any(CreateMatchRequest.class)))
                .thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(post("/api/matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
    }

    @Test
    void concedeMatch_yaTerminado_retorna409() throws Exception {
        mockAuth(playerId);

        when(matchApplicationService.concedeMatch(matchId, playerId))
                .thenThrow(new ConflictException("Match already finished"));

        mockMvc.perform(post("/api/matches/{id}/concede", matchId)
                        .principal(authentication))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }
}
