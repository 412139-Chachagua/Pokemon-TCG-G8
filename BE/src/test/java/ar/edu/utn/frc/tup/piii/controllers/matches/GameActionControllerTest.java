package ar.edu.utn.frc.tup.piii.controllers.matches;

import ar.edu.utn.frc.tup.piii.advice.GlobalExceptionHandler;
import ar.edu.utn.frc.tup.piii.dtos.matches.GameActionRequest;
import ar.edu.utn.frc.tup.piii.dtos.matches.GameActionResponse;
import ar.edu.utn.frc.tup.piii.exceptions.NotFoundException;
import ar.edu.utn.frc.tup.piii.services.matches.MatchApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GameActionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MatchApplicationService matchApplicationService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new GameActionController(matchApplicationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void executeAction_accionValida_retorna200() throws Exception {
        UUID matchId = UUID.randomUUID();
        GameActionRequest request = new GameActionRequest(
                "PLAY_CARD", "player1-id", Map.of("cardIndex", 0), "req-1");
        GameActionResponse response = new GameActionResponse(
                true, "req-1", null, null, List.of(), null);

        when(matchApplicationService.executeAction(eq(matchId), any(GameActionRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/matches/{id}/actions", matchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.clientRequestId").value("req-1"));
    }

    @Test
    void executeAction_noExiste_retorna404() throws Exception {
        UUID matchId = UUID.randomUUID();
        GameActionRequest request = new GameActionRequest(
                "PLAY_CARD", "player1-id", Map.of("cardIndex", 0), "req-1");

        when(matchApplicationService.executeAction(eq(matchId), any(GameActionRequest.class)))
                .thenThrow(new NotFoundException("Match not found"));

        mockMvc.perform(post("/api/matches/{id}/actions", matchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void executeAction_tipoInvalido_retorna400() throws Exception {
        GameActionRequest request = new GameActionRequest(
                "", "player1-id", Map.of("cardIndex", 0), "req-1");

        mockMvc.perform(post("/api/matches/{id}/actions", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void executeAction_payloadNull_retorna400() throws Exception {
        GameActionRequest request = new GameActionRequest(
                "PLAY_CARD", "player1-id", null, "req-1");

        mockMvc.perform(post("/api/matches/{id}/actions", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void executeAction_playerIdVacio_retorna400() throws Exception {
        GameActionRequest request = new GameActionRequest(
                "PLAY_CARD", "", Map.of("cardIndex", 0), "req-1");

        mockMvc.perform(post("/api/matches/{id}/actions", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
